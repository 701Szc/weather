package com.example.asus.coolweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.example.asus.coolweather.WeatherActivity;
import com.example.asus.coolweather.gson.Weather;
import com.example.asus.coolweather.util.HttpUtil;
import com.example.asus.coolweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AutoUpdateService extends Service {
    public AutoUpdateService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;

    }
//    定时
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateBingPic();
        updateWeather();
        AlarmManager manager = (AlarmManager)getSystemService(ALARM_SERVICE);
        int anHour = 8*60*60*1000;//这是8小时的毫秒数
        long tiggerAtTime = SystemClock.elapsedRealtime() + anHour;
        Intent i = new Intent(this,AutoUpdateService.class);
        PendingIntent pi = PendingIntent.getService(this,0,i,0);
        manager.cancel(pi);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,tiggerAtTime,pi);
        return super.onStartCommand(intent, flags, startId);
    }
//更新天气
    private void updateWeather(){
//        创建sp用于更新数据
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
//        若没有  默认为null
        String weatherString = prefs.getString("weather",null);
        if(weatherString != null){
//            有缓存时直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            String weatherId = weather.basic.weatherId;
            String watherUrl = "http://guolin.tech/api/weather?cityid="+weatherId + "&key=f6d2d9d1cf074d4c8d304c766b439964";
//            传入网址 并且回调数据
            HttpUtil.sendOkHttpRequest(watherUrl, new Callback() {
//                若连接失败
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }
//              若连接成功
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseText = response.body().string();
                    Weather weather = Utility.handleWeatherResponse(responseText);
//                    若连接数据获取成功
                    if(weather != null && "ok".equals(weather.status)){
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                        editor.putString("weather",responseText);
                        editor.apply();
                    }
                }
            });
        }
    }
    /*
    *更新必应每日一图
    * */
    private void updateBingPic(){
        //        获取图片地址
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
            }
        });
    }
}
