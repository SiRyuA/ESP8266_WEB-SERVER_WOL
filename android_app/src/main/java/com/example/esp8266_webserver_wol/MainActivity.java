package com.example.esp8266_webserver_wol;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Toast;
import android.util.Base64;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public static Context mContext;

    // 버튼
    private FloatingActionButton fab;

    // Recycler View
    RecyclerView mRecyclerView = null;
    RecyclerTextAdapter mAdapter = null;
    ArrayList<RecyclerItem> mList = new ArrayList<RecyclerItem>();

    // WoL ArrayList
    ArrayList<WoLItem> wolList = new ArrayList<WoLItem>();

    // 데이터 저장
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private String WOL;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;
        
        // 데이터 불러오기
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        editor = preferences.edit();
        WOL = preferences.getString("WOL", "");
        Json2WoLArrayList();

        // RecyclerView 띄우기
        mRecyclerView = findViewById(R.id.ListWoL);
        mAdapter = new RecyclerTextAdapter(mList);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 플로팅 버튼
        fab = (FloatingActionButton) findViewById(R.id.btnAdd);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ProcessAdd();
            }
        });
    }

    // 다른 클래스 Toast 출력 용
    public void ToastShow(String msg) {
        Toast.makeText(mContext,msg, Toast.LENGTH_SHORT).show();
    }

    public void ToastShowInt(int address) {
        Toast.makeText(mContext,getString(address), Toast.LENGTH_SHORT).show();
    }

    // 다른 클래스 R.string 전달 용
    public String getStringValue(int address){
        return getString(address);
    }


    // 수정 또는 삭제 동작
    public void ProcessItemControl(int val) {
        // 경고 다이얼로그
        AlertDialog.Builder dia = new AlertDialog.Builder(this);
        dia.setMessage(getString(R.string.itemcontrol_question))
                // 삭제
                .setPositiveButton(getString(R.string.remove), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ProcessRemove(val);
                        Toast.makeText(mContext,getString(R.string.remove), Toast.LENGTH_SHORT).show();
                    }
                })
                // 수정
                .setNegativeButton(getString(R.string.modified), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ProcessModified(val);
                        //Toast.makeText(mContext,getString(R.string.modified), Toast.LENGTH_SHORT).show();
                    }
                })
                // 취소
                .setNeutralButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(mContext,getString(R.string.cancel), Toast.LENGTH_SHORT).show();
                    }
                })
                .setCancelable(false)
                .show();
    }

    // 추가 동작
    public void ProcessAdd() {
        // 다이얼로그 불러오기
        DialogAdd dig = new DialogAdd(MainActivity.this);
        dig.call(false, -1,"","","","");
    }

    // 삭제 동작
    public void ProcessRemove(int val) {
        // 데이터 삭제
        wolList.remove(val);
        mList.remove(val);
        mAdapter.notifyDataSetChanged() ;

        // 데이터 저장
        WOL = WoLItem2Json();
        editor.remove("WOL");
        editor.putString("WOL",WOL);
        editor.apply();
    }

    // 수정 동작
    public void ProcessModified(int val) {
        // 값 불러오기
        String name = wolList.get(val).getName();
        String url = wolList.get(val).getUrl();
        String key = wolList.get(val).getKey();
        String mac = wolList.get(val).getMac();

        // 다이얼로그 불러오기
        DialogAdd dig = new DialogAdd(MainActivity.this);
        dig.call(true, val, name, url, key, mac);
    }

    public void ProcessModifiedData(int val, String name, String url, String key, String mac) {
        // WoL 데이터 수정
        WoLItem wol = new WoLItem();
        wol.setName(name);
        wol.setUrl(url);
        wol.setKey(key);
        wol.setMac(mac);
        wolList.set(val, wol);

        // Recycler View 데이터 수정
        RecyclerItem re = new RecyclerItem();
        re.setName(name);
        re.setMac(mac);
        mList.set(val, re);
        mAdapter.notifyDataSetChanged() ;

        // 데이터 저장
        WOL = WoLItem2Json();
        editor.remove("WOL");
        editor.putString("WOL",WOL);
        editor.apply();
    }

    // 데이터 저장
    public void ProcessAddData(String name, String url, String key, String mac) {
        // WoL 리스트 추가
        WoLItem wol = new WoLItem();
        wol.setName(name);
        wol.setUrl(url);
        wol.setKey(key);
        wol.setMac(mac);
        wolList.add(wol);

        // Recycler View 리스트 추가
        RecyclerItem re = new RecyclerItem();
        re.setName(name);
        re.setMac(mac);
        mList.add(re);
        mAdapter.notifyDataSetChanged() ;

        // 데이터 저장
        WOL = WoLItem2Json();
        editor.remove("WOL");
        editor.putString("WOL",WOL);
        editor.apply();
    }

    // WoL 패킷 전송
    public void WoLSend(int val) {
        // 데이터 불러오기
        String url = wolList.get(val).getUrl();
        String key = wolList.get(val).getKey();
        String mac = wolList.get(val).getMac();

        // 주소에서 http 나 https 빠져있으면 http 붙이기
        if(!url.contains("https://")) {
            if(!url.contains("http://")) {
                url = "http://" + url;
            }
        }

        // 토큰 값 생성
        String token = key + "/" + mac;
        token = getEncode(token);

        // 주소 생성
        String wol = url + "/ESP-WOL/" + token;
        
        // HTTP 전송
        WoLSendProcessHttp(wol);
    }

    // HTTP 연결
    private void WoLSendProcessHttp(String wol) {
        // 쓰레드 생성
        try {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {

                    String result = "";

                    try {
                        // Http 연결
                        URL url = new URL(wol);
                        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
                        httpConn.setReadTimeout(3000);
                        httpConn.setConnectTimeout(3000);
                        httpConn.setDoOutput(true);
                        httpConn.setDoInput(true);
                        httpConn.setRequestMethod("GET");
                        httpConn.setUseCaches(false);
                        httpConn.connect();

                        // 연결 상태 확인
                        int statusCode = httpConn.getResponseCode();
                        InputStream inputStream;

                        // 이상 없을 경우 -> 값 가져오기 / 이상 있으면 -> 에러 값
                        if(statusCode == HttpURLConnection.HTTP_OK) {
                            inputStream = httpConn.getInputStream();
                        } else {
                            inputStream = httpConn.getErrorStream();
                        }

                        // 값 읽어들이기
                        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                        StringBuilder sb = new StringBuilder();
                        String line;

                        while((line = bufferedReader.readLine()) != null) {
                            sb.append(line);
                        }

                        // 연결 해제
                        bufferedReader.close();
                        httpConn.disconnect();;

                        // result에 값 저장
                        result = sb.toString().trim();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // 다음 프로세스로 전달
                    WoLSendProcessJson(result);
                }
            });
            // 쓰레드 시작
            thread.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // JSON 값 분해 후 결과 출력
    private void WoLSendProcessJson(String jsonString) {
        try {
            // 들어온 값이 0 이상이면
            if(jsonString.length() > 0) {
                JSONObject json = new JSONObject(jsonString);
                String result = json.getString("result");

                // 피드백
                if(result.equals("success")) { // 성공일 경우
                    String mac = json.getString("mac");
                    Looper.prepare();
                    Toast.makeText(mContext, getString(R.string.wol_packet)+" "+getString(R.string.send)+" "+getString(R.string.success)+".\n(MAC : "+mac+")", Toast.LENGTH_LONG).show();
                    Looper.loop();
                }
                else { // 실패일 경우
                    Looper.prepare();
                    Toast.makeText(mContext, getString(R.string.failed)+".", Toast.LENGTH_LONG).show();
                    Looper.loop();
                }
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    // Base64 인코딩
    public String getEncode(String str) {
        return Base64.encodeToString(str.getBytes(), 0);
    }

    // Base64 디코딩
    public String getDecode(String str) {
        return new String(Base64.decode(str, 0));
    }

    // 데이터 저장 -> WoL ArrayList를 JSON으로
    private String WoLItem2Json() {
        String jsonString = "";

        try {
            JSONArray jsonArray = new JSONArray();
            for(int i = 0; i < wolList.size(); i++) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", wolList.get(i).getName());
                jsonObject.put("url", wolList.get(i).getUrl());
                jsonObject.put("key",wolList.get(i).getKey());
                jsonObject.put("mac",wolList.get(i).getMac());
                jsonArray.put(jsonObject);
            }
            jsonString = jsonArray.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return jsonString;
    }

    // 데이터 불러오기 -> JSON을 WoL ArrayList으로
    public void Json2WoLArrayList() {
        try {
            StringBuffer sb = new StringBuffer();
            JSONArray jsonArray = new JSONArray(WOL);
            wolList.clear();
            mList.clear();
            for(int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String name = jsonObject.getString("name");
                String url = jsonObject.getString("url");
                String key = jsonObject.getString("key");
                String mac = jsonObject.getString("mac");

                WoLItem wol = new WoLItem();
                wol.setName(name);
                wol.setUrl(url);
                wol.setKey(key);
                wol.setMac(mac);
                wolList.add(wol);

                RecyclerItem re = new RecyclerItem();
                re.setName(name);
                re.setMac(mac);
                mList.add(re);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}