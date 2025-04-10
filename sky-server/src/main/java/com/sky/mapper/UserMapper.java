package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {

    /**
     * 根据openid查询用户
     *
     * @param openid
     * @return
     */
    @Select("select * from user where openid = #{openid}")
    User getByOpenid(String openid);

    /**
     * 新增用户数据
     *
     * @param user
     */
    void insert(User user);

    /**
     * 根据id查询用户
     *
     * @param userId
     * @return
     */
    @Select("select * from user where id = #{userId}")
    User getById(Long userId);

    /**
     * 根据条件统计用户数量
     *
     * @param dateList
     * @return
     */
    @MapKey("user_num")
    List<Map<String, Integer>> sumByMap(List<LocalDateTime> dateList);

    /**
     * 根据条件查询新增用户数
     * @param map
     * @return
     */
    Integer deltaByMap(Map map);
}
