package com.example.christinemaxwell.cameraapp;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Manifest;

public class LaunchCamera extends AppCompatActivity {
    private static final String TAG = "camera2api";
    private Button takePictureBtn;
    private TextureView  textureView;
    //SparseIntArrays map integers to integers. Unlike a normal array of integers, there can be gaps
    // in the indices. It is intended to be more memory efficient than using a HashMap to map Integers
    // to Integers, both because it avoids auto-boxing keys and values and its data structure doesn't
    // rely on an extra entry object for each mapping.
    private  static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {ORIENTATIONS.append(Surface.ROTATION_0, 90);
            ORIENTATIONS.append(Surface.ROTATION_90, 0);
            ORIENTATIONS.append(Surface.ROTATION_180, 270);
            ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSession;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimensions;
    private ImageReader imageReader;
    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch_camera);
      /*  Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
        textureView = (TextureView) findViewById(R.id.texture);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        takePictureBtn = (Button) findViewById(R.id.takePic_btn);
        assert takePictureBtn !=null;

        takePictureBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //TODO:takePicture();
            }
        });
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //TODO: openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            //TODO: Transform you image captured size according to the surface width and height

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //called when camera is opened
            Log.e(TAG, "opOpened");
            cameraDevice = camera;
            //TODO: createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback(){

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Toast.makeText(getApplicationContext(),"saved: "+file, Toast.LENGTH_LONG).show();
            //TODO: createCameraPreview()
        }
    };

    protected void startBackgroundThread()
    {
        mBackgroundThread = new HandlerThread("Camera bckground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackGroundThread()
    {
        mBackgroundThread.quitSafely();
        mBackgroundThread = null;
        mBackgroundHandler = null;

        try {
            mBackgroundThread.join();
        } catch (InterruptedException e) {
            Log.e(TAG, "stopBackground thread" + e.toString());
            e.printStackTrace();
        }
    }

    protected void takePicture()
    {
        int width, height, rotation;
        ImageReader reader;
        final File file;
        List<Surface> outputSurfaces;
        final CaptureRequest.Builder captureBuilder;
        if(null == cameraDevice)
        {
            Log.e(TAG, "cmaera device is null");
            return;
        }

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;

            if(characteristics != null)
            {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);

            }
            width = 640;
            height = 480;

            if(jpegSizes !=null && 0 < jpegSizes.length)
            {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();

            }

            reader = ImageReader.newInstance(width,height, ImageFormat.JPEG, 1);
            outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            //orientation, rotation

            rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            file = new File(Environment.getExternalStorageDirectory() +"/pic.jpg");
            ImageReader.OnImageAvailableListener readListener = new ImageReader.OnImageAvailableListener()
            {
                /**
                 * Callback that is called when a new image is available from ImageReader.
                 *
                 * @param reader the ImageReader the callback is associated with.
                 * @see ImageReader
                 * @see Image
                 */
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;

                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);

                        save(bytes);
                    }
                    catch(FileNotFoundException e)
                    {
                        e.printStackTrace();
                    }catch (IOException io)
                    {
                        io.printStackTrace();
                    }finally {
                        if(image != null)
                        {
                            image.close();
                        }
                    }
                }

                private void save(byte[] bytes) throws IOException
                {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(file);
                        output.write(bytes);
                    }finally {
                        if(null != output)
                        {
                            output.close();
                        }
                    }

                }
            };

            reader.setOnImageAvailableListener(readListener, mBackgroundHandler);

            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback()
            {
                /**
                 * This method is called when an image capture has fully completed and all the
                 * result metadata is available.
                 *
                 * @param session the session returned by {@link CameraDevice#createCaptureSession}
                 * @param request The request that was given to the CameraDevice
                 * @param result  The total output metadata from the capture, including the
                 *                final capture parameters and the state of the camera system during
                 *                capture.
                // * @see #capture
                // * @see #captureBurst
               //  * @see #setRepeatingRequest
               //  * @see #setRepeatingBurst
                 */
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(getApplicationContext(), "Saved: "+file, Toast.LENGTH_SHORT).show();
                    //TODO:createCameraPreview()
                }
            };

            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback()
            {
                /**
                 * This method is called when the camera device has finished configuring itself, and the
                 * session can start processing capture requests.
                 * <p/>
                 * <p>If there are capture requests already queued with the session, they will start
                 * processing once this callback is invoked, and the session will call {@link #onActive}
                 * right after this callback is invoked.</p>
                 * <p/>
                 * <p>If no capture requests have been submitted, then the session will invoke
                 * {@link #onReady} right after this callback.</p>
                 * <p/>
                 * <p>If the camera device configuration fails, then {@link #onConfigureFailed} will
                 * be invoked instead of this callback.</p>
                 *
                 * @param session the session returned by {@link CameraDevice#createCaptureSession}
                 */
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    protected void createCameraPreview()
    {
        try{

            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimensions.getWidth(), imageDimensions.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession (Arrays.asList(surface), new CameraCaptureSession.StateCallback()
            {



                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    //the camera already closed
                    if(null != cameraDevice)
                    {
                        return;
                    }

                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSession = cameraCaptureSession;
                    //TODO: updatePreview()

                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Toast.makeText(getApplicationContext(), "configuration change", Toast.LENGTH_SHORT).show();

                }
            }, null);

        }catch(CameraAccessException e){
            e.printStackTrace();
        }
    }

    private void openCamera()
    {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");

        try
        {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;

            imageDimensions = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
           // if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.ch)

        }catch(CameraAccessException e)
        {
            e.printStackTrace();
        }
    }
    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_launch_camera, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    */
}
