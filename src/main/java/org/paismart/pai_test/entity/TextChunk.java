package org.paismart.pai_test.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TextChunk {
    // Getters/Setters
    private int chunkId;       // 分块序号
    private String content;    // 分块内容
}
