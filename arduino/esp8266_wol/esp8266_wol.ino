#include <ESP8266WiFi.h>      // ESP8266 기본, 와이파이
#include <WiFiUdp.h>          // 아두이노 기본, WoL 패킷 전송
#include <WakeOnLan.h>        // 추가, WoL 패킷 전송
#include <ESP8266WebServer.h> // ESP8266 기본, 웹서버
#include <WiFiClient.h>       // 아두이노 기본, 웹서버
#include <ESP8266mDNS.h>      // ESP8266 기본, 웹서버
#include <uri/UriBraces.h>    // 아두이노 기본, 웹서버
#include <uri/UriRegex.h>     // 아두이노 기본, 웹서버
//#include "base64.h"         // 추가, 감추는 용도.. 
                              // 제대로 된 보안을 구현하고 싶으신 분은 AES 사용하세요 :)


/* 
 * 설정 - 필요에 맞추어서 수정해서 사용하세요.
 */

// Wi-Fi
const char*   WIFISSID    = "Your Wi-Fi SSID";      //Wi-Fi SSID
const char*   WIFIPASS    = "Your Wi-Fi Password";  // Wi-Fi 비밀번호

// 시리얼 모니터링
const bool    CONSOLE     = false;                  // 콘솔 출력 안하려면 false
const int     BANDRATE    = 115200;                 // NodeMCU 설정에 맞추어서 수정

// 웹서버
const int     WEBPORT     = 80;                     // 접속 포트 설정
const String  WEBREDIRECT = "https://google.com";   // 리다이렉트 주소
const String  WEBKEY      = "ABCDEFGH";             // 구분자, 16자 이하 추천, 그 이상은 뻗음
const int     TOKEN_MAX_SIZE = 256;                 // 주고 받을 토큰 값 최대 크기, 최대 256까지만..


/*
 * 전역 변수
 */
// WoL
WiFiUDP UDP;
WakeOnLan WOL(UDP);
// 웹서버
ESP8266WebServer server(WEBPORT);
const String REDIRECTHTML = "<html><script>location.href='"+String(WEBREDIRECT)+"';</script></html>";

void setup() {
  // 시리얼 설정
  if(CONSOLE) Serial.begin(BANDRATE); 

  // 100 ms 간격 3회 출력
  WOL.setRepeat(3, 100); 

  // Wi-Fi 연결, 5분 간 연결되지 않으면 재시작
  WiFiConnection();

  // 브로드캐스트 주소 계산
  WOL.calculateBroadcastAddress(WiFi.localIP(), WiFi.subnetMask());

  // MDNS 설정
  if (MDNS.begin("esp8266")) {
    if(CONSOLE) Serial.println("MDNS responder started");
  }

  // 웹서버 페이지 정의
  WebPage();

  // 웹서버 시작
  server.begin();
  Serial.println("HTTP server started");
}

void loop() {
  // 웹서버 클라이언드 핸들링
  server.handleClient();
  SerialEncode();
}

// Wi-Fi 연결
void WiFiConnection() {
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFISSID, WIFIPASS);
  
  int cnt = 0;
  bool conn = true;

  // Wi-Fi 연결 될 때까지 무한 반복
  while (WiFi.status() != WL_CONNECTED) {
    if(CONSOLE) {
      String msg = "Wi-Fi Searching(" + String(cnt) + ")";
      Serial.println(msg);
    }
    delay(500);
    cnt++;

    // 5분 이상 못 찾을 경우 while 강제 탈출
    if(cnt > 600) {
      conn = false;
      break;
    }
  }

  if(!conn) ESP.restart(); // NodeMCU 재시작하기
  else { // 아닐 경우 콘솔 여부에 따라 결과 출력
    if(CONSOLE) {
      Serial.println("Wi-Fi Connected.");
      Serial.print("Wi-Fi : ");
      Serial.print(WiFi.SSID());
      Serial.print(" / IP : ");
      Serial.println(WiFi.localIP());
    }
  }
}

void WebPage() {
  // 메인 페이지 접근하는 경우 리다이렉트
  server.on(F("/"), []() {
    if(CONSOLE) Serial.println("Unusual approach, redirect");
    server.send(200, "text/html", REDIRECTHTML);
  });
  
  // 404 페이지 전부 리다이렉트
  server.onNotFound(WebPageNotFound);
  
  // 정상 접근하는 경우
  server.on(UriBraces("/ESP-WOL/{}"), []() {
    // 전달받은 파라미터 수신
    String in = server.pathArg(0); 
    if(CONSOLE) Serial.println("WOL IN : " + in);

    // Base64 디코딩
    char out[TOKEN_MAX_SIZE] = {0};
    b64_decode(out, (char *)in.c_str(), in.length());
    
    in = String(out);
    if(CONSOLE) Serial.println("WOL Decode -> " + in);

    // 키 값 분리
    String key = getValue(in, '/', 0);

    // 키 값 일치 유무 확인
    if(key == WEBKEY) {
      if(CONSOLE) Serial.println("WOL Key -> OK");
      String mac = getValue(in, '/', 1);

      // WoL 패킷 발송
      WOL.sendMagicPacket(mac);
      if(CONSOLE) Serial.println("WOL Send.");

      // 결과 피드백 -> 성공
      server.send(200, "text/json", "{'result':'success','mac':'"+mac+"'}");
    }
    else { // 결과 피드백 -> 실패
      if(CONSOLE) Serial.println("WOL Key -> FAIL");
      server.send(200, "text/json", "{'result':'failed'}");
    }
  });
}

// 404 처리
void WebPageNotFound() { 
  server.send(404, "text/html", REDIRECTHTML);
}

// 문자열 잘라서 값으로 반환
String getValue(String data, char separator, int index)
{
  int found = 0;
  int strIndex[] = {0, -1};
  int maxIndex = data.length()-1;

  for(int i=0; i<=maxIndex && found<=index; i++){
    if(data.charAt(i)==separator || i==maxIndex){
        found++;
        strIndex[0] = strIndex[1]+1;
        strIndex[1] = (i == maxIndex) ? i+1 : i;
    }
  }

  return found>index ? data.substring(strIndex[0], strIndex[1]) : "";
}

// 시리얼 입력 Base64 인코딩
void SerialEncode() {
  if(CONSOLE) {
    char temp;
    String data = "";

    // 시리얼 입력이 있는 경우
    if(Serial.available() > 0) {
      // 시리얼 입력 받고 출력
      String data = Serial.readString();
      Serial.println("Serial IN -> " + data);
  
      // Base64 인코딩 처리 후 출력
      char b64data[TOKEN_MAX_SIZE] = {0};
      b64_encode(b64data, (char *)data.c_str(), data.length());
      Serial.println("Serial Encode -> " + String(b64data));
    }
    
    // 시리얼 비우기
    Serial.flush();
  }
}
