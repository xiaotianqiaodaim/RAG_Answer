package org.paismart.pai_test.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
*     tag_id      varchar(255) not null
        primary key,
    created_at  datetime(6)  null,
    description text         null,
    name        varchar(255) not null,
    parent_tag  varchar(255) null,
    updated_at  datetime(6)  null,
    created_by  bigint       not null,
    constraint FK47ryc85iy3a7u66385y6vrelc
        foreign key (created_by) references users (id)
*
*
* */

@NoArgsConstructor
@AllArgsConstructor
@Data
public class OrganizationTags {
    private String tagId;
    private LocalDateTime createdAt;
    private String description;
    private String name;
    private String parentTag;
    private LocalDateTime updatedAt;
    private Integer createdBy;
}
