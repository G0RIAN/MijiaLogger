package de.gorian.mijiaLogger.ui.display;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.gorian.mijiaLogger.R;

public class DisplayFragment extends Fragment {

    public static final String TAG = "MijiaLogger";
    private static final long BTLE_SEARCH_INTERVAL_MS = 30000;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private List<BluetoothDevice> devices = new ArrayList<>();
    private List<BluetoothDevice> supportedDevices = new ArrayList<>();
    private Handler handler = new Handler();
    private ScanCallback leScanCallback;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             final ViewGroup container, Bundle savedInstanceState) {
        DisplayViewModel displayViewModel = ViewModelProviders.of(this).get(DisplayViewModel.class);
        View root = inflater.inflate(R.layout.fragment_display, container, false);

        initBTLE();

        leScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                BluetoothDevice device = result.getDevice();
                if (devices.contains(device)) {
                    return;
                }
                Log.i(TAG, "New BLE device found: " + device.getName() + " " + device.getAddress());
                if (device.getName().contains("MJ_HT_V1")) {
                    bluetoothLeScanner.stopScan(leScanCallback);
                }
                devices.add(device);
            }
        };
        FloatingActionButton fab = root.findViewById(R.id.fab);
        if (fab == null) {
            Log.e(TAG, "FloatingAction Button fab is null. " + this);
            return root;
        }

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetFoundDevices();
                Log.i(TAG, "Suche nach BLE Geräten gestartet...");
                view.setEnabled(false);

                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        bluetoothLeScanner.startScan(leScanCallback);
                    }
                });

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "Suche nach BLE Geräten beendet.");
                        View frag_view = Objects.requireNonNull(requireFragmentManager().findFragmentByTag("ACTIVE_FRAGMENT")).requireView();
                        TextView status = frag_view.findViewById(R.id.status);
                        status.setText(R.string.scanning);
                        frag_view.findViewById(R.id.fab).setEnabled(true);
                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                bluetoothLeScanner.stopScan(leScanCallback);
                            }
                        });

                        Log.d(TAG, devices.size() + " devices found.");

                        // display found devices
                        for (BluetoothDevice device : devices) {
                            if (device.getName() != null && device.getName().contains("MJ_HT_V1")) {
                                // found LYWSDCGQ sensor
                                supportedDevices.add(device);
                            }
                        }
                        Log.d(TAG, supportedDevices.size() + " devices found.");
                        LinearLayout layout = frag_view.findViewById(R.id.card_layout);
                        if (supportedDevices.size() > 0) {
                            TableRow status_card = frag_view.findViewById(R.id.status_card);
                            status_card.setVisibility(View.GONE);
                        }
                        status.setText(R.string.no_devices);
                        for (BluetoothDevice supportedDevice : supportedDevices) {
                            Log.d(TAG, "Supported device found: " + supportedDevice.getName() + " " + supportedDevice.getAddress());
                            TableRow card = new TableRow(getActivity());
                            TableRow.LayoutParams params = new TableRow.LayoutParams(
                                    TableRow.LayoutParams.MATCH_PARENT,
                                    TableRow.LayoutParams.WRAP_CONTENT
                            );
                            int margin = dpToPixel(8);
                            params.setMargins(margin, margin, margin, margin);
                            card.setLayoutParams(params);
                            layout.addView(card);

                            card.addView(generateImageView());

                            LinearLayout textLayout = new LinearLayout(getActivity(), null, 0);
                            textLayout.setLayoutParams(new LinearLayout.LayoutParams(dpToPixel(150), LinearLayout.LayoutParams.MATCH_PARENT));
                            textLayout.setOrientation(LinearLayout.VERTICAL);


                            TextView card_name = new TextView(getActivity());
                            card_name.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                            ));
                            card_name.setText(supportedDevice.getName());

                            textLayout.addView(card_name);

                            TextView mac = new TextView(getActivity());
                            mac.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                            ));
                            mac.setText(supportedDevice.getAddress());
                            textLayout.addView(mac);

                            card.addView(textLayout);
                        }

                        //lay

                    }
                }, BTLE_SEARCH_INTERVAL_MS);
            }
        });
        return root;
    }

    public void initBTLE() {

        Log.d(TAG, "Check permissions: " + hasRequiredPermissions());
        if (!hasRequiredPermissions()) {
            Log.d(TAG, "Requesting permissions...");
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_COARSE_LOCATION}, PackageManager.PERMISSION_GRANTED);
            Log.d(TAG, "Done!");
        }
        Log.d(TAG, "Recheck permissions: " + hasRequiredPermissions());

        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) requireActivity().getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        }

        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            int REQUEST_ENABLE_BT = 42;
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

    }

    private boolean hasRequiredPermissions() {
        boolean hasBluetoothPermission = hasPermission(Manifest.permission.BLUETOOTH);
        boolean hasBluetoothAdminPermission = hasPermission(Manifest.permission.BLUETOOTH_ADMIN);
        boolean hasLocationPermission = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        return hasBluetoothPermission && hasBluetoothAdminPermission && hasLocationPermission;
    }

    private boolean hasPermission(String permission) {
        return ActivityCompat.checkSelfPermission(requireActivity(), permission) == PackageManager.PERMISSION_GRANTED;
    }

    private ImageView generateImageView() {
        ImageView imageView = new ImageView(getActivity());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPixel(100), dpToPixel(100));
        imageView.setLayoutParams(params);
        imageView.setImageResource(R.drawable.sensor);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setContentDescription(this.getResources().getString(R.string.sensor_image_desc));
        return imageView;
    }

    public void resetFoundDevices() {
        LinearLayout cards_layout = requireView().findViewById(R.id.card_layout);
        for (int ii = 0; ii < cards_layout.getChildCount(); ii++) {
            View child = cards_layout.getChildAt(ii);
            if (child.getId() != R.id.status_card) {
                cards_layout.removeViewAt(ii);
            }
        }
        requireView().findViewById(R.id.status_card).setVisibility(View.VISIBLE);
        this.devices.clear();
    }

    public int dpToPixel(int dp) {
        return Math.round(dp * this.getResources().getDisplayMetrics().density);
    }

}
