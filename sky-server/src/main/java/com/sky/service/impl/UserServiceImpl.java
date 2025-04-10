package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WeChatProperties weChatProperties;

    public static final String wxUrl = "https://api.weixin.qq.com/sns/jscode2session";


    /**
     * 调用微信服务查询用户信息，获取openid
     * @param code
     * @return
     */
    private String getOpenid(String code) {
        Map<String, String> param = new HashMap<>();
        param.put("appid", weChatProperties.getAppid());
        param.put("secret", weChatProperties.getSecret());
        param.put("js_code", code);
        param.put("grant_type", "authorization_code");

        String response = HttpClientUtil.doGet(wxUrl, param);

        JSONObject jsonObject = JSON.parseObject(response);
        return jsonObject.getString("openid");
    }


    /**
     * 微信登录
     *
     * @param userLoginDTO
     * @return
     */
    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {
        // 调用微信服务查询用户信息，获取openId
        String openid = getOpenid(userLoginDTO.getCode());
        // 判断openId是否为空，如果为空则登录失败
        if (openid == null) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        // 判断当前微信用户是否为新用户，如果是新用户就自动完成注册
        User user = userMapper.getByOpenid(openid);
        if (user == null) { // 是新用户
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }

        return user;
    }
}
