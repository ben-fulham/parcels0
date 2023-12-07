package com.example.parcels;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.parcels.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private ProgressDialog mProgressDialog;
    private TesseractOCR mTessOCR;
    private Context context;
    private String mCurrentPhotoPath;
    private Uri photoURI1;
    private Uri oldPhotoURI;

    private static final String errorFileCreate = "Error file create!";
    private static final String errorConvert = "Error convert!";
    private static final int REQUEST_IMAGE1_CAPTURE = 1;

    private ActivityMainBinding binding;

    int PERMISSION_ALL = 1;
    boolean flagPermissions = true;

    String[] PERMISSIONS = {
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        binding.scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickScanButton();
            }
        });

        //setContentView(R.layout.activity_main);
        context = MainActivity.this;

        //ButterKnife.bind(this);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        if (!flagPermissions) {
            checkPermissions();
        }
        String language = "eng";
        mTessOCR = new TesseractOCR(this, language);
    }

    @Override
    protected void onActivityResult(int requestcode, int resultcode, @Nullable Intent data) {
        super.onActivityResult(requestcode, resultcode, data);

        switch (requestcode) {
            case REQUEST_IMAGE1_CAPTURE: {
                if (resultcode == RESULT_OK) {
                    Bitmap bmp = null;
                    try {
                        InputStream is = context.getContentResolver().openInputStream(photoURI1);
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        bmp = BitmapFactory.decodeStream(is, null, options);
                    } catch (Exception ex) {
                        Log.i(getClass().getSimpleName(), ex.getMessage());
                        Toast.makeText(context, errorConvert, Toast.LENGTH_SHORT).show();
                    }

                    binding.ocrImage.setImageBitmap(bmp);
                    doOCR(bmp);

                    OutputStream os;
                    try {
                        os = new FileOutputStream(photoURI1.getPath());
                        if (bmp != null) {
                            bmp.compress(Bitmap.CompressFormat.JPEG, 100, os);
                        }
                        os.flush();
                        os.close();
                        } catch (Exception ex) {
                        Log.e(getClass().getSimpleName(), ex.getMessage());
                        Toast.makeText(context, errorFileCreate, Toast.LENGTH_SHORT).show();
                        }
                    }

                else {
                    photoURI1 = oldPhotoURI;
                    binding.ocrImage.setImageURI(photoURI1);

                    }
                }
            }
        }

    void checkPermissions() {
        if (!hasPermissions(context, PERMISSIONS)) {
            requestPermissions(PERMISSIONS,
                    PERMISSION_ALL);
            flagPermissions = false;
        }
        flagPermissions = true;
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("MMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    //@OnClick(R.id.scan_button)
    void onClickScanButton() {
        // check permissions
        if (!flagPermissions) {
            checkPermissions();
            return;
        }
        //prepare intent
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(context.getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(context, errorFileCreate, Toast.LENGTH_SHORT).show();
                Log.i("File error", ex.toString());
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                oldPhotoURI = photoURI1;
                photoURI1 = Uri.fromFile(photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI1);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE1_CAPTURE);
            }
        }
    }

    private void doOCR(final Bitmap bitmap) {
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog.show(this, "Processing", "Doing OCR...", true);
        }
        else {
            mProgressDialog.show();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String srcText = mTessOCR.getOCRResult(bitmap);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (srcText != null && !srcText.equals("")) {
                            binding.ocrText.setText(srcText);
                        }
                        mProgressDialog.dismiss();
                    }
                });
            }
        }).start();
    }

}
