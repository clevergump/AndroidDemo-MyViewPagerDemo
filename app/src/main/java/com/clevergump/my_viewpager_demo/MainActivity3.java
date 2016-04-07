package com.clevergump.my_viewpager_demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.clevergump.my_viewpager_demo.widget.MyViewPager3;

public class MainActivity3 extends AppCompatActivity {

    private MyViewPager3 mMyViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);

        initView();
    }

    private void initView() {
        mMyViewPager = (MyViewPager3) findViewById(R.id.myViewPager);
    }

    public void getCurrScrollX(View view) {
        int scrollX = mMyViewPager.getScrollX();
        Toast.makeText(MainActivity3.this, "scrollX = " + scrollX, Toast.LENGTH_LONG).show();
    }
}