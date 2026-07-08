package org.paismart.pai_test.Client;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.paismart.pai_test.Config.AiProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import org.springframework.http.HttpHeaders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class DeepSeekClient {
    //WebClient,支持非阻塞的任务
    //线程发起 I/O 请求后，不阻塞等待结果，而是把等待过程交给底层事件机制。线程可以释放去处理其他任务；当 I/O 结果返回时，事件循环会通知并调度后续回调逻辑执行。


    //这个类的作用和Embedding类似，携带着检索到的

    private String apiurl;


    private String apimodel;


    private String apikey;

    private WebClient webClient;

    //AI相关配置类，类似于提示词
    private AiProperties aiProperties;

    public DeepSeekClient(AiProperties aiProperties,
                          @Value("${deepseek.api.url}") String apiurl,
                          @Value("${deepseek.api.model}") String apimodel,
                          @Value("${deepseek.api.key}") String apikey
                          ){
        this.apiurl=apiurl;
        this.apimodel=apimodel;
        this.apikey=apikey;
        this.aiProperties=aiProperties;
        WebClient.Builder builder = WebClient.builder().baseUrl(apiurl);
        //添加认证字段，携带API-KEY
        builder.defaultHeader(HttpHeaders.AUTHORIZATION,"Bearer " + apikey);
        this.webClient=builder.build();
    }



    public Map<String,Object> buildRequest(String userMessage, String context, List<Map<String, String>> history){

        Map<String,Object> request=new HashMap<>();
        request.put("model", apimodel);
        request.put("messages", buildMessages(userMessage, context, history));
        request.put("stream", true);//告诉大模型接口：不要等完整答案生成完再一次性返回，而是边生成边返回
        AiProperties.Generation gen = aiProperties.getGeneration();
        if (gen.getTemperature() != null) {
            request.put("temperature", gen.getTemperature());
        }
        if (gen.getTopP() != null) {
            request.put("top_p", gen.getTopP());
            //top_p 也是控制模型随机性的参数，全称通常叫 nucleus sampling。
            //
            //它控制模型在生成下一个词时，从多大范围的候选词里选择。
            //top_p 越小，模型选择范围越窄，回答更保守；
            //top_p 越大，模型选择范围越宽，回答更多样。
        }
        if (gen.getMaxTokens() != null) {
            request.put("max_tokens", gen.getMaxTokens());
            //max_tokens 控制模型最多生成多少 token。
            //
            //注意它控制的是 输出长度上限，不是输入长度。
            //如果太小了，没输出完就可能结束了

        }
        return request;

    }

    /**
     * 返回一个队列，其中有系统指令，和用户指令，assistant：用户的历史回答
     *参考的资料也作为系统指令了？
     * **/
    private List<Map<String, String>> buildMessages(String userMessage, String context, List<Map<String, String>> history) {
        List<Map<String, String>> messages = new ArrayList<>();

        AiProperties.Prompt promptCfg = aiProperties.getPrompt();
        //提示词

        //控制大模型“应该怎么回答问题”的提示词配置。
        //
        //在你的 RAG 系统里，用户的问题不能直接丢给大模型，而是要先拼成一个完整的 Prompt
        //系统规则 + 检索到的资料 + 用户问题

        // 1. 构建统一的 system 指令（规则 + 参考信息）
        StringBuilder sysBuilder = new StringBuilder();
        String rules = promptCfg.getRules();
        if (rules != null) {
            sysBuilder.append(rules).append("\n\n");
        }

        //给大模型标记“参考资料从哪里开始、到哪里结束”。
        String refStart = promptCfg.getRefStart() != null ? promptCfg.getRefStart() : "<<REF>>";
        String refEnd = promptCfg.getRefEnd() != null ? promptCfg.getRefEnd() : "<<END>>";
        sysBuilder.append(refStart).append("\n");

        if (context != null && !context.isEmpty()) {
            sysBuilder.append(context);
        } else {
            String noResult = promptCfg.getNoResultText() != null ? promptCfg.getNoResultText() : "（本轮无检索结果）";
            sysBuilder.append(noResult).append("\n");
        }

        sysBuilder.append(refEnd);

        //system 是 系统级指令，用来规定大模型的行为规则。
        String systemContent = sysBuilder.toString();
        messages.add(Map.of(
                "role", "system",
                "content", systemContent
        ));

        // 2. 追加历史消息（若有）
        if (history != null && !history.isEmpty()) {
            messages.addAll(history);
        }

        //user 表示 用户当前输入的问题或指令。
        // 3. 当前用户问题
        messages.add(Map.of(
                "role", "user",
                "content", userMessage
        ));
        return messages;
    }


    private void processChunk(String chunk, Consumer<String> onChunk) {
        try {
            // 检查是否是结束标记
            if ("[DONE]".equals(chunk)) {
                return;
            }

            // 直接解析 JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(chunk);
            String content = node.path("choices")
                    .path(0)
                    .path("delta")
                    .path("content")
                    .asText("");

            if (!content.isEmpty()) {
                onChunk.accept(content);
            }
        } catch (Exception e) {
        }
    }

    public void streamResponse(String userMessage,
                               String context,
                               List<Map<String, String>> history,
                               Consumer<String> onChunk,
                               Consumer<Throwable> onError) throws JsonProcessingException {

        Map<String, Object> request = buildRequest(userMessage, context, history);
        ObjectMapper mapper = new ObjectMapper();
        System.out.println("发送的完整请求体: " + mapper.writeValueAsString(request));

        webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .subscribe(
                        chunk -> processChunk(chunk, onChunk),
                        onError
                );
    }
}
