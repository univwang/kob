package com.kob.backend.service.impl.ranklist;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.User;
import com.kob.backend.service.ranklist.GetRanklistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class GetRanklistServiceImpl implements GetRanklistService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public JSONObject getList(Integer page) {

        List<User> users = getListByRedis(page);
        if(users == null) {
            users = getListByMysql(page);
        }
        JSONObject resp = new JSONObject();
        resp.put("users", users);
        resp.put("users_count", getAllSize());
        return resp;
    }

    private List<User> getListByRedis(Integer page) {
        int pageSize = 10;
        int start = (page - 1) * pageSize;
        int end = start + pageSize - 1;
        Set<String> users = stringRedisTemplate.opsForZSet().reverseRange("users:rating", start, end);
        //将redis中的数据转换为User对象
        if(users == null || users.size() == 0) {
            return null;
        }
        List<User> userList = new ArrayList<>();
        users.forEach(user -> {
            userList.add(JSON.parseObject(user, User.class));
        });
        return userList;
    }
    private List<User> getListByMysql(int page) {
        IPage<User> userIPage = new Page<>(page, 10);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("rating");
        List<User> users = userMapper.selectPage(userIPage, queryWrapper).getRecords();
        SetListByRedis(users);
        return users;
    }

    private void SetListByRedis(List<User> users) {
        System.out.println("更新redis");
        for (User user : users) {
            stringRedisTemplate.opsForZSet().add("users:rating", JSON.toJSONString(user), user.getRating());
        }
        //设置过期时间
        //5分钟刷新一次排行榜
        stringRedisTemplate.expire("users:rating", 5, TimeUnit.MINUTES);
    }

    private long getAllSize() {
        Long aLong = stringRedisTemplate.opsForZSet().zCard("users:rating");
        if(aLong == null) {
            return 0;
        }
        return aLong;
    }

}
