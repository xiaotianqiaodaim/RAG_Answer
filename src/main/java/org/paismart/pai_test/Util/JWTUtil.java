package org.paismart.pai_test.Util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.paismart.pai_test.Service.UserService;
import org.paismart.pai_test.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.*;

@Component
public class JWTUtil {
    @Value("${jwt.secret-key}")
    private String secretkey;

    @Autowired
    UserService userService;

    private static final long EXPIRATION_TIME = 36000000;//令牌的过期时间

    private static final long REFRESH_TOKEN_EXPIRATION_TIME = 604800000; // 7 days (refresh token有效期)

    private static final long REFRESH_THRESHOLD = 300000;//刷新门槛，剩余时间小于5分钟刷新

    private static final long REFRESH_WINDOW = 600000; // 10分钟：token过期后的宽限期

    private SecretKey base64Secretkey(){
        byte[] decode = Base64.getDecoder().decode(secretkey);
        return Keys.hmacShaKeyFor(decode);
    }
    public long getREFRESH_THRESHOLD(){
        return REFRESH_THRESHOLD;
    }
    public long getRefreshWindow(){
        return REFRESH_WINDOW;
    }

    private String generateTokenId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    //未结合Redis做，没有进行Redis缓存
    public String getJWT(Integer id){
        SecretKey secretKey2 = base64Secretkey();
        String tokenId = generateTokenId();
        User user = userService.getById(id);
        Map<String,Object> clams=new HashMap<>();//负载部分

        Long exp=System.currentTimeMillis()+EXPIRATION_TIME;//获取相对于Unix的毫秒时间戳并加上多久之后过期
        //或者说，获取当前时间，加上一个数之后，得到过期时间，时间超过过期时间之后，

        clams.put("tokenId",tokenId);//tokenID，token的唯一标识。
        clams.put("role",user.getRole());//用户的角色，目前用于测试JWT能否跑通
        clams.put("userId",user.getId().toString());//用户的ID
        clams.put("orgTags",user.getOrgTag());
        clams.put("primaryOrg",user.getPrimaryTag());

        return Jwts.builder().setClaims(clams)
                .setSubject(user.getUserName()) // 设置 token 属于哪个用户，对应 sub 字段
                .setExpiration(new Date(exp))//设置过期时间
                .signWith(secretKey2, SignatureAlgorithm.HS256)
                .compact();

        //tokenCacheService.cacheToken(tokenId, user.getId().toString(), username, expireTime);
    }
    //未结合Redis做，没有进行Redis缓存
    public String getRefresToken(User user){
        //用于生成对普通token进行刷新的Token的生成
        SecretKey secretKey = base64Secretkey();
        String tokenId = generateTokenId();
        long exp=System.currentTimeMillis()+REFRESH_TOKEN_EXPIRATION_TIME;
        Map<String, Object> claims = new HashMap<>();
        claims.put("refreshTokenId", tokenId); // 添加refreshTokenId
        claims.put("userId", user.getId().toString());//保存了用户的ID用于后续的刷新？
        claims.put("type", "refresh"); // 标识这是一个refresh token
        String refreshToken = Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getUserName())
                .setExpiration(new Date(exp))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        return refreshToken;
        //tokenCacheService.cacheRefreshToken(refreshTokenId, user.getId().toString(), null, expireTime);

    }



    //解析Token的类，获取Token是否有问题，返回long的数据+不过过期，-过期了。
    public long if_Out(String JWT){
        try {
            Claims claims = get_claims(JWT);
            //返回距离过期要多久
            Date expiration = claims.getExpiration();//获取过期的时间
            long res=expiration.getTime()-System.currentTimeMillis();
            return res;

        }
        catch (ExpiredJwtException e){//合法，但已经过期
            Claims claims=e.getClaims();
            long out_time=claims.getExpiration().getTime();
            return out_time-System.currentTimeMillis();

        } catch (JwtException e) {//被篡改了,直接抛出异常
            throw new RuntimeException(e);
        }
    }
    //用于获取负载的方法
    public Claims get_claims(String JWT){
        return Jwts.parser()
                .verifyWith(base64Secretkey())
                .build()
                .parseSignedClaims(JWT)
                .getPayload();
    }

    public String[] get_two_token(HttpServletRequest request){
        String refreshToken = request.getHeader("refreshToken");
        String token=request.getHeader("token");
        if(refreshToken==null || refreshToken.isEmpty()||token==null ||token.isEmpty()){
            return null;
        }
        return new String[]{refreshToken,token};
    }

}
