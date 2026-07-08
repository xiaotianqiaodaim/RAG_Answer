package org.paismart.pai_test.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoryChat {
    private  Integer userId;
    private  String chatId;

    private String topic;
    private  String userQuestion;
    private  String deepseekResponse;
}
