package com.kob.backend.chat;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.kob.backend.consumer.WebSocketServer;
import com.kob.backend.consumer.utils.Game;
import com.kob.backend.consumer.utils.JwtAuthentication;
import com.kob.backend.mapper.BotMapper;
import com.kob.backend.mapper.RecordMapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.Bot;
import com.kob.backend.pojo.ChatMessage;
import com.kob.backend.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Thread.sleep;

@Component
@ServerEndpoint("/websocket/chat/{token}")  // 注意不要以'/'结尾
public class WebSocketChatServer {

    final public static ConcurrentHashMap<Integer, WebSocketChatServer> users = new ConcurrentHashMap<>();
    private User user;
    private Session session = null;

    public static UserMapper userMapper;

    public static RestTemplate restTemplate;

    public static StringRedisTemplate stringRedisTemplate;


    @Autowired
    public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
        WebSocketChatServer.stringRedisTemplate = stringRedisTemplate;
    }


    @Autowired
    public void setUserMapper(UserMapper userMapper) {
        WebSocketChatServer.userMapper = userMapper;
    }
    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        //如果加了autowired，检查唯一的一个@Bean函数，初始化后复制过来
        WebSocketChatServer.restTemplate = restTemplate;
    }


    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) throws IOException {
        this.session = session;
//        System.out.println("connected!");
        Integer userId = JwtAuthentication.getUserId(token);
        this.user = userMapper.selectById(userId);

        if (this.user != null) {
            users.put(userId, this);
        } else {
            this.session.close();
        }
        //将缓存中的聊天记录发送给新连接的用户
        //取出缓存中的聊天记录
        //判断缓存中是否有聊天记录
        if(stringRedisTemplate.opsForList().size("chatRecord") != 0) {
            List<String> record = stringRedisTemplate.opsForList().range("chatRecord", 0, -1);
            //将聊天记录转发给新连接的用户
            for(String msg : record) {
                ChatMessage bean = JSONUtil.toBean(msg, ChatMessage.class);
                if(bean.getUsername().equals(this.user.getUsername())) {
                    bean.setIsMyMessage(true);
                } else {
                    bean.setIsMyMessage(false);
                }
                this.sendMessage(JSONUtil.toJsonStr(bean));
            }
        }

    }

    @OnClose
    public void onClose() {
//        System.out.println("disconnected!");
        if (this.user != null) {
            users.remove(this.user.getId());
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {  // 当做路由
        ChatMessage msg = JSONUtil.toBean(message, ChatMessage.class);
        //收到消息后的处理
        //1.修改time为当前时间
        //2.将消息转发给所有人

        //时间格式：HH:mm:ss
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String currentTime = sdf.format(new Date());
        msg.setTime(currentTime);

        //转发给自己
        msg.setIsMyMessage(true);
        this.sendMessage(JSONUtil.toJsonStr(msg));

        //转发给其他人
        msg.setIsMyMessage(false);
        for (WebSocketChatServer user : users.values()) {
            if(user == this) continue;
            user.sendMessage(JSONUtil.toJsonStr(msg));
        }


        //格式为月日时分
        SimpleDateFormat sdf2 = new SimpleDateFormat("MM-dd HH:mm");
        String currentTime2 = sdf2.format(new Date());
        msg.setIsMyMessage(false);
        msg.setTime(currentTime2);

        //缓存1小时的聊天记录
        //如果缓存中的聊天记录超过100条，删除最早的50条
        stringRedisTemplate.opsForList().rightPush("chatRecord", JSONUtil.toJsonStr(msg));
        if(stringRedisTemplate.opsForList().size("chatRecord") > 100) {
            stringRedisTemplate.opsForList().trim("chatRecord", 50, -1);
        }

    }

    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }

    public void sendMessage(String message) {
        synchronized (this.session) {
            try {
                this.session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}