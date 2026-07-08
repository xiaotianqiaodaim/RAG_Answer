package org.paismart.pai_test.Mapper;

import org.apache.ibatis.annotations.Mapper;
import org.paismart.pai_test.entity.FileUpload;

import java.util.List;

@Mapper
public interface FileUploadMapper {
    public List<FileUpload> get_byMD5(String MD5);

    public Integer Insert_one(FileUpload fileUpload);

    public Integer set_Status(String MD5);
}
