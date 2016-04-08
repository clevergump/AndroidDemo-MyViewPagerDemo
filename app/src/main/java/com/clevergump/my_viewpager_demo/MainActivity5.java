package com.clevergump.my_viewpager_demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.clevergump.my_viewpager_demo.widget.MyViewPager5;

import java.util.LinkedList;
import java.util.List;

public class MainActivity5 extends AppCompatActivity {

    private MyViewPager5 mMyViewPager;
    private ListView mLv;
    private List<String> mDataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main5);

        initView();
        initData();
        initSetting();
    }

    private void initView() {
        mMyViewPager = (MyViewPager5) findViewById(R.id.myViewPager);
        mLv = (ListView) findViewById(R.id.lv);
    }

    private void initData() {
        mDataList = new LinkedList<String>();
        for (int i = 0; i < 100; i++) {
            mDataList.add("List Item " + i);
        }
    }

    private void initSetting() {
        mLv.setAdapter(new ArrayAdapter<String>(MainActivity5.this, android.R.layout.simple_list_item_1,
                android.R.id.text1, mDataList));
    }

    public void getCurrScrollX(View view) {
        int scrollX = mMyViewPager.getScrollX();
        Toast.makeText(MainActivity5.this, "scrollX = " + scrollX, Toast.LENGTH_LONG).show();
    }
}