package org.paismart.pai_test.Config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

@Component
public class EsIndexInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(EsIndexInitializer.class);

    private static final String INDEX_NAME = "knowledge_base";

    private final ElasticsearchClient esClient;

    @Value("classpath:es-mappings/knowledge_base.json")
    private Resource mappingResource;

    public EsIndexInitializer(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    @Override
    public void run(String... args) {
        int maxRetry = 5;
        long sleepMillis = 3000;

        for (int i = 1; i <= maxRetry; i++) {
            try {
                initializeIndex();
                return;
            } catch (Exception e) {
                logger.error("初始化 Elasticsearch 索引失败，第 {} 次重试，总次数 {}，原因：{}",
                        i, maxRetry, e.getMessage(), e);

                if (i == maxRetry) {
                    throw new RuntimeException("初始化 Elasticsearch 索引失败，已达到最大重试次数", e);
                }

                try {
                    Thread.sleep(sleepMillis);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("初始化 Elasticsearch 索引时线程被中断", interruptedException);
                }
            }
        }
    }

    private void initializeIndex() throws Exception {
        BooleanResponse existsResponse = esClient.indices()
                .exists(e -> e.index(INDEX_NAME));

        if (existsResponse.value()) {
            logger.info("索引 '{}' 已存在", INDEX_NAME);
            return;
        }

        createIndex();
    }

    private void createIndex() throws Exception {
        String mappingJson;

        try (InputStream inputStream = mappingResource.getInputStream()) {
            mappingJson = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        esClient.indices().create(c -> c
                .index(INDEX_NAME)
                .withJson(new StringReader(mappingJson))
        );

        logger.info("索引 '{}' 已创建", INDEX_NAME);
    }
}