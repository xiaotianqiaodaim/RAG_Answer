package org.paismart.pai_test.Config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;

import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrap_servers;//kafka运行的地址和端口

    @Value("${spring.kafka.topic.file-processing}")
    private String file_processing;//消息的主题

    @Value("${spring.kafka.topic.dlt}")
    private String dlt;//消费失败放入的主题

    @Value("${spring.kafka.consumer.group-id}")
    private String group_id;//消费者组ID

    @Value("${spring.kafka.consumer.properties.spring.json.trusted.packages}")
    private String jsonPackages;

    public String getFile_processing(){return file_processing;}

    public String getGroup_id(){return group_id;}

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
//        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap_servers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        // 可靠投递配置
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        // 全部 ISR 落盘才确认,不只发送了消息，要等消息进入主题且备份生成了才认为确认成功
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // 幂等生产者
        config.put(ProducerConfig.RETRIES_CONFIG, 3); // 自动重试 3 次

        DefaultKafkaProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(config);
        // 设置事务前缀，启用事务能力
        factory.setTransactionIdPrefix("file-upload-tx-");
        return factory;
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {

        return new KafkaTemplate<>(producerFactory());
    }


    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
//        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // 禁用自动提交偏移量,相应的认为结束之后，由消费者提交
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap_servers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, group_id);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        config.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, jsonPackages);
        return new DefaultKafkaConsumerFactory<>(config);
    }

    // 带自动重试和死信队列的监听器工厂
    /*
    * 由 Spring Kafka 创建一个监听容器负责
    *1. 连接 Kafka
    *2. 拉取消息
    *3. 调用你的 @KafkaListener 方法
    *4. 判断消费成功还是失败
    *5. 提交 offset
    *6. 出错时执行重试或死信处理
    *
    * */

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate) {
        // 当重试失败后，消息发送至 file-processing-dlt 主题，分区与原消息保持一致
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(dlt, record.partition()));

        // 固定退避策略：每 3 秒重试一次，最多重试 4 次（加首次共 5 次）
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(3000L, 4));

        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

}
