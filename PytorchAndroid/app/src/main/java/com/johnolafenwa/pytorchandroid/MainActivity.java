package com.johnolafenwa.pytorchandroid;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    int cameraRequestCode = 001;
    int albumRequestCode = 002;

    Classifier classifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Button capture = findViewById(R.id.capture);
        Button album = findViewById(R.id.album);
        Button detect = findViewById(R.id.detect);
        final TextView textView = findViewById(R.id.result);

        classifier = new Classifier(Utils.assetFilePath(this,"mobilenet-v2.pt"));

        detect.setEnabled(false);

        capture.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view){
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent,cameraRequestCode);
            }


        });

        album.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent albumIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//                albumIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(albumIntent, albumRequestCode);
            }
        });

        detect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bitmap = null;
                //Getting the image from the image view
                ImageView imageView = (ImageView) findViewById(R.id.ivPreview);
                //Read the image as Bitmap
                bitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
                String pred = classifier.predict(bitmap);
                textView.setText(pred);
            }
        });

        verifyPermissions(this);
    }

    private static final int GET_CAMERA = 1;

    private static String[] PERMISSION_ALL = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
    };

    /** Get CAMERA and STORAGE permissions */
    public static void verifyPermissions(Activity activity) {
        boolean permission = (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                || (ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                || (ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED);
        if (permission) {
            ActivityCompat.requestPermissions(activity, PERMISSION_ALL, GET_CAMERA);
        }
    }

    public static String getPath(Context context, Uri uri) {
        String result = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver( ).query(uri, proj, null, null, null);
        if(cursor != null){
            if ( cursor.moveToFirst( ) ) {
                int column_index = cursor.getColumnIndexOrThrow( proj[0] );
                result = cursor.getString( column_index );
            }
            cursor.close( );
        }
        if(result == null) {
            result = "Not found";
        }
        return result;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == cameraRequestCode && resultCode == RESULT_OK){
//            Intent resultView = new Intent(this,Result.class);
//            resultView.putExtra("imagedata",data.getExtras());

            Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
            ImageView ivPreview = findViewById(R.id.ivPreview);
            ivPreview.setImageBitmap(imageBitmap);
            ((TextView) findViewById(R.id.result)).setText("");
            findViewById(R.id.detect).setEnabled(true);

//            String pred = classifier.predict(imageBitmap);
//            resultView.putExtra("pred",pred);
//            startActivity(resultView);
        }
        else if(requestCode == albumRequestCode && resultCode == RESULT_OK && data != null){
            Log.d("album", String.format("image selected"));
            Uri photoUri = data.getData();

            String realUri = getPath(getApplicationContext(), photoUri);
            Bitmap selectedImage = BitmapFactory.decodeFile(realUri);

            ImageView ivPreview = (ImageView) findViewById(R.id.ivPreview);
            ivPreview.setImageBitmap(selectedImage);
            ((TextView) findViewById(R.id.result)).setText("");
            findViewById(R.id.detect).setEnabled(true);
        }
    }
}
