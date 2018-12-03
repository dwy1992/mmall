package com.mmall.dao;

import com.mmall.pojo.User;
import org.apache.ibatis.annotations.Param;

public interface UserMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(User record);

    int insertSelective(User record);

    User selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(User record);

    int updateByPrimaryKey(User record);

    int checkUserName(String username);

    User selectLogin(@Param("username") String username,@Param("password") String password);

    int checkUserEmail(String email);

    String selectQuestionByUserName(String username);

    int checkAnswer(@Param("username") String username,@Param("question") String question,@Param("answer") String answer);

    int updateNewPwd(@Param("username") String username,@Param("password") String password);

    int checkPassWord(@Param("userId") Integer userId,@Param("password") String password);

    int checkEmailByUserId(@Param(value="email")String email,@Param(value="userId")Integer userId);
}