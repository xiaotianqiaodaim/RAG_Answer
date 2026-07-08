package org.paismart.pai_test.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
*
* create table chunk_info
(
    id           bigint auto_increment
        primary key,
    chunk_index  int          not null,
    chunk_md5    varchar(255) null,
    file_md5     varchar(255) null,
    storage_path varchar(255) null
);
*
* */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChunkInfo {
    private Integer id;
    private Integer chunkIndex;
    private String chunkMd5;
    private String fileMd5;
    private String storagePath;
}
