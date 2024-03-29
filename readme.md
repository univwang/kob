# 在线游戏对战平台——贪吃蛇

## 项目介绍

基于springboot微服务+websocket+redis+vue3实现贪吃蛇游戏的对战平台。
用户可手动或者添加bot与对手匹配对战,能够查看对战记录以及排行榜信息,还实现了游戏大厅聊天功能。
## 项目特色

- 支持双人在线匹配对战
- 支持人人、人机对战（添加自己的AI去匹配对战）
- 排行榜功能
- 对战记录回放功能
- 聊天大厅

## 相关技术

1. 采用websocket实现服务器和前端实时交互，完成匹配和对战
2. 采用微服务架构，包括用户后端、匹配系统、Bot运行系统。
3. 采用生产者-消费者模型和java并发编程构建bot运行缓存区。
4. 采用redis缓存对战记录、排行榜等数据。
5. 使用redisson分布式锁、redis异步更新、redis过期时间等实现redis的数据更新,防止缓存雪崩。

## 系统架构

