package com.sparta.hh99_actualproject.service;

import com.sparta.hh99_actualproject.dto.*;
import com.sparta.hh99_actualproject.exception.PrivateException;
import com.sparta.hh99_actualproject.exception.StatusCode;
import com.sparta.hh99_actualproject.model.*;
import com.sparta.hh99_actualproject.repository.MemberRepository;
import com.sparta.hh99_actualproject.repository.SelectionRepository;
import com.sparta.hh99_actualproject.repository.VoteBoardRepository;
import com.sparta.hh99_actualproject.repository.VoteContentRepository;
import com.sparta.hh99_actualproject.service.validator.Validator;
import com.sparta.hh99_actualproject.util.SecurityUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.management.Query;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
@AllArgsConstructor
@Service
public class VoteBoardService {
    private final Validator validator;
    private final VoteBoardRepository voteBoardRepository;
    private final VoteContentRepository voteContentRepository;
    private final MemberRepository memberRepository;
    private final SelectionRepository selectionRepository;
    private final AwsS3Service awsS3Service;


    @Transactional
    public VoteBoardResponseDto createVoteBoard(VoteBoardRequestDto requestDto) {
        //null Check
        if (validator.hasNullDtoField(requestDto)){
            throw new PrivateException(StatusCode.NULL_INPUT_ERROR);
        }

        //Member 가져오기
        String memberId = SecurityUtil.getCurrentMemberId();
        Member findedMember = memberRepository.findByMemberId(memberId)
                .orElseThrow(()-> new PrivateException(StatusCode.NOT_FOUND_MEMBER));

        //MultipartFile 들을 List로 추출하기
        List<MultipartFile> multipartFileList = new ArrayList<>(2);
        if(requestDto.getImgLeftFile() != null)
            multipartFileList.add(requestDto.getImgLeftFile());

        if(requestDto.getImgRightFile() != null)
            multipartFileList.add(requestDto.getImgRightFile());

        //사진은 2개만 들어오거나 , 안들어오는 경우만 가능
        //1개 들어오면 예외처리
        if(multipartFileList.size() != 2 && !multipartFileList.isEmpty()){
            throw new PrivateException(StatusCode.WRONG_INPUT_VOTE_BOARD_IMAGE_NUM);
        }

        String imgLeftFilePath = null, imgRightFilePath = null;

        if(multipartFileList.size() == 2){
            //MultipartFile들 저장하기
            List<String> savedImgPaths = awsS3Service.uploadFiles(multipartFileList);

            if(savedImgPaths.size() == 2) {
                imgLeftFilePath = savedImgPaths.get(0);
                imgRightFilePath = savedImgPaths.get(1);
            }
        }

        //VoteBoard 제작하기
        VoteBoard savedVoteBoard = voteBoardRepository.save(VoteBoard.of(findedMember,requestDto));

        //Dto를 VoteContents Model 로 변경
        VoteContent leftVoteContent = VoteContent.builder()
                .voteBoard(savedVoteBoard)
                .imageUrl(imgLeftFilePath)
                .imageTitle(requestDto.getImgLeftTitle())
                .build();
        leftVoteContent = voteContentRepository.save(leftVoteContent);

        VoteContent rightVoteContent = VoteContent.builder()
                .voteBoard(savedVoteBoard)
                .imageUrl(imgRightFilePath)
                .imageTitle(requestDto.getImgRightTitle())
                .build();
        rightVoteContent = voteContentRepository.save(rightVoteContent);

        //VoteContents 를 만들어서 VoteBoard에 할당 변경
        List<VoteContent> voteContentList = Arrays.asList(leftVoteContent,rightVoteContent);

        savedVoteBoard.setVoteContentList(voteContentList);

        //VoteBoardResponseDto 를 만들기 위해서 List<VoteContentResponseDto>를 만들자
        List<VoteContentResponseDto> voteContentResponseDtoList = Arrays.asList(VoteContentResponseDto.of(leftVoteContent), VoteContentResponseDto.of(rightVoteContent));

        //VoteBoardResponseDto return 해주기
        return VoteBoardResponseDto.builder()
                .postId(savedVoteBoard.getVoteBoardId())
                .memberId(memberId)
                .vote(voteContentResponseDtoList)
                .createdAt(savedVoteBoard.getCreatedAt())
                .title(savedVoteBoard.getTitle())
                .contents(savedVoteBoard.getContents())
                .build();
    }

    public VoteBoardResponseDto getVoteBoard(Long postId) {
        final int LEFT_VOTE_CONTENT_NUM = 0;
        final int RIGHT_VOTE_CONTENT_NUM = 1;
        final int LEFT_VOTE_CONTENT_SELECTION_NUM = 1;
        final int RIGHT_VOTE_CONTENT_SELECTION_NUM = 2;
        //VoteBoard 가져오기
        VoteBoard findedVoteBoard = voteBoardRepository.findById(postId)
                .orElseThrow(() -> new PrivateException(StatusCode.NOT_FOUND_POST));

        //VoteContent 가져오기
        List<VoteContent> voteContentList = findedVoteBoard.getVoteContentList();
        List<VoteContentResponseDto> voteContentResponseDtoList = null;

        //leftVoteContent 가져오기
        VoteContent leftVoteContent = voteContentList.get(LEFT_VOTE_CONTENT_NUM);
        //Selection List 확인
        List<Selection> leftVoteContentSelectionList = selectionRepository.findAllByVoteBoardIdAndSelectionNum(postId, LEFT_VOTE_CONTENT_SELECTION_NUM);
        //MerberId List 가져오기
        List<String> leftVoteContentMemberIdList = getMemberIdListInVoteSelectionList(leftVoteContentSelectionList);

        //rightVoteContent 가져오기
        VoteContent rightVoteContent = voteContentList.get(RIGHT_VOTE_CONTENT_NUM);
        //Selection List 확인
        List<Selection> rightVoteContentSelectionList = selectionRepository.findAllByVoteBoardIdAndSelectionNum(postId, RIGHT_VOTE_CONTENT_SELECTION_NUM);
        //MerberId List 가져오기
        List<String> rightVoteContentMemberIdList = getMemberIdListInVoteSelectionList(rightVoteContentSelectionList);

        voteContentResponseDtoList = Arrays.asList(VoteContentResponseDto.of(leftVoteContent,leftVoteContentMemberIdList), VoteContentResponseDto.of(rightVoteContent,rightVoteContentMemberIdList));

        return VoteBoardResponseDto.builder()
                .postId(findedVoteBoard.getVoteBoardId())
                .memberId(findedVoteBoard.getMember().getMemberId())
                .vote(voteContentResponseDtoList)
                .createdAt(findedVoteBoard.getCreatedAt())
                .title(findedVoteBoard.getTitle())
                .contents(findedVoteBoard.getContents())
                .build();
    }


    public List<VoteBoardResponseDto> getTop12RankVoteBoard() {
        List<Long> top3RankVoteBoardIds= selectionRepository.findTop12VoteBoardIdOrderByTotalVoteNum();
        List<VoteBoardResponseDto> voteContentResponseDtoList = new ArrayList<>();
        for (Long voteBoardId : top3RankVoteBoardIds) {
            voteContentResponseDtoList.add(getVoteBoard(voteBoardId));
        }
        return voteContentResponseDtoList;
    }

    private List<String> getMemberIdListInVoteSelectionList(List<Selection> selectionList) {
        List<String> memberIdList = new ArrayList<>();
        for (Selection selection : selectionList) {
            memberIdList.add(selection.getMemberId());
        }

        return memberIdList;
    }

    @Transactional
    public void deleteVoteBoard(Long postId) {
        //자기 게시글이 아니면 지울수없다.
            //1.게시글 가져오기
        VoteBoard findedVoteBoard = voteBoardRepository.findById(postId)
                .orElseThrow(() -> new PrivateException(StatusCode.NOT_FOUND_POST));
            //2.자기 게시글이 맞는지 확인
        String memberId = SecurityUtil.getCurrentMemberId();
        if(!findedVoteBoard.getMember().getMemberId().equals(memberId)){
            throw new PrivateException(StatusCode.WRONG_ACCESS_POST_DELETE);
        }

        //Selection 삭제 [postId가 사라지므로 얘를 먼저 지워야함]
        selectionRepository.deleteAllByVoteBoardId(postId);
        //VoteContents를 가져와서 해당하는 이미지를 삭제해줘야함
        List<String> imgPathList = getVoteContentsImgPathList(findedVoteBoard);
        if (imgPathList.size() != 0) {
            awsS3Service.deleteAllWithImgPathList(imgPathList);
        }
        //post 삭제 시에 Contents도 같이 삭제되는지 확인 필요.
        voteBoardRepository.deleteById(postId);
    }

    private List<String> getVoteContentsImgPathList(VoteBoard findedVoteBoard) {
        List<VoteContent> voteContentList = findedVoteBoard.getVoteContentList();
        String imgLeftFilePath = voteContentList.get(0).getImageUrl();
        String imgRightFilePath = voteContentList.get(1).getImageUrl();

        List<String> imgPathList = new ArrayList<>(2);
        if(imgLeftFilePath != null)
            imgPathList.add(imgLeftFilePath);

        if(imgRightFilePath != null)
            imgPathList.add(imgRightFilePath);

        return imgPathList;
    }

    public List<BoardResponseDto.MainResponse> getAllVoteBoard() {
        List<VoteBoard> voteBoards = voteBoardRepository.findAllByOrderByCreatedAtDesc();
        List<BoardResponseDto.MainResponse> voteBoardResponse = new ArrayList<>();
        for (VoteBoard voteBoard : voteBoards) {
            BoardResponseDto.MainResponse boardDto = BoardResponseDto.MainResponse
                    .builder()
                    .boardPostId(voteBoard.getVoteBoardId())
                    .createAt(voteBoard.getCreatedAt())
                    .title(voteBoard.getTitle())
                    .build();
            voteBoardResponse.add(boardDto);
        }
        return voteBoardResponse;
    }
}
