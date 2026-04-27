package com.example.smartscan;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CaptureActivity extends AppCompatActivity {

    private static final String TAG = "CaptureActivity";
    
    private PreviewView previewView;
    private ProgressBar progressBar;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private SummaryService summaryService;

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    processImage(uri);
                }
            }
    );

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_capture);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainCapture), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        previewView = findViewById(R.id.previewView);
        progressBar = findViewById(R.id.progressBar);
        summaryService = new SummaryService();
        cameraExecutor = Executors.newSingleThreadExecutor();

        findViewById(R.id.btnTakePhoto).setOnClickListener(v -> takePhoto());
        findViewById(R.id.btnPickImage).setOnClickListener(v -> galleryLauncher.launch("image/*"));

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        progressBar.setVisibility(View.VISIBLE);
        File photoFile = new File(getExternalCacheDir(), System.currentTimeMillis() + ".jpg");

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                processImage(Uri.fromFile(photoFile));
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CaptureActivity.this, "Capture failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processImage(Uri uri) {
        progressBar.setVisibility(View.VISIBLE);
        InputImage image;
        try {
            image = InputImage.fromFilePath(this, uri);
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            return;
        }

        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(image)
                .addOnSuccessListener(visionText -> {
                    String rawText = visionText.getText();
                    if (rawText.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "No text detected", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    summarizeAndNavigate(rawText);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "OCR failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void summarizeAndNavigate(String rawText) {
        summaryService.summarize(rawText, new SummaryService.SummaryCallback() {
            @Override
            public void onSuccess(String summary, String keywords) {
                progressBar.setVisibility(View.GONE);
                Intent intent = new Intent(CaptureActivity.this, NoteDetailActivity.class);
                intent.putExtra("EXTRA_RAW_TEXT", rawText);
                intent.putExtra("EXTRA_SUMMARY", summary);
                intent.putExtra("EXTRA_KEYWORDS", keywords);
                intent.putExtra("IS_NEW_SCAN", true);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CaptureActivity.this, "Summary failed: " + message, Toast.LENGTH_SHORT).show();
                // Navigate anyway with empty summary
                Intent intent = new Intent(CaptureActivity.this, NoteDetailActivity.class);
                intent.putExtra("EXTRA_RAW_TEXT", rawText);
                intent.putExtra("IS_NEW_SCAN", true);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
