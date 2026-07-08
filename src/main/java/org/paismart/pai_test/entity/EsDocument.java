package org.paismart.pai_test.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EsDocument {
    private String id;             // 文档唯一标识
    private String fileMd5;        // 文件指纹
    private Integer chunkId;       // 文本分块序号
    private String textContent;    // 文本内容
    private float[] vector;        // 向量数据（768维）
    private String modelVersion;   // 向量生成模型版本
    private String userId;         // 上传用户ID
    private String orgTag;         // 组织标签
    private boolean isPublic;      // 是否公开
}
