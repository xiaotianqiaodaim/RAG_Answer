package org.paismart.pai_test.Service;

import org.paismart.pai_test.entity.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public interface UserService {
    public Integer register(User user);

    public User login(User user);

    public User getById(Integer id);

    public UserDetails getDetails(Integer id);
}
