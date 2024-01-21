package com.example.parcels;

import static java.security.AccessController.getContext;

import android.Manifest;
import android.app.Activity;
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

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

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
    protected String mCurrentPhotoPath;
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

    /*ActivityResultLauncher<Intent> scanBtnResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Log.d("MainActivity", "onActivityResult: began OAR");
                    Log.d("MainActivity", String.format("onActivityResult: result.getResultCode: %d", result.getResultCode()));
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Log.d("MainActivity", String.format("onActivityResult: resultcode good: %d", result.getResultCode()));
                        Intent data = result.getData();
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
    );*/

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

        context = MainActivity.this;

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

        Log.d("MainActivity", "onActivityResult: began OAR");

        switch (requestcode) {
            case REQUEST_IMAGE1_CAPTURE: {
                if (resultcode == RESULT_OK) {
                    Log.d("MainActivity", String.format("onActivityResult: resultcode good: %d, requestcode: %d", resultcode, requestcode));
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
                    Log.d("MainActivity", String.format("onActivityResult: resultcode bad: %d, requestcode: %d", resultcode, requestcode));
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

    public static String bundle2string(Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        StringBuilder string = new StringBuilder("Bundle{");
        for (String key : bundle.keySet()) {
            string.append(" ").append(key).append(" => ").append(bundle.get(key)).append(";");
        }
        string.append(" }Bundle");
        return string.toString();
    }

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
                /*
                if (photoURI1 != null) {
                    Log.d("MainActivity", String.format("onClickScanButton: photoURI1: %s", photoURI1.toString()));
                }
                if (oldPhotoURI != null) {
                    Log.d("MainActivity", String.format("onClickScanButton: %s", oldPhotoURI.toString()));
                }
                if (photoURI1 == null) {
                    Log.d("MainActivity", "onClickScanButton: photoURI1 null");
                }
                if (oldPhotoURI == null) {
                    Log.d("MainActivity", "onClickScanButton: oldPhotoURI null");
                }
                */

                oldPhotoURI = photoURI1;
                photoURI1 = FileProvider.getUriForFile(getApplicationContext(), "com.example.parcels.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI1);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                Log.d("MainActivity", "onClickScanButton: EXTRA_OUTPUT: "+bundle2string(takePictureIntent.getExtras()));

                //scanBtnResultLauncher.launch(takePictureIntent);

                startActivityForResult(takePictureIntent, REQUEST_IMAGE1_CAPTURE);

                if (photoURI1 != null) {
                    Log.d("MainActivity", String.format("onClickScanButton: photoURI1: %s", photoURI1.toString()));
                }
                /*
                if (oldPhotoURI != null) {
                    Log.d("MainActivity", String.format("onClickScanButton: %s", oldPhotoURI.toString()));
                }
                if (photoURI1 == null) {
                    Log.d("MainActivity", "onClickScanButton: photoURI1 null");
                }
                if (oldPhotoURI == null) {
                    Log.d("MainActivity", "onClickScanButton: oldPhotoURI null");
                }
                */
            }
        }
        else {
            Log.d("MainActivity", "onClickScanButton: takepicintent resolveactivity was null");
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
