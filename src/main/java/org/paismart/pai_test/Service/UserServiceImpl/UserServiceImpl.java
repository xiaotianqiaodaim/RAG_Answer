package org.paismart.pai_test.Service.UserServiceImpl;

import org.paismart.pai_test.Mapper.UsersMapper;
import org.paismart.pai_test.Service.UserService;
import org.paismart.pai_test.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UsersMapper usersMapper;

    @Override
    public Integer register(User user) {
        //将前端的数据映射为user
        //一个用户注册的时候，涉及到创建一个用户的私人空间，将其作为主空间，还有将一些组织信息存储到Redis当中
        user.setCreateDateTime(LocalDateTime.now());
        user.setLoginTime(LocalDateTime.now());
        //创建一个私人的空间，或者说是一个组织，我感觉没必要，干脆直接给他一个default空间得了，要啥私人空间
        user.setRole("user");
        user.setOrgTag("default");
        user.setPrimaryTag("default");
        return usersMapper.register(user);
    }

    @Override
    public User login(User user) {
        return usersMapper.login(user);
    }

    @Override
    public User getById(Integer id) {
        return usersMapper.getByID(id);
    }

    @Override
    public UserDetails getDetails(Integer id) {
        User user = usersMapper.getByID(id);//获取用户的信息

        return new org.springframework.security.core.userdetails.User(
                user.getUserName(),
                user.getPassWord(),
                getAuthorities(user.getRole()) // 获取用户的角色权限
        );
    }

    /**
     * 将用户的角色转换为 Spring Security 的权限格式。
     */
    private Collection<? extends GrantedAuthority> getAuthorities(String role) {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
