package com.example.dnervecairo;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.dnervecairo.fragments.HomeFragment;
import com.example.dnervecairo.fragments.LeaderboardFragment;
import com.example.dnervecairo.fragments.RewardsFragment;
import com.example.dnervecairo.fragments.ProfileFragment;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_navigation);

        // Load home fragment by default
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        // Handle navigation clicks
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (itemId == R.id.nav_leaderboard) {
                fragment = new LeaderboardFragment();
            } else if (itemId == R.id.nav_rewards) {
                fragment = new RewardsFragment();
            } else if (itemId == R.id.nav_profile) {
                fragment = new ProfileFragment();
            }

            return loadFragment(fragment);
        });
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }
}