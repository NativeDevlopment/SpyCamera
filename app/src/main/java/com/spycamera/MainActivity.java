package com.spycamera;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
Button buttonCamera,buttonVideo,btnpdf;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buttonCamera=(Button)findViewById(R.id.btnCamera);
        buttonVideo=(Button)findViewById(R.id.btnVideo);
        btnpdf=(Button)findViewById(R.id.btnpdf);

    }
    public void  CameraClick(View view){
        startActivity(new Intent(this,CameraActivity.class));

    }
    public  void VideoClick(View view){
        startActivity(new Intent(this,VideoActivity.class));
    }
    public  void PdfClick(View view){
        startActivity(new Intent(this,GenerarPDFActivity.class));
    }
}
