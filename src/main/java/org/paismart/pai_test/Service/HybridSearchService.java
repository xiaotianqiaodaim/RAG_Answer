package org.paismart.pai_test.Service;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import org.paismart.pai_test.Client.EmbeddingClient;
import org.paismart.pai_test.Mapper.FileUploadMapper;
import org.paismart.pai_test.Mapper.UsersMapper;
import org.paismart.pai_test.entity.EsDocument;
import org.paismart.pai_test.entity.FileUpload;
import org.paismart.pai_test.entity.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 混合搜索服务，结合文本匹配和向量相似度搜索
 * 支持权限过滤，确保用户只能搜索其有权限访问的文档
 */
@Service
public class HybridSearchService {

    @Autowired
    ElasticsearchClient elasticsearchClient;

    @Autowired
    EmbeddingClient embeddingClient;

    @Autowired
    OrgTagCacheService orgTagCacheService;

    @Autowired
    UsersMapper usersMapper;

    @Autowired
    FileUploadMapper fileUploadMapper;


    /**
     * 一个获得搜索结果的类，包含权限的过滤
     * @param username :搜索的用户
     * @param query :用户的查询
     * **/
    public List<SearchResult> search(String username,String query,int topK) throws Exception {
        //获取用户的有效组织
        //List<String> userEffectiveOrgTags = orgTagCacheService.getUserEffectiveOrgTags(username);

        List<String> userEffectiveOrgTags=new ArrayList<>();
        userEffectiveOrgTags.add("default");
        //通过用户名获得用户的ID,数据库当中
        //Integer id = usersMapper.getByUsername(username).getId();
        Integer id=5;

        //对查询进行处理，得到查询向量
        List<Float> query_vec=query_toVec(query);
        //开始搜索,并过滤出相应的类

        SearchResponse<EsDocument> response = elasticsearchClient.search(s -> {
            s.index("knowledge_base");
            // KNN 召回
            int recallK = topK * 30; // KNN 召回窗口
            s.knn(kn -> kn
                    .field("vector")
                    .queryVector(query_vec)
                    .k(recallK)
                    .numCandidates(recallK)
            );
            // 必须命中关键词 + 权限过滤
            s.query(q -> q.bool(b -> b
                    .must(mst -> mst.match(m -> m.field("textContent").query(query)))
                    .filter(f -> f.bool(bf -> bf
                            // 条件1: 用户可访问自己的文档
                            .should(s1 -> s1.term(t -> t.field("userId").value(id)))
                            // 条件2: 公开文档
                            .should(s2 -> s2.term(t -> t.field("public").value(true)))
                            // 条件3: 组织标签
                            .should(s3 -> {
                                if (userEffectiveOrgTags.isEmpty()) {
                                    return s3.matchNone(mn -> mn);
                                } else if (userEffectiveOrgTags.size() == 1) {
                                    return s3.term(t -> t.field("orgTag").value(userEffectiveOrgTags.get(0)));
                                } else {
                                    return s3.bool(inner -> {
                                        userEffectiveOrgTags.forEach(tag -> inner.should(sh2 -> sh2.term(t -> t.field("orgTag").value(tag))));
                                        return inner;
                                    });
                                }
                            })
                    ))
            ));

            // 第二阶段 BM25 rescore
            s.rescore(r -> r
                    .windowSize(recallK)
                    .query(rq -> rq
                            .queryWeight(0.2d)               // 保留部分 KNN 分
                            .rescoreQueryWeight(1.0d)        // BM25 主导
                            .query(rqq -> rqq.match(m -> m
                                    .field("textContent")
                                    .query(query)
                                    .operator(Operator.And)
                            ))
                    )
            );
            s.size(topK);
            return s;
        }, EsDocument.class);


        List<SearchResult> results = response.hits().hits().stream()
                .map(hit -> {
                    return new SearchResult(
                            hit.source().getFileMd5(),
                            hit.source().getChunkId(),
                            hit.source().getTextContent(),
                            hit.score(),
                            hit.source().getUserId(),
                            hit.source().getOrgTag(),
                            hit.source().isPublic()
                    );
                })
                .toList();
        attachFileNames(results);
        return results;

    }

    //只有一个查询
    public List<Float> query_toVec(String query) throws Exception {
        List<String> l=new ArrayList<>();
        l.add(query);
        List<Float> emded = convertToFloatObjectList(embeddingClient.emded(l));
        if(emded!=null && !emded.isEmpty()){
            System.out.println("文本的嵌入得到成功");
        }
        return emded;
    }
    private List<Float> convertToFloatObjectList(List<float[]> embeddings) {
        List<Float> result = new ArrayList<>();

        for (float[] embedding : embeddings) {

            for (int i = 0; i < embedding.length; i++) {
                result.add(embedding[i]); // 自动装箱 float -> Float
            }
        }

        return result;
    }

    private void attachFileNames(List<SearchResult> results) {
        if (results == null || results.isEmpty()) {
            return;
        }
        try {
            // 收集所有唯一的 fileMd5
            Set<String> md5Set = results.stream()
                    .map(SearchResult::getFileMd5)
                    .collect(Collectors.toSet());
            List<FileUpload> uploads=new ArrayList<>();
            for(String md5:md5Set){
                uploads.addAll(fileUploadMapper.get_byMD5(md5));
            }
            Map<String, String> md5ToName = uploads.stream()
                    .collect(Collectors.toMap(FileUpload::getFileMd5, FileUpload::getFileName));
            // 填充文件名
            results.forEach(r -> r.setFileName(md5ToName.get(r.getFileMd5())));
        } catch (Exception e) {
        }
    }
}
