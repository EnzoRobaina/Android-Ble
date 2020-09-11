package com.enzorobaina.bleapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.google.android.material.snackbar.Snackbar;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private static final int SCAN_DURATION = 5000; // ms

    public static final int REQUEST_BT_PERMISSIONS = 0;
    public static final int REQUEST_BT_ENABLE = 1;

    public static final int REQUEST_BT_RESULT_YES = -1;
    public static final int REQUEST_BT_RESULT_NO = 0;

    private Handler autoStopScanHandler = new Handler();

    private boolean isScanning = false;

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            Log.d("scanCallback/result", String.format("callbackType: %d\nresult: %s", callbackType, result.toString()));
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.d("scanCallback/failed", String.format("errorCode: %d", errorCode));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    private void verifyAll(){
        if (hasLeBluetoothFeature()){
            if (BluetoothAdapter.getDefaultAdapter() != null && BluetoothAdapter.getDefaultAdapter().isEnabled()){
                checkBtPermissions();
            }
            else {
                askToTurnOnBluetooth();
            }
        }
        else {
            Toast.makeText(this, "O Dispositivo não suporta LEBT", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    public void onFabClick(View view){
        verifyAll();
    }

    private boolean hasLeBluetoothFeature(){
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    private void askToTurnOnBluetooth(){
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_BT_ENABLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("onActivityResult", String.format("requestCode was %d\nresultCode is %d\ndata is %s", requestCode, resultCode, data == null ? "no data" : data.toString()));
        if (requestCode == REQUEST_BT_ENABLE) { // Resposta da solicitação de ativação do bt
            if (resultCode == REQUEST_BT_RESULT_YES){
                checkBtPermissions();
            }
            else {
                Snackbar.make(findViewById(R.id.mainConstraint), "Por favor, ative o bluetooth!", Snackbar.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        Log.d("onRequestPermissionsRes", String.format("requestCode was %d\naskedPermissions are %s\ngrantedResults are %s", requestCode, Arrays.toString(permissions), Arrays.toString(grantResults)));

        if (requestCode == REQUEST_BT_PERMISSIONS){ // Resposta da solicitação da permissão do bt
            boolean allPermissionsWereGranted = false;
            for (int grantResult : grantResults) {
                if (grantResult == 0) {
                    allPermissionsWereGranted = true;
                }
                else {
                    allPermissionsWereGranted = false;
                    break;
                }
            } // Todo: kill me

            if (allPermissionsWereGranted){
                scanForLeDevices(BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner());
            }
            else {
                Snackbar.make(findViewById(R.id.mainConstraint), "Por favor, permita acesso ao bluetooth e à localização!", Snackbar.LENGTH_LONG).show();
            }
        }
    }

    public void checkBtPermissions() {
        this.requestPermissions(
            new String[]{
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            },
            REQUEST_BT_PERMISSIONS
        );
    }

    public void scanForLeDevices(@NonNull BluetoothLeScanner leScanner) {
        if (isScanning){ return; }

        isScanning = true;
        leScanner.startScan(scanCallback);
        Log.d("scan", "started!");
        Snackbar.make(findViewById(R.id.mainConstraint), "Scan em progresso...", Snackbar.LENGTH_SHORT).show();

        autoStopScanHandler.postDelayed(() -> stopScan(leScanner), SCAN_DURATION);
    }

    private void stopScan(@NonNull BluetoothLeScanner leScanner){
        if (!isScanning){ return; }

        isScanning = false;
        leScanner.stopScan(scanCallback);
        Log.d("scan", "stopped!");
        Snackbar.make(findViewById(R.id.mainConstraint), "Scan finalizado.", Snackbar.LENGTH_SHORT).show();
    }
}
