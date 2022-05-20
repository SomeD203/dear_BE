package com.sparta.hh99_actualproject.service;
import com.sparta.hh99_actualproject.dto.MessageDto.MessageDetailResponseDto;
import com.sparta.hh99_actualproject.dto.MessageDto.MessageRequestDto;
import com.sparta.hh99_actualproject.exception.PrivateException;
import com.sparta.hh99_actualproject.exception.StatusCode;
import com.sparta.hh99_actualproject.model.ChatRoom;
import com.sparta.hh99_actualproject.model.Member;
import com.sparta.hh99_actualproject.model.Message;
import com.sparta.hh99_actualproject.repository.ChatRoomRepository;
import com.sparta.hh99_actualproject.repository.MemberRepository;
import com.sparta.hh99_actualproject.repository.MessageRepository;
import com.sparta.hh99_actualproject.service.validator.Validator;
import com.sparta.hh99_actualproject.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;


@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;
    private final MemberRepository memberRepository;
    private final Validator validator;
    private final ChatRoomRepository chatRoomRepository;
    
    public MessageDetailResponseDto getMessageDetail(Long messageId) {

        Message message = messageRepository.findById(messageId).orElseThrow(
                () -> new PrivateException(StatusCode.NOT_FOUND_MESSAGE));

        List<ChatRoom> findMatchChatRoom = chatRoomRepository.findAllByReqNicknameAndResNicknameOrderByCreatedAtDesc(
                message.getReqUserNickName(), message.getResUserNickName());


        return MessageDetailResponseDto.builder()
                .reqUserNickName(message.getReqUserNickName())
                .resUserNickName(message.getResUserNickName())
                .createdAt(String.valueOf(message.getCreatedAt()))
                .message(message.getMessage())
                .matchTime(findMatchChatRoom.get(0).getMatchTime())
                .build();
    }

    public void sendMessage(MessageRequestDto messageRequestDto) {
        String memberId = SecurityUtil.getCurrentMemberId();

        Member reqMember = memberRepository.findByMemberId(memberId).orElseThrow(
                () -> new PrivateException(StatusCode.NOT_FOUND_MEMBER));

        Member resMember = memberRepository.findByNickname(messageRequestDto.getResUserNickName()).orElseThrow(
                () -> new PrivateException(StatusCode.NOT_FOUND_MEMBER));


        validator.hasNullCheckMessage(messageRequestDto);

        Message message = Message.builder()
                .member(resMember) //받는 멤버의 테이블에 저장되어야하기 때에 member에는 수신자
                .reqUserNickName(reqMember.getNickname())
                .resUserNickName(resMember.getNickname())
                .message(messageRequestDto.getMessage())
                .build();

        messageRepository.save(message);
    }
}
