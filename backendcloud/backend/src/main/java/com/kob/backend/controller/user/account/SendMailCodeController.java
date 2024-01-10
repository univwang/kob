package com.kob.backend.controller.user.account;

import com.kob.backend.service.user.account.RegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SendMailCodeController {
    @Autowired
    private RegisterService registerService;

    @PostMapping("/api/user/account/sendMail/")
    public Map<String, String> register(@RequestParam Map<String, String> map) {
        String mail = map.get("mail");
        return registerService.sendMail(mail);
    }
}
