package com.sparta.hh99_actualproject.service;

import com.sparta.hh99_actualproject.dto.MessageDto.MessageDetailResponseDto;
import com.sparta.hh99_actualproject.dto.MessageDto.MessageRequestDto;
import com.sparta.hh99_actualproject.exception.PrivateException;
import com.sparta.hh99_actualproject.exception.StatusCode;
import com.sparta.hh99_actualproject.model.Member;
import com.sparta.hh99_actualproject.model.Message;
import com.sparta.hh99_actualproject.model.NotiTypeEnum;
import com.sparta.hh99_actualproject.model.Notification;
import com.sparta.hh99_actualproject.repository.MemberRepository;
import com.sparta.hh99_actualproject.repository.MessageRepository;
import com.sparta.hh99_actualproject.service.validator.Validator;
import com.sparta.hh99_actualproject.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;
    private final MemberRepository memberRepository;
    private final Validator validator;
    private final NotificationService notificationService;
    
    public MessageDetailResponseDto getMessageDetail(Long messageId) {

        Message message = messageRepository.findById(messageId).orElseThrow(
                () -> new PrivateException(StatusCode.NOT_FOUND_MESSAGE));


        return MessageDetailResponseDto.builder()
                .reqUserNickName(message.getReqUserNickName())
                .resUserNickName(message.getResUserNickName())
                .createdAt(String.valueOf(message.getCreatedAt()))
                .message(message.getMessage())
                .build();
    }


    @Transactional
    public void sendMessage(MessageRequestDto messageRequestDto) {
        String memberId = SecurityUtil.getCurrentMemberId();

        Member reqMember = memberRepository.findByMemberId(memberId).orElseThrow(
                () -> new PrivateException(StatusCode.NOT_FOUND_MEMBER));

        Member resMember = memberRepository.findByMemberId(messageRequestDto.getResUserNickName()).orElseThrow(
                () -> new PrivateException(StatusCode.NOT_FOUND_MEMBER));


        validator.hasNullCheckMessage(messageRequestDto);

        Message message = Message.builder()
                .member(resMember) //받는 멤버의 테이블에 저장되어야하기 때에 member에는 수신자
                .reqUserId(reqMember.getMemberId())
                .resUserId(resMember.getMemberId())
                .reqUserNickName(reqMember.getNickname())
                .resUserNickName(resMember.getNickname())
                .message(messageRequestDto.getMessage())
                .build();

        //알람 기능을 위해 알람 내용을 DB에 추가 . ※메세지를 받는 사람의 알림에 떠야 하므로!
        Notification savedNotification = notificationService.saveNotification(resMember.getMemberId(),NotiTypeEnum.MESSAGE, reqMember.getNickname() , null);
        //상대방의 color 도 전달해야해서 저장
        if (savedNotification != null) {
            savedNotification.setOppositeMemberColor(reqMember.getColor());
        }

        messageRepository.save(message);
    }
}
