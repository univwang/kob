package com.kob.backend.pojo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {

    private String avatar;
    private String username;
    private String time;
    private String text;
    private Boolean isMyMessage;
}
