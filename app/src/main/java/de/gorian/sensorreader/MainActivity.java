package de.gorian.sensorreader;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {

	public static String TAG;
	private AppBarConfiguration mAppBarConfiguration;
	private BluetoothLeService bluetoothLeService;
	private Handler handler = new Handler();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		TAG = getResources().getString(R.string.app_name);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		FloatingActionButton fab = findViewById(R.id.fab);
		DrawerLayout drawer = findViewById(R.id.drawer_layout);
		NavigationView navigationView = findViewById(R.id.nav_view);
		// Passing each menu ID as a set of Ids because each
		// menu should be considered as top level destinations.
		mAppBarConfiguration = new AppBarConfiguration.Builder(
				R.id.nav_sensors, R.id.nav_logging, R.id.nav_settings)
				.setOpenableLayout(drawer)
				.build();
		final NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
		NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
		NavigationUI.setupWithNavController(navigationView, navController);

		// start bluetooth service
		startService(new Intent(this, BluetoothLeService.class));

		fab.setOnClickListener(view -> {
			navController.navigate(R.id.nav_sensors);

			Snackbar sb = Snackbar.make(findViewById(R.id.nav_host_fragment), R.string.scanning, Snackbar.LENGTH_INDEFINITE).setAction("STOP", view1 -> stopScan());
			sb.getView().setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.snackbarColor));
			sb.show();

			searchBLEDevices();
		});

		bluetoothLeService = BluetoothLeService.getINSTANCE();

	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onSupportNavigateUp() {
		NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
		return NavigationUI.navigateUp(navController, mAppBarConfiguration)
				|| super.onSupportNavigateUp();
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 42 && resultCode == RESULT_OK) {
			// user enabled Bluetooth, so start Scanning
			searchBLEDevices();
		} else {
			Log.d(TAG, "Bluetooth enable request returned " + resultCode);
		}
	}

	private boolean requestPermissions() {
		if (!bluetoothLeService.hasRequiredPermissions()) {
			Log.d(TAG, "Requesting permissions...");
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_COARSE_LOCATION}, PackageManager.PERMISSION_GRANTED);
			Log.d(TAG, "Done!");
		} else return true;
		Log.d(TAG, "Recheck permissions: " + bluetoothLeService.hasRequiredPermissions() + "!!!");
		if (!bluetoothLeService.hasRequiredPermissions()) {
			Snackbar sb = Snackbar.make(findViewById(R.id.nav_host_fragment), "Bluetooth and Location permission are needed to use the bluetooth module. ", Snackbar.LENGTH_LONG).setAction("ASK AGAIN", view -> ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_COARSE_LOCATION}, PackageManager.PERMISSION_GRANTED));
			sb.getView().setBackgroundColor(ContextCompat.getColor(this, R.color.snackbarColor));
			sb.show();
			return false;
		}
		return true;
	}

	public int dpToPixel(int dp) {
		return Math.round(dp * this.getResources().getDisplayMetrics().density);
	}

	private void resetFoundDevices() {
		bluetoothLeService.clearDevices();
		LinearLayout devicesList = findViewById(R.id.devices_list);
		devicesList.removeAllViews();
	}

	void stopScan() {
		bluetoothLeService.stopScan();
		int nDevices = bluetoothLeService.getNumberOfDevices();
		String resultMessage = (nDevices == 0 ? "No" : nDevices) + " compatible " + (nDevices == 1 ? "device" : "devices") + " found.";
		Snackbar sb;
		if (nDevices == 0) {
			sb = Snackbar.make(findViewById(R.id.nav_host_fragment), resultMessage, Snackbar.LENGTH_LONG).setAction("RETRY", view -> searchBLEDevices());
		} else {
			sb = Snackbar.make(findViewById(R.id.nav_host_fragment), resultMessage, Snackbar.LENGTH_LONG);
			final Snackbar sbF = sb;
			sb.setAction("DISMISS", view -> sbF.dismiss());
		}
		sb.getView().setBackgroundColor(ContextCompat.getColor(this, R.color.snackbarColor));
		sb.show();
		Log.d(TAG, resultMessage);


	}

	void startScan() {
		bluetoothLeService.startScan();
	}

	void searchBLEDevices() {

		if (bluetoothLeService == null) {
			bluetoothLeService = BluetoothLeService.getINSTANCE();
		}
		requestPermissions();
		if (!bluetoothLeService.isInitialized()) return;
		bluetoothLeService.clearDevices();
		Log.i(TAG, "Suche nach BLE Geräten gestartet...");
		AsyncTask.execute(this::startScan);

		handler.postDelayed(() -> {
			Log.i(TAG, "Suche nach BLE Geräten beendet.");
			AsyncTask.execute(this::stopScan);
		}, BluetoothLeService.BLE_SEARCH_INTERVAL_MS);
	}


}