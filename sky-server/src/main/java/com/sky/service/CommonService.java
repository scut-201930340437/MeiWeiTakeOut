package com.sky.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

public interface CommonService {

    String upload(MultipartFile file);
}
