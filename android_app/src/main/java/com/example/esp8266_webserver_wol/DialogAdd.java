package com.example.esp8266_webserver_wol;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class DialogAdd {

    private Context context;
    public DialogAdd(Context context) {
        this.context = context;
    }

    // 다이얼로그 부르는 경우
    public void call(boolean Modified, int Pos, String setName, String setUrl, String setKey, String setMac) {
        final Dialog dig = new Dialog(context);

        // 액티비티바 숨김
        dig.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // 레이아웃 화면 설정
        dig.setContentView(R.layout.dialog_add);

        // 출력
        dig.show();

        // 내부 항목 설정
        final EditText getName = (EditText) dig.findViewById(R.id.input_name);
        final EditText getUrl = (EditText) dig.findViewById(R.id.input_url);
        final EditText getKey = (EditText) dig.findViewById(R.id.input_key);
        final EditText getMac = (EditText) dig.findViewById(R.id.input_mac);
        final Button btnAdd = (Button) dig.findViewById(R.id.btnAdd);
        final Button btnCancel = (Button) dig.findViewById(R.id.btnCancel);

        // 값을 수정하는 경우
        if(Modified) {
            String btnModiText = ((MainActivity)MainActivity.mContext).getStringValue(R.string.modified);

            getName.setText(setName);
            getUrl.setText(setUrl);
            getKey.setText(setKey);
            getMac.setText(setMac);
            btnAdd.setText(btnModiText);
        }

        // 버튼 클릭 이벤트 처리 -> 추가 버튼
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = getName.getText().toString();
                String url = getUrl.getText().toString();
                String key = getKey.getText().toString();
                String mac = getMac.getText().toString();

                // 빈 값 유무 확인
                if(name.isEmpty() || url.isEmpty() || key.isEmpty() || mac.isEmpty()) {
                    ((MainActivity)MainActivity.mContext).ToastShowInt(R.string.checkitems);
                } else {
                    // MAC 주소 확인
                    int cnt = 0;
                    for(int i = 0; i < mac.length(); i++) {
                        if(mac.charAt(i) == ':') cnt++;
                    }
                    if(cnt < 5) {
                        ((MainActivity)MainActivity.mContext).ToastShowInt(R.string.checkmac);
                    } else {
                        // KEY 값 확인
                        if(key.contains("/")) {
                            ((MainActivity)MainActivity.mContext).ToastShowInt(R.string.checkkey);
                        }
                        else {
                            // 이상이 없는 경우
                            if(Modified) { // 수정 시
                                ((MainActivity)MainActivity.mContext).ProcessModifiedData(Pos, name,url,key,mac);
                                ((MainActivity)MainActivity.mContext).ToastShowInt(R.string.modified);
                            }
                            else { // 추가 시
                                ((MainActivity)MainActivity.mContext).ProcessAddData(name,url,key,mac);
                                ((MainActivity)MainActivity.mContext).ToastShowInt(R.string.add);
                            }
                            dig.dismiss();
                        }
                    }
                }
            }
        });

        // 버튼 클릭 이벤트 처리 -> 취소 버튼
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Toast.makeText(context, "Cancel", Toast.LENGTH_SHORT).show();
                ((MainActivity)MainActivity.mContext).ToastShowInt(R.string.cancel);
                dig.dismiss();
            }
        });

    }

}
