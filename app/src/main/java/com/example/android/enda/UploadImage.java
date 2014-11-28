package com.example.android.enda;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

public class UploadImage {
    private static final String DEBUG_TAG = "HttpExample";
    private static String attachmentName = "file";
    private static String attachmentFileName = "input.jpeg";
    private static String crlf = "\r\n";
    private static String twoHyphens = "--";
    private static String boundary = "*****";
    private String strurl;
    private String fileName;


    public UploadImage(String strurl, String fileName) {
        this.strurl = strurl;
        this.fileName = fileName;

    }

    public String uploadImage() throws IOException {
        InputStream iStream = null;
        int len = 500;
        try {
            URL url = new URL(strurl);
            /** Creating an http connection to communcate with url */
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            /** Connecting to url */
            urlConnection.connect();
            int response = urlConnection.getResponseCode();
            Log.d(DEBUG_TAG, "The response is: " + response);
            /** Reading data from url */
            iStream = urlConnection.getInputStream();


            InputStream in = new BufferedInputStream(iStream);
            return readIt(in, len);


//            /** Creating a bitmap from the stream returned from the url */
//            bitmap = BitmapFactory.decodeStream(iStream);

        } finally {
            if (iStream != null) {
                iStream.close();
            }
        }

    }


    // Reads an InputStream and converts it to a String.
    public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
        Reader reader = null;
        reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[len];
        reader.read(buffer);
        return new String(buffer);
    }


    public Bitmap uploadBitmapAndDownload(Bitmap bitmap) throws IOException {

//		String[] result = strurl.split("/");


        InputStream iStream = null;
        int len = 500;

        HttpURLConnection httpUrlConnection = null;
        URL url = new URL(strurl);
        try {
            httpUrlConnection = (HttpURLConnection) url.openConnection();
            httpUrlConnection.setUseCaches(false);
            httpUrlConnection.setDoOutput(true);

            httpUrlConnection.setRequestMethod("POST");
            httpUrlConnection.setRequestProperty("Connection", "Keep-Alive");
            httpUrlConnection.setRequestProperty("Cache-Control", "no-cache");
            httpUrlConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + this.boundary);


            DataOutputStream request = new DataOutputStream(httpUrlConnection.getOutputStream());

            request.writeBytes(this.twoHyphens + this.boundary + this.crlf);
            request.writeBytes("Content-Disposition: form-data; name=\"" + this.attachmentName + "\";filename=\"" + fileName + "\"" + this.crlf);
            request.writeBytes("Content-Type: image/jpeg" + this.crlf);
            request.writeBytes(this.crlf);


//		//calculate how many bytes our image consists of.
//		int bytes = bitmap.getByteCount();
//		//or we can calculate bytes this way. Use a different value than 4 if you don't use 32bit images.
//		//int bytes = b.getWidth()*b.getHeight()*4; 
//
//		ByteBuffer buffer = ByteBuffer.allocate(bytes); //Create a new buffer
//		bitmap.copyPixelsToBuffer(buffer); //Move the byte data to the buffer
//
//		byte[] array = buffer.array(); //Get the underlying array containing the data.
//		
//		request.write(array);

            ByteArrayOutputStream imageByteStream = new ByteArrayOutputStream();
//        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, imageByteStream   );
            bitmap.compress(Bitmap.CompressFormat.JPEG, 94, imageByteStream);

            request.write(imageByteStream.toByteArray());

            request.writeBytes(this.crlf);
            request.writeBytes(this.twoHyphens + this.boundary + this.twoHyphens + this.crlf);

            request.flush();
            request.close();


            /** Reading data from url */
            iStream = httpUrlConnection.getInputStream();


//            InputStream in = new BufferedInputStream(iStream);
            Bitmap resultBitmap = BitmapFactory.decodeStream(iStream);

            return resultBitmap;

//            /** Creating a bitmap from the stream returned from the url */
//            bitmap = BitmapFactory.decodeStream(iStream);

        } finally {
            if (iStream != null) {
                iStream.close();
                httpUrlConnection.disconnect();
            }
        }

    }

    public String uploadBitmap(String sourceFileUrl) throws IOException {

//		String[] result = strurl.split("/");

        File sourceFile = new File(sourceFileUrl);
        InputStream iStream = null;
        int len = 500;
        int bytesRead, bytesAvailable, bufferSize;
        int maxBufferSize = 1 * 1024 * 1024;
        byte[] buffer;
        HttpURLConnection httpUrlConnection = null;
        URL url = new URL(strurl);
        try {
            httpUrlConnection = (HttpURLConnection) url.openConnection();
            httpUrlConnection.setUseCaches(false);
            httpUrlConnection.setDoOutput(true);

            httpUrlConnection.setRequestMethod("POST");
            httpUrlConnection.setRequestProperty("Connection", "Keep-Alive");
            httpUrlConnection.setRequestProperty("Cache-Control", "no-cache");
            httpUrlConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + this.boundary);


            DataOutputStream request = new DataOutputStream(httpUrlConnection.getOutputStream());

            request.writeBytes(this.twoHyphens + this.boundary + this.crlf);
            request.writeBytes("Content-Disposition: form-data; name=\"" + this.attachmentName + "\";filename=\"" + fileName + "\"" + this.crlf);
            request.writeBytes("Content-Type: image/jpeg" + this.crlf);
            request.writeBytes(this.crlf);


//		//calculate how many bytes our image consists of.
//		int bytes = bitmap.getByteCount();
//		//or we can calculate bytes this way. Use a different value than 4 if you don't use 32bit images.
//		//int bytes = b.getWidth()*b.getHeight()*4;
//
//		ByteBuffer buffer = ByteBuffer.allocate(bytes); //Create a new buffer
//		bitmap.copyPixelsToBuffer(buffer); //Move the byte data to the buffer
//
//		byte[] array = buffer.array(); //Get the underlying array containing the data.
//
//		request.write(array);

            FileInputStream fileInputStream = new FileInputStream(sourceFile);

            bytesAvailable = fileInputStream.available();

            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            // read file and write it into form...
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            while (bytesRead > 0) {

                request.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            }


            request.writeBytes(this.crlf);
            request.writeBytes(this.twoHyphens + this.boundary + this.twoHyphens + this.crlf);

            request.flush();
            request.close();


            /** Reading data from url */
//            iStream = httpUrlConnection.getInputStream();

            BufferedInputStream in = new BufferedInputStream(httpUrlConnection.getInputStream());
            byte[] contents = new byte[1024];

            int bytesRead2 = 0;
            String strFileContents = null;
            while ((bytesRead2 = in.read(contents)) != -1) {
                strFileContents = new String(contents, 0, bytesRead2);
            }

//            InputStream in = new BufferedInputStream(iStream);
//            String resultBitmap = String.decodeStream(iStream);

            if (strFileContents != null && !strFileContents.isEmpty())
                return strFileContents;
            else return null;

//            /** Creating a bitmap from the stream returned from the url */
//            bitmap = BitmapFactory.decodeStream(iStream);

        } finally {
            if (iStream != null) {
                iStream.close();
                httpUrlConnection.disconnect();
            }
        }

    }


}
