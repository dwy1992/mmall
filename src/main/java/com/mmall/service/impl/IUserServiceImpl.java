package com.mmall.service.impl;

import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.common.TokenCache;
import com.mmall.dao.UserMapper;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import com.mmall.util.MD5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * Created by dwy on 2018/12/2.
 */
@Service("iUserService")
public class IUserServiceImpl implements IUserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public ServerResponse<User> login(String username, String password) {
        int resultCount = userMapper.checkUserName(username);
        if(resultCount == 0){
          return ServerResponse.createByMessage("用户名不存在");
        }
        //// TODO: 2018/12/2  密码MD5
        String md5Password = MD5Util.MD5EncodeUtf8(password);
        User user = userMapper.selectLogin(username,md5Password);
        if (user == null) {
            return  ServerResponse.createByMessage("密码错误");
        }

        user.setPassword(org.apache.commons.lang3.StringUtils.EMPTY);
        return ServerResponse.createBySuccess("登陆成功",user);
    }


    public ServerResponse<String> register(User user){

//        int resultCount = userMapper.checkUserName(user.getUsername());
//        if(resultCount > 0){
//            return ServerResponse.createByMessage("用户名已存在");
//        }

        ServerResponse vaildResponse = this.checkVaild(user.getUsername(),Const.USERNAME);
        if (!vaildResponse.isSuccess()){
          return vaildResponse;
        }

        vaildResponse = this.checkVaild(user.getEmail(),Const.EMAIL);
        if (!vaildResponse.isSuccess()){
            return vaildResponse;
        }


//        resultCount = userMapper.checkUserEmail(user.getEmail());
//        if(resultCount > 0){
//            return ServerResponse.createByMessage("邮箱已被占用");
//        }

        user.setRole(Const.Role.ROLE_CUSTOMER);
        user.setPassword(MD5Util.MD5EncodeUtf8(user.getPassword()));

        int resultCount = userMapper.insert(user);

        if (resultCount  == 0 ){
            return ServerResponse.createByError("注册失败");
        }

        return ServerResponse.createByMessage("注册成功");
    }


    public ServerResponse<String> checkVaild(String str,String type){
      if(org.apache.commons.lang3.StringUtils.isNotBlank(type)){

          if (Const.USERNAME.equals(type)){
              int resultCount = userMapper.checkUserName(str);
              if(resultCount > 0){
                  return ServerResponse.createByMessage("用户名已存在");
              }
          }

          if (Const.EMAIL.equals(type)){
             int resultCount = userMapper.checkUserEmail(str);
              if(resultCount > 0){
                  return ServerResponse.createByMessage("邮箱已被占用");
              }

          }

      }else{
          return ServerResponse.createByMessage("参数错误");
      }
        return ServerResponse.createByMessage("校验成功");
    }


    public ServerResponse<String> forgetGetQuestion(String username){

           ServerResponse validResponse = this.checkVaild(username,Const.USERNAME);
           if (!validResponse.isSuccess()){
               return  ServerResponse.createByMessage("用户不存在");
           }
        String question = userMapper.selectQuestionByUserName(username);

        if (org.apache.commons.lang3.StringUtils.isNotBlank(question)){
          return ServerResponse.createBySuccess(question);
        }
        return  ServerResponse.createByMessage("获取问题失败");
    }


    public ServerResponse<String> checkAnswer(String username,String question,String answer){
               int resultCount = userMapper.checkAnswer(username,question,answer);
        if (resultCount > 0){
            String forgetToken = UUID.randomUUID().toString();
            TokenCache.setKey("token_"+username,forgetToken); //把token放入缓存中
            return ServerResponse.createBySuccess(forgetToken);
        }
        return ServerResponse.createByMessage("问题答案错误");
    }


    public ServerResponse<String> forgetRestPassWord(String username,String passWordNew,String token){
       if(org.apache.commons.lang3.StringUtils.isBlank(token)){
         return ServerResponse.createByMessage("token为空");
        }
        ServerResponse validResponse = this.checkVaild(username,Const.USERNAME);
        if (validResponse.isSuccess()){
          return ServerResponse.createByMessage("用户不存在");
        }
        String tokenCache = TokenCache.getKey(TokenCache.TOKEN_PREFIX+username);
        if (org.apache.commons.lang3.StringUtils.isBlank(tokenCache)){
          return ServerResponse.createByMessage("token不存在");
        }

        if(org.apache.commons.lang3.StringUtils.equals(token,tokenCache)){
        String newPwd = MD5Util.MD5EncodeUtf8(passWordNew);
        int resultCount = userMapper.updateNewPwd(username,newPwd);

            if (resultCount > 0 ){
                return ServerResponse.createByMessage("修改密码成功");
            }
        }else{
            return  ServerResponse.createByMessage("token错误");
        }
        return ServerResponse.createByMessage("修改密码失败");
    }

    public ServerResponse<String> resetPassWord(String passWordOld,String passWordNew,User user){

        int resultCount = userMapper.checkPassWord(user.getId(),MD5Util.MD5EncodeUtf8(passWordOld));
        if (resultCount == 0){
        return  ServerResponse.createByError("原密码错误");
        }
        user.setPassword(MD5Util.MD5EncodeUtf8(passWordNew));
        int updateResult = userMapper.updateByPrimaryKeySelective(user);
       if (updateResult > 0){
           return ServerResponse.createByMessage("修改密码成功");
       }
        return ServerResponse.createByError("修改密码失败");
    }

    public ServerResponse<User> updateInformation(User user){
        //username是不能被更新的
        //email也要进行一个校验,校验新的email是不是已经存在,并且存在的email如果相同的话,不能是我们当前的这个用户的.
        int resultCount = userMapper.checkEmailByUserId(user.getEmail(),user.getId());
        if(resultCount > 0){
            return ServerResponse.createByError("email已存在,请更换email再尝试更新");
        }
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setEmail(user.getEmail());
        updateUser.setPhone(user.getPhone());
        updateUser.setQuestion(user.getQuestion());
        updateUser.setAnswer(user.getAnswer());

        int updateCount = userMapper.updateByPrimaryKeySelective(updateUser);
        if(updateCount > 0){
            return ServerResponse.createBySuccess("更新个人信息成功",updateUser);
        }
        return ServerResponse.createByError("更新个人信息失败");
    }

    public ServerResponse<User> getInformation(Integer userId){
        User user = userMapper.selectByPrimaryKey(userId);
        if(user == null){
            return ServerResponse.createByError("找不到当前用户");
        }
        user.setPassword(org.apache.commons.lang3.StringUtils.EMPTY);
        return ServerResponse.createBySuccess(user);

    }

    @Override
    public ServerResponse checkAdminRole(User user) {
        if (user != null && user.getRole().intValue() == Const.Role.ROLE_ADMIN) {
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();
    }

}
