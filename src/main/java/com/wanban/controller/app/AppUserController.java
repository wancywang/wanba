package com.wanban.controller.app;

import com.wanban.pojo.User;
import com.wanban.service.UserService;
import com.wanban.utils.IMRegisterUtil;
import com.wanban.utils.MdUtil;
import com.wanban.utils.SendSmsUtil;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.wanban.utils.JsonUtils.objectToJson;

/**
 * Created by CHLaih on 2018/1/20.
 */
@Controller
@RequestMapping({"/user"})
public class AppUserController
{

    @Autowired
    private UserService userService;
    //查询所有用户
    @RequestMapping(value = "/getAllUser",produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getAllUser(){
        List<User> list = userService.getAllUser();

        return list !=null?objectToJson(list): "0";
    }

    //查询个人信息
    @RequestMapping(value = "/findUser", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String findUser(@RequestParam(value = "userId", required = true) int userId) {
        User user = userService.getUser(userId);
        return user !=null?objectToJson(user): "0";
    }

    //修改个人信息  根据主键修改用户资料(不包括密码)
    //user : 传入所需要修改的值,不需要修改的值不用传
    //成功修改个人资料返回1，失败返回0
    @RequestMapping(value = "/updateUser", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public int updateUser(@RequestParam("imageFile") MultipartFile imageFile,User user, HttpServletRequest request)throws Exception{

        String filePath = request.getServletContext().getRealPath("/");
        String imageName = com.wanban.utils.DateUtil.getCurrentDateStr() + "."
                + imageFile.getOriginalFilename().split("\\.")[1];
        imageFile.transferTo(new File(filePath + "static/levelImages/"
                + imageName));
        if (!imageFile.isEmpty()) {
            File oldFile = new File(filePath+ "static/levelImages/"+user.getImageName());
            oldFile.delete();
        }
        user.setImageName(imageName );
        return userService.updateUser(user)>0 ? 1:0;
    }

    //用户注册
    @RequestMapping(value = "/register",method = RequestMethod.POST,produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String register(HttpServletRequest request,HttpServletResponse response,@RequestParam("username")String username,@RequestParam("password")String password,@RequestParam("confirmpassword")String confirmpassword,@RequestParam("phone")String phone) throws IOException {

        if(username == null || username.length() < 3)
        {
            return objectToJson("0:用户名至少3位！");
        }

        if(password == null || confirmpassword == null)
        {
            return objectToJson("0:请输入密码！");
        }



        if(!userService.checkRegisterPhone(phone))
        {
            return objectToJson("0:电话号码已被注册！");
        }

        if(!userService.checkRegisterUsername(username))
        {
            return objectToJson("0:用户名重复！");
        }

        if(!password.equals(confirmpassword))
        {
            return objectToJson("0:密码不一致");
        }

        User User = new User();

        if(userService.findUserByName(username) == null)
        {
            User.setUserName(username);
            User.setPassword(MdUtil.md5(password));
            User.setPhone(phone);
            userService.addUser(User);
            IMRegisterUtil.createUser(User);
            return objectToJson("1");
        }
        else
        {
            return objectToJson("0");
        }
    }

    //登陆功能
    @RequestMapping(value = "/login",method = {RequestMethod.GET,RequestMethod.POST},produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String login(HttpServletRequest request, HttpServletResponse response, @RequestParam("username")String username, @RequestParam("password")String password, ModelAndView mv, HttpSession session) throws Exception {
        password = MdUtil.md5(password);
        User User = userService.checkLogin(username,password);
        return User != null?objectToJson(User):"0";
    }

    //修改密码功能
    @RequestMapping(value = "/updatepassword",method = RequestMethod.POST,produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String updatepassword(HttpServletRequest request,HttpServletResponse response,@RequestParam("username")String username,@RequestParam("oldpw")String oldpw,@RequestParam("newpw")String newpw)
    {
        User user = userService.findUserByName(username);
        oldpw = MdUtil.md5(oldpw);
        if(!oldpw.equals(user.getPassword()))
        {
            return objectToJson("0:密码不一致");
        }
        else
        {
            user.setPassword(newpw);
            userService.updateUser(user);
            return "1";
        }
    }


    //注销
    @RequestMapping(value = "/loginout",method = RequestMethod.DELETE)
    public void loginout(HttpServletRequest request)
    {
        HttpSession session = request.getSession();
        session.removeAttribute("user");
        session.removeAttribute("username");
    }



    @WebServlet("/sendSms")
    @ResponseBody
    public class SendSms extends HttpServlet {
        @Override
        public void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            String phone=req.getParameter("phone");
            //根据获取到的手机号发送验证码
            String code= SendSmsUtil.getCode(phone);
            System.out.println(code);
            resp.getWriter().print(code);
        }
    }
}
