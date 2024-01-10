package com.kob.backend.service.user.account;

import java.util.Map;

public interface RegisterService {
    public Map<String, String> register(String username, String password, String confirmedPassword, String photo, String mail, String mailCode);

    Map<String, String> sendMail(String mail);
}
