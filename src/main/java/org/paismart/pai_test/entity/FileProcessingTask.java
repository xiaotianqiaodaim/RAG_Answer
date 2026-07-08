package org.paismart.pai_test.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileProcessingTask {
    private String fileMd5; // 文件的 MD5 校验值
    private String filePath; // 文件存储路径
    private String fileName; // 文件名
    private String userId;   // 上传用户ID
    private String orgTag;   // 文件所属组织标签
    private Boolean isPublic; // 文件是否公开
}
