package org.paismart.pai_test.Mapper;

import org.apache.ibatis.annotations.Mapper;
import org.paismart.pai_test.entity.ChunkInfo;
import org.paismart.pai_test.entity.FileUpload;

import java.util.List;

@Mapper
public interface ChunkInfoMapper {
    public Integer add_ChunkInfo(ChunkInfo chunkInfo);

    public List<ChunkInfo> getChun(String MD5);

    public Integer deleteByMD5(String MD5);
}
