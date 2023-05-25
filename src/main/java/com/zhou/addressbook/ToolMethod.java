package com.zhou.addressbook;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.DatePicker;
import android.widget.ImageButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class ToolMethod {

    public static Bitmap LoadPicture(Context context, String id){
        if (id.equals(MainActivity.AVATAR_DEF)){
            return null;
        }
        ContextWrapper cw = new ContextWrapper(context);
        File myPath = cw.getDir("book", Context.MODE_PRIVATE);
        try {
            File myFile = new File(myPath, id + ".png");
            Bitmap mybmp = BitmapFactory.decodeStream(new FileInputStream(myFile));
            return mybmp;
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static void SavePicture(Context context, String filename, ImageButton iBtn) {
        ContextWrapper cw = new ContextWrapper(context);
        File myPath = cw.getDir("book", Context.MODE_PRIVATE);
        File myFile = new File(myPath, filename + ".png");
        //Log.d("Debug: ", myFile.toString());

        FileOutputStream outStream = null;
        try {
            outStream = new FileOutputStream(myFile);
            Bitmap pic = ((BitmapDrawable) iBtn.getDrawable()).getBitmap();
            pic.compress(Bitmap.CompressFormat.PNG, 100, outStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                outStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void deletePicture(Context context, String filename) {
        if (filename.equals(MainActivity.AVATAR_DEF)){
            return;
        }
        ContextWrapper cw = new ContextWrapper(context);
        File myPath = cw.getDir("book", Context.MODE_PRIVATE);
        File myFile = new File(myPath, filename + ".png");
        myFile.delete();
        Log.d("Debug Delete: ", myFile.toString());
    }

    public static Bitmap ScalePicture(Bitmap bitmap){
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        int newHeight = 240;
        int newWidth = 240;
        float scaleHeight = (float) newHeight / height;
        float scaleWidth = (float) newWidth / width;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap scaleBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        return scaleBitmap;
    }

    public static String getAge(DatePicker d){
        int _year = d.getYear(); int _month = d.getMonth() + 1; int _day = d.getDayOfMonth();
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - _year;
        if (today.get(Calendar.MONTH) < _month){
            return Integer.toString(age - 1);
        }
        if (today.get(Calendar.MONTH) > _month){
            return Integer.toString(age);
        }
        if (_day < today.get(Calendar.DAY_OF_MONTH)){
            return Integer.toString(age - 1);
        }
        return Integer.toString(age);
    }
}
