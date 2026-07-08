package org.paismart.pai_test.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import org.paismart.pai_test.Client.DeepSeekClient;
import org.paismart.pai_test.Mapper.HistoryChatMapper;
import org.paismart.pai_test.Service.HybridSearchService;
import org.paismart.pai_test.entity.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

//WebSocket 处理器
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    @Autowired
    HybridSearchService hybridSearchService;

    @Autowired
    DeepSeekClient deepSeekClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private HistoryChatMapper historyChatMapper;

    /**
     * 一个对searchResults当中的文本进行处理，格式化拼接为[编号] [文件名] [文本内容]的数据，方便大模型知道，这是一个参考文献的内容
     * @param searchResults:搜索的结果集
     * **/
    private String buildContent(List<SearchResult> searchResults){
        if(searchResults==null ||searchResults.isEmpty()){
            return "";
        }
        StringBuilder stringBuilder=new StringBuilder();
        for(int i=0;i<searchResults.size();i++){
            String textContent = searchResults.get(i).getTextContent();
            String filename= searchResults.get(i).getFileName();
            stringBuilder.append(String.format("[%d] (%s) %s\\n",i+1,filename,textContent));
        }
        String res=stringBuilder.toString();
        return res;
    }
    private void sendJson(WebSocketSession session, Object data) throws Exception {
        if (session != null && session.isOpen()) {
            String json = objectMapper.writeValueAsString(data);
            // WebSocketSession 不建议多线程同时 sendMessage
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        }
    }

    /**
     * 连接建立之后运行的类，经常做一些初始化的操作
     * **/
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("一个WebSocket连接建立了");
        System.out.println("----------------------------------------------------");
    }

    /**
     * 连接保持阶段，持续处理文本内容
     * **/
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        System.out.println("后端收到消息，开始处理消息");
        String payload = message.getPayload();
        Map<String,String> map1=objectMapper.readValue(
                payload,
                new TypeReference<Map<String, String>>() {}
        );
        List<SearchResult> search = hybridSearchService.search("tlk",map1.get("content"),30);
        String content = buildContent(search);
        List<Map<String,String>> list=new ArrayList<>();
        Map<String,String> map =new HashMap<>();
        map.put("test","test");
        String chatId=map1.get("chatId");
        System.out.println("从数据库当中得到历史对话作为historty");
        List<Map<String, String>> chatByChatID = historyChatMapper.getChatByChatID(chatId);
        List<Map<String,String>> list1=new ArrayList<>();
        for(Map<String,String> map2:chatByChatID){
            Map<String,String> map_1=new HashMap<>();
            Map<String,String> map_2=new HashMap<>();
            map_1.put("role","user");
            map_1.put("content",map2.get("user_question"));
            map_2.put("role","assistant");
            map_2.put("content",map2.get("deepseek_response"));
            list1.add(map_1);
            list1.add(map_2);

        }
        System.out.println(list1);



        deepSeekClient.streamResponse(
                map1.get("content"),
                content,
                list1,
                chunk -> {
                    try {
                        sendJson(session, Map.of(
                                "type", "delta",
                                "content", chunk,
                                "chatId",chatId
                        ));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                },
                error -> {
                    try {
                        sendJson(session, Map.of(
                                "type", "error",
                                "message", error.getMessage(),
                                "chatId",chatId
                        ));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
        );

        System.out.println("----------------------消息处理完成------------------------------");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("连接关闭");
        System.out.println("----------------------------------------------------");
    }
}