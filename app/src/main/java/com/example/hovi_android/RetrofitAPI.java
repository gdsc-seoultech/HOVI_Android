package com.example.hovi_android;

import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RetrofitAPI{
//    @GET("user/{userId}")
//    Call<Boolean> userData(@Path("userId") String userId);

    @GET("user/{userId}")
    Call<Check> checkData(@Path("userId") String userId);

    @POST("set/{userId}")
    Call<User> setUser(@Path("userId") String userId);

    @POST("update")
    Call<UpdateResponse> updateData(@Body Update update);

    @GET("user_action/{userId}")
    Call<Action> getActions(@Path("userId") String userId);


//
//    @POST("user")
//    Call<User> userData(@Body User user);

}
