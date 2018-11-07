package com.okg.utils.toastutil;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.baymax.util.ToastUtil;

public class MainActivity extends AppCompatActivity {
    Button btnUnfixed,btnFixed;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initListener();
    }

    private void initView(){
        btnUnfixed = (Button)findViewById(R.id.btn_unfixed);
        btnFixed = (Button)findViewById(R.id.btn_fixed);
    }

    private void initListener(){
        btnUnfixed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,"I am origin Toast without fix",Toast.LENGTH_SHORT).show();
                try {
                    // just sleep and block the main thread which will reappear the BadTokenException
                    Thread.sleep(10000);
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        });

        btnFixed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // just sleep and block the main thread which will reappear the BadTokenException
                ToastUtil.showToast(MainActivity.this,"I am fixed Toast");
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        });
    }
}
