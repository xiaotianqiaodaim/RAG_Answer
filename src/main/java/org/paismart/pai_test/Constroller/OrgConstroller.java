package org.paismart.pai_test.Constroller;


import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.paismart.pai_test.Service.OrganizationTagsService;
import org.paismart.pai_test.entity.OrganizationTags;
import org.paismart.pai_test.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Component
@RestController
@RequestMapping("/api/org")
public class OrgConstroller {
    @Autowired
    OrganizationTagsService organizationTagsService;


    @GetMapping("/all")
    public ResponseEntity<?> get_all(HttpServletRequest httpServletRequest){//获取用户的详细信息的接口，也顺便看一下，JWT能否正常运行有合法的Token就能正常的访问，没有就不能访问
        List<OrganizationTags> all = organizationTagsService.get_all();


        return ResponseEntity.ok(Map.of("code", 200, "detail", all));
    }


}
