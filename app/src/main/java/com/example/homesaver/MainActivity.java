package com.example.homesaver;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ProgressBar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.homesaver.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private Uri selectedFileUri;
    private Handler mainHandler;
    private Thread sendingThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mainHandler = new Handler(Looper.getMainLooper());
        ActivityResultLauncher<String[]> openDocumentLauncher =
                registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri ->
                {
                    if(uri != null)
                    {
                        selectedFileUri = uri;
                    }
                });

        binding.pickFile.setOnClickListener(v -> {
            openDocumentLauncher.launch(new String[]{"*/*"});
        });

        binding.sendFile.setOnClickListener(v-> {
            if(selectedFileUri != null)
            {
                binding.sendBar.setVisibility(ProgressBar.VISIBLE);
                sendFile();
                simulateFileSending();
            }
        });
    }
    private void simulateFileSending() {
        if (sendingThread != null && sendingThread.isAlive())
            sendingThread.interrupt();

        sendingThread = new Thread(() -> {
            try {
                for (int i = 0; i <= 100; i += 10) {
                    final int progress = i;
                    mainHandler.post(() -> binding.sendBar.setProgress(progress));

                    Thread.sleep(200); // имитация работы
                }
            } catch (InterruptedException e) {
                mainHandler.post(() -> binding.sendBar.setProgress(0));
            }
        });
        sendingThread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sendingThread != null && sendingThread.isAlive()) sendingThread.interrupt();
        if (mainHandler != null) mainHandler.removeCallbacksAndMessages(null);
        binding = null;
    }

    public void sendFile()
    {
        selectedFileUri = null;
    }
}
