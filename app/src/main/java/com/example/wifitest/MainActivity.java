package com.example.wifitest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ThemedSpinnerAdapter;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 1;//设置权限之后回调函数中用于区别不同权限回调的自定义常量值
    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION = 1;//设置权限之后回调函数中用于区别不同权限回调的自定义常量值

    WifiManager wifiManager;
    String wifi_name;
    int wifi_rssi;
    boolean isUpdate = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registerBroadcast();
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        //定义按键实例
        Button button1 = findViewById(R.id.wifi_rssi);
        //定义按键实例
        Button wifiScanButton = findViewById(R.id.wifi_scan);
        //定义位置文本框
        EditText locationEdit = findViewById(R.id.location);
        //定义扫描次数文本框
        EditText scanTimesEdit = findViewById(R.id.scan_times);

        //定义按钮点击事件
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //想要获得wifi信息就必须要一个WifiManager对象
                wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);//获取wifi服务
                assert wifiManager != null;

                //创建WifiInfo对象
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                wifi_name = wifiInfo.getSSID();

                //华为手机要通过下面代码才可以获得wifi名称
                int networkID = wifiInfo.getNetworkId();
                List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
                for (WifiConfiguration wifiConfiguration : configuredNetworks) {
                    if (wifiConfiguration.networkId == networkID) {
                        wifi_name = wifiConfiguration.SSID;
                        break;
                    }
                }

                wifi_rssi = wifiInfo.getRssi();
                //通过Toast输出
                Toast.makeText(MainActivity.this, "current WIFI:" + "rssi:" + wifi_rssi + "---wifiId:" + wifi_name, Toast.LENGTH_SHORT).show();
            }
        });

        //定义按钮点击事件
        wifiScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Toast.makeText(MainActivity.this, "开始扫描", Toast.LENGTH_SHORT).show();

                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);
                            return;
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION);
                            return;
                        }

                        wifiManager.setWifiEnabled(true);
                        int scanTime = Integer.parseInt(scanTimesEdit.getText().toString());
                        String location = locationEdit.getText().toString();
                        WifiScanResult wifiScanResult = getWifiList(scanTime);//调用上面函数获取wifi列表
                        Long startTs = System.currentTimeMillis();
                        writeRssi(startTs.toString() + "-" + location + ".txt", wifiScanResult.toString());
                        //通过Toast输出
                        Looper.prepare();
                        Toast.makeText(MainActivity.this, "扫描完成", Toast.LENGTH_SHORT).show();
                        Looper.loop();
                    }
                });

                t.start();

            }
        });

    }


    private void registerBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.setPriority(2147483647);
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(mReceiver, filter);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            // 该扫描已成功完成。
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                isUpdate = true;
            }
        }
    };


    //扫描wifi列表
    //通过wifiManager获取wifi列表
    public WifiScanResult getWifiList(int scanTimes) {

        wifiManager.setWifiEnabled(true);
        WifiScanResult wifiScanResult = new WifiScanResult();

        isUpdate = false;
        for (int t = 0; t < scanTimes; t++) {
            wifiManager.startScan();
            while (!isUpdate) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            isUpdate = false;

            List<ScanResult> scanWifiList = wifiManager.getScanResults();
            Map<String, Integer> map = new HashMap<>();
            if (scanWifiList != null && scanWifiList.size() > 0) {
                for (int i = 0; i < scanWifiList.size(); i++) {
                    ScanResult scanResult = scanWifiList.get(i);
                    System.out.println(scanResult.BSSID+": "+scanResult.SSID);
                    map.put(scanResult.BSSID, scanResult.level);
                }
            }
            wifiScanResult.result.add(map);

        }
        return wifiScanResult;
    }


    /**
     * @param filesname: 文件夹的名字
     * @Function: 测试内部存储  在公共存储目录下新建文件夹
     * @attention: 一直存在 app卸载后依然存在 存储在文件的公共目录下的 比如说 打开 NANDFlash 会出现 Movies Pictures Downlaod 等等
     * @Return:
     */
    private String writeRssi(String filesname, String data) {
        String pulicfileDir = null;

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) { //测试是否挂载SD卡，并且是否加载了权限
            return null;
        }

        pulicfileDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getPath();
        String fileName = pulicfileDir + File.separator + filesname;
        File subfile = new File(fileName);

        if (subfile.exists()) {
            subfile.setWritable(true);

        } else {
            try {
                subfile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        FileOutputStream fileOutputStream = null;
        BufferedWriter bufferedWriter = null;
        OutputStreamWriter outputStreamWriter = null;
        try {
            //fileOutputStream = openFileOutput(FileName, Context_Mode);  contains a path separator 报错
            fileOutputStream = new FileOutputStream(subfile);
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(fileOutputStream, "utf-8"));  //解决输入中文的问题

            bufferedWriter.write(data);

            bufferedWriter.flush();
            bufferedWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
            //Log.i(LOG_Error, "写入数据出错 " + e.getMessage());
        } finally {
            if (bufferedWriter != null) {
                try {
                    bufferedWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return (pulicfileDir + File.separator + filesname);   //返回的是文件的目录
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION) {
            //getWifiList();
        }
    }

}
