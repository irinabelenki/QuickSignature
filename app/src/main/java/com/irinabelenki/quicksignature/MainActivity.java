package com.irinabelenki.quicksignature;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.FileDescriptor;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private Button openImageButton;
    private Button overlayBitmapButton;
    private Button loadButton, upButton, downButton, leftButton, rightButton;

    private TouchImageView imageView;
    private final int PICK_FILE_RESULT_CODE = 1000;
    public static String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        openImageButton = (Button)findViewById(R.id.open_image_button);
        openImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,
                        "Select Picture"), PICK_FILE_RESULT_CODE);
            }
        });

        overlayBitmapButton = (Button)findViewById(R.id.test_button_overlay);
        overlayBitmapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testOverlay();
            }
        });

        loadButton = (Button)findViewById(R.id.load_button);
        loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageView.setRectCoordinates(100, 100, 200, 200);
                imageView.invalidate();
            }
        });
        upButton = (Button)findViewById(R.id.up_button);
        upButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageView.rectUp();
                imageView.invalidate();
            }
        });
        downButton = (Button)findViewById(R.id.down_button);
        downButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageView.rectDown();
                imageView.invalidate();
            }
        });
        leftButton = (Button)findViewById(R.id.left_button);
        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageView.rectLeft();
                imageView.invalidate();
            }
        });
        rightButton = (Button)findViewById(R.id.right_button);
        rightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageView.rectRight();
                imageView.invalidate();
            }
        });

        imageView = (TouchImageView)findViewById(R.id.image_view);
        imageView.setMaxZoom(4f);
        Log.i(TAG, "MainActivity onCreate");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case PICK_FILE_RESULT_CODE:
                    try {
                        Uri selectedImageUri = data.getData();
                        Toast.makeText(this, "URI from intent: " + selectedImageUri, Toast.LENGTH_LONG).show();
                        Log.i(TAG, "URI from intent: " + selectedImageUri);

                        Bitmap bitmap = getBitmapFromUri(selectedImageUri);
                        imageView.setImageBitmap(bitmap);
                    }
                    catch (Exception e) {
                        Log.e(TAG, "Exception in onActivityResult: " + e.getMessage());
                        Toast.makeText(this, "Exception in onActivityResult: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    break;
            }
        } else {
            Log.i(TAG, "onActivityResult: Result is not OK");
            Toast.makeText(this, "onActivityResult: Result is not OK", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        //if (id == R.id.action_settings) {
        //    return true;
        //}

        return super.onOptionsItemSelected(item);
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

    public static Bitmap createTopBitmap() {
        Bitmap tempBitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888); // this creates a MUTABLE bitmap
        Canvas tempCanvas = new Canvas(tempBitmap);
        tempCanvas.drawBitmap(tempBitmap, 0, 0, null);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        tempCanvas.drawRect(new RectF(100, 100, 200, 200), paint);
        return tempBitmap;
    }

    private Bitmap overlay(Bitmap bmp1, Bitmap bmp2) {
        Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bmp1, new Matrix(), null);
        canvas.drawBitmap(bmp2, new Matrix(), null);
        return bmOverlay;
    }

    private void testOverlay() {
        Bitmap bitmap = overlay(((BitmapDrawable)imageView.getDrawable()).getBitmap(),
                createTopBitmap());
        imageView.setImageBitmap(bitmap);
    }

}
