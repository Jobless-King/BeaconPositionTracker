// Copyright 2015 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sample.eddystonevalidator;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MainActivity for the Eddystone Validator sample app.
 */
public class MainActivity extends Activity {

    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private static final double XLength = 800;
    private static final double YLength = 800;
    private static final String TAG = "EddystoneValidator";
    private static final String FrontWindowAddress = "D6:83:D0:44:2D:0A";
    private static final String FrontHallwayAddress = "F8:FA:13:45:7B:40";
    private static final String BackWindowAddress = "E1:D6:CF:CE:8F:3D";
    private static final String BackHallwayAddress = "EA:6D:32:1C:97:20";

    // An aggressive scan for nearby devices that reports immediately.
    private static final ScanSettings SCAN_SETTINGS =
            new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(0)
                    .build();

    // The Eddystone Service UUID, 0xFEAA.
    private static final ParcelUuid EDDYSTONE_SERVICE_UUID = ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB");
    private BluetoothLeScanner scanner;
    private List<ScanFilter> scanFilters;
    private ScanCallback scanCallback;
    private Map<String, Beacon> deviceToBeaconMap = new HashMap<>();
    private TextView show1, show2;
    private PositionCanvas positionCanvas;
    private BeaconInfomation[] beaconInfo;
    private ArrayList<PositionSet> positionlist, positionobjlist;
    private String myJSON;
    private JSONArray positions;
    //정보출력용
    private String[] info;
    //시간체크용
    int currtime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        positionlist = new ArrayList<PositionSet>();
        init();

        scanFilters = new ArrayList<>();
        scanFilters.add(new ScanFilter.Builder().setServiceUuid(EDDYSTONE_SERVICE_UUID).build());
        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                ScanRecord scanRecord = result.getScanRecord();
                if (scanRecord == null) {
                    return;
                }
                String deviceAddress = result.getDevice().getAddress();
                Beacon beacon;
                if (!deviceToBeaconMap.containsKey(deviceAddress)) {
                    beacon = new Beacon(deviceAddress, result.getRssi());
                    deviceToBeaconMap.put(deviceAddress, beacon);
                } else {
                    deviceToBeaconMap.get(deviceAddress).rssi = result.getRssi();
                }
                byte[] serviceData = scanRecord.getServiceData(EDDYSTONE_SERVICE_UUID);

                setFilteredValue(deviceAddress, result.getRssi(), (int) serviceData[1]);
                setNewPosition();

                Log.v(TAG, deviceAddress + " " + Utils.toHexString(serviceData));
                validateServiceData(deviceAddress, serviceData);
            }

            @Override
            public void onScanFailed(int errorCode) {
                switch (errorCode) {
                    case SCAN_FAILED_ALREADY_STARTED:
                        logErrorAndShowToast("SCAN_FAILED_ALREADY_STARTED");
                        break;
                    case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                        logErrorAndShowToast("SCAN_FAILED_APPLICATION_REGISTRATION_FAILED");
                        break;
                    case SCAN_FAILED_FEATURE_UNSUPPORTED:
                        logErrorAndShowToast("SCAN_FAILED_FEATURE_UNSUPPORTED");
                        break;
                    case SCAN_FAILED_INTERNAL_ERROR:
                        logErrorAndShowToast("SCAN_FAILED_INTERNAL_ERROR");
                        break;
                    default:
                        logErrorAndShowToast("Scan failed, unknown error code");
                        break;
                }
            }
        };
    }

    @Override
    public void onPause() {
        super.onPause();
        if (scanner != null) {
            scanner.stopScan(scanCallback);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (scanner != null) {
            scanner.startScan(scanFilters, SCAN_SETTINGS, scanCallback);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == Activity.RESULT_OK) {
                init();
            } else {
                finish();
            }
        }
    }

    // Attempts to create the scanner.
    private void init() {

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        String time = String.valueOf(dateFormat.format(cal.getTime()));
        String time_s = time.substring(14, 15);
        currtime = Integer.parseInt(time_s);

        show1 = (TextView) findViewById(R.id.showDistances1);
        show2 = (TextView) findViewById(R.id.showDistances2);
        positionCanvas = (PositionCanvas) findViewById(R.id.seat);

        beaconInfo = new BeaconInfomation[4];
        beaconInfo[0] = new BeaconInfomation(FrontWindowAddress);
        beaconInfo[1] = new BeaconInfomation(FrontHallwayAddress);
        beaconInfo[2] = new BeaconInfomation(BackWindowAddress);
        beaconInfo[3] = new BeaconInfomation(BackHallwayAddress);

        info = new String[12];
        info[0] = FrontWindowAddress;
        info[3] = FrontHallwayAddress;
        info[6] = BackWindowAddress;
        info[9] = BackHallwayAddress;

        positions = null;

        BluetoothManager manager = (BluetoothManager) getApplicationContext()
                .getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = manager.getAdapter();
        if (btAdapter == null) {
            showFinishingAlertDialog("Bluetooth Error", "Bluetooth not detected on device");
        } else if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            this.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
        } else {
            scanner = btAdapter.getBluetoothLeScanner();
        }
    }
    private void setFilteredValue(String address, double rssi, double txPower) {

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        String time = String.valueOf(dateFormat.format(cal.getTime()));
        String time_s = time.substring(14, 15);
        int time_n = Integer.parseInt(time_s);

        if(currtime != time_n){
            //currtime = Integer.parseInt(time);
            init();
        }

        PositionSet newPositionSet;
        int beaconNum;
        if(address.equals(FrontWindowAddress)) {
            beaconNum = 0;
        } else if (address.contains(FrontHallwayAddress)) {
            beaconNum = 1;
        } else if (address.contains(BackWindowAddress)) {
            beaconNum = 2;
        } else if (address.contains(BackHallwayAddress)) {
            beaconNum = 3;
        }else{
            return;
        }
        valueFilter(beaconInfo[beaconNum], rssi, txPower);
        info[beaconNum*3 + 1] = String.valueOf(beaconInfo[beaconNum].getDistance());
    }
    private void valueFilter(BeaconInfomation info, double rssi, double txPower){
        info.getRawrssi().add(rssi);
        int size = info.getRawrssi().size();
        info.setTxpower(txPower);
        if(size > 10) {
            info.setRssi((info.getRssi() * (size - 1) / size) + (info.getFilter().update(rssi) / size));
            info.setDistance(Math.pow(10, ((txPower - info.getRssi()) / (10 * 2))));
        }else if(size == 10){//초깃값 셋팅
            ArrayList<Double> temp = info.getRawrssi();
            Collections.sort(temp);
            info.setRssi((temp.get(4) + temp.get(5)) / 2);
            info.getFilter().update(info.getRssi());
        }
    }
    private void setNewPosition() {
        PositionSet newPositionSet;
        if (beaconInfo[0].getDistance() != 0.0 && beaconInfo[1].getDistance() != 0.0 && beaconInfo[2].getDistance() != 0.0 && beaconInfo[3].getDistance() != 0.0) {
            //show1.setText(info[0] + " : " + info[1].substring(0, 6) + ", " + info[2].substring(0, 6) + "\n" + info[3] + " : " + info[4].substring(0, 6) + ", " + info[5].substring(0, 6) + "\n" + info[6] + " : " + info[7].substring(0, 6) + ", " + info[8].substring(0, 6) + "\n" + info[9] + " : " + info[10].substring(0, 6) + ", " + info[11].substring(0, 6) + "\n");
            show1.setText("4 completed");
            newPositionSet = trilateration(beaconInfo);


            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Calendar cal = Calendar.getInstance();
            String time = String.valueOf(dateFormat.format(cal.getTime()));
            time = time.substring(0,4) + time.substring(5,7) + time.substring(8,10) + " " + time.substring(11,13) + time.substring(14,15) + " ";

            String name = time+"recode.csv";
            String recodetext = beaconInfo[0].getDistance() + ", " + beaconInfo[1].getDistance() + ", " + beaconInfo[2].getDistance() + ", " +beaconInfo[3].getDistance() +", " + newPositionSet.getPosX() + ", " + newPositionSet.getPosY();
            recodeFile(name, recodetext);

            setName(newPositionSet);
            positionCanvas.setSoloPosition(newPositionSet);
        }
    }
    private PositionSet trilateration(BeaconInfomation[] beaconInfo){
        double fw = beaconInfo[0].getDistance();
        double fh = beaconInfo[1].getDistance();
        double bw = beaconInfo[2].getDistance();
        double bh = beaconInfo[3].getDistance();
        double[] compare = {fw, fh, bw, bh};
        Arrays.sort(compare);
        double x, y;
        if (compare[3] == bh) {
            x = (Math.pow(fh, 2) - Math.pow(fw, 2) + Math.pow(XLength, 2)) / (2 * XLength);
            y = (Math.pow(fw, 2) - Math.pow(bw, 2) + Math.pow(YLength, 2)) / (2 * YLength);
        } else if (compare[3] == bw) {
            x = (Math.pow(fh, 2) - Math.pow(fw, 2) + Math.pow(XLength, 2)) / (2 * XLength);
            y = (Math.pow(fh, 2) - Math.pow(bh, 2) + Math.pow(YLength, 2)) / (2 * YLength);
        } else if (compare[3] == fh) {
            x = (Math.pow(bh, 2) - Math.pow(bw, 2) + Math.pow(XLength, 2)) / (2 * XLength);
            y = (Math.pow(fw, 2) - Math.pow(bw, 2) + Math.pow(YLength, 2)) / (2 * YLength);
        } else {
            x = (Math.pow(bh, 2) - Math.pow(bw, 2) + Math.pow(XLength, 2)) / (2 * XLength);
            y = (Math.pow(fh, 2) - Math.pow(bh, 2) + Math.pow(YLength, 2)) / (2 * YLength);
        }

        PositionSet new_posititon = new PositionSet(null, x, y);
        return new_posititon;
    }

    private void setName(PositionSet positionset) {
        //이름정보를 받아옴
        //positionset.setName(UUID.randomUUID().toString().substring(0,5));
        positionset.setName("Dameit");
    }

    private void drawPosition(ArrayList<PositionSet> positionset) {
        positionCanvas.setPosition(positionset);
    }

    private void recodeFile(String name, String str) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File file = new File(Environment.getExternalStorageDirectory(), name);
            try {
                FileWriter fw = new FileWriter(file, true);
                fw.write(str + "\n");
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("error", "error");
            }
        } else
            Log.d("error", "External Storage is not ready");
    }

    private ArrayList<PositionSet> makeMap() {
        try {
            positionobjlist = new ArrayList<PositionSet>();
            JSONObject jsonObj = new JSONObject(myJSON);
            positions = jsonObj.getJSONArray("result");
            String temp = "";
            for (int i = 0; i < positions.length(); i++) {
                JSONObject c = positions.getJSONObject(i);
                String _id = c.getString("_id");
                String deviceid = c.getString("deviceid");
                double X = Double.parseDouble(c.getString("X"));
                double Y = Double.parseDouble(c.getString("Y"));
                PositionSet new_posititon = new PositionSet(deviceid, X, Y);
                positionobjlist.add(new_posititon);
            }
            return positionobjlist;
        } catch (Exception e) {
            Log.d("errpr", e.toString());
            return null;
        }
    }

    public void logOut(View view) {
        SetData setData = new SetData("andro111", 143,1113);
        setData.execute();
    }

    // Pops an AlertDialog that quits the app on OK.
    private void showFinishingAlertDialog(String title, String message) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                }).show();
    }

    // Checks the frame type and hands off the service data to the validation module.
    private void validateServiceData(String deviceAddress, byte[] serviceData) {
        Beacon beacon = deviceToBeaconMap.get(deviceAddress);
        if (serviceData == null) {
            String err = "Null Eddystone service data";
            beacon.frameStatus.nullServiceData = err;
            logDeviceError(deviceAddress, err);
            return;
        }
        switch (serviceData[0]) {
            case Constants.UID_FRAME_TYPE:
                UidValidator.validate(deviceAddress, serviceData, beacon);
                break;
            case Constants.TLM_FRAME_TYPE:
                TlmValidator.validate(deviceAddress, serviceData, beacon);
                break;
            case Constants.URL_FRAME_TYPE:
                UrlValidator.validate(deviceAddress, serviceData, beacon);
                break;
            default:
                String err = String.format("Invalid frame type byte %02X", serviceData[0]);
                beacon.frameStatus.invalidFrameType = err;
                logDeviceError(deviceAddress, err);
                break;
        }
    }
    private void logErrorAndShowToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.e(TAG, message);
    }
    private void logDeviceError(String deviceAddress, String err) {
        Log.e(TAG, deviceAddress + ": " + err);
    }
    class SendData extends Thread {
        private String sql;
        public SendData(String pra_sql) {
            sql = pra_sql;
        }
        public void run() {
            try {
                DatagramSocket socket = new DatagramSocket();
                InetAddress serverAddr = InetAddress.getByName("168.188.129.136");
                byte[] buf = sql.getBytes();
                DatagramPacket packet = new DatagramPacket(buf, buf.length, serverAddr, 1848);
                socket.send(packet);
                Toast.makeText(getApplicationContext(), "SEND", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.d("Exception", e.toString());
            }
        }
    }

    class GetData extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String uri = "http://168.188.129.136/BeaconProject/getPosition.php";
            BufferedReader bufferedReader = null;
            try {
                URL url = new URL(uri);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                StringBuilder sb = new StringBuilder();
                bufferedReader = new BufferedReader((new InputStreamReader(con.getInputStream())));
                String json;
                while ((json = bufferedReader.readLine()) != null) {
                    sb.append(json + '\n');
                }
                return sb.toString().trim();
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            myJSON = s;
            drawPosition(makeMap());
        }
    }
    class SetData extends AsyncTask<String, Void, String> {
        String deviceid, X, Y;
        public SetData(String pra_id, double pra_x, double pra_y){
            deviceid = pra_id;
            X = String.valueOf(pra_x);
            Y = String.valueOf(pra_y);
        }
        @Override
        protected String doInBackground(String... strings) {
            String uri = "http://168.188.129.136/BeaconProject/setPosition.php";
            uri += "?deviceid=" + deviceid;
            uri += "&X=" + X;
            uri += "&Y=" + Y;
            try {
                URL url = new URL(uri);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.getInputStream();
                return null;
            } catch (Exception e) {
                return null;
            }
        }
    }
}