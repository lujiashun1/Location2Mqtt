# Location2Mqtt
android手机获取位置信息并发送到MQTT服务器
1、获取高德地图key(https://lbs.amap.com/api/android-location-sdk/guide/create-project/get-key android应用)包名：com.ljs.location2mqtt，申请完替换\app\src\main\AndroidManifest.xml文件<meta-data
            android:name="com.amap.api.v2.apikey"
            android:value="key" />节点中的key,如果失败参照https://blog.csdn.net/m0_37471638/article/details/76849958解决
2、