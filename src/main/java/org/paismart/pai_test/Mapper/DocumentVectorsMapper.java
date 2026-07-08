package org.paismart.pai_test.Mapper;

import org.apache.ibatis.annotations.Mapper;
import org.paismart.pai_test.entity.DocumentVectors;

import java.util.List;

@Mapper
public interface DocumentVectorsMapper {
    public List<DocumentVectors>  findByMd5(String MD5);

    public Integer deleteByMd5(String MD5);

    public Integer save(DocumentVectors documentVectors);
}
