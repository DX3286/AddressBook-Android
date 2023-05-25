package com.zhou.addressbook;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.nfc.Tag;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AddActivity extends AppCompatActivity {
    SQLiteDatabase db = MainActivity.db;

    private boolean picChanged = false;
    private boolean insertHaveEmpty = false;
    final private int REQUEST_CODE_CHOOSE_AVATAR = 99;
    final private int REQUEST_CODE_CAM_AVATAR = 199;

    DatePicker myDatePicker;
    ImageButton iBtn_confirm, iBtn_avatar;
    EditText ET_name, ET_birth, ET_age, ET_mobile, ET_home, ET_address, ET_mail, ET_web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        // Toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.add_Toolbar);

        ET_name = findViewById(R.id.addET_name); ET_birth = findViewById(R.id.addET_birth); ET_age = findViewById(R.id.addET_age);
        ET_mobile = findViewById(R.id.addET_mobile); ET_home = findViewById(R.id.addET_home); ET_address = findViewById(R.id.addET_address);
        ET_mail = findViewById(R.id.addET_mail); ET_web = findViewById(R.id.addET_web);
        iBtn_confirm = findViewById(R.id.addBtn_confirm); iBtn_avatar = findViewById(R.id.addBtn_avatar);

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
                final View picker = LayoutInflater.from(AddActivity.this).inflate(R.layout.datepicker, null);
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
                confirm_insert();
            }
        });

        iBtn_avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu pMenu = new PopupMenu(AddActivity.this, v);
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
                                if (ContextCompat.checkSelfPermission(AddActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                                {
                                    requestPermissions(new String[]{Manifest.permission.CAMERA}, MainActivity.REQUEST_PERMISSION_CAMERA_CODE);
                                }else{
                                    Intent intentCapture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                    startActivityForResult(intentCapture, REQUEST_CODE_CAM_AVATAR);
                                }
                                break;
                            case R.id.avatarMenu_clear:
                                iBtn_avatar.setImageResource(R.drawable.avatar);
                                picChanged = false;
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

    private void mySetDate(DatePicker d){
        if (ET_birth.getText().toString().trim().equals("")){
            return;
        }
        String[] tmp = ET_birth.getText().toString().trim().split("/");
        d.updateDate(Integer.parseInt(tmp[0]), Integer.parseInt(tmp[1]) - 1, Integer.parseInt(tmp[2]));
    }

    private void confirm_insert(){
        SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy_MMdd_HHmm_ss");
        String lastChange = simpleDate.format(new Date());
        if (picChanged)
            ToolMethod.SavePicture(getApplicationContext(), lastChange, iBtn_avatar);
        else
            lastChange = MainActivity.AVATAR_DEF;
        // SQL
        String _name, _age, _birth, _mobile, _home, _address, _mail, _web;
        _name = checkEmpty(ET_name, "姓名");
        _age = "'" + ET_age.getText().toString().trim() + "'";
        _birth = "'" + ET_birth.getText().toString().trim() + "'";
        _mobile = checkEmpty(ET_mobile, "手機號碼");
        _home = "'" + ET_home.getText().toString().trim() + "'";
        _address = "'" + ET_address.getText().toString().trim() + "'"; // 台北市大安區建國南路二段231號
        _mail = "'" + ET_mail.getText().toString().trim() + "'";
        _web = "'" + ET_web.getText().toString().trim() + "'";
        // Check Any Empty EditText Before Execute SQL
        if (insertHaveEmpty){
            String column = "";
            for (String s:emptyColumn){
                column += "[" + s + "]";
            }
            Toast.makeText(AddActivity.this, column + "不能是空白的", Toast.LENGTH_SHORT).show();
            insertHaveEmpty = false; emptyColumn.clear();
            return;
        }
        // SQL Execute
        String sql = "insert into People(_name, _age, _birth, _mobile, _home, _address, _mail, _web, _lastchange) values("
                + _name + ", " + _age + ", " + _birth + ", " + _mobile + ", " + _home + ", " + _address + ", " + _mail + ", " + _web + ", '" + lastChange + "')";
        //Log.d("Debug: ", sql);
        db.execSQL(sql);
        Toast.makeText(this, "資料新增成功", Toast.LENGTH_SHORT).show();
        finish();
    }

    List<String> emptyColumn = new ArrayList<String>();
    private String checkEmpty(EditText t, String columnName){
        String s = t.getText().toString().trim();
        if (s.equals("")){
            insertHaveEmpty = true;
            emptyColumn.add(columnName);
            return null;
        }
        else
            return "'" + s + "'";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == REQUEST_CODE_CHOOSE_AVATAR && data != null){
            picChanged = true;
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
            picChanged = true;
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            iBtn_avatar.setImageBitmap(ToolMethod.ScalePicture(photo));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}