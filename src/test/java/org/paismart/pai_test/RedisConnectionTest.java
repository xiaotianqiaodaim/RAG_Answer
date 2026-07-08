package org.paismart.pai_test;


import io.minio.errors.*;
import org.junit.jupiter.api.Test;
import org.paismart.pai_test.Client.EmbeddingClient;
import org.paismart.pai_test.Mapper.*;
import org.paismart.pai_test.Service.HybridSearchService;
import org.paismart.pai_test.Util.JWTUtil;
import org.paismart.pai_test.consumer.FileProcessingConsumer;
import org.paismart.pai_test.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class RedisConnectionTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;//注入Redsi

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private UsersMapper usersMapper;

    @Autowired
    private OrganizationTagsMapper organizationTagsMapper;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private ChunkInfoMapper chunkInfoMapper;

    @Autowired
    FileUploadMapper fileUploadMapper;

    @Autowired
    FileProcessingConsumer fileProcessingConsumer;

    @Autowired
    EmbeddingClient embeddingClient;

    @Autowired
    DocumentVectorsMapper documentVectorsMapper;

    @Autowired
    HybridSearchService hybridSearchService;

    private String query="信用卡";

    @Autowired
    HistoryChatMapper historyChatMapper;



    //经测试，搜索服务正常
    @Test
    public void test_h() throws Exception {
        List<Map<String, String>> list = historyChatMapper.gethistList(5);
        System.out.println(list.isEmpty());


    }


    @Test
    public void test_o(){
        organizationTagsMapper.findById("AI");
    }

    @Test
    public void test_em() throws Exception {
        String MD5="c9eacecb5b03536210dc77c368013896";
        List<DocumentVectors> byMd5 = documentVectorsMapper.findByMd5(MD5);

        List<String> l=new ArrayList<>();
        for(DocumentVectors documentVectors:byMd5){
            l.add(documentVectors.getTextContent());
        }
        List<float[]> emded = embeddingClient.emded(l.subList(0,2));
        System.out.println("提取的嵌入数量为：");
        for(float[] f:emded){
            System.out.println(f[0]);
        }
        System.out.println(emded.toString());
        System.out.println(emded.size());


    }

    @Test
    public void testRedisConnection() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        InputStream inputStream = fileProcessingConsumer.downloadFileFromStorage("bj_poi_wgs84_merged_good_only.csv");
        if(inputStream!=null){
            System.out.println("读取成功");
        }
    }

    //1.启动redis(容器已经创建了，运行就行了，还有一个创建并运行的命令，之前给搞混了)
    // docker start -i myredis
    //2.启动kafka
    //  先启动zookeeper
    //  docker start -i zookeeper
    //  启动kafka
    //

    @Test
    public void testUser(){
        Integer user = usersMapper.register(new User(1, "admin", "admin123456", "admin", "default", "default", LocalDateTime.now(), LocalDateTime.now()));

        if(user!=0){
            System.out.println("添加成功");
        }
    }

    @Test
    public void testOrgan(){
        Integer i=organizationTagsMapper.createOrganization(new OrganizationTags("AL",LocalDateTime.now(),"AI组织，存放AI有关的文档","AI","",LocalDateTime.now(),3));

        if(i!=0){
            System.out.println("加入成功");
        }
    }

    @Test
    public void testJwt(){
        String s="2ae6d3cb567d825561ac2b6200bf1a6e";
        String key = "upload:" + s;
        redisTemplate.delete(key);
    }

}