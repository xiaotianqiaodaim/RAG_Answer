package org.paismart.pai_test.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUpload {
    private Long id; // 自增主键
    private String fileMd5;
    private String fileName;
    private long totalSize;
    private int status; // 0-上传中 1-已完成
    private String userId;
    private String orgTag;
    private boolean isPublic = false;
    private LocalDateTime createdAt;
    private LocalDateTime mergedAt;
}
// fileMd5,fileName,totalSize,status,userId,orgTag,isPublic,createdAt,mergedAt