package org.paismart.pai_test.entity;

//用户的实体类

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Integer id;
    private String userName;
    private String passWord;//用户的基本信息

    private String role;//用户的角色，管理员或者用户

    private String orgTag;//用户的组织
    private String primaryTag;//用户的主要标签

    private LocalDateTime createDateTime;
    private LocalDateTime loginTime;

}
