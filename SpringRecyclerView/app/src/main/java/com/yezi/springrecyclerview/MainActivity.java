package com.yezi.springrecyclerview;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
    }

    public void SRVClick(View view) {
        startActivity(new Intent(this, SpringRVActivity.class));
    }

    public void SSVClick(View view) {
        startActivity(new Intent(this, SpringSVActivity.class));
    }
}
