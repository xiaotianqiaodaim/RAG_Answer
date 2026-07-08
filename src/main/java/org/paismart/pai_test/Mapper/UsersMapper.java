package org.paismart.pai_test.Mapper;

import org.apache.ibatis.annotations.Mapper;
import org.paismart.pai_test.entity.User;

@Mapper
public interface UsersMapper {
    public Integer register(User user);

    public User login(User user);

    public User getByID(Integer id);

    public User getByUsername(String username);

}
