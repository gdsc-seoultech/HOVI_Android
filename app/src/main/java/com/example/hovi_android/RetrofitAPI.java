package com.example.hovi_android;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface RetrofitAPI{
//    @GET("set")
//    Call<List<Post>> getData(@Query("id") String id);


    @POST("set")
    Call<Post> postData(@Body Post post);
}
