package com.qixin.ecgview;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.qixin.ecg.EcgView;

public class MainActivity extends AppCompatActivity {

    EcgView mEcgView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mEcgView = findViewById(R.id.ecg_view);

        float[] floats = getFloats();
        mEcgView.setLeadData(floats);
    }

    private float[] getFloats() {
        String[] array = Constant.STRING_ECG_VALUE.split(",");
        float[] floats = new float[array.length];
        for (int i = 0; i < floats.length; i++) {
            floats[i] = Float.parseFloat(array[i]);
        }
        return floats;
    }
}
