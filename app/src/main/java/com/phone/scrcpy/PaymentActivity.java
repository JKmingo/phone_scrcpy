package com.phone.scrcpy;
import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import java.io.IOException;
import java.io.OutputStream;

public class PaymentActivity extends AppCompatActivity {

    private ImageView alipayImageView, wechatImageView;
    private TextView saveButton;
    private ImageButton backButton;

    private static final int REQUEST_STORAGE_PERMISSION = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // 初始化视图
        alipayImageView = findViewById(R.id.alipay_image);
        wechatImageView = findViewById(R.id.wechat_image);
        saveButton = findViewById(R.id.save_button);
        backButton = findViewById(R.id.back_button);

        // 设置返回按钮点击事件
        backButton.setOnClickListener(v -> onBackPressed());

        // 设置保存按钮点击事件
        saveButton.setOnClickListener(v -> {
            // 检查权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    // 请求权限
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
                }
            }
            // 保存两张图片
            saveImageToGallery(alipayImageView, "alipay_image");
            saveImageToGallery(wechatImageView, "wechat_image");
        });
    }

    // 保存图片到图库
    private void saveImageToGallery(ImageView imageView, String imageName) {
        imageView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(imageView.getDrawingCache());
        imageView.setDrawingCacheEnabled(false);

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, imageName);
        values.put(MediaStore.Images.Media.DISPLAY_NAME, imageName + ".png");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Image saved from app");

        // 将图片插入到图库
        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try (OutputStream outStream = getContentResolver().openOutputStream(uri)) {
                if (outStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
                    outStream.flush();
                    Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to open output stream", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Failed to create image", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被授予，可以执行保存操作
                Toast.makeText(this, "Storage Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                // 权限被拒绝，提示用户
                Toast.makeText(this, "Storage Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
