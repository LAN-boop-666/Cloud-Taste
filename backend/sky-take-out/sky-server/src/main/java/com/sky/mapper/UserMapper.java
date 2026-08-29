package com.sky.mapper;

import com.sky.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {

    /**
     * 根据动态条件统计新增用户数量，供工作台使用。
     *
     * @param map 查询条件
     * @return 用户数量
     */
    Integer countByMap(Map map);


    /**
     * 根据openid查询用户
     * @param openid
     * @return
     */
    @Select("select * from user where openid = #{openid}")
    User getByOpenid(String openid);

    @Select("select * from user where id = #{id}")
    User getById(Long id);

    /**
     * 新增用户
     * @param user
     */

    void insert(User user);

    /**
     * 查询区间内每日新增注册用户（user表create_time）
     */
    @Select("select date(create_time) as day, count(id) as new_user_count " +
            "from user " +
            "where create_time >= #{beginTime} and create_time <= #{endTime} " +
            "group by date(create_time)")
    List<Map<String, Object>> getEveryDayNewUser(@Param("beginTime") LocalDateTime beginTime,
                                                 @Param("endTime") LocalDateTime endTime);


    /**
     * 查询指定时间之前注册的用户数量
     */
    @Select("select count(*) from user where create_time < #{beforeBegin}")
    long countUserBeforeDate(@Param("beforeBegin") LocalDateTime beforeBegin);
}
