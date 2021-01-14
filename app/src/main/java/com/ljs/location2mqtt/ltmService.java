package com.ljs.location2mqtt;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.os.BatteryManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.HashMap;

public class ltmService extends Service {
    public static String HOST = "";
    //服务器内置主题，用来监测当前服务器上连接的客户端数量（$SYS/broker/clients/connected）
    public static String TOPIC1 = "location/myphone";
    private static String clientid = "client15";
    private MqttClient client;
    private MqttConnectOptions options;
    private String userName = "";
    private String passWord = "";
    private String TAG = "ljstag";
    private int time = 0;

    public static boolean isfrommain=false;

    private AMapLocationClient locationClient = null;
    private AMapLocationClientOption locationOption = null;
    private int cnt=0;
    Intent serviceIntent = null;
    boolean isSartLocation = false;
    double PI = 3.14159265358979324;
    int mode=0;
    public ltmService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: ");

        init();
        if(isSartLocation) {
            //如果使用{@link AMapLocationClient#enableBackgroundLocation(int, Notification)}，这段代码可以不要
            if (null != serviceIntent) {
                startService(serviceIntent);
            }

        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: ");
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }




    private void init() {
        if(isfrommain||locationClient==null) {
            DataBaseOpenHelper dataBaseOpenHelper = new DataBaseOpenHelper(this);
            SQLiteDatabase db = dataBaseOpenHelper.getWritableDatabase();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            Cursor cursor = db.rawQuery("select * from " + Contant.TABLENAME, null);
            while (cursor.moveToNext()) {
                HOST = "tcp://" + cursor.getString(0).toString() + ":" + cursor.getInt(1);
                clientid = cursor.getString(2).toString();
                userName = cursor.getString(3).toString();
                passWord = cursor.getString(4).toString();
                time = cursor.getInt(5);
                TOPIC1 = cursor.getString(6).toString();
                mode = cursor.getInt(7);
            }
            cursor.close();
            db.close();
            try {
                client = new MqttClient(HOST, clientid, new MemoryPersistence());
            } catch (MqttException e) {
                e.printStackTrace();
            }
            // MQTT的连接设置
            options = new MqttConnectOptions();
            // 设置是否清空session,这里如果设置为false表示服务器会保留客户端的连接记录，设置为true表示每次连接到服务器都以新的身份连接
            options.setCleanSession(true);
            // 设置连接的用户名
            options.setUserName(userName);
            // 设置连接的密码
            options.setPassword(passWord.toCharArray());
            // 设置超时时间 单位为秒
            options.setConnectionTimeout(10);
            // 设置会话心跳时间 单位为秒 服务器会每隔1.5*20秒的时间向客户端发送个消息判断客户端是否在线，但这个方法并没有重连的机制
            options.setKeepAliveInterval(20);
            // 设置回调
            client.setCallback(new MqttCallback() {
                public void connectionLost(Throwable cause) {
                    //Log.d(TAG, "onCreate: 2");
                    try {
                        client = new MqttClient(HOST, clientid, new MemoryPersistence());
                        client.connect(options);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }

                }

                public void deliveryComplete(IMqttDeliveryToken token) {

                }

                public void messageArrived(String topic, MqttMessage message) throws Exception {

                }
            });
//      MqttTopic topic = client.getTopic(TOPIC1);
//      setWill方法，如果项目中需要知道客户端是否掉线可以调用该方法。设置最终端口的通知消息
//      options.setWill(topic, "close".getBytes(), 2, true);  //遗嘱
            try {
                client.connect(options);
                //Toast.makeText(getApplicationContext(), "连接MQTT服务器成功", Toast.LENGTH_SHORT).show();
            } catch (MqttException e) {
                Toast.makeText(getApplicationContext(), "连接MQTT服务器失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                return;
            }

            serviceIntent = new Intent();
            serviceIntent.setClass(this, LocationForcegroundService.class);
            locationClient = new AMapLocationClient(this.getApplicationContext());
            locationOption = getDefaultOption();
            //设置定位参数
            locationClient.setLocationOption(locationOption);
            // 设置定位监听
            locationClient.setLocationListener(locationListener);

            locationClient.setLocationOption(locationOption);
            // 启动定位
            locationClient.startLocation();
            isSartLocation = true;
            isfrommain=false;
            Toast.makeText(getApplicationContext(), "开始定位", Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * 默认的定位参数
     * @since 2.8.0
     * @author hongming.wang
     *
     */
    private AMapLocationClientOption getDefaultOption(){
        AMapLocationClientOption mOption = new AMapLocationClientOption();
        switch(mode)
        {
            case 0:
                mOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Battery_Saving);
                break;
            case 1:
                mOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Device_Sensors);
                break;
            case 2:
                mOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
                break;
            default:
                break;
        }
        //mOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);//可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
        mOption.setGpsFirst(true);//可选，设置是否gps优先，只在高精度模式下有效。默认关闭
        mOption.setHttpTimeOut(600000);//可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
        mOption.setInterval(time*1000);//可选，设置定位间隔。默认为2秒
        mOption.setNeedAddress(true);//可选，设置是否返回逆地理地址信息。默认是true
        mOption.setOnceLocation(false);//可选，设置是否单次定位。默认是false
        mOption.setOnceLocationLatest(false);//可选，设置是否等待wifi刷新，默认为false.如果设置为true,会自动变为单次定位，持续定位时不要使用
        AMapLocationClientOption.setLocationProtocol(AMapLocationClientOption.AMapLocationProtocol.HTTP);//可选， 设置网络请求的协议。可选HTTP或者HTTPS。默认为HTTP
        mOption.setSensorEnable(true);//可选，设置是否使用传感器。默认是false
        mOption.setWifiScan(true); //可选，设置是否开启wifi扫描。默认为true，如果设置为false会同时停止主动刷新，停止以后完全依赖于系统刷新，定位位置可能存在误差
        mOption.setLocationCacheEnable(true); //可选，设置是否使用缓存定位，默认为true
        return mOption;
    }

    /**
     * 定位监听
     */
    AMapLocationListener locationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation location) {
            Log.d(TAG, "onLocationChanged: -2");
            if (null != location) {
                Log.d(TAG, "onLocationChanged: -1");
                StringBuffer sb = new StringBuffer();
                //errCode等于0代表定位成功，其他的为定位失败，具体的可以参照官网定位错误码说明
                if(location.getErrorCode() == 0){

                    Log.d(TAG, "onLocationChanged: 0");
                    MqttMessage msg = new MqttMessage();
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();
                    float acc = location.getAccuracy();
                    double alt = location.getAltitude();
                    HashMap<String, Double> hm = delta(lat,lon);
                    lat=hm.get("lat");
                    lon=hm.get("lon");
                    BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
                    int battery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                    float speed = location.getSpeed();
                    float bearing = location.getBearing();
                    int satellites = location.getSatellites();
                    String country = location.getCountry();
                    String province = location.getProvince();
                    String city = location.getCity();
                    String cityCode = location.getCityCode();
                    String district = location.getDistrict();
                    String street = location.getStreet();
                    String streetNum = location.getStreetNum();
                    String adCode = location.getAdCode();
                    String address = location.getAddress();
                    String poiName = location.getPoiName();
                    //String str = "{\"latitude\":" + lat + ",\"longitude\":" + lon + ",\"gps_accuracy\":" + acc  + ",\"battery\":" + battery + "}";
                    String str="{    \"latitude\":"+lat+",    \"longitude\":"+lon+",    \"altitude\":"+alt+",    \"gps_accuracy\":"+acc+",    \"battery\":"+battery+",    \"info\":{        \"speed\":"+speed+",        \"bearing\":"+bearing+",        \"satellites\":"+satellites+",        \"country\":\""+country+"\",        \"province\":\""+province+"\",        \"city\":\""+city+"\",        \"citycode\":\""+cityCode+"\",        \"district\":\""+district+"\",        \"adcode\":\""+adCode+"\",\"street\":\""+street+"\", \"streetnum\":\""+streetNum+"\",       \"address\":\""+address+"\",        \"poiname\":\""+poiName+"\"    }}";
                    msg.setPayload(str.getBytes());
                    msg.setQos(0);
                    msg.setRetained(true);
                    try {

                        client.publish(TOPIC1, msg);
                    } catch (MqttException e) {
                        try {
                            client = new MqttClient(HOST, clientid, new MemoryPersistence());
                            client.connect(options);
                        } catch (MqttException ex) {
                            ex.printStackTrace();
                        }
                        //Log.d(TAG, "onCreate: 10");
                        e.printStackTrace();
                    }

//                    sb.append("定位成功" + "\n");
//                    sb.append("定位类型: " + location.getLocationType() + "\n");
//                    sb.append("经    度    : " + location.getLongitude() + "\n");
//                    sb.append("纬    度    : " + location.getLatitude() + "\n");
//                    sb.append("精    度    : " + location.getAccuracy() + "米" + "\n");
//                    sb.append("提供者    : " + location.getProvider() + "\n");
//
//                    sb.append("速    度    : " + location.getSpeed() + "米/秒" + "\n");
//                    sb.append("角    度    : " + location.getBearing() + "\n");
//                    // 获取当前提供定位服务的卫星个数
//                    sb.append("星    数    : " + location.getSatellites() + "\n");
//                    sb.append("国    家    : " + location.getCountry() + "\n");
//                    sb.append("省            : " + location.getProvince() + "\n");
//                    sb.append("市            : " + location.getCity() + "\n");
//                    sb.append("城市编码 : " + location.getCityCode() + "\n");
//                    sb.append("区            : " + location.getDistrict() + "\n");
//                    sb.append("区域 码   : " + location.getAdCode() + "\n");
//                    sb.append("地    址    : " + location.getAddress() + "\n");
//                    sb.append("兴趣点    : " + location.getPoiName() + "\n");
//                    //定位完成的时间
//                    //sb.append("定位时间: " + formatUTC(location.getTime(), "yyyy-MM-dd HH:mm:ss") + "\n");
//                    Toast.makeText(getApplicationContext(),sb.toString(),Toast.LENGTH_LONG).show();
                } else {

                    //定位失败
                    sb.append("定位失败" + "\n");
                    sb.append("错误码:" + location.getErrorCode() + "\n");
                    sb.append("错误信息:" + location.getErrorInfo() + "\n");
                    sb.append("错误描述:" + location.getLocationDetail() + "\n");
                    Log.d(TAG, "onLocationChanged: "+sb.toString());
                    Toast.makeText(getApplicationContext(),sb.toString(),Toast.LENGTH_LONG).show();
                }
//                sb.append("***定位质量报告***").append("\n");
//                sb.append("* WIFI开关：").append(location.getLocationQualityReport().isWifiAble() ? "开启":"关闭").append("\n");
//                sb.append("* GPS状态：").append(getGPSStatusString(location.getLocationQualityReport().getGPSStatus())).append("\n");
//                sb.append("* GPS星数：").append(location.getLocationQualityReport().getGPSSatellites()).append("\n");
//                sb.append("****************").append("\n");
//                //定位之后的回调时间
//                sb.append("回调时间: " + formatUTC(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss") + "\n");
//
//                //解析定位结果，
//                String result = sb.toString();
//                cnt++;
                //tvResult.setText(result+"\n更新次数："+cnt);
                //Toast.makeText(getApplicationContext(),result,Toast.LENGTH_LONG).show();
            } else {
                //tvResult.setText("定位失败，loc is null");
            }
        }
    };

    /**
     * 停止定位
     *
     * @since 2.8.0
     * @author hongming.wang
     *
     */
    private void stopLocation(){
        // 停止定位
        locationClient.stopLocation();
        isSartLocation = false;
    }
    /**
     * 销毁定位
     *
     * @since 2.8.0
     * @author hongming.wang
     *
     */
    private void destroyLocation(){
        if (null != locationClient) {
            /**
             * 如果AMapLocationClient是在当前Activity实例化的，
             * 在Activity的onDestroy中一定要执行AMapLocationClient的onDestroy
             */
            locationClient.onDestroy();
            locationClient = null;
            locationOption = null;
        }
        isSartLocation = false;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate:");
        IntentFilter recevierFilter=new IntentFilter();
        recevierFilter.addAction(Intent.ACTION_SCREEN_ON);
        recevierFilter.addAction(Intent.ACTION_SCREEN_OFF);
        recevierFilter.addAction(Intent.ACTION_USER_PRESENT);
        recevierFilter.addAction("com.ljs.ltmservice.start");
        BootBroadcastReceiver receiver=new BootBroadcastReceiver();
        registerReceiver(receiver, recevierFilter);
        //init();
        super.onCreate();
    }

    @Override
    public void onDestroy() {   //com.ljs.ltmservice.start
        Log.d(TAG, "onDestroy: ");
        if(null != serviceIntent){
            stopService(serviceIntent);
        }
        stopForeground(true);
        Intent  intent=new Intent("com.ljs.ltmservice.start");
        sendBroadcast(intent);
        super.onDestroy();
    }


    public HashMap<String, Double> delta(double lat, double lon) {
        double a = 6378245.0;//克拉索夫斯基椭球参数长半轴a
        double ee = 0.00669342162296594323;//克拉索夫斯基椭球参数第一偏心率平方
        double dLat = this.transformLat(lon - 105.0, lat - 35.0);
        double dLon = this.transformLon(lon - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * this.PI;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * this.PI);
        dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * this.PI);

        HashMap<String, Double> hm = new HashMap<String, Double>();
        hm.put("lat",lat - dLat);
        hm.put("lon",lon - dLon);

        return hm;
    }
    //转换经度
    public double transformLon(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * this.PI) + 20.0 * Math.sin(2.0 * x * this.PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * this.PI) + 40.0 * Math.sin(x / 3.0 * this.PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * this.PI) + 300.0 * Math.sin(x / 30.0 * this.PI)) * 2.0 / 3.0;
        return ret;
    }
    //转换纬度
    public double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * this.PI) + 20.0 * Math.sin(2.0 * x * this.PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * this.PI) + 40.0 * Math.sin(y / 3.0 * this.PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * this.PI) + 320 * Math.sin(y * this.PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

}