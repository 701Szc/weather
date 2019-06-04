package com.example.asus.coolweather;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.asus.coolweather.db.City;
import com.example.asus.coolweather.db.County;
import com.example.asus.coolweather.db.Province;
import com.example.asus.coolweather.util.HttpUtil;
import com.example.asus.coolweather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
    //    省级别
    public static final int LEVEL_PROVINCE = 0;
    //    市级别
    public static final int LEVEL_CITY = 1;
    //    县级别
    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();
    /*
     * 省列表
     * */
    private List<Province> provinceList;
    /*
     * 市列表
     * */
    private List<City> cityList;
    /*
     * 县列表
     * */
    private List<County> countyList;
    /*
     * 选中的省份
     * */
    private Province selectedProvince;
    /*
     * 选中的城市
     * */
    private City selectedCity;
    /*
     * 当前选中的级别
     * */
    private int currentLevel;
    //  加载选择省市县列表布局
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.choose_area,container,false);
        titleText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button)view.findViewById(R.id.back_button);
        listView = (ListView)view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        return  view;
    }
    //    选中哪一个列表
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currentLevel == LEVEL_PROVINCE){
//                    将选中的省份放入  selectedProvince
                    selectedProvince = provinceList.get(position);
                    queryCities();//查询这个市所有的县

                }else if(currentLevel == LEVEL_CITY){
                    selectedCity = cityList.get(position);
                    queryCounties();
                }else if ( currentLevel == LEVEL_COUNTY){
                    String weatherId = countyList.get(position).getWeatherId();
//               instanceof     判断一个对象是否属于某个类的实例
                    if(getActivity()instanceof MainActivity) {
//                    将天气id传值
                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
                        intent.putExtra("weather_id", weatherId);
                        startActivity(intent);
//                    直接返回到市界面
                        getActivity().finish();
                    }else if(getActivity()instanceof WeatherActivity){
                        WeatherActivity activity = (WeatherActivity)getActivity();
//                            关闭滑动菜单
                        activity.drawerLayout.closeDrawers();
//                            显示下拉刷新进度条
                        activity.swipeRefresh.setRefreshing(true);
//                            请求天气id
                        activity.requestWeather(weatherId);
                    }
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(currentLevel == LEVEL_COUNTY){//当前选中为县则返回到市
                    queryCities();//查询所属的市
                }else if(currentLevel == LEVEL_CITY ){
//                    从此开始加载省级数据
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }
    /*
     * 查询全国所有的省，有限从数据库查询，如果没有查询到再去服务器查询
     * */
    private void queryProvinces(){
        titleText.setText("中国");
//        不可见，且不占据空间。
        backButton.setVisibility(View.GONE);
//        litePal的查询接口
        provinceList = DataSupport.findAll(Province.class);
        if(provinceList.size() > 0){
            dataList.clear();
            for(Province province: provinceList){
                dataList.add(province.getProvinceName());
            }
//            更新listView
            adapter.notifyDataSetChanged();
//            按顺序排列下来
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        }else{
            String address = "http://guolin.tech/api/china";
            queryFromServer(address,"province");
        }
    }
    /*
     * 查询选中的省内所有的市，有限从数据库查询，如果没有查询到再去服务器上查询
     * */
    private void queryCities(){
        titleText.setText(selectedProvince.getProvinceName());
//        可见
        backButton.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceid = ?",String.valueOf(selectedProvince.getId())).find(City.class);
        if(cityList.size() >0){
            dataList.clear();
            for(City city: cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        }else{
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/"+provinceCode;
            queryFromServer(address,"city");
        }
    }
    /*
     * 查询选中市内所有的县，优先从数据库查询，如果没有查询到再去服务器上查询
     * */
    private void queryCounties(){
        titleText.setText(selectedCity.getCityName());
//        可见
        backButton.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityid = ?",String.valueOf(selectedCity.getId())).find(County.class);
        if(countyList.size() > 0){
            dataList.clear();
            for(County county: countyList){
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        }else{
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/"+ provinceCode +"/"+cityCode;
            queryFromServer(address,"county");
        }
    }
    /*
     * 根据传入的地址和类型从服务器上查询省市县数据
     * */
    private void queryFromServer(String address,final String type){
        showProgressDialog();

//        向服务器发送请求
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if("province".equals(type)){
                    result = Utility.handleProvinceResponse(responseText);
                }else if("city".equals(type)){
                    result = Utility.handleCityResponse(responseText,selectedProvince.getId());
                }else if("county".equals(type)){
                    result = Utility.handleCountyResponse(responseText,selectedCity.getId());
                }
                if(result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if("province".equals(type)){
                                queryProvinces();
                            } else if ("city".equals(type)){
                                queryCities();
                            } else if("county".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }
            }
            @Override
            public void onFailure(Call call, IOException e) {
//                通过runOnUiThread()方法回到主线程处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_LONG).show();
                    }
                });
            }


        });
    }
    /*
     *显示进度对话框
     * */
    private void showProgressDialog(){
        if(progressDialog == null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载中...");
//            dialog弹出后会点击屏幕，dialog不消失；点击物理返回键dialog消失
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    /*
     * 关闭进度对话框
     * */
    private void closeProgressDialog(){
        if(progressDialog != null){
            progressDialog.dismiss();
        }
    }

}
