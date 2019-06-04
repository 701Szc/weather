package com.example.asus.coolweather.gson;

import com.google.gson.annotations.SerializedName;

import java.util.List;
/*
* 将basic,aqi,now,suggestion,daily_forecast对应的实体类全部创建在一起
* */
public class Weather {
    public String status;
    public Basic basic;
    public AQI aqi;
    public Now now;
    public Suggestion suggestion;
//未来的天气  解析为一个数组
    @SerializedName("daily_forecast")
    public List<Forecast> forecastList;

}
