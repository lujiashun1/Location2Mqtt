# Location2Mqtt
# android手机获取位置信息并发送到MQTT服务器

## 方式

1. ### 自己修改编译：获取 [高德地图key](https://lbs.amap.com/api/android-location-sdk/guide/create-project/get-key) ( android应用)包名：**com.ljs.location2mqtt**，申请完替换\app\src\main\AndroidManifest.xml文件下面节点中的key,如果失败参照 [本链接](https://blog.csdn.net/m0_37471638/article/details/76849958) 解决  ，然后编译生成自己的apk安装

   ```xml
   <meta-data
               android:name="com.amap.api.v2.apikey"
               android:value="key" />
   ```

   

2. ### 直接下载release中的apk文件安装



## 配置

1. ### 根据自己的mqtt服务器及定位需求配置app，需要权限（定位总是允许、自启动、系统设置）

   ![IMG_20210202_113646](https://i.loli.net/2021/02/02/CuUgjIRVZNSony2.png)

2. HA配置

   - device_tracker下增加如下代码（根据app提示修改）

     ```yaml
       - platform: mqtt_json
         devices:
            myphone: 'location/myphone'
     ```

     ![QQ截图20210202114557](https://i.loli.net/2021/02/02/BKx7Odu42XLtenY.png)

   - sensor下增加如何代码，实现电量及位置信息获取

     ```yaml
       - platform: mqtt
         state_topic: "location/myphone"
         name: "Battery Level"
         unit_of_measurement: "%"
         value_template: '{{ value_json.battery }}'   
       - platform: mqtt
         state_topic: "location/myphone"
         name: "Location Info"
         value_template: '{{ value_json.info.address }}'
         json_attributes_topic: "location/myphone"
         json_attributes_template: "{{ value_json.info | tojson }}"
     ```

     ![QQ截图20210202114657](https://i.loli.net/2021/02/02/xn92mDAQHTlXJNz.png)

### 



