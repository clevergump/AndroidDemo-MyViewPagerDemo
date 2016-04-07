package com.clevergump.my_viewpager_demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.clevergump.my_viewpager_demo.widget.RefactoredMyViewPager2;

public class MainActivity2 extends AppCompatActivity {

    private RefactoredMyViewPager2 mMyViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        initView();
    }

    private void initView() {
        mMyViewPager = (RefactoredMyViewPager2) findViewById(R.id.myViewPager2);
    }

    public void getCurrScrollX(View view) {
        int scrollX = mMyViewPager.getScrollX();
        Toast.makeText(MainActivity2.this, "scrollX = " + scrollX, Toast.LENGTH_LONG).show();
    }
}