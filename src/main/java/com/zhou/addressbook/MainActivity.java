package com.zhou.addressbook;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.support.design.widget.FloatingActionButton;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    static final String AVATAR_DEF = "0000_0000_0000_00";
    static final int REQUEST_PERMISSION_CAMERA_CODE = 123;
    static SQLiteDatabase db = null;

    public static int listPosition = 0;
    private String delete_tempName = null;
    private boolean listScrolled = false;

    ImageButton iBtn_search, iBtn_closeSearch; EditText ET_search; View View_search;
    Toolbar myToolbar;
    ListView mylistview;
    BookAdapter myadapter;
    FloatingActionButton myfab;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = (new SQL_DAO(this)).getWritableDatabase();
        iBtn_search = findViewById(R.id.mainBtn_search); iBtn_closeSearch = findViewById(R.id.mainBtn_closeSearch);
        ET_search = findViewById(R.id.mainET_search); View_search = findViewById(R.id.main_searchView);
        mylistview = findViewById(R.id.main_listView); myfab = findViewById(R.id.main_fab_add);

        // Toolbar
        myToolbar = (Toolbar) findViewById(R.id.main_Toolbar);
        myToolbar.inflateMenu(R.menu.menu1);

        myToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.menu_reload:
                        reload();
                        checkAndDeletePic(); // 刪除未使用的照片
                        return false;
                    case R.id.menu_default:
                        confirm_default(0);
                        return false;
                    case R.id.menu_delete_all:
                        confirm_default(1);
                        return false;
                    // Info
                    case R.id.menu_info:
                        confirm_default(2);
                        return false;
                    default:
                        return false;
                }
            }
        });

        iBtn_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iBtn_search.setVisibility(v.GONE);
                View_search.setVisibility(v.VISIBLE);
                listScrolled = false; myfab.show();
                ET_search.setText("");
                ET_search.requestFocus();
            }
        });

        iBtn_closeSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ET_search.getText().toString().equals("")){
                    clearSearch();
                }
                else{
                    ET_search.setText("");
                }
            }
        });

        myfab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent_add = new Intent(MainActivity.this, AddActivity.class);
                startActivity(intent_add);
            }
        });

        mylistview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final int pos = position;
                final View picker = LayoutInflater.from(MainActivity.this).inflate(R.layout.menu_for_main, null);
                Button btn_edit = picker.findViewById(R.id.menuBtn_edit);
                Button btn_delete = picker.findViewById(R.id.menuBtn_delete);
                Button btn_web = picker.findViewById(R.id.menuBtn_web);
                //final ImageButton btn_fav = picker.findViewById(R.id.menuBtn_favorite);

                final AlertDialog dialog;
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setView(picker);
                dialog = builder.create();
                //dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();

                /*btn_fav.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        btn_fav.setImageResource(android.R.drawable.btn_star_big_on);
                    }
                });*/

                btn_edit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listPosition = mylistview.getFirstVisiblePosition();
                        Toast.makeText(MainActivity.this, "Avatar :\n" + myadapter.myItemList.get(pos).get("_lastchange").toString(), Toast.LENGTH_SHORT).show();
                        edit(myadapter.myItemList.get(pos));
                        dialog.cancel();
                    }
                });

                btn_web.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try{
                            Uri web = Uri.parse(formatUrl(myadapter.myItemList.get(pos).get("_web").toString().trim()));
                            Intent intent = new Intent(Intent.ACTION_VIEW, web);
                            startActivity(intent);
                        }catch (ActivityNotFoundException e){
                            Toast.makeText(MainActivity.this, "無法開啟網頁\n原因：網址錯誤", Toast.LENGTH_SHORT).show();
                        }
                        dialog.cancel();
                    }
                });

                btn_delete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        delete_tempName = myadapter.myItemList.get(pos).get("_name").toString();
                        confirm_default(3);
                        dialog.cancel();
                    }
                });
                return false;
            }
        });

        // 獲取捲動方向 並對FAB調整
        mylistview.setOnTouchListener(new View.OnTouchListener() {
            float initialY, finalY;
            boolean isScrollingUp;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mylistview.getCount() <= 2){
                    return false;
                }
                switch(event.getAction()) {
                    case (MotionEvent.ACTION_DOWN):
                        initialY = event.getY();
                    case (MotionEvent.ACTION_UP):
                        finalY = event.getY();
                        if (initialY < finalY){
                            isScrollingUp = true;
                        }
                        else if (initialY > finalY){
                            isScrollingUp = false;
                        }
                }

                if (isScrollingUp){
                    if (!listScrolled){
                        return false;
                    }
                    myfab.show(); listScrolled = false;
                }
                else{
                    if (listScrolled){
                        return false;
                    }
                    myfab.hide(); listScrolled = true;
                }
                return false;
            }
        });

        // 搜尋功能使用的
        ET_search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //
            }

            @Override
            public void afterTextChanged(Editable s) {
                //
                if (!s.toString().trim().equals("")){
                    reload(ET_search.getText().toString().trim());
                }else{
                    reload();
                }
            }
        });
        reload(); //重新整理
    }

    private String formatUrl(String url){
        if (url.length() >= 7 && url.substring(0, 7).equals("http://")){
            //Log.d("Debug: ", url);
            return url;
        }else{
            String newUrl = "http://" + url;
            //Log.d("Debug: ", newUrl);
            return newUrl;
        }
    }

    @Override //返回時到指定位置和重整
    protected void onResume() {
        super.onResume();
        listScrolled = false;
        myfab.show();
        clearSearch();
        reload();
        mylistview.setSelection(listPosition); listPosition = 0;
    }

    // 搜尋功能使用的
    private void clearSearch(){
        View_search.setVisibility(View.GONE);
        iBtn_search.setVisibility(View.VISIBLE);
    }

    // 讀取預設和刪除全部的確認視窗
    private void confirm_default(final int Rcode){
        AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        switch (Rcode){
            case 0:
                builder.setTitle("確定要讀取預設5筆資料？");
                break;
            case 1:
                builder.setTitle("確定要刪除所有資料？");
                break;
            case 2:
                builder.setTitle("通訊錄 (版本1.0)\n作者：周定憲");
                break;
            case 3:
                builder.setTitle("確定要刪除這筆資料？");
                break;
        }

        builder.setPositiveButton("確定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (Rcode == 0)
                    default5();
                if (Rcode == 1)
                    delete_all();
                if (Rcode == 2)
                    dialog.cancel();
                if (Rcode == 3)
                    delete_this();
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

    // 刪除未被關聯到的照片
    private void checkAndDeletePic() {
        ContextWrapper cw = new ContextWrapper(MainActivity.this);
        File myPath = cw.getDir("book", Context.MODE_PRIVATE);
        // 取得APP_BOOK路徑裡的所有.PNG 存於LIST中
        List<File> files = getListFiles(new File(myPath.toString()));

        // 檢查是否被關連到
        for (int i = 0; i < myadapter.myItemList.size(); i++) {
            String exists_name = myadapter.myItemList.get(i).get("_lastchange").toString();
            for (File f : files) {
                int len = f.toString().length();
                String filename = f.toString().substring(len - 21, len - 4); // 擷取FILE路徑中 _lastchange 的名字
                if (exists_name.equals(filename)){
                    files.remove(f); // 有被關連到，從LIST中移除
                    break;
                }
            }
        }
        // 將LIST中剩餘的FILE刪除
        for (File f : files){
            Log.d("(Deleted) Debug Files: ", f.toString());
            f.delete();
        }
    }

    // 取得APP_BOOK路徑裡的所有.PNG
    private List<File> getListFiles(File parentDir) {
        ArrayList<File> inFiles = new ArrayList<File>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (file.getName().endsWith(".png")){
                Log.d("(Before) Debug Files: ", file.toString());
                inFiles.add(file);
            }
        }
        return inFiles;
    }

    // 刪除全部
    private void delete_all(){
        String sql = "delete from People";
        db.execSQL(sql);
        reload(); // 重新整理
    }

    // 刪除這筆
    private void delete_this(){
        if (delete_tempName != null){
            String sql = "delete from People where _name = '" + delete_tempName + "'";
            db.execSQL(sql);
            clearSearch();
            reload();
            delete_tempName = null;
        }
    }

    // 讀取預設5筆
    private void default5(){
        // 預設資料的東西
        String[] p_name = getResources().getStringArray(R.array.array_name);
        String[] p_age = getResources().getStringArray(R.array.array_age);
        String[] p_birth = getResources().getStringArray(R.array.array_birth);
        String p_mobile = "0900000000", p_home  = "0200000000";
        String p_address = "台北市", p_mail = "default@gmail.com", p_web = "";
        String p_lastchange = AVATAR_DEF;

        for(int i = 0; i < 5; i++){
            String sql_def = "delete from People where _name = '" + p_name[i] + "'";
            db.execSQL(sql_def);
            String sql = "insert into People(_name,_age,_birth,_mobile,_home,_address,_mail,_web,_lastchange) values('" + p_name[i] + "','"
                    + p_age[i] + "','" + p_birth[i] + "','" + p_mobile + "','" + p_home + "','" + p_address + "','" + p_mail + "','" + p_web + "','" + p_lastchange + "')";
            db.execSQL(sql);
        }
        reload(); // 重新整理
    }

    // 重新整理
    private void reload(){
        reload_main("select * from People");
    }

    // 重新整理(搜尋)
    private void reload(String search){
        reload_main("select * from People where _name = '" + search + "'");
    }

    // 重新整理主程序
    private void reload_main(String sql){
        Cursor mycursor = db.rawQuery(sql,null);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        mycursor.moveToFirst();
        for(int i = 0; i < mycursor.getCount(); i++) {
            HashMap<String, Object> itemx = new HashMap<String, Object>();
            itemx.put("_name", mycursor.getString(0));
            itemx.put("_age", mycursor.getString(1));
            itemx.put("_birth", mycursor.getString(2));
            itemx.put("_mobile", mycursor.getString(3));
            itemx.put("_home", mycursor.getString(4));
            itemx.put("_address", mycursor.getString(5));
            itemx.put("_mail", mycursor.getString(6));
            itemx.put("_web", mycursor.getString(7));
            itemx.put("_lastchange", mycursor.getString(8)); // 此值紀錄最後修改時間，用以找照片
            items.add(itemx);
            mycursor.moveToNext();
        }
        myadapter = new BookAdapter(MainActivity.this, items);
        mylistview.setAdapter(myadapter);
    }

    // 編輯指定欄位資料
    public void edit(Map<String, Object> list){
        Intent intent = new Intent(MainActivity.this, EditActivity.class);
        intent.putExtra("_name", list.get("_name").toString());
        intent.putExtra("_age", list.get("_age").toString());
        intent.putExtra("_birth", list.get("_birth").toString());
        intent.putExtra("_mobile", list.get("_mobile").toString());
        intent.putExtra("_home", list.get("_home").toString());
        intent.putExtra("_address", list.get("_address").toString());
        intent.putExtra("_mail", list.get("_mail").toString());
        intent.putExtra("_web", list.get("_web").toString());
        intent.putExtra("_lastchange", list.get("_lastchange").toString());
        startActivity(intent);
    }
}

class BookAdapter extends BaseAdapter
{
    private Context myContext;
    private LayoutInflater mLayInf;
    List<Map<String, Object>> myItemList;

    public BookAdapter(Context context, List<Map<String, Object>> itemList)
    {
        mLayInf = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        myItemList = itemList;
        myContext = context;
    }

    @Override
    public int getCount()
    {
        return myItemList.size();
    }

    @Override
    public Object getItem(int position)
    {
        return  myItemList.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View v = mLayInf.inflate(R.layout.perdata, parent, false);

        TextView p_name = (TextView) v.findViewById(R.id._name);
        p_name.setText(myItemList.get(position).get("_name").toString());

        TextView p_age = (TextView) v.findViewById(R.id._age);
        p_age.setText(myItemList.get(position).get("_age").toString());

        TextView p_birth = (TextView) v.findViewById(R.id._birth);
        p_birth.setText(myItemList.get(position).get("_birth").toString());

        final TextView p_mobile = (TextView) v.findViewById(R.id._mobile);
        p_mobile.setText(myItemList.get(position).get("_mobile").toString());

        final TextView p_home = (TextView) v.findViewById(R.id._home);
        p_home.setText(myItemList.get(position).get("_home").toString());

        TextView p_address = (TextView) v.findViewById(R.id._address);
        p_address.setText(myItemList.get(position).get("_address").toString());

        TextView p_mail = (TextView) v.findViewById(R.id._mail);
        p_mail.setText(myItemList.get(position).get("_mail").toString());

        TextView p_web = (TextView) v.findViewById(R.id._web);
        p_web.setText(myItemList.get(position).get("_web").toString());

        // Avatar
        ImageView p_avatar = (ImageView) v.findViewById(R.id.img_avatar);
        Bitmap mybmp = ToolMethod.LoadPicture(myContext, myItemList.get(position).get("_lastchange").toString());
        if (mybmp != null){
            p_avatar.setImageBitmap(mybmp);
        }

        // 撥打電按鈕
        ImageButton call_mobile = v.findViewById(R.id.btn_call_mobile);
        call_mobile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.listPosition = position;
                call_phone(myContext, p_mobile.getText().toString());
            }
        });
        ImageButton call_home = v.findViewById(R.id.btn_call_home);
        call_home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.listPosition = position;
                call_phone(myContext, p_home.getText().toString());
            }
        });

        // 地圖按鈕
        ImageButton map_home = v.findViewById(R.id.btn_map_home);
        map_home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.listPosition = position;
                String address = myItemList.get(position).get("_address").toString();
                toggle_map(address);
            }
        });
        return v;
    }

    // 啟動地圖
    public void toggle_map(String address){
        Intent map = new Intent(myContext, MapsActivity.class);
        map.putExtra("_address", address);
        myContext.startActivity(map);
    }

    // 撥打電話
    public void call_phone(Context context, String num){
        Intent call = new Intent(Intent.ACTION_DIAL);
        call.setData(Uri.parse("tel:" + num));
        try{
            context.startActivity(call);
        }catch(Exception ex){
            Toast.makeText(context, "無法撥打電話: " + num, Toast.LENGTH_SHORT).show();
        }
    }
}

class SQL_DAO extends SQLiteOpenHelper {

    public SQL_DAO(Context context) {
        super(context,"PeopleDataBase",null,1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS People";
        sql += "(_name VARCHAR(30) ";
        sql += ",_age VARCHAR(30) ";
        sql += ",_birth VARCHAR(30) ";
        sql += ",_mobile VARCHAR(30) ";
        sql += ",_home VARCHAR(30) ";
        sql += ",_address VARCHAR(30) ";
        sql += ",_mail VARCHAR(30) ";
        sql += ",_web VARCHAR(30) ";
        sql += ",_lastchange VARCHAR(30))";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "Drop Table People";
        db.execSQL(sql);
        onCreate(db);
    }
}