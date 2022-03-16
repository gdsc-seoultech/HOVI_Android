package com.example.hovi_android;

import com.google.gson.annotations.SerializedName;

public class Post {
    @SerializedName("id")
    private String id;

    @SerializedName("action1")
    private String action1;

    @SerializedName("action2")
    private String action2;

    public String getId(){
        return id;
    }

    public void setId(String id){
        this.id = id;
    }

    public String getAction1(){
        return action1;
    }

    public void setAction1(String action1){
        this.action1 = action1;
    }

    public String getAction2(){
        return action2;
    }

    public void setAction2(String action2){
        this.action2 = action2;
    }
}
