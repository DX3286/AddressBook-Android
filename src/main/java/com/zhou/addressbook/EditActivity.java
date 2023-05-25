package com.zhou.addressbook;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.app.AlertDialog;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EditActivity extends AppCompatActivity {
    SQLiteDatabase db = MainActivity.db;

    private boolean picChanged = false;
    private boolean defaultAvatar = false;
    final private int REQUEST_CODE_CHOOSE_AVATAR = 99;
    final private int REQUEST_CODE_CAM_AVATAR = 199;

    DatePicker myDatePicker;
    ImageButton iBtn_confirm, iBtn_delete, iBtn_avatar;
    EditText ET_name, ET_age, ET_birth, ET_mobile, ET_home, ET_address, ET_mail, ET_web;
    String oldName, oldAge, oldBirth, oldMobile, oldHome, oldAddress, oldMail, oldWeb;
    String lastChange; // Used By Avatar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        // Toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.edit_Toolbar);

        iBtn_confirm = findViewById(R.id.editBtn_confirm); iBtn_delete = findViewById(R.id.editBtn_delete); iBtn_avatar = findViewById(R.id.editBtn_avatar);
        ET_name = findViewById(R.id.editET_name); ET_age = findViewById(R.id.editET_age); ET_birth = findViewById(R.id.editET_birth);
        ET_mobile = findViewById(R.id.editET_mobile); ET_home = findViewById(R.id.editET_home); ET_address = findViewById(R.id.editET_address);
        ET_mail = findViewById(R.id.editET_mail); ET_web = findViewById(R.id.editET_web);

        // Load Avatar
        lastChange = getIntent().getStringExtra("_lastchange");
        Bitmap mybmp = ToolMethod.LoadPicture(this, lastChange);
        if (mybmp != null){
            iBtn_avatar.setImageBitmap(mybmp);
        }
        // Set Text
        mySetText(ET_name, "_name");
        mySetText(ET_age, "_age");
        mySetText(ET_birth, "_birth");
        mySetText(ET_mobile, "_mobile");
        mySetText(ET_home, "_home");
        mySetText(ET_address, "_address");
        mySetText(ET_mail, "_mail");
        mySetText(ET_web, "_web");
        mySetOldText(); // Save old text before edit

        //View decorv = getWindow().getDecorView();
        //int uioptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        //decorv.setSystemUiVisibility(uioptions);

        myToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ET_birth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final View picker = LayoutInflater.from(EditActivity.this).inflate(R.layout.datepicker, null);
                myDatePicker = picker.findViewById(R.id.datepicker);
                mySetDate(myDatePicker);

                AlertDialog dialog;
                AlertDialog.Builder  builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle("選擇生日");
                builder.setView(picker);
                builder.setPositiveButton("確定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ET_birth.setText(myDatePicker.getYear() + "/" + (myDatePicker.getMonth() + 1) + "/" + myDatePicker.getDayOfMonth());
                        ET_age.setText(ToolMethod.getAge(myDatePicker));
                    }
                });
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                dialog = builder.create();
                dialog.show();
            }
        });

        iBtn_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirm_update();
            }
        });
        iBtn_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteRecord();
            }
        });
        iBtn_avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu pMenu = new PopupMenu(EditActivity.this, v);
                pMenu.inflate(R.menu.avatar_menu);
                pMenu.show();

                pMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()){
                            case R.id.avatarMenu_choose:
                                Intent myIntent = new Intent();
                                myIntent.setType("image/*");
                                myIntent.setAction(Intent.ACTION_GET_CONTENT);
                                startActivityForResult(myIntent, REQUEST_CODE_CHOOSE_AVATAR);
                                break;
                            case R.id.avatarMenu_photo:
                                // Permission
                                if (ContextCompat.checkSelfPermission(EditActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                                {
                                    requestPermissions(new String[]{Manifest.permission.CAMERA}, MainActivity.REQUEST_PERMISSION_CAMERA_CODE);
                                }else{
                                    Intent intentCapture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                    startActivityForResult(intentCapture, REQUEST_CODE_CAM_AVATAR);
                                }
                                break;
                            case R.id.avatarMenu_clear:
                                iBtn_avatar.setImageResource(R.drawable.avatar);
                                picChanged = false; defaultAvatar = true;
                                break;
                        }
                        return false;
                    }
                });
            }
        });
    }

    // Permission Request Stuff
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MainActivity.REQUEST_PERMISSION_CAMERA_CODE){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"取得存取相機權限", Toast.LENGTH_SHORT).show();
                Intent intentCapture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intentCapture, REQUEST_CODE_CAM_AVATAR);
            }else{
                Toast.makeText(this,"存取相機權限遭拒", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void mySetText(EditText et, String key){
        et.setText(getIntent().getStringExtra(key));
    }

    private void mySetOldText(){
        oldName = ET_name.getText().toString();
        //oldAge = ET_age.getText().toString();
        //oldBirth = ET_birth.getText().toString();
        oldMobile = ET_mobile.getText().toString();
        //oldHome = ET_home.getText().toString();
        //oldAddress = ET_address.getText().toString();
        //oldMail = ET_mail.getText().toString();
        //oldWeb = ET_web.getText().toString();
    }

    private void mySetDate(DatePicker d){
        if (ET_birth.getText().toString().trim().equals("")){
            return;
        }
        String[] tmp = ET_birth.getText().toString().trim().split("/");
        d.updateDate(Integer.parseInt(tmp[0]), Integer.parseInt(tmp[1]) - 1, Integer.parseInt(tmp[2]));
    }

    private void confirm_update(){
        SimpleDateFormat simpledate = new SimpleDateFormat("yyyy_MMdd_HHmm_ss");
        String changeTime = simpledate.format(new Date());
        if (picChanged){
            ToolMethod.SavePicture(getApplicationContext(), changeTime, iBtn_avatar);
            ToolMethod.deletePicture(getApplicationContext(), lastChange);
        }else{
            changeTime = lastChange;
        }
        if (defaultAvatar){
            changeTime = MainActivity.AVATAR_DEF;
            ToolMethod.deletePicture(getApplicationContext(), lastChange);
        }
        // SQL
        String _name, _age, _birth, _mobile, _home, _address, _mail, _web;
        _name = check(ET_name, oldName);
        _age = "'" + ET_age.getText().toString().trim() + "'";
        _birth = "'" + ET_birth.getText().toString() + "'";
        _mobile = check(ET_mobile, oldMobile);
        _home = "'" + ET_home.getText().toString().trim() + "'";
        _address = "'" + ET_address.getText().toString().trim() + "'";
        _mail = "'" + ET_mail.getText().toString().trim() + "'";
        _web = "'" + ET_web.getText().toString().trim() + "'";

        // SQL Execute
        String sql = "update People set _name = " + _name + ", _age = " + _age + ", _birth = " + _birth + ", _mobile = "
                + _mobile + ", _home = " + _home + ", _address = " + _address + ", _mail = " + _mail + ", _web = " + _web + ", _lastchange = '"
                + changeTime + "' where _name = '" + oldName + "'";
        db.execSQL(sql);
        //Log.d("Debug: ", sql);
        Toast.makeText(this, "資料修改成功", Toast.LENGTH_SHORT).show();
        finish();
    }

    private String check(EditText et, String def){
        String s = et.getText().toString().trim();
        if (s.equals("")){
            return "'" + def + "'";
        }else{
            return "'" + s + "'";
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,Intent data){
        if ((requestCode == REQUEST_CODE_CHOOSE_AVATAR) && data != null){
            picChanged = true; defaultAvatar = false;
            Uri uri = data.getData();
            ContentResolver cr = this.getContentResolver();
            try{
                Bitmap bitmap = BitmapFactory.decodeStream(cr.openInputStream(uri));
                iBtn_avatar.setImageBitmap(ToolMethod.ScalePicture(bitmap));
            }
            catch (FileNotFoundException e){
                e.printStackTrace();
            }
        }
        if (requestCode == REQUEST_CODE_CAM_AVATAR && resultCode == Activity.RESULT_OK){
            picChanged = true; defaultAvatar = false;
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            iBtn_avatar.setImageBitmap(ToolMethod.ScalePicture(photo));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void deleteRecord(){
        AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("確定要刪除這筆資料嗎？");
        builder.setPositiveButton("確定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String sql = "delete from People where _name = '" + oldName + "'";
                db.execSQL(sql);
                Toast.makeText(getApplicationContext(), "資料刪除成功", Toast.LENGTH_SHORT).show();
                ToolMethod.deletePicture(getApplicationContext(), lastChange);
                finish();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        dialog = builder.create();
        dialog.show();
    }
}