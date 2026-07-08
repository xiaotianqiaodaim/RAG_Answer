package org.paismart.pai_test.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVectors {
    private Integer vectorId;
    private Integer chunkId;
    private String fileMd5;
    private Boolean isPublic;
    private String modelVersion;
    private String orgTag;
    private String textContent;
    private String userId;
}
