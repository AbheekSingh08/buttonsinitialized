package com.example.myapplication;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.FileNotFoundException;
import java.io.IOException;

public class PdfViewActivity extends AppCompatActivity {

    private ImageView pdfImageView;
    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private ParcelFileDescriptor parcelFileDescriptor;
    private int currentPageIndex = 0;
    private Button prevPageButton, nextPageButton;
    private TextView pageNumberText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_view);

        pdfImageView = findViewById(R.id.pdfImageView);
        prevPageButton = findViewById(R.id.prevPageButton);
        nextPageButton = findViewById(R.id.nextPageButton);
        pageNumberText = findViewById(R.id.pageNumberText);

        prevPageButton.setOnClickListener(v -> showPage(currentPageIndex - 1));
        nextPageButton.setOnClickListener(v -> showPage(currentPageIndex + 1));

        Uri pdfUri = Uri.parse(getIntent().getStringExtra("pdfUri"));
        if (pdfUri != null) {
            try {
                openPdfFile(pdfUri);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error opening PDF file", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Failed to open PDF file", Toast.LENGTH_SHORT).show();
        }
    }

    private void openPdfFile(Uri pdfUri) throws IOException {
        parcelFileDescriptor = getContentResolver().openFileDescriptor(pdfUri, "r");
        if (parcelFileDescriptor != null) {
            pdfRenderer = new PdfRenderer(parcelFileDescriptor);
            showPage(0); // Show the first page initially
        }
    }

    private void showPage(int index) {
        if (pdfRenderer.getPageCount() <= index || index < 0) return;

        if (currentPage != null) currentPage.close();

        currentPage = pdfRenderer.openPage(index);
        Bitmap bitmap = Bitmap.createBitmap(currentPage.getWidth(), currentPage.getHeight(), Bitmap.Config.ARGB_8888);
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        pdfImageView.setImageBitmap(bitmap);

        currentPageIndex = index;
        pageNumberText.setText(String.format("Page %d", index + 1));

        prevPageButton.setEnabled(index > 0);
        nextPageButton.setEnabled(index < pdfRenderer.getPageCount() - 1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentPage != null) {
            currentPage.close();
        }
        if (pdfRenderer != null) {
            pdfRenderer.close();
        }
        if (parcelFileDescriptor != null) {
            try {
                parcelFileDescriptor.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
