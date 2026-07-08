package org.paismart.pai_test.Client;

import com.fasterxml.jackson.core.JsonProcessingException;

import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.paismart.pai_test.entity.EsDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class EmbeddingClient {

    @Value("${embedding.api.model}")
    private String modeId;

    @Value("${embedding.api.batch-size:10}")
    private int batchsize;

    @Value("${embedding.api.dimension}")
    private int dimension;

    @Value("${embedding.api.key}")
    private String apiKey;

    @Autowired
    WebClient webClient;

    @Autowired
    ObjectMapper objectMapper;


    /**
     * @param l:传入需要向量化的文本
     * **/

    public List<float[]> emded(List<String> l) throws Exception {
        //之后，调用模型
        //batchsize个String上传一次,执行一次嵌入
        System.out.println("对文本进行处理得到嵌入");
        List<float[]> all=new ArrayList<>();
        for(int i=0;i<l.size();i=i+batchsize){
            int start=i;
            int end=Math.min(l.size(),i+batchsize);
            List<String> batch=l.subList(start,end);
            //调用嵌入的算法
            String s = callApiOnce(batch);
            all.addAll(parseVectors(s));
        }
        return all;
    }

    private String callApiOnce(List<String> batch) {
        // ✅ 1. 防御性检查：DashScope 限制单次最多 10 条
        if (batch == null || batch.isEmpty() || batch.size() > 10) {
            throw new IllegalArgumentException("批次大小必须在 1-10 之间，当前大小: " + (batch == null ? 0 : batch.size()));
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", modeId);
        requestBody.put("input", batch);

        // ✅ 2. 维度容错：如果是 v4 模型，强制纠正为 1024
        requestBody.put("dimensions", dimension);
        requestBody.put("encoding_format", "float");

        // 打印最终发出的 JSON，方便肉眼核对
        try {
        } catch (Exception e) {}

        return webClient.post()
                .uri("/embeddings")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(requestBody)
                .retrieve()
                // ✅ 3. 捕获并打印服务器返回的真实错误原因
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    System.err.println("❌ 服务器返回的详细错误: " + errorBody);
                                    return Mono.error(new RuntimeException("API Error: " + errorBody));
                                })
                )
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(30));
    }






    private List<float[]> parseVectors(String response) throws Exception {
        tools.jackson.databind.JsonNode jsonNode = objectMapper.readTree(response);
        tools.jackson.databind.JsonNode data = jsonNode.get("data");  // 兼容模式下使用data字段
        if (data == null || !data.isArray()) {
            throw new RuntimeException("API 响应格式错误: data 字段不存在或不是数组");
        }

        List<float[]> vectors = new ArrayList<>();
        for (tools.jackson.databind.JsonNode item : data) {
            JsonNode embedding = item.get("embedding");
            if (embedding != null && embedding.isArray()) {
                float[] vector = new float[embedding.size()];
                for (int i = 0; i < embedding.size(); i++) {
                    vector[i] = (float) embedding.get(i).asDouble();
                }
                vectors.add(vector);
            }
        }
        return vectors;
    }


}
