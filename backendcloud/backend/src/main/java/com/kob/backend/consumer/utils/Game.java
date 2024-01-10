package com.kob.backend.consumer.utils;

import com.alibaba.fastjson.JSONObject;
import com.kob.backend.consumer.WebSocketServer;
import com.kob.backend.pojo.Bot;
import com.kob.backend.pojo.Record;
import com.kob.backend.pojo.User;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public class Game extends Thread {
    final private Integer rows;
    final private Integer cols;
    final private Integer inner_walls_count;
    final private int[][] g;
    final private static int[] dx = {-1, 0, 1, 0}, dy = {0, 1, 0, -1};

    private final Player playerA, playerB ;


    //两个线程同时读写
    private Integer nextStepA = null;
    private Integer nextStepB = null;

    private ReentrantLock lock = new ReentrantLock();

    private final static String addBotUrl = "http://127.0.0.1:3002/bot/add/";

    private String status = "playing";
    private String loser = "";
    public Game(Integer rows, Integer cols, Integer inner_walls_count, Integer idA, Integer idB, Bot botA, Bot botB) {

        Integer botIdA = -1, botIdB = -1;
        String botCodeA = "", botCodeB = "";
        if(botA != null) {
            botIdA = botA.getId();
            botCodeA = botA.getContent();
        }
        if(botB != null) {
            botIdB = botB.getId();
            botCodeB = botB.getContent();
        }
        playerA = new Player(idA, rows - 2, 1, new ArrayList<>(), botIdA, botCodeA);
        playerB = new Player(idB, 1, cols - 2, new ArrayList<>(), botIdB, botCodeB);
        this.rows = rows;
        this.cols = cols;
        this.inner_walls_count = inner_walls_count;
        this.g = new int[rows][cols];
    }
    public Player getPlayerA() {
        return playerA;
    }
    public Player getPlayerB() {
        return playerB;
    }

    public void setNextStepA(Integer nextStepA) {
        lock.lock();
        try {
            this.nextStepA = nextStepA;
        } finally {
            lock.unlock();
        }
    }
    public void setNextStepB(Integer nextStepB) {
        lock.lock();
        try {
            this.nextStepB = nextStepB;
        } finally {
            lock.unlock();
        }
    }

    public int[][] getG() {
        return g;
    }

    private boolean check_connectivity(int sx, int sy, int tx, int ty) {
        if (sx == tx && sy == ty) return true;
        g[sx][sy] = 1;

        for (int i = 0; i < 4; i ++ ) {
            int x = sx + dx[i], y = sy + dy[i];
            if (x >= 0 && x < this.rows && y >= 0 && y < this.cols && g[x][y] == 0) {
                if (check_connectivity(x, y, tx, ty)) {
                    g[sx][sy] = 0;
                    return true;
                }
            }
        }

        g[sx][sy] = 0;
        return false;
    }

    private boolean draw() {  // 画地图
        for (int i = 0; i < this.rows; i ++ ) {
            for (int j = 0; j < this.cols; j ++ ) {
                g[i][j] = 0;
            }
        }

        for (int r = 0; r < this.rows; r ++ ) {
            g[r][0] = g[r][this.cols - 1] = 1;
        }
        for (int c = 0; c < this.cols; c ++ ) {
            g[0][c] = g[this.rows - 1][c] = 1;
        }

        Random random = new Random();
        for (int i = 0; i < this.inner_walls_count / 2; i ++ ) {
            for (int j = 0; j < 1000; j ++ ) {
                int r = random.nextInt(this.rows);
                int c = random.nextInt(this.cols);

                if (g[r][c] == 1 || g[this.rows - 1 - r][this.cols - 1 - c] == 1)
                    continue;
                if (r == this.rows - 2 && c == 1 || r == 1 && c == this.cols - 2)
                    continue;

                g[r][c] = g[this.rows - 1 - r][this.cols - 1 - c] = 1;
                break;
            }
        }

        return check_connectivity(this.rows - 2, 1, 1, this.cols - 2);
    }
    private void updateUserRating(Player player, Integer rating) {
        User user = WebSocketServer.userMapper.selectById(player.getId());
        user.setRating(rating);
        WebSocketServer.userMapper.updateById(user);
    }
    private void saveToDatabase() {
        Integer ratingA = WebSocketServer.userMapper.selectById(playerA.getId()).getRating();
        Integer ratingB = WebSocketServer.userMapper.selectById(playerB.getId()).getRating();
        if (loser.equals("A")) {
            ratingA -= 2;
            ratingB += 5;
        } else if (loser.equals("B")) {
            ratingA += 5;
            ratingB -= 2;
        }
        updateUserRating(playerA, ratingA);
        updateUserRating(playerB, ratingB);
        Record record = new Record(
                null,
                playerA.getId(),
                playerA.getSx(),
                playerA.getSy(),
                playerB.getId(),
                playerB.getSx(),
                playerB.getSy(),
                playerA.getStepsString(),
                playerB.getStepsString(),
                getMapString(),
                loser,
                new Date()
        );
        WebSocketServer.recordMapper.insert(record);
    }

    private String getMapString() {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < rows; i ++ ) {
            for(int j = 0; j < cols; j ++ ) {
                sb.append(g[i][j]);
            }
        }
        return sb.toString();
    }

    public void createMap() {
        for (int i = 0; i < 1000; i ++ ) {
            if (draw())
                break;
        }
    }

    private String getInput(Player player) {
        //地图信息#
        Player me, you;
        if(playerA.getId().equals(player.getId())) {
            me = playerA;
            you = playerB;
        } else {
            me = playerB;
            you = playerA;
        }
        return getMapString() + "#"
                + me.getSx() + "#"
                + me.getSy() + "#("
                + me.getStepsString() + ")#"
                + you.getSx() + "#"
                + you.getSy() + "#("
                + you.getStepsString() + ")#";
    }
    private void sendBotCode(Player player) {
        if (player.getBotId() == -1) {
            return;
        }
        MultiValueMap<String ,String> data = new LinkedMultiValueMap<>();
        data.add("user_id", String.valueOf(player.getId()));
        data.add("bot_code", player.getBotCode());
        data.add("input", getInput(player));
        WebSocketServer.restTemplate.postForObject(addBotUrl, data, String.class);
    }

    private boolean nextStep() {
        //等待两名玩家的下一步操作


        //每一步200ms，前端画的比较慢，所以这里等待200ms
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        sendBotCode(playerA);
        sendBotCode(playerB);

        for(int i = 0; i < 50; i ++ ) {
            try {
                Thread.sleep(100);
                //判断
                lock.lock();
                try {
                    if(nextStepA != null && nextStepB != null) {
                        playerA.getSteps().add(nextStepA);
                        playerB.getSteps().add(nextStepB);
                        return true;
                    }
                } finally {
                    lock.unlock();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }
    private boolean check_valid(List<Cell> cellsA, List<Cell> cellsB) {
        int n = cellsA.size();
        Cell cell = cellsA.get(n - 1);
        if(g[cell.x][cell.y] == 1) return false;


        for (int i = 0; i < n - 1; i ++ ) {
            if(cellsA.get(i).x == cell.x && cellsA.get(i).y == cell.y) {
                return false;
            }
        }

        for (int i = 0; i < n - 1; i ++ ) {
            if(cellsB.get(i).x == cell.x && cellsB.get(i).y == cell.y) {
                return false;
            }
        }
        return true;
    }
    private void judge() {
        //判断合法
        List<Cell> cellsA = playerA.getCells();
        List<Cell> cellsB = playerB.getCells();
        //判断合法性
        boolean validA = check_valid(cellsA, cellsB);
        boolean validB = check_valid(cellsB, cellsA);

        if(!validA || !validB) {
            //有人违规
            status = "finished";
            if(!validA && !validB) {
                loser = "all";
            } else if(!validA) {
                loser = "A";
            } else {
                loser = "B";
            }
        }
    }

    private void sendMove() {
        //发送移动
        lock.lock();
        try {
            JSONObject resp = new JSONObject();
            resp.put("event", "move");
            resp.put("a_direction", nextStepA);
            resp.put("b_direction", nextStepB);
            //清空
            nextStepA = nextStepB = null;

            sendAllMessage(resp.toJSONString());
        } finally {
            lock.unlock();
        }
    }
    private void sendResult() {
        //发送结果
        JSONObject result = new JSONObject();
        result.put("event", "result");
        result.put("loser", loser);
        saveToDatabase();
        sendAllMessage(result.toJSONString());
    }

    private void sendAllMessage(String message) {
        //发送所有信息
        if(WebSocketServer.users.get(playerA.getId()) != null)
            WebSocketServer.users.get(playerA.getId()).sendMessage(message);
        if(WebSocketServer.users.get(playerB.getId()) != null)
            WebSocketServer.users.get(playerB.getId()).sendMessage(message);
    }
    @Override
    public void run() {
        //一回合一回合的操作
        for(int i = 0; i < 1000; i ++ ) {
            if(nextStep()) {
                judge();
                if(status.equals("playing")) {
                    //广播两名玩家的输入
                    sendMove();
                } else {
                    sendResult();
                    break;
                }
            } else {
                status = "finished";
                lock.lock();
                try {
                    if(nextStepA == null && nextStepB == null) {
                        loser = "all";
                    } else if(nextStepA == null) {
                        loser = "A";
                    } else {
                        loser = "B";
                    }
                } finally {
                    lock.unlock();
                }
                sendResult();
                break;
            }
        }
        super.run();
    }
}
