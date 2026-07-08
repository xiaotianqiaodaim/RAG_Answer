package org.paismart.pai_test.Service;

import org.paismart.pai_test.Mapper.OrganizationTagsMapper;
import org.paismart.pai_test.entity.OrganizationTags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class OrgTagCacheService {

    //用户的组织标签的前缀
    private static final String USER_ORG_TAGS_KEY_PREFIX = "user:org_tags:";
    //用户的主要组织标签的前缀
    private static final String USER_PRIMARY_ORG_KEY_PREFIX = "user:primary_org:";

    //这个还不知道
    private static final String USER_EFFECTIVE_TAGS_KEY_PREFIX = "user:effective_org_tags:";

    //缓存的时间
    private static final long CACHE_TTL_HOURS = 24;

    //默认的组织标签
    private static final String DEFAULT_ORG_TAG = "DEFAULT";

    @Autowired
    private OrganizationTagsMapper organizationTagsMapper;

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;


    /**
     * 缓存用户的组织标签
     * **/
    public void cacheUserOrgTags(String username, List<String> orgTags){
        String key=USER_ORG_TAGS_KEY_PREFIX+username;
        redisTemplate.opsForList().rightPushAll(key,orgTags.toArray());
        redisTemplate.expire(key,CACHE_TTL_HOURS, TimeUnit.HOURS);
    }

    /**
     * 获取用户的组织标签
     * **/
    public List<String> getUserOrgTags(String username){
        String key=USER_ORG_TAGS_KEY_PREFIX+username;
        List<Object> range = redisTemplate.opsForList().range(key, 0, -1);
        return range.stream().map(obj->(String) obj).toList();
    }

    /**
     * 缓存用户的主要标签
     * **/
    public void cacheUserPrimaryOrg(String username,String primaryOrg){
        String key=USER_PRIMARY_ORG_KEY_PREFIX+username;
        redisTemplate.opsForValue().set(key,primaryOrg,CACHE_TTL_HOURS,TimeUnit.HOURS);
    }

    /**
     * 得到用户的主要标签
     * **/
    public String getUserPrimaryOrg(String username){
        String key=USER_PRIMARY_ORG_KEY_PREFIX+username;
        Object o = redisTemplate.opsForValue().get(key);
        return (String) o;
    }

    /**
     * 删除用户标签
     * **/
    public void deleteUserOrgTags(String username){
        String key=USER_ORG_TAGS_KEY_PREFIX+username;
        redisTemplate.delete(key);
    }

    /**
     * 删除用户的主要标签
     * **/

    public void deleteUserPrimaryOrg(String username){
        String key=USER_PRIMARY_ORG_KEY_PREFIX+username;
        redisTemplate.delete(key);
    }

    /**
     * 删除所有标签
     * **/
    public void deleteOrg(String username){
        deleteUserOrgTags(username);
        deleteUserPrimaryOrg(username);
    }

    /**
     *获取用户的有效标签权限集合（包含用户直接拥有的标签及其所有父标签）
     * 不知道获取父标签的作用是什么？父标签的东西也能查询。
     * **/
    public List<String> getUserEffectiveOrgTags(String username){
        String key=USER_EFFECTIVE_TAGS_KEY_PREFIX+username;
        List<Object> range = redisTemplate.opsForList().range(key, 0, -1);
        //获取所有的结果
        if(range!=null && !range.isEmpty()){
            return range.stream().map(o->(String)o ).toList();
        }

        //否则，没有命中，先获取用户的标签
        List<String> userOrgTags = getUserOrgTags(username);
        Set<String> allEff=new HashSet<>();
        if(userOrgTags!=null &&!userOrgTags.isEmpty()){
            allEff.addAll(userOrgTags);
        }
        //查找父标签
        for(String s:userOrgTags){
            collectParentTags(s,allEff);
        }
        allEff.add("default");

        List<String> result=new ArrayList<>(allEff);

        //缓存用户的标签
        redisTemplate.opsForList().rightPushAll(key,result);
        redisTemplate.expire(key,CACHE_TTL_HOURS,TimeUnit.HOURS);
        return result;
    }

    private void collectParentTags(String tagId, Set<String> result) {
        OrganizationTags tag = organizationTagsMapper.findById(tagId);
        if (tag != null && tag.getParentTag() != null && !tag.getParentTag().isEmpty()) {
            String parentTagId = tag.getParentTag();
            result.add(parentTagId);
            collectParentTags(parentTagId, result);
        }
    }

    /**
     * 清楚某一个用户的所有的有效标签的缓存
     * **/
    public void deleteEffectiveOrgTags(String username){
        String key=USER_EFFECTIVE_TAGS_KEY_PREFIX+username;
        redisTemplate.delete(key);
    }

    /**
     * 删除所有用户的有效标签，组织架构变化的时候使用。
     * **/
    public void deleteAllEffectiveOrgTags(){
        Set<String> keys = redisTemplate.keys(USER_EFFECTIVE_TAGS_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }









}
