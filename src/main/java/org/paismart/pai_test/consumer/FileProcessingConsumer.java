package org.paismart.pai_test.consumer;

import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.errors.*;
import org.apache.tika.exception.TikaException;
import org.paismart.pai_test.Mapper.DocumentVectorsMapper;
import org.paismart.pai_test.Service.ElasticsearchService;
import org.paismart.pai_test.Service.ParseService;
import org.paismart.pai_test.entity.DocumentVectors;
import org.paismart.pai_test.entity.EsDocument;
import org.paismart.pai_test.entity.FileProcessingTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FileProcessingConsumer {

    @Autowired
    MinioClient minioClient;

    @Autowired
    ParseService parseService;
    @Autowired
    private DocumentVectorsMapper documentVectorsMapper;

    @Autowired
    ElasticsearchService elasticsearchService;


    /**
    *从MinIO当中下载数据，并返回流
    *
    *
    * @param  filePath 文件所在的地址
    * @return 一个对应文件的流
    * */
    public InputStream downloadFileFromStorage(String filePath) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        filePath="merge/"+filePath;
        InputStream uploads = minioClient.
                getObject(GetObjectArgs.builder().
                        bucket("uploads").
                        object(filePath).build());
        return uploads;
    }

    @KafkaListener(topics = "${spring.kafka.topic.file-processing}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void processTask(FileProcessingTask task) throws Exception {

        System.out.println("========== Kafka消费者收到任务 ==========");
        System.out.println(task.getFileName());

        InputStream inputStream = downloadFileFromStorage(task.getFilePath());

        if(!inputStream.markSupported()){
            //判断是否支持mark和reset
            inputStream = new BufferedInputStream(inputStream);
        }
        /**
         * mark()  ：在当前位置打一个标记
         * reset() ：回到刚才标记的位置重新读
         * inputStream.mark(1024);
         *
         * byte[] header = inputStream.readNBytes(20);
         *
         * // 回到 mark 的位置
         * inputStream.reset();
         * 有什么用？Apache Tika等需要读取一点流的内容判断文件的类型，之后回退到标记的位置
         * **/
        System.out.println("开始对原始文本分割，向量化得到嵌入向量");
        List<float[]> list = parseService.parseAndSave(task.getFileMd5(), inputStream, task.getUserId(), task.getOrgTag(), task.getIsPublic());
        System.out.println("test1");
        List<DocumentVectors> byMd5 = documentVectorsMapper.findByMd5(task.getFileMd5());

        System.out.println("test");
        List<EsDocument> esDocuments=new ArrayList<>();//保存到ES当中的数据
        try {
            for (int i = 0; i < list.size(); i++) {
                System.out.println(i);
                esDocuments.add(new EsDocument(
                                UUID.randomUUID().toString(),
                                task.getFileMd5(),
                                byMd5.get(i).getChunkId(),
                                byMd5.get(i).getTextContent(),
                                list.get(i),
                                "ali-em",
                                task.getUserId(),
                                task.getOrgTag(),
                                task.getIsPublic()
                        )
                );
            }
        }
        catch (Exception e) {
            System.out.println("构造 ES 文档时出错了");
            e.printStackTrace();
        }
        System.out.println("出来了");
        elasticsearchService.bulkIndex(esDocuments);
        System.out.println("保存到elasticsearch");
    }
}


