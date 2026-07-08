package org.paismart.pai_test.Service;

import org.paismart.pai_test.entity.FileUpload;
import org.springframework.stereotype.Service;

@Service
public interface FileUploadService {
    public boolean if_exit(String MD5);

    public boolean insert(FileUpload fileUpload);
}
