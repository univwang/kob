package com.kob.backend.service.impl.pk;

import com.kob.backend.consumer.WebSocketServer;
import com.kob.backend.service.pk.StartGameService;
import org.springframework.stereotype.Service;

@Service
public class StartGameServiceImpl implements StartGameService {
    @Override
    public String startGame(Integer aId, Integer bId, Integer aBotId, Integer bBotId) {
//        System.out.println("start game" + aId + " " + bId);
        WebSocketServer.startGame(aId, bId, aBotId, bBotId);
        return "start game";
    }
}
