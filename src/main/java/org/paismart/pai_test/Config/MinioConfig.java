package org.paismart.pai_test.Config;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/*
* 1. 创建 MinioClient 对象，并交给 Spring 容器管理
2. 项目启动时，检查 MinIO 中的 bucket 是否存在，不存在就创建
* */
@Configuration
public class MinioConfig {
    @Value("${minio.endpoint}")
    private String endpoint;
    @Value("${minio.accessKey}")
    private String accessKey;
    @Value("${minio.secretKey}")
    private String secretKey;
    @Value("${minio.bucketName}")
    private String bucketName;

    private MinioClient minioClient;
    @Bean
    public MinioClient minioClient() {
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build(); return this.minioClient;
    }
    @PostConstruct
    public void initBucket() {
        try {
            MinioClient client = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
            boolean exists = client.bucketExists( BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build() );
            if (!exists) {
                client.makeBucket( MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build() );
            } }
        catch (Exception e) {
            throw new RuntimeException("MinIO bucket 初始化失败：" + e.getMessage(), e);
        }
    }
}
