package com.example.homesaver;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.homesaver.databinding.ActivityMainBinding;

import java.io.InputStream;

import okhttp3.*;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private final List<Uri> selectedFileUris = new ArrayList<>();
    private Handler mainHandler;
    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        mainHandler = new Handler(Looper.getMainLooper());
        setContentView(binding.getRoot());

        ActivityResultLauncher<String[]> openDocumentLauncher =
                registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
                    if (uris != null && !uris.isEmpty()) {
                        selectedFileUris.clear();
                        selectedFileUris.addAll(uris);
                        binding.pickFile.setText("Выбрано: " + selectedFileUris.size());
                    }
                });


        binding.pickFile.setOnClickListener(v -> {
            openDocumentLauncher.launch(new String[]{"*/*"});
        });

        binding.sendFile.setOnClickListener(v -> {
            if (selectedFileUris != null) {
                binding.sendBar.setVisibility(ProgressBar.VISIBLE);
                sendFiles(binding.address.getText().toString());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mainHandler != null) mainHandler.removeCallbacksAndMessages(null);
        binding = null;
    }

    public void sendFiles(String serverUrl) {
        if (selectedFileUris == null || selectedFileUris.isEmpty()) return;

        new Thread(() -> {
            try {
                MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM);

                for (Uri uri : selectedFileUris) {
                    try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                        if (inputStream == null) continue;

                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                        byte[] data = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(data)) != -1)
                            buffer.write(data, 0, bytesRead);

                        byte[] fileBytes = buffer.toByteArray();
                        String fileName = getFileName(uri);

                        RequestBody fileBody = RequestBody.create(
                                fileBytes,
                                MediaType.parse("application/octet-stream")
                        );

                        multipartBuilder.addFormDataPart("files", fileName, fileBody);
                    }
                }

                MultipartBody requestBody = multipartBuilder.build();
                Request request = new Request.Builder()
                        .url(serverUrl)
                        .post(requestBody)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    final boolean success = response.isSuccessful();
                    final int code = response.code();
                    mainHandler.post(() -> {
                        if (success)
                            Toast.makeText(MainActivity.this, "Файлы отправлены!", Toast.LENGTH_SHORT).show();
                        else
                            Toast.makeText(MainActivity.this, "Ошибка сервера: " + code, Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() ->
                        Toast.makeText(MainActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

        @SuppressLint("Range")
        private String getFileName (Uri uri){
            String result = null;
            if (uri.getScheme().equals("content")) {
                try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst())
                        result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }

            if (result == null)
                result = uri.getLastPathSegment();

            return result;
        }
    }
