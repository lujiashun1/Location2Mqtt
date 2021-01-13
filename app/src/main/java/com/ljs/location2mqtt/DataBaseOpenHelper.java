package com.ljs.location2mqtt;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

public class DataBaseOpenHelper extends SQLiteOpenHelper {
    private String TAG="LJSTAG";
    public DataBaseOpenHelper(@Nullable Context context) {
        super(context, Contant.DATABASE, null, Contant.VERSION);

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "onCreate: create");
        String sql="create table "+Contant.TABLENAME+" (url varchar,port integer,id varchar,name varchar,pwd varchar,time integer,topic varchar,mode integer)";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//     switch(oldVersion)
//     {
//         case 1:
//             switch(newVersion)
//             {
//                 case 2:
//                     break;
//                 default:
//                     break;
//             }
//             break;
//         default:
//             break;
//     }
    }
}
