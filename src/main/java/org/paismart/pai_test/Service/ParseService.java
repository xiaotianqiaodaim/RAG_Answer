package org.paismart.pai_test.Service;

import org.apache.tika.exception.TikaException;
import org.apache.tika.sax.BodyContentHandler;
import org.paismart.pai_test.Mapper.DocumentVectorsMapper;
import org.paismart.pai_test.entity.DocumentVectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.AutoDetectParser;
import org.xml.sax.SAXException;

@Service
public class ParseService {

    @Autowired
    DocumentVectorsMapper documentVectorsMapper;
    @Autowired
    VectorizationService vectorizationService;


    private final double maxMemoryThreshold=0.8;

    public List<float[]> parseAndSave(String fileMd5, InputStream fileStream,
                             String userId, String orgTag, Boolean isPublic) throws Exception {

        //开始解析文件
        checkMemoryThreshold();
        System.out.println("对文本进行分割，并报错分片");
        String s = extractText(fileStream);
        List<String> chunks= splitTextIntoChunks(s,512);
        saveChunks(fileMd5, chunks, userId, orgTag, isPublic);//保存分片
        System.out.println("对文本进行向量化");
        List<float[]> vectorize = vectorizationService.vectorize(fileMd5);

        return vectorize;


    }

    private void saveChunks(String fileMd5, List<String> chunks,
                            String userId, String orgTag, boolean isPublic) {
        for (int i = 0; i < chunks.size(); i++) {
            var vector = new DocumentVectors();
            vector.setFileMd5(fileMd5);
            vector.setChunkId(i + 1);
            vector.setTextContent(chunks.get(i));
            vector.setUserId(userId);
            vector.setOrgTag(orgTag);
            vector.setIsPublic(isPublic);
            documentVectorsMapper.save(vector);
        }
    }

    private List<String> splitTextIntoChunks(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += chunkSize) {
            String chunk = text.substring(i, Math.min(i + chunkSize, text.length()));
            chunks.add(chunk);
        }
        return chunks;
    }

    private String extractText(InputStream inputStream) throws TikaException, IOException, SAXException {
        checkMemoryThreshold();
        BufferedInputStream bufferedInputStream=new BufferedInputStream(inputStream,262144);//256KB的缓冲区，每一次从网络流读取256KB的缓冲区
        StreamingContentHandler handler = new StreamingContentHandler();
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();
        AutoDetectParser parser = new AutoDetectParser();

        parser.parse(bufferedInputStream,handler,metadata,context);

        for (String name : metadata.names()) {
            System.out.println(name);
            System.out.println(metadata.get(name));
        }

        String content = handler.getContent();
        System.out.println("长度为:"+content.length());
        return content;
    }

    //检查内存使用内
    private void checkMemoryThreshold() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        double memoryUsage = (double) usedMemory / maxMemory;

        if (memoryUsage > maxMemoryThreshold) {

            // 重新检查
            usedMemory = runtime.totalMemory() - runtime.freeMemory();
            memoryUsage = (double) usedMemory / maxMemory;

            if (memoryUsage > maxMemoryThreshold) {
                throw new RuntimeException("内存不足，无法处理大文件。当前使用率: " +
                        String.format("%.2f%%", memoryUsage * 100));
            }
        }
    }


    private static class StreamingContentHandler extends BodyContentHandler {
        private final StringBuilder content = new StringBuilder();
        private static final int MAX_CHUNK_SIZE = 1024 * 1024; // 1MB chunks

        public StreamingContentHandler() {
            super(-1);
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            // 分块处理字符数据
            if (content.length() + length > MAX_CHUNK_SIZE) {
                // 如果添加新内容会超过阈值，先处理当前内容
                processChunk();
            }
            content.append(ch, start, length);
        }

        private void processChunk() {
            // 这里可以实现流式处理逻辑，如写入临时文件
            // 当前简化实现，保留在内存中
        }

        public String getContent() {
            return content.toString();
        }
    }
}
