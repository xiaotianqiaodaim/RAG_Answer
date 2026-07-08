package org.paismart.pai_test.Service.UserServiceImpl;

import org.paismart.pai_test.Mapper.OrganizationTagsMapper;
import org.paismart.pai_test.Service.OrganizationTagsService;
import org.paismart.pai_test.entity.OrganizationTags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrganizationTagsServiceImpl implements OrganizationTagsService {

    @Autowired
    OrganizationTagsMapper organizationTagsMapper;
    @Override
    public Integer createOrganization(OrganizationTags organizationTags) {
        return organizationTagsMapper.createOrganization(organizationTags);
    }

    @Override
    public List<OrganizationTags> get_all() {

        return organizationTagsMapper.getAll();
    }
}
