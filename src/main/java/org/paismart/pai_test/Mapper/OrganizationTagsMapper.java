package org.paismart.pai_test.Mapper;

import org.apache.ibatis.annotations.Mapper;
import org.paismart.pai_test.entity.OrganizationTags;
import org.paismart.pai_test.entity.User;

import java.util.List;

@Mapper
public interface OrganizationTagsMapper {
    public Integer createOrganization(OrganizationTags organizationTags);

    public OrganizationTags findById(String tagId);

    public List<OrganizationTags> getAll();

}
