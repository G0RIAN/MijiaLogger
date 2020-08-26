package de.gorian.mijiaLogger;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.ui.AppBarConfiguration;

import com.google.android.material.navigation.NavigationView;

import de.gorian.mijiaLogger.ui.display.DisplayFragment;
import de.gorian.mijiaLogger.ui.logging.LoggingFragment;
import de.gorian.mijiaLogger.ui.settings.SettingsFragment;

public class MainActivity extends AppCompatActivity {

    public DisplayFragment displayFragment;
    public LoggingFragment loggingFragment;
    public SettingsFragment settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationContentDescription("Navigation");
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each menu should be considered as top level destinations.
        AppBarConfiguration mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_display, R.id.nav_logs, R.id.nav_settings)
                .setDrawerLayout(drawer)
                .build();
//        NavController navController = Navigation.findNavController(this, R.id.nav_bar_fragment);
//        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
//        NavigationUI.setupWithNavController(navigationView, navController);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == R.id.nav_display) {
                    if (displayFragment == null) displayFragment = new DisplayFragment();
                    loadFragment(displayFragment);
                } else if (id == R.id.nav_logs) {
                    if (loggingFragment == null) loggingFragment = new LoggingFragment();
                    loadFragment(loggingFragment);
                } else if (id == R.id.nav_settings) {
                    if (settingsFragment == null) settingsFragment = new SettingsFragment();
                    loadFragment(settingsFragment);
                }
                ((DrawerLayout) findViewById(R.id.drawer_layout)).closeDrawer(GravityCompat.START);
                return true;
            }
        });

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.frame, displayFragment = new DisplayFragment(), "ACTIVE_FRAGMENT");
        transaction.commit();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

//    @Override
//    public boolean onSupportNavigateUp() {
//        NavController navController = Navigation.findNavController(this, R.id.nav_bar_fragment);
//        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
//                || super.onSupportNavigateUp();
//    }

    public void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.frame, fragment, "ACTIVE_FRAGMENT");
        transaction.addToBackStack(null);
        transaction.commit();
    }

}