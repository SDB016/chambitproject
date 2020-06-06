package com.example.chambitproject;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private long backPressedTime = 0;
    String[] REQUIRED_PERMISSIONS  = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    Document doc = null;
    Button btn_station;
    CustomDialog1 customDialog1;
    Handler handler;
    DrawerLayout mDrawerLayout;
    MyService mService;
    IntentIntegrator qrScan;
    TextView tv_baggage;
    TextView tv_tb;
    WebView wv;
    StationItem stationItem;
    StationInfo stationInfo;
    Boolean isChoose_start = false;
    Boolean isChoose_end = false;
    Boolean mBound = false;
    Boolean isStart = true;
    Boolean state_service = false;
    String start_station = "미지정";
    String end_station = "미지정";
    ArrayList<String> station_list = new ArrayList<>();

    boolean thread_run = false;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MyService.LocalBinder binder = (MyService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.activity_main);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        TextView textView = (TextView) findViewById(R.id.tv_tb);
        tv_tb = textView;
        textView.setText("지하철 지도   " + start_station + " - > " + end_station);
        wv = (WebView) findViewById(R.id.webview);
        tv_baggage = (TextView) findViewById(R.id.baggage);
        stationItem = new StationItem();
        handler = new Handler();
        setSupportActionBar((androidx.appcompat.widget.Toolbar) findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator((int) R.mipmap.ic_launcher_round);
        IntentIntegrator intentIntegrator = new IntentIntegrator(this);
        qrScan = intentIntegrator;
        intentIntegrator.setBeepEnabled(false);
        thread_run = true;
        stationInfo = new StationInfo();

        wv.getSettings().setJavaScriptEnabled(true);
        wv.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
        new Thread(new Runnable() {
            public void run() {
                while (thread_run) {
                    handler.post(new Runnable() {
                        public void run() {
                            if (mBound) {
                                stationItem = mService.get_station();

                                Toast.makeText(MainActivity.this, stationItem.getName() + " : " + stationItem.getDistance() + "m", Toast.LENGTH_LONG).show();
                                WebView webView = wv;
                                webView.loadUrl("https://www.google.com/maps/@" + stationItem.getX() + "," + stationItem.getY() + ",17z");
                            }
                        }
                    });
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS,
                    PERMISSIONS_REQUEST_CODE);
        }
    }

    public void set_start(View v) {
        isStart = true;
        mDrawerLayout.closeDrawers();
        CustomDialog1 customDialog12 = new CustomDialog1(this, new CustomDialog1.DialogListener() {
            public void clicked(String hosun) {
                change_station(hosun);
            }
        });
        customDialog1 = customDialog12;
        customDialog12.show();
        btn_station = (Button) v;
    }

    public void set_end(View v) {
        isStart = false;
        mDrawerLayout.closeDrawers();
        CustomDialog1 customDialog12 = new CustomDialog1(this, new CustomDialog1.DialogListener() {
            public void clicked(String hosun) {
                change_station(hosun);
            }
        });
        customDialog1 = customDialog12;
        customDialog12.show();
        btn_station = (Button) v;
    }

    public void btn_service(View v) {
        Button btn = (Button) v;
        mDrawerLayout.closeDrawers();
        if(isChoose_start && isChoose_end){
            Intent intent = new Intent(getApplicationContext(), MyService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            if (!state_service) {
                intent.putExtra(MyService.MESSAGE_KEY, true);
                intent.putExtra(MyService.MESSAGE_LIST, station_list);
                state_service = true;
                btn.setText("종료");
                mBound = true;
            } else {
                intent.putExtra(MyService.MESSAGE_KEY, false);
                intent.putExtra(MyService.MESSAGE_LIST, station_list);
                station_list.clear();
                state_service = false;
                btn.setText("시작");
                mBound = false;
            }
            startService(intent);
        }
        else{
            Toast.makeText(getApplicationContext(), "역을 선택해 주세요", Toast.LENGTH_LONG).show();
        }
    }

    public void btn_qr(View v) {
        qrScan.setPrompt("Scanning..");
        qrScan.initiateScan();
    }

    public void change_station(String name) {
        if (isStart) {
            start_station = name;
            isChoose_start=true;
            if(!isChoose_end)
                Toast.makeText(getApplicationContext(), "도착역을 선택해 주세요", Toast.LENGTH_LONG).show();
        } else {
            end_station = name;
            isChoose_end=true;
            if(!isChoose_start)
                Toast.makeText(getApplicationContext(), "출발역을 선택해 주세요", Toast.LENGTH_LONG).show();
        }
        btn_station.setText(name);
        TextView textView = tv_tb;
        textView.setText("지하철 지도   " + start_station + " - > " + end_station);
        if(isChoose_start && isChoose_end)
            set_path();
    }

    public void set_path(){
        GetXMLTask task = new GetXMLTask();
        String startX,startY,endX,endY;
        startX = String.valueOf(stationInfo.get_y(start_station));
        startY = String.valueOf(stationInfo.get_x(start_station));
        endX = String.valueOf(stationInfo.get_y(end_station));
        endY = String.valueOf(stationInfo.get_x(end_station));

        task.execute("http://ws.bus.go.kr/api/rest/pathinfo/getPathInfoBySubway?serviceKey=cxE1AcPJ6HiMMHx3rbDUNCZuN8cOL7PgSeOlflCQ7D%2BZPPRagYOkviX4euEZxJ0x65OzE%2FGhUQJbMrVyywmY%2Bg%3D%3D&" +
                "startX="+ startX + "&startY="+startY+"&endX="+endX+"&endY="+endY+"&");

        Log.e("link", "http://ws.bus.go.kr/api/rest/pathinfo/getPathInfoBySubway?serviceKey=cxE1AcPJ6HiMMHx3rbDUNCZuN8cOL7PgSeOlflCQ7D%2BZPPRagYOkviX4euEZxJ0x65OzE%2FGhUQJbMrVyywmY%2Bg%3D%3D&" +
                "startX="+ startX + "&startY="+startY+"&endX="+endX+"&endY="+endY+"&");
    }

    public void shut_dialog(View v) {
        customDialog1.dismiss();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result == null) {
            super.onActivityResult(requestCode, resultCode, data);
        } else if (result.getContents() == null) {
            Toast.makeText(this, "취소!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "스캔완료!", Toast.LENGTH_SHORT).show();
            try {
                JSONObject obj = new JSONObject(result.getContents());
                Toast.makeText(this, "열차번호 : " + obj.getString("열차번호") + "호차\n짐칸번호 : " + obj.getString("짐칸번호") + "번칸", Toast.LENGTH_SHORT).show();
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(this, result.getContents(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() != 16908332) {
            return super.onOptionsItemSelected(item);
        }
        mDrawerLayout.openDrawer((int) GravityCompat.START);
        return true;
    }

    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen((int) GravityCompat.START)) {
            drawer.closeDrawer((int) GravityCompat.START);
            return;
        }
        long tempTime = System.currentTimeMillis();
        long intervalTime = tempTime - backPressedTime;
        if (0 > intervalTime || 2000 < intervalTime) {
            backPressedTime = tempTime;
            Toast.makeText(getApplicationContext(), "한번 더 누르면 종료됩니다", Toast.LENGTH_LONG).show();
            return;
        }
        super.onBackPressed();
    }

    private class GetXMLTask extends AsyncTask<String, Document, Document> {
        @Override
        protected Document doInBackground(String... urls) {
            URL url;
            try {
                url = new URL(urls[0]);
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                doc = db.parse(new InputSource(url.openStream()));
                doc.getDocumentElement().normalize();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return doc;
        }

        @Override
        protected void onPostExecute(Document doc) {
            NodeList nodeList = doc.getElementsByTagName("pathList");
            int i = 0;
            station_list.clear();
            while (true) {
                Node node = nodeList.item(i);
                Element fstElmnt = (Element) node;
                NodeList startList = fstElmnt.getElementsByTagName("fname");
                NodeList endList = fstElmnt.getElementsByTagName("tname");

                Element startElement = (Element) startList.item(0);
                startList = startElement.getChildNodes();

                Element endElement = (Element) endList.item(0);
                endList = endElement.getChildNodes();

                String startStation = ((Node) startList.item(0)).getNodeValue();
                String startStation_temp = startStation.substring(0,startStation.length()-1);

                String endStation = ((Node) endList.item(0)).getNodeValue();
                String endStation_temp = endStation.substring(0,endStation.length()-1);

                station_list.add(startStation_temp);

                if (endStation_temp.equals(end_station)) {
                    station_list.add(endStation_temp);
                    break;
                } else
                    i++;
            }

            String s="";
            for(int j=0;j<station_list.size();j++){
                s += station_list.get(j) + "\n";
            }
            Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();

            super.onPostExecute(doc);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
        thread_run = false;
    }
}