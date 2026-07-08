package org.paismart.pai_test.Constroller;


import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.errors.*;
import org.paismart.pai_test.Config.KafaConfig;
import org.paismart.pai_test.Service.FileTypeValidationService;
import org.paismart.pai_test.Service.FileUpService;
import org.paismart.pai_test.Service.FileUploadService;
import org.paismart.pai_test.entity.FileProcessingTask;
import org.paismart.pai_test.entity.FileUpload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
@RestController
@RequestMapping("/api/v1/file")
public class FileConsreoller {
    @Autowired
    MinioClient minioClient;

    @Autowired
    FileTypeValidationService fileTypeValidationService;

    @Autowired
    FileUploadService fileUploadService;

    @Autowired
    FileUpService fileUpService;

    @Autowired
    KafkaTemplate kafkaTemplate;

    @Autowired
    KafaConfig kafaConfig;


    @PostMapping("/test")
    public ResponseEntity<?> register() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        boolean exists = minioClient.bucketExists( BucketExistsArgs.builder()
                .bucket("uploads")
                .build() );
        if(exists){
            System.out.println("uploads已经创建了");
        }
        System.out.println("访问了test");
        return ResponseEntity.ok(Map.of("code", 200, "message", "User registered successfully"));
    }


    /**
     * 验证文件类型是否支持
     *
     * @param fileMd5 文件的MD5值，用于唯一标识文件
     * @param chunkIndex 分片索引，表示当前分片的位置
     * @param totalSize 文件总大小
     * @param fileName 文件名
     * @param totalChunks 总分片数量
     * @param orgTag 组织标签，如果未指定则使用用户的主组织标签
     * @param isPublic 是否公开，默认为false
     * @param file 分片文件对象
     * @return 返回包含已上传分片和上传进度的响应
     * @throws IOException 当文件读写发生错误时抛出
     * SUPPORTED_DOCUMENT_EXTENSIONS 和UNSUPPORTED_EXTENSIONS定义了支持的扩展名和不支持的扩展名
     */

    @PostMapping("/chunk")
    public ResponseEntity<?> chunk(@RequestParam("fileMd5") String fileMd5,@RequestParam("chunkIndex") int chunkIndex, @RequestParam("totalSize") long totalSize, @RequestParam("fileName") String fileName, @RequestParam(value = "totalChunks", required = false) Integer totalChunks, @RequestParam(value = "orgTag", required = false) String orgTag, @RequestParam(value = "isPublic", required = false, defaultValue = "false") boolean isPublic, @RequestParam("file") MultipartFile file, @RequestAttribute("userId") String userId) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        //开始上传了。
        FileTypeValidationService.FileTypeValidationResult fileTypeValidationResult = fileTypeValidationService.validateFileType(fileName);
        if(!fileTypeValidationResult.isValid()){
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", HttpStatus.BAD_REQUEST.value());
            errorResponse.put("message", fileTypeValidationResult.getMessage());
            errorResponse.put("fileType",fileTypeValidationResult.getFileType());
            errorResponse.put("supportedTypes", fileTypeValidationService.getSupportedFileTypes());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        //支持的类型，开始上传，查询当前的数据库是否有对应的文件信息，根据MD5进行查询
        if(fileUploadService.if_exit(fileMd5)){
            System.out.println("数据库当中还不存在");
            if(fileUploadService.insert(new FileUpload((long)1,fileMd5,fileName,totalSize,0,userId,orgTag,isPublic, LocalDateTime.now(),null))){
                System.out.println("记录成功");
            }
        }
        //否则对应的分片信息已经存在了。
        if(!fileUpService.isChunkUploaded(fileMd5,chunkIndex)){
            System.out.println("分片还没上传");
        }

        //启动分片上传功能
        if(fileUpService.upChunkMinIO(fileMd5,file,chunkIndex)){
            System.out.println("分片上传成功");
        }
        else{
            System.out.println("分片上传失败");
        }

        //是否需要合并？
        System.out.println(totalChunks);
        System.out.println(fileUpService.countUploadedChunks(fileMd5));
        System.out.println(fileMd5);
        if (fileUpService.countUploadedChunks(fileMd5)==totalChunks){//上传成功完毕，启动合并步骤
            System.out.println("分片全部上传成功");
            //分片上传成功，开始合并
            fileUpService.merge(fileName,fileMd5,totalChunks);
            FileProcessingTask task = new FileProcessingTask(
                    fileMd5,
                    fileName,
                    fileName,
                    userId,
                    orgTag,
                    isPublic
            );
            //启动文件上传任务

            kafkaTemplate.executeInTransaction(kt -> {
                kt.send(kafaConfig.getFile_processing(), task).whenComplete((result,ex)->{
                    if(ex!=null){
                        System.out.println("消息发送失败");
                    }
                    else {
                        System.out.println("消息发送成功");
                    }
                });
                return true;
            });

        }



        return null;


    }
}
