package com.kob.backend.service.impl.user.account;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.User;
import com.kob.backend.service.user.account.RegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class RegisterServiceImpl implements RegisterService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @Value("${spring.mail.username}")
    private String from;

    @Autowired
    private JavaMailSender mailSender;

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Override
    public Map<String, String> register(String username, String password, String confirmedPassword, String photo, String mail, String mailCode) {
        Map<String, String> map = new HashMap<>();
        if (username == null) {
            map.put("error_message", "用户名不能为空");
            return map;
        }
        if (password == null || confirmedPassword == null) {
            map.put("error_message", "密码不能为空");
            return map;
        }

        username = username.trim();
        if (username.length() == 0) {
            map.put("error_message", "用户名不能为空");
            return map;
        }

        if (password.length() == 0 || confirmedPassword.length() == 0) {
            map.put("error_message", "密码不能为空");
            return map;
        }

        if (username.length() > 100) {
            map.put("error_message", "用户名长度不能大于100");
            return map;
        }

        if (password.length() > 100 || confirmedPassword.length() > 100) {
            map.put("error_message", "密码长度不能大于100");
            return map;
        }

        if (!password.equals(confirmedPassword)) {
            map.put("error_message", "两次输入的密码不一致");
            return map;
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        List<User> users = userMapper.selectList(queryWrapper);
        if (!users.isEmpty()) {
            map.put("error_message", "用户名已存在");
            return map;
        }

        String encodedPassword = passwordEncoder.encode(password);
        if (photo == null || photo.length() == 0) {
            photo = "https://cdn.acwing.com/media/user/profile/photo/115622_lg_37336fb910.jpg";
        }

        String code = stringRedisTemplate.opsForValue().get(mail);
        if (code == null || !code.equals(mailCode)) {
            map.put("error_message", "验证码不正确");
            return map;
        }
        System.out.println(mail + " " + mailCode);

        User user = new User(null, username, encodedPassword, photo, 1500, mail);
        userMapper.insert(user);

        map.put("error_message", "success");
        return map;
    }

    @Override
    public Map<String, String> sendMail(String mail) {
        String checkCode = String.valueOf(new Random().nextInt(899999) + 100000);
        String message = "您的验证码为：" + checkCode + "\n验证码5分钟内有效，请勿泄露于他人。";
        sendSimpleMail(mail, "kob验证码", message);

        //TODO: 把验证码存储到redis中，5分钟后过期
        stringRedisTemplate.opsForValue().set(mail, checkCode, 5, java.util.concurrent.TimeUnit.MINUTES);

        Map<String, String> map = new HashMap<>();
        map.put("error_message", "success");
        return map;
    }
    public void sendSimpleMail(String to,String title,String content){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(title);
        message.setText(content);
        mailSender.send(message);
    }

}
