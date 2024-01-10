package com.kob.backend.service.impl.record;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kob.backend.mapper.RecordMapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.Record;
import com.kob.backend.pojo.User;
import com.kob.backend.service.record.GetRecordListService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.sql.SQLOutput;
import java.sql.Time;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class GetRecordListServiceImpl implements GetRecordListService {

    @Autowired
    private RecordMapper recordMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public JSONObject getList(Integer page) {
//        stringRedisTemplate.opsForValue().set("records:latestTime", Instant.now().toString());
        List<Record> recordByRedis = getRecordByRedis(page);

        //可以直接返回，用另外的线程更新redis

        //查询record的最新时间

        //获取当前时间
        String s = stringRedisTemplate.opsForValue().get("records:latestTime");
        Instant latestTime = Instant.parse(s);
        //redis假过期，可以直接返回
        //如果redis中的数据是最新的，就直接返回，不用更新
        if (recordByRedis != null && Duration.between(latestTime, Instant.now()).toMinutes() < 5) {
            System.out.println("直接返回");
            return getResp(recordByRedis);
        }
        //如果redis中的数据不是最新的，就更新redis

        //立刻更新最新的时间
        stringRedisTemplate.opsForValue().set("records:latestTime", Instant.now().toString());

        //更新数据，不能用page，需要新的方法
        new Thread(() -> {
            List<Record> recordByRedis2 = getRecordByRedis();
            List<Record> recordByMysql2 = getRecordByMysql();
            //更新redis的操作
            System.out.println("更新redis");
            SetRecordByRedis(recordByMysql2, recordByRedis2);
        }).start();

        if (recordByRedis == null) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error_message", "暂无数据");
            return jsonObject;
        }
        return getResp(recordByRedis);
    }

    @NotNull
    private JSONObject getResp(List<Record> records) {
        JSONObject resp = new JSONObject();
        List<JSONObject> items = new LinkedList<>();
        for (Record record : records) {
            User userA = userMapper.selectById(record.getAId());
            User userB = userMapper.selectById(record.getBId());
            JSONObject item = new JSONObject();
            item.put("a_photo", userA.getPhoto());
            item.put("a_username", userA.getUsername());
            item.put("b_photo", userB.getPhoto());
            item.put("b_username", userB.getUsername());
            String result = "平局";
            if ("A".equals(record.getLoser())) {
                result = "B 胜";
            } else if ("B".equals(record.getLoser())) {
                result = "A 胜";
            }
            item.put("result", result);
            item.put("record", record);
            items.add(item);
        }
        resp.put("records", items);
        resp.put("records_count", getAllSize());
        return resp;
    }

    private long getAllSize() {
        Long size = stringRedisTemplate.opsForList().size("records:100");
        if (size == null) {
            return 0;
        }
        return size;
    }

    private List<Record> getRecordByMysql(int page) {
        IPage<Record> recordIPage = new Page<>(page, 10);
        QueryWrapper<Record> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("id");
        List<Record> records = recordMapper.selectPage(recordIPage, queryWrapper).getRecords();
        return records;
    }

    private List<Record> getRecordByMysql() {
        QueryWrapper<Record> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("id");
        List<Record> records = recordMapper.selectList(queryWrapper);
        return records;
    }

    private List<Record> getRecordByRedis(int page) {
        int pageSize = 10;
        int start = (page - 1) * pageSize;
        int end = start + pageSize - 1;
        //记录最多100个record
        List<String> record = stringRedisTemplate.opsForList().range("records:100", start, end);
        //将redis中的数据转换为User对象
        if (record == null || record.size() == 0) {
            return null;
        }
        return JSON.parseArray(record.toString(), Record.class);
    }


    private List<Record> getRecordByRedis() {
        List<String> record = stringRedisTemplate.opsForList().range("records:100", 0, -1);
        //将redis中的数据转换为User对象
        if (record == null || record.size() == 0) {
            return null;
        }
        return JSON.parseArray(record.toString(), Record.class);
    }


    private void SetRecordByRedis(List<Record> recordsByMysql, List<Record> recordsByRedis) {


        //首先找到redis中最新的数据的时间
        if (recordsByRedis == null || recordsByRedis.size() == 0) {
            for (int i = recordsByMysql.size() - 1; i >= 0; i--) {
                stringRedisTemplate.opsForList().leftPush("records:100", JSON.toJSONString(recordsByMysql.get(i)));
            }
            return;
        }
        Instant latestRecord = recordsByRedis.get(0).getCreateTime().toInstant();
        //二分查找records应该从哪个位置开始插入
        int left = 0;
        int right = recordsByMysql.size() - 1;
        //如果left == right，说明records中的所有数据都在redis中，不需要更新
        int ans = -1;
        while (left <= right) {
            int mid = (left + right) / 2;
            if (recordsByMysql.get(mid).getCreateTime().toInstant().isAfter(latestRecord)) {
                System.out.println(recordsByRedis.get(0));
                System.out.println(recordsByMysql.get(mid));
                left = mid + 1;
                ans = Math.max(ans, mid);
            } else {
                right = mid - 1;
            }
        }
        for (int i = ans; i >= 0; i--) {
            stringRedisTemplate.opsForList().leftPush("records:100", JSON.toJSONString(recordsByMysql.get(i)));
        }
        //如果redis中的数据超过100个，就删除多余的数据
        Long size = stringRedisTemplate.opsForList().size("records:100");
        if (size != null && size > 100) {
            stringRedisTemplate.opsForList().trim("records:100", 0, 99);
        }

    }

}
