package com.sparta.hh99_actualproject.dto;

import com.sparta.hh99_actualproject.model.Follow;
import lombok.*;

import javax.persistence.Column;
import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class MemberResponseDto {
    private String memberId;
    private String nickname;
    private String gender;
    private String dating;
    private String age;
    private String loveType;
    private String lovePeriod;
    private String color;
    private Float reward;
    private Float score;
    private Integer follower;
    private String resTag1;
    private String resTag2;

    @AllArgsConstructor
    @Builder
    @Data
    public static class MainMemberResponseDto {
        private String nickname;
        private String color;
        private Float score;
        private String resTag1;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Data
    public static class ResTagResponseDto {
        private String resTag1;
        private String resTag2;
    }
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Data
    public static class RewardResponseDto {
        private Float reward ;
    }
}
