package org.paismart.pai_test.Constroller;


import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.paismart.pai_test.Service.UserService;
import org.paismart.pai_test.Util.JWTUtil;
import org.paismart.pai_test.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    UserService userService;

    @Autowired
    JWTUtil jwtUtil;

    @PostMapping("/test")
    public ResponseEntity<?> register(){
        System.out.println("访问了test");
        return ResponseEntity.ok(Map.of("code", 200, "message", "User registered successfully"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user){
        //后续需要补充异常处理
        Integer i= userService.register(user);
        if(i!=0){
            return ResponseEntity.ok(Map.of("code", 200, "message", "User registered successfully"));
        }
        else {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "Username and password cannot be empty"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user){
        //登录需要生成相应的令牌
        System.out.println(user.toString());
        User user1=userService.login(user);
        System.out.println(user1);
        if(user1!=null){
            String token = jwtUtil.getJWT(user1.getId());
            String refresToken = jwtUtil.getRefresToken(user1);
            return ResponseEntity
                    .ok(Map.of("code", 200, "message", "登录成功","data",
                            Map.of("token",token,"refreshToken",refresToken)));
        }
        else {
            return ResponseEntity.badRequest()
                    .body(Map.of("code", 400, "message", "登录失败"));
        }
    }

    @GetMapping("/detail")
    public ResponseEntity<?> detail(HttpServletRequest httpServletRequest){//获取用户的详细信息的接口，也顺便看一下，JWT能否正常运行有合法的Token就能正常的访问，没有就不能访问
        String[] twoToken = jwtUtil.get_two_token(httpServletRequest);
        Claims claims = jwtUtil.get_claims(twoToken[0]);
        String userId = (String)claims.get("userId");
        Integer id=Integer.parseInt(userId);
        User user_detail = userService.getById(id);
        return ResponseEntity.ok(Map.of("code", 200, "detail", user_detail.toString()));
    }


}
