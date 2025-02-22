package com.sparta.hh99_actualproject.repository;

import com.sparta.hh99_actualproject.model.ChatRoom;
import org.checkerframework.checker.units.qual.C;
import org.kurento.client.internal.server.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom , String> {
    //고민러가 참가신청
    //고민러 테이블이 null이며 리스너 테이블이 null이면 참가할 수 있는 방이 존재하지 않음을 의미한다.
    List<ChatRoom> findAllByReqMemberIdIsNullAndResMemberIdIsNull();

    //고민러가 참가신청
    //고민러 테이블이 null이며 리스너 테이블이 null이 아니면 고민러가 참가할 수 있는 방이 존재함을 의미한다.
    List<ChatRoom> findAllByReqMemberIdIsNullAndResMemberIdIsNotNull();

    //리스너가 참가신청
    //리스너 테이블이 null이며 고민러 테이블이 null이 아니면 리스너가 참가할 수 있는 방이 존재함을 의미한다.
    List<ChatRoom> findAllByReqMemberIdIsNotNullAndResMemberIdIsNull();

    //마이페이지 유저 채팅히스토리 조회
    List<ChatRoom> findAllByReqMemberIdOrResMemberId(String checkReq , String checkRes);

}
