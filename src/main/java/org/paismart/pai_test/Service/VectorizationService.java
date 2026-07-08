package org.paismart.pai_test.Service;

import org.paismart.pai_test.Client.EmbeddingClient;
import org.paismart.pai_test.Mapper.DocumentVectorsMapper;
import org.paismart.pai_test.entity.DocumentVectors;
import org.paismart.pai_test.entity.TextChunk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


//一个对
@Service
public class VectorizationService {

    @Autowired
    DocumentVectorsMapper documentVectorsMapper;

    @Autowired
    EmbeddingClient embeddingClient;





    //开始向量化
    public List<float[]> vectorize(String MD5) throws Exception {
        //得到某个文件所有需要向量化的文字片段
        List<DocumentVectors> byMd5 = documentVectorsMapper.findByMd5(MD5);//得到有序的

        List<TextChunk> l=new ArrayList<>();



        //调用第三方的API对其进行向量化
        for(DocumentVectors documentVectors:byMd5){
            l.add(new TextChunk(documentVectors.getVectorId(),documentVectors.getTextContent()));
        }

        List<String> l1=new ArrayList<>();
        for(TextChunk textChunk:l){
            l1.add(textChunk.getContent());
        }
        System.out.println("调用第三方的模型进行向量化");
        List<float[]> emded = embeddingClient.emded(l1);

        //完成
        return emded;

    }


}
