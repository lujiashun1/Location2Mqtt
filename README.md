# Location2Mqtt
## android手机获取位置信息并发送到MQTT服务器

### 1、获取 [高德地图key](https://lbs.amap.com/api/android-location-sdk/guide/create-project/get-key) ( android应用)包名：**com.ljs.location2mqtt**，申请完替换\app\src\main\AndroidManifest.xml文件下面节点中的key,如果失败参照 [本链接](https://blog.csdn.net/m0_37471638/article/details/76849958) 解决  

<meta-data
            android:name="com.amap.api.v2.apikey"
            android:value="key" />

### 2、编译生成apk安装到自己的手机上或者下载release文件安装

### 3、配置app



<img src="C:\Users\Administrator\Desktop\IMG_20210202_113646.png" alt="IMG_20210202_113646" style="zoom: 33%;" />

### 4、HA配置：

- 根据APP下方提示在device_tracker下增加相应的代码

- sensor下增加如下代码可显示电量及地址、速度等信息

   \- platform: mqtt
    state_topic: "location/myphone"
    name: "Battery Level"
    unit_of_measurement: "%"
    value_template: '{{ value_json.battery }}'
   \- platform: mqtt
    state_topic: "location/myphone"
    name: "Location Info"
    value_template: '{{ value_json.info.address }}'
    json_attributes_topic: "location/myphone"
    json_attributes_template: "{{ value_json.info | tojson }}"

  QQ截图![20210202114557](C:\Users\Administrator\Desktop\QQ截图20210202114557.png)