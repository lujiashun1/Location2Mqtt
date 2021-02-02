# Location2Mqtt
## android手机获取位置信息并发送到MQTT服务器

### 1、获取 [高德地图key](https://lbs.amap.com/api/android-location-sdk/guide/create-project/get-key) ( android应用)包名：**com.ljs.location2mqtt**，申请完替换\app\src\main\AndroidManifest.xml文件下面节点中的key,如果失败参照 [本链接](https://blog.csdn.net/m0_37471638/article/details/76849958) 解决  

<meta-data
            android:name="com.amap.api.v2.apikey"
            android:value="key" />

### 2、编译生成apk安装到自己的手机上或者下载release文件安装

### 3、配置app



<img src="https://raw.githubusercontent.com/lujiashun1/picture/master/img/IMG_20210202_113646.png?token=ABQZ77523ZHGHPX6QCQZ46DADDPX4" alt="IMG_20210202_113646" style="zoom:25%;" />

### 4、HA配置：

- 根据APP下方提示在device_tracker下增加相应的代码

  ```yaml
    - platform: mqtt_json
      devices:
         myphone: 'location/myphone'
  ```

  ![QQ截图20210202114557](https://raw.githubusercontent.com/lujiashun1/picture/master/img/QQ%E6%88%AA%E5%9B%BE20210202114557.png?token=ABQZ772THRRSDCDEDZSP5JTADDPV2)

- sensor下增加如下代码可显示电量及地址、速度等信息

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

  ![QQ截图20210202114657](https://raw.githubusercontent.com/lujiashun1/picture/master/img/QQ%E6%88%AA%E5%9B%BE20210202114657.png?token=ABQZ774ODDY2JYSDNJDMY33ADDPWW)