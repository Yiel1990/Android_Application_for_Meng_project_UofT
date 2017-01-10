package com.example.yiou.fer_app;

import com.loopj.android.http.*;
import org.apache.http.Header;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;
import android.widget.Toast;
import android.text.format.DateFormat;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.view.View.OnClickListener;
import android.widget.Button;
import java.io.ByteArrayOutputStream;
import android.content.ContentResolver;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;

public class MainActivity extends Activity {

    // These status codes are used for determining the target intent for processing
    private final int  ALBUM_OK = 1, CAMERA_OK = 2,CUT_OK = 3,UPLOAD = 4;
    private ImageView showIv;
    private File file;
    private Bitmap uploadbitmap;
    private String address = Environment.getExternalStorageDirectory() + "/FaceExpression/";;
    private String name = "";
    private final String url = "http://192.168.2.34/Upload.php";// The address for processing the image
    private String base = "I guess you are feeling ";
    private String []feelarray = {"Angry","Disgust","Fear","Happy","Sad","Surprise","Neutral"};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create a temporary file used for process in the default directory
        file = new File(Environment.getExternalStorageDirectory(), "temp.jpg");
        file.delete();// If the file has already existed then delete it
        Button button1 = (Button) findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                //Create an intent for using the camera and the images created are used for further process
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
                startActivityForResult(cameraIntent, CAMERA_OK);
            }
        });
        Button button2 = (Button) findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                // Create an intent for selecting images from the gallery and for further process
                Intent albumIntent = new Intent(Intent.ACTION_PICK, null);
                albumIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(albumIntent, ALBUM_OK);
            }
        });
        Button button3 = (Button) findViewById(R.id.button3);
        button3.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {

                upload(url);//This method is used for sending the images to the server and

            }
        });

        showIv = (ImageView) findViewById(R.id.imageView1);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        System.out.println("requestCode = " + requestCode);
        switch (requestCode) {
            //If it has already read the image, then start the crop process
            case ALBUM_OK:
                if (data != null) {

                    clipPhoto(data.getData());

                }
                break;
            // If you have already taken photo using the camera, then start the crop process
            case CAMERA_OK:

                if (file.exists()) {
                    clipPhoto(Uri.fromFile(file));//Crop process
                }
                break;
            // Display the processed image in the screen
            case CUT_OK:

                if (data != null) {
                    setPicToView(data);
                }
                break;
            case UPLOAD:
                if(data != null){
                    Uri uri = data.getData();
                    try
                    {

                        uploadbitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }


                }
            default:
                break;

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //This function is used for cropping the images
    public void clipPhoto(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 200);
        intent.putExtra("outputY", 200);
        intent.putExtra("return-data", true);
        startActivityForResult(intent, CUT_OK);
    }

    //This method is used for showing the image in the screen
    private void setPicToView(Intent picdata) {

        File directory = new File(address);
        directory.mkdirs();// Create a directory where the processed images are stored

        name = new DateFormat().format("yyyyMMdd_hhmmss", Calendar.getInstance(Locale.CHINA)) + ".jpg";//Create the names for the photoes
        Toast.makeText(this, name, Toast.LENGTH_LONG).show();
        String fileName = address + name;
        Bundle extras = picdata.getExtras();
        Bitmap bitmap = (Bitmap) extras.get("data");// Convert the file data into bitmap data for further process
        FileOutputStream b = null;
        try {
            b = new FileOutputStream(fileName);
            MediaStore.Images.Media.insertImage(getContentResolver(),bitmap,"","");
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, b);// Save the data into target file

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                b.flush();
                b.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        if (extras != null) {
            Bitmap photo = extras.getParcelable("data");
            Drawable drawable = new BitmapDrawable(photo);
            showIv.setImageDrawable(drawable);//Display the photo
            file.delete();//Delete the
        }

    }

    public  void upload(String url) {//Use the AsyncHttp package for the communication between the android application and the server

        String path = address + name;
        RequestParams params = new RequestParams();
        try {
            params.put("uploadfile", new File(path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        AsyncHttpClient client = new AsyncHttpClient();
        //System.out.println("sdadadsa");
        client.post(url, params, new AsyncHttpResponseHandler() {

            @Override
            public void onFailure(int arg0, Header[] arg1, byte[] response, Throwable arg3) {
                Toast.makeText(getApplicationContext(), "Upload Failed!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSuccess(int arg0, Header[] arg1, byte[] response) {
                try
                {
                    String content = new String(response, "UTF-8");
                    JSONObject object =  new JSONObject(content);
                    int text = object.getInt("code");
                    String message = base + feelarray[text];
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();

                }catch (Exception e) {
                    e.toString();
                    Toast.makeText(getApplicationContext(),e.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

}