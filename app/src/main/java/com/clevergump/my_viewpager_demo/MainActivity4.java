package com.clevergump.my_viewpager_demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.clevergump.my_viewpager_demo.widget.MyViewPager4;

public class MainActivity4 extends AppCompatActivity {

    private MyViewPager4 mMyViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main4);

        initView();
    }

    private void initView() {
        mMyViewPager = (MyViewPager4) findViewById(R.id.myViewPager);
    }

    public void getCurrScrollX(View view) {
        int scrollX = mMyViewPager.getScrollX();
        Toast.makeText(MainActivity4.this, "scrollX = " + scrollX, Toast.LENGTH_LONG).show();
    }
}