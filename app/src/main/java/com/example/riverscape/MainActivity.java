package com.example.riverscape;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.Closeable;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.os.Environment.getExternalStoragePublicDirectory;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    String currentPhotoPath;
    private Bitmap mImageBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /** Called when the user taps the cameraButton */
    public void cameraButtonListener(View view) {
        PackageManager pm = getPackageManager();

        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{ Manifest.permission.CAMERA }, MY_CAMERA_REQUEST_CODE);
            }
            else {
                launchCameraAndTakePicture();
            }
        }
        else {
            Toast.makeText(this, "Camera is not available.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCameraAndTakePicture();
            } else {
                Toast.makeText(this, "Camera permissions were denied.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void launchCameraAndTakePicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            ex.printStackTrace();
            // Error occurred while creating the File
            Toast.makeText(this, "Image could not be saved in a file.", Toast.LENGTH_LONG).show();

        }
        if (photoFile != null) {
            Uri photoURI = FileProvider.getUriForFile(this,
                    "com.example.android.fileprovider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        Log.i(data, "TRIAL LOGCAT TEXT");
        ImageView ivShowImage = findViewById(R.id.imageView);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            try {
                mImageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.fromFile(new File(currentPhotoPath)));

                try {
                    ExifInterface exif = new ExifInterface(currentPhotoPath);
                    int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
                    Log.d("EXIF", "Exif: " + orientation);
                    Matrix matrix = new Matrix();
                    if (orientation == 6) {
                        matrix.postRotate(90);
                    }
                    else if (orientation == 3) {
                        matrix.postRotate(180);
                    }
                    else if (orientation == 8) {
                        matrix.postRotate(270);
                    }
                    mImageBitmap = Bitmap.createBitmap(mImageBitmap, 0, 0, mImageBitmap.getWidth(), mImageBitmap.getHeight(), matrix, true); // rotating bitmap
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Couldn't rotate image.", Toast.LENGTH_LONG).show();
                }

                ivShowImage.setImageBitmap(mImageBitmap);
            } catch (Exception e) {         // convert this back to IOException
                e.printStackTrace();
                Toast.makeText(this, "Exception in getBitmap() method.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        Log.d("storageDir", storageDir.toString());
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
}