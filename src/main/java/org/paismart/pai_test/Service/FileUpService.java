package org.paismart.pai_test.Service;

import io.minio.*;
import io.minio.errors.*;
import lombok.Value;
import org.paismart.pai_test.Mapper.ChunkInfoMapper;
import org.paismart.pai_test.Mapper.FileUploadMapper;
import org.paismart.pai_test.Util.ChunkMD5;
import org.paismart.pai_test.entity.ChunkInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import org.springframework.data.redis.core.RedisCallback;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class FileUpService {



    @Autowired
    private RedisTemplate<String, Object> redisTemplate;//注入Redsi

    @Autowired
    FileUploadService fileUploadService;//MiIO的上传bean

    @Autowired
    MinioClient minioClient;

    @Autowired
    ChunkInfoMapper chunkInfoMapper;

    @Autowired
    ChunkMD5 chunkMD5;
    @Autowired
    private FileUploadMapper fileUploadMapper;


    /*
    * 分片上传,功能,上传之后设置为上传的状态
    * */
    public boolean upChunkMinIO(String fileMd5, MultipartFile file, Integer chunkIndex) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        String objectName="chunks/"+fileMd5+"/"+chunkIndex;//定义文件上传的位置
        ObjectWriteResponse uploads = minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket("uploads")
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
        if(uploads!=null){
            markChunkUploaded(fileMd5,chunkIndex);
            //还要写入chunkInfo这个表
            chunkInfoMapper.add_ChunkInfo(new ChunkInfo((Integer) 1,chunkIndex,chunkMD5.calculateChunkMd5(file),fileMd5,objectName));
            return true;
        }
        else{
            return false;
        }

    }



    /*
    * key = "upload:" + fileMd5;
    * 并且将chunkIndex设置为上传的状态
    * */
    public void markChunkUploaded(String fileMd5, int chunkIndex) {
        String key = "upload:" + fileMd5;

        redisTemplate.opsForValue().setBit(key, chunkIndex, true);
    }

    /*
    * 判断某一个分片是否上传
    * */

    public boolean isChunkUploaded(String fileMd5, int chunkIndex) {
        String key = "upload:" + fileMd5;

        Boolean uploaded = redisTemplate.opsForValue().getBit(key, chunkIndex);

        return Boolean.TRUE.equals(uploaded);
    }

    //获取分片上传的数量
    public long countUploadedChunks(String fileMd5) {
        String key = "upload:" + fileMd5;

        Long count = redisTemplate.execute((RedisCallback<Long>) connection ->
                connection.stringCommands().bitCount(key.getBytes(StandardCharsets.UTF_8))
        );

        return count == null ? 0 : count;
    }
    /*
    *  // 7. 构造 MinIO compose source
        List<ComposeSource> sources = chunks.stream()
                .map(chunk -> ComposeSource.builder()
                        .bucket("uploads")
                        .object(chunk.getStoragePath())
                        .build())
                .toList();

        // 8. 调用 MinIO 合并
        minioClient.composeObject(
                ComposeObjectArgs.builder()
                        .bucket("uploads")
                        .object(mergedObjectName)
                        .sources(sources)
                        .build()
        );
    *
    * */

    //分片上传成功后的合并算法
    public boolean merge(String filename,String fileM5,Integer totalChunks) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        //能调用这个算法，就已经说名上传完了
        List<ChunkInfo> chun = chunkInfoMapper.getChun(fileM5);
        //遍历chun，查看对应的分片是否存在

        for(ChunkInfo chunkInfo:chun){
            System.out.println(chunkInfo.getStoragePath());

            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket("uploads")
                            .object(chunkInfo.getStoragePath())
                            .build()
            );//没有会抛出异常

        }

        List<ComposeSource> sources = chun.stream()
                .map(chunk -> ComposeSource.builder()
                        .bucket("uploads")
                        .object(chunk.getStoragePath())
                        .build())
                .toList();


        minioClient.composeObject(
                ComposeObjectArgs.builder()
                        .bucket("uploads")
                        .object("merge/"+filename)
                        .sources(sources)
                        .build());

        System.out.println(filename+"上传成功");
        //清理分片工作
        Integer i = chunkInfoMapper.deleteByMD5(fileM5);

        if(i!=0){
            System.out.println("清理完成");
        }

        if(fileUploadMapper.set_Status(fileM5)!=0){
            System.out.println("文件状态设置成功");
        }
        String key = "upload:" + fileM5;
        redisTemplate.delete(key);
        return true;
    }










}
