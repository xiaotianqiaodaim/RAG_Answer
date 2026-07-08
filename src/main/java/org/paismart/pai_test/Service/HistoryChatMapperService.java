package org.paismart.pai_test.Service;

import org.paismart.pai_test.entity.HistoryChat;

import java.util.List;
import java.util.Map;

public interface HistoryChatMapperService {
    public Integer insertOneChat(HistoryChat historyChat);

    public List<Map<String,String>> gethistList(Integer userId);

    public List<Map<String,String>> getChat(String chatId);
}
