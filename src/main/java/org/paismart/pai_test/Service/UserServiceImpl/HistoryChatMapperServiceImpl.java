package org.paismart.pai_test.Service.UserServiceImpl;

import org.paismart.pai_test.Mapper.HistoryChatMapper;
import org.paismart.pai_test.Service.HistoryChatMapperService;
import org.paismart.pai_test.entity.HistoryChat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class HistoryChatMapperServiceImpl implements HistoryChatMapperService {

    @Autowired
    HistoryChatMapper historyChatMapper;

    @Override
    public Integer insertOneChat(HistoryChat historyChat) {
        return historyChatMapper.insertOneChat(historyChat);
    }

    @Override
    public List<Map<String, String>> gethistList(Integer userId) {
        return historyChatMapper.gethistList(userId);
    }

    @Override
    public List<Map<String, String>> getChat(String chatId) {
        return historyChatMapper.getChatByChatID(chatId);
    }
}
