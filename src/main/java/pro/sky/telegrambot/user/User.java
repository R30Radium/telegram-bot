package pro.sky.telegrambot.user;

import java.util.ArrayList;
import java.util.List;

public class User {

    private String login;
    private String password;

    User(String login, String password) {
        this.login  = login;
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }


}