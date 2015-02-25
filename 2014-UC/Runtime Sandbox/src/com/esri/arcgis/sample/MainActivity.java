package com.esri.arcgis.sample;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;

public class MainActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    FragmentManager fragmentManager = getFragmentManager();
    fragmentManager.beginTransaction()
        .add(R.id.container, MapFragment.newInstance(), MapFragment.TAG)
        .commit();
    
    ActionBar actionBar = getActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setHomeButtonEnabled(true);
    actionBar.setDisplayShowTitleEnabled(false);
  }
}
