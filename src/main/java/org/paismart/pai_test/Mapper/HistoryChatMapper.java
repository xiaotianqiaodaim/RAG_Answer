package org.paismart.pai_test.Mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.paismart.pai_test.entity.HistoryChat;

import java.util.List;
import java.util.Map;

@Mapper
public interface HistoryChatMapper {
    public Integer insertOneChat(HistoryChat historyChat);

    public List<Map<String,String>> gethistList(@Param("userId") Integer userId);

    /**
     * 通过对话ID获取历史对话
     */
    public List<Map<String,String>> getChatByChatID(@Param("chatId" ) String chatId);





}
