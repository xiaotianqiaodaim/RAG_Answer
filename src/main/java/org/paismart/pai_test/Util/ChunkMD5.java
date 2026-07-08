package org.paismart.pai_test.Util;

import org.springframework.stereotype.Component;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component
public class ChunkMD5 {

    public String calculateChunkMd5(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        return DigestUtils.md5Hex(bytes);
    }
}
