package com.clevergump.my_viewpager_demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.clevergump.my_viewpager_demo.widget.MyViewPager1;

public class MainActivity1 extends AppCompatActivity {

    private MyViewPager1 mMyViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main1);

        initView();
    }

    private void initView() {
        mMyViewPager = (MyViewPager1) findViewById(R.id.myViewPager1);
    }
}