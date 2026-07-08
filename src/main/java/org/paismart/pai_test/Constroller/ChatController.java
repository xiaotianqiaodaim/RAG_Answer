package org.paismart.pai_test.Constroller;


import org.paismart.pai_test.Service.HistoryChatMapperService;
import org.paismart.pai_test.entity.HistoryChat;
import org.paismart.pai_test.entity.User;
import org.paismart.pai_test.handler.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.Map;

@Component
@RestController
@RequestMapping("/api/chat")
public class ChatController extends TextWebSocketHandler {

    @Autowired
    HistoryChatMapperService historyChatMapperService;

    /**
     * 接受前端的对话，缓存
     * **/
    @PostMapping("/stortHistory")
    public void stortHistory(@RequestBody HistoryChat historyChat){
        historyChatMapperService.insertOneChat(historyChat);
        System.out.println("保存一次对话");
    }

    @GetMapping("/getHisttory")
    public ResponseEntity<?> getHistory_list(@Param("id" ) Integer id){
        //通过用户的id，获取ID下所有的对话

        List<Map<String, String>> list = historyChatMapperService.gethistList(id);

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "登录成功",
                "data", list
        ));
    }

    @GetMapping("/getChat")
    public ResponseEntity<?> getChat(@Param("chatId") String chatId){
        List<Map<String, String>> chat = historyChatMapperService.getChat(chatId);
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "登录成功",
                "data", Map.of("sessionID",chat)
        ));
    }





}
