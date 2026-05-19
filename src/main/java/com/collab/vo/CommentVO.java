package com.collab.vo;

import lombok.Data;

@Data
public class CommentVO {

    private Long id;

    private String content;

    private String nickname;

    private String avatar;

    private String createTime;
}