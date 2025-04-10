package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.service.CommonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
public class CommonServiceImpl implements CommonService {

    private static final Logger log = LoggerFactory.getLogger(CommonServiceImpl.class);
    @Value("${file.upload-dir}")
    private String uploadPath;
    @Override
    public String upload(MultipartFile file) {
        // 获取原始文件名
        String originalFilename = file.getOriginalFilename();

        String stringUUID = UUID.randomUUID().toString();
        try {
            assert originalFilename != null;
            file.transferTo(new File(uploadPath + stringUUID + "_" + originalFilename));
            return uploadPath + stringUUID + "_" + originalFilename;
        } catch (IOException e) {
            log.error("上传失败{}", e.getMessage());
            return MessageConstant.UPLOAD_FAILED;
        }
    }
}
