package org.paismart.pai_test.Filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.paismart.pai_test.Service.UserService;
import org.paismart.pai_test.Util.JWTUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    JWTUtil jwtUtil;

    @Autowired
    UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        //先获取Token，如果过期了，忽略过期异常提取clams
        String[] twoToken = jwtUtil.get_two_token(request);
        String id = null;
        String RequestURI=request.getRequestURI();
        //这几个不需要登录信息，直接放行
        if(RequestURI.equals("/api/v1/users/login") || RequestURI.equals("/api/v1/users/register") || RequestURI.equals("/api/v1/users/test")||RequestURI.startsWith("/ws/" )) {
            filterChain.doFilter(request,response);
            return;
        }
        if(twoToken!=null){
            String Token=twoToken[1];//0是普通的ReToken，1是Token
            String RefToken=twoToken[0];
            //1.判断是否过期，（函数的返回值设计为：+表示没有过期，距离过期还有多久，用于判断是否需要刷新？-已经过期，过期多久了？是否在宽限时间内？）如果没有过期，看看是否需要刷新
            try{
                Claims claims1 = jwtUtil.get_claims(RefToken);
                long l = jwtUtil.if_Out(Token);//大于零没有过期
                if(l>0){//大于0没有过期，判断是否需要刷新
                    //使用RefToken对Token进行刷新
                    id=(String) claims1.get("userId");
                    if(l<jwtUtil.getREFRESH_THRESHOLD()){
                        System.out.println("刷新1");
                        String newToken=jwtUtil.getJWT(Integer.parseInt(id));
                        if (newToken != null) {
                            response.setHeader("New-Token", newToken);
                        }
                    }
                }
                if(l<0){//小于零，过期，判断是否在宽限期内
                    if(-l<jwtUtil.getRefreshWindow()){//在宽限期内
                        id=(String) claims1.get("userId");
                        System.out.println("刷新2");
                        String newToken=jwtUtil.getJWT(Integer.parseInt(id));
                        if (newToken != null) {
                            response.setHeader("New-Token", newToken);
                        }
                    }
                }
            }
            catch (JwtException e){
                System.out.println("被篡改了");
                //说明被篡改了，拦截这个请求
                SecurityContextHolder.clearContext();

                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"Token被篡改，非法访问\"}");
                return;
            }

            //用id是否为空，作为是否可以正常登录的依据
            //通过这一步，Spring Security已经知道了
            /*
             *当前用户已经登录,当前用户是谁,当前用户有哪些角色权限
             * */
            if(id!=null &&!id.isEmpty()){//不为，索命JWT检验完成
                Integer id1=Integer.parseInt(id);
                request.setAttribute("userId",id);
                UserDetails userDetails = userService.getDetails(id1);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                filterChain.doFilter(request,response);
                return;
            }


        }
        else{
            //有问题，没法登录
            SecurityContextHolder.clearContext();

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"没Token，未登录\"}");
            return;
        }



    }


}
