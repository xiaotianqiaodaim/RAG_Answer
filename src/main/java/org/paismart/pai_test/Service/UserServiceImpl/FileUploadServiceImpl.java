package org.paismart.pai_test.Service.UserServiceImpl;

import org.paismart.pai_test.Mapper.FileUploadMapper;
import org.paismart.pai_test.Service.FileUploadService;
import org.paismart.pai_test.entity.FileUpload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class FileUploadServiceImpl implements FileUploadService {

    @Autowired
    FileUploadMapper fileUploadMapper;

    @Override
    public boolean if_exit(String MD5) {
        return fileUploadMapper.get_byMD5(MD5).isEmpty();
    }

    @Override
    public boolean insert(FileUpload fileUpload) {
        return fileUploadMapper.Insert_one(fileUpload)!=0;
    }


}
