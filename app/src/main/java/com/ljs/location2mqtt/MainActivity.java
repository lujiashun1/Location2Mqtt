package com.ljs.location2mqtt;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocationClientOption;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private EditText txtUrl,txtPort,txtID,txtName,txtPass,txtTime,txtTopic;
    private Button btnID,btnStart;
    private TextView lblHaconfig;
    String TAG="LJSTAG";
    RadioButton rb_batterySaving,rb_deviceSensors,rb_hightAccuracy;
    private RadioGroup rgLocationMode;
    //AMapLocationClientOption.AMapLocationMode mode;
    int intmode=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
                ActivityCompat.requestPermissions(this, permissions, 1);
            }
        }



        IntentFilter recevierFilter=new IntentFilter();
        recevierFilter.addAction(Intent.ACTION_SCREEN_ON);
        recevierFilter.addAction(Intent.ACTION_SCREEN_OFF);
        recevierFilter.addAction(Intent.ACTION_USER_PRESENT);
        recevierFilter.addAction("com.ljs.ltmservice.start");
        BootBroadcastReceiver receiver=new BootBroadcastReceiver();
        registerReceiver(receiver, recevierFilter);

        txtUrl=(EditText) findViewById(R.id.txtUrl);
        txtPort=(EditText) findViewById(R.id.txtPort);
        txtID=(EditText) findViewById(R.id.txtID);
        txtName=(EditText) findViewById(R.id.txtName);
        txtPass=(EditText) findViewById(R.id.txtPass);
        txtTime=(EditText) findViewById(R.id.txtTime);
        txtTopic=(EditText) findViewById(R.id.txtTopic);
        lblHaconfig=(TextView)findViewById(R.id.lblHaconfig) ;
        rgLocationMode = (RadioGroup) findViewById(R.id.rg_locationMode);
        rgLocationMode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                switch (checkedId) {
                    case R.id.rb_batterySaving :
                        //mode= AMapLocationClientOption.AMapLocationMode.Battery_Saving;
                        intmode=0;
                        break;
                    case R.id.rb_deviceSensors :
                        //mode=AMapLocationClientOption.AMapLocationMode.Device_Sensors;
                        intmode=1;
                        break;
                    case R.id.rb_hightAccuracy :
                        //mode=AMapLocationClientOption.AMapLocationMode.Hight_Accuracy;
                        intmode=2;
                        break;
                    default :
                        break;
                }
            }
        });
        rb_batterySaving=(RadioButton)findViewById(R.id.rb_batterySaving) ;
        rb_deviceSensors=(RadioButton)findViewById(R.id.rb_deviceSensors) ;
        rb_hightAccuracy=(RadioButton)findViewById(R.id.rb_hightAccuracy) ;
        //Log.d(TAG, "ljs: 1");
        DataBaseOpenHelper dataBaseOpenHelper=new DataBaseOpenHelper(this);
        SQLiteDatabase db = dataBaseOpenHelper.getWritableDatabase();
        //Log.d(TAG, "ljs: 2");
        Cursor cursor = db.rawQuery("select * from " + Contant.TABLENAME, null);
        //Log.d(TAG, "ljs: 3");
        boolean blconfig=false;
        if(cursor.getCount()==0){
            String sql="insert into "+Contant.TABLENAME+"(url,port,id,name,pwd,time,topic,mode)values('xxxx.duckdns.org',1883,'ljs123456','name','12345678',60,'location/myphone',0)";
            db.execSQL(sql);
        }
        cursor = db.rawQuery("select * from " + Contant.TABLENAME, null);
        while(cursor.moveToNext())
        {
            txtUrl.setText(cursor.getString(0).toString());
            txtPort.setText(cursor.getInt(1)+"");
            txtID.setText(cursor.getString(2).toString());
            txtName.setText(cursor.getString(3).toString());
            txtPass.setText(cursor.getString(4).toString());
            txtTime.setText(cursor.getInt(5)+"");
            txtTopic.setText(cursor.getString(6).toString());
            intmode=cursor.getInt(7);
            switch(intmode)
            {
                case 0:
                    rb_batterySaving.setChecked(true);
                    break;
                case 1:
                    rb_deviceSensors.setChecked(true);
                    break;
                case 2:
                    rb_hightAccuracy.setChecked(true);
                    break;
                default:
                    break;
            }
        }
        lblHaconfig.setText("冲天枭龙：请在HA configuration.yaml文件中添加一下代码(xxxxxxx替换为你想要的的名字)\ndevice_tracker:\n  - platform: mqtt_json\n    devices:\n       xxxxxxx: '"+txtTopic.getText().toString()+"'");
        cursor.close();
        db.close();
        btnID=(Button)findViewById(R.id.btnID) ;
        btnID.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtID.setText("ljs"+getRandomString(20));
            }
        });
        btnStart=(Button)findViewById(R.id.btnStart) ;
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(txtUrl.getText().toString().trim().equals("")||txtPort.getText().toString().trim().equals("")||txtID.getText().toString().trim().equals("")||txtName.getText().toString().trim().equals("")||txtPass.getText().toString().trim().equals("")||txtTime.getText().toString().trim().equals("")||txtTopic.getText().toString().trim().equals(""))
                {
                    Toast.makeText(MainActivity.this,"填写项不能为空",Toast.LENGTH_LONG).show();
                    return;
                }
                else{
                    SQLiteDatabase db = dataBaseOpenHelper.getWritableDatabase();

                    String sql="update "+Contant.TABLENAME+" set url='"+txtUrl.getText().toString().trim()+"',port="+txtPort.getText().toString().trim()+",id='"+txtID.getText().toString().trim()+"',name='"+txtName.getText().toString().trim()+"',pwd='"+txtPass.getText().toString().trim()+"',time="+txtTime.getText().toString().trim()+",topic='"+txtTopic.getText().toString().trim()+"',mode="+intmode;
                    db.execSQL(sql);
                    lblHaconfig.setText("冲天枭龙：请在HA configuration.yaml文件中添加一下代码(xxxxxxx替换为你想要的的名字)\ndevice_tracker:\n  - platform: mqtt_json\n    devices:\n       xxxxxxx: '"+txtTopic.getText().toString()+"'");
                    ltmService.isfrommain=true;
                    Intent intent=new Intent(MainActivity.this,ltmService.class);
                    startService(intent);
                    //Toast.makeText(MainActivity.this, "启动广播发送完成", Toast.LENGTH_SHORT).show();
                    db.close();
                }

            }
        });

    }

    public  String getRandomString(int length){
        String str="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random=new Random();
        StringBuffer sb=new StringBuffer();
        for(int i=0;i<length;i++){
            int number=random.nextInt(62);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

}