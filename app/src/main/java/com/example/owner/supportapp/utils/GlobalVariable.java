package com.example.owner.supportapp.utils;

import android.app.Application;

/**
 * Created by Owner on 7/18/2016.
 */
public class GlobalVariable extends Application {
    public String token;
    public String getToken(){
        return token;
    }
    public void setToken(String token){
        this.token=token;
    }
}
