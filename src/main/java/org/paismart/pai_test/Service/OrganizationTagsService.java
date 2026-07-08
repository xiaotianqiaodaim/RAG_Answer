package org.paismart.pai_test.Service;

import org.paismart.pai_test.entity.OrganizationTags;

import java.util.List;

public interface OrganizationTagsService {
    public Integer createOrganization(OrganizationTags organizationTags);

    public List<OrganizationTags> get_all();
}
