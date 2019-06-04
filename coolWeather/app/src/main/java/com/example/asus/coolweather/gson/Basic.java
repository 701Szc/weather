package com.example.asus.coolweather.gson;

import com.google.gson.annotations.SerializedName;
//使用gson解析json对象
//解析basic中的具体内容
public class Basic {
//    使用Gson解析的时候就会将city对应的值赋值到cityName属性上
//    解决了java对象里属性名跟json里字段名不匹配的情况了
    @SerializedName("city")
    public String cityName;

    @SerializedName("id")
    public String weatherId;

    public Update update;

    public class Update{
        @SerializedName("loc")
        public String updateTime;
    }

}
