package com.example.facestickers;



import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.PixelCopy;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListPopupWindow;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.snackbar.Snackbar;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedFace;
import com.google.ar.core.Frame;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.DeadlineExceededException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.Texture;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.AugmentedFaceNode;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This is an example activity that uses the Sceneform UX package to make common Augmented Faces
 * tasks easier.
 */
public class AugmentedFacesActivity extends AppCompatActivity {
    private static final String TAG = AugmentedFacesActivity.class.getSimpleName();

    private static final double MIN_OPENGL_VERSION = 3.0;

    private FaceArFragment arFragment;

    private ModelRenderable faceRegionsRenderable;
    private Texture faceMeshTexture;

    private final HashMap<AugmentedFace, AugmentedFaceNode> faceNodeMap = new HashMap<>();

    private AugmentedFaceNode faceNode = null;
    private AugmentedFace myface = null;
    private ProgressBar loading;

    private Node lightBulb = null;
    private boolean saveFile = false;

    int[] face_textures = {R.drawable.ic_insert_photo, R.drawable.pattern1, R.drawable.pattern2,
            R.drawable.pattern3, R.drawable.pattern4, R.drawable.pattern6, R.drawable.mona,
            R.drawable.walk, R.drawable.starry, R.drawable.mondrian, R.drawable.blue,
            R.drawable.scribbles, R.drawable.scribbles2, R.drawable.ic_cancel};
    int[] hover_object = {R.drawable.ic_insert_photo, R.drawable.plumbob, R.drawable.plumbob_yellow,
            R.drawable.plumbob_red, R.drawable.light_bulb_transparent, R.drawable.halo,
            R.drawable.heart, R.drawable.crown, R.drawable.ic_cancel};

    Texture[] myTexture = new Texture[face_textures.length];

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    // CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);


        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }
        if (ContextCompat.checkSelfPermission( AugmentedFacesActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions( AugmentedFacesActivity.this, new String[]{Manifest.permission.CAMERA}, 0);
        }
        if (ContextCompat.checkSelfPermission( AugmentedFacesActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions( AugmentedFacesActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
        if (ContextCompat.checkSelfPermission( AugmentedFacesActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions( AugmentedFacesActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 2);
        }

        setContentView(R.layout.activity_face_mesh);
        arFragment = (FaceArFragment) getSupportFragmentManager().findFragmentById(R.id.face_fragment);


        MySpinner spinner = (MySpinner) findViewById(R.id.spn_model);
        MySpinner spinner2 = (MySpinner) findViewById(R.id.spn_model2);
        loading = (ProgressBar) findViewById(R.id.loading);


        ImageButton mybtn = (ImageButton) findViewById(R.id.take_photo);

        mybtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loading.setVisibility(View.VISIBLE);
                saveFile = true;
            }
        });


        ImageAdapter adapter = new ImageAdapter(this, face_textures);
        ImageAdapter adapter2 = new ImageAdapter(this, hover_object);

        spinner.setAdapter(adapter);
        spinner.setSelection(1);
        spinner2.setAdapter(adapter2);
        spinner2.setSelection(1);


        // Keep immersive view on spinner opening
        Field popup = null;
        try {
            popup = Spinner.class.getDeclaredField("mPopup");
            popup.setAccessible(true);
            ListPopupWindow popupWindow = (ListPopupWindow) popup.get(spinner);
            popupWindow.setModal(false);
            popupWindow = (ListPopupWindow) popup.get(spinner2);
            popupWindow.setModal(false);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }


        spinner.setOnItemSelectedEvenIfUnchangedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(position == myTexture.length-1){
                    faceNode.setFaceMeshTexture(null);
                    faceNodeMap.put(myface, faceNode);
                }
                else if (position == 0){
                    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("image/*");
                    startActivityForResult(photoPickerIntent, 0);
                }
                if(faceNode != null && myface != null && myTexture[position] != null) {
                    faceNode.setFaceMeshTexture(myTexture[position]);
                    faceNodeMap.put(myface, faceNode);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spinner2.setOnItemSelectedEvenIfUnchangedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(lightBulb != null){
                    if(position == myTexture.length-1){
                        changeHoverObject(null);
                    }
                    else if (position == 0){
                        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                        photoPickerIntent.setType("image/*");
                        startActivityForResult(photoPickerIntent, 1);
                    }
                    else{
                        changeHoverObject(drawableToBitmap(hover_object[position]));
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        for(int i = 1; i < face_textures.length-1 ; i++){
            Bitmap mytexture = drawableToBitmap(face_textures[i]);
            mytexture = restrictMaxBitmap(mytexture);
            mytexture = adjustOpacity(mytexture, 160);
            mytexture = Bitmap.createScaledBitmap(mytexture, 2048, 2048, false);
            mytexture = cropFace(mytexture, drawableToBitmap(R.drawable.myfacemask));
            int finalI = i;
            Texture.builder()
                    .setSource(mytexture)
                    .build()
                    .thenAccept(texture -> myTexture[finalI] = texture);
        }

        ArSceneView sceneView = arFragment.getArSceneView();

        // This is important to make sure that the camera stream renders first so that
        // the face mesh occlusion works correctly.
        sceneView.setCameraStreamRenderPriority(Renderable.RENDER_PRIORITY_FIRST);

        Scene scene = sceneView.getScene();


        scene.addOnUpdateListener(
                (FrameTime frameTime) -> {
                    Collection<AugmentedFace> faceList =
                            sceneView.getSession().getAllTrackables(AugmentedFace.class);


                    if(saveFile) {
                        saveFile = false;
                        takePhoto(sceneView);
                    }

                    // Make new AugmentedFaceNodes for any new faces.
                    for (AugmentedFace face : faceList) {
                        if (!faceNodeMap.containsKey(face)) {
                            faceNode = new AugmentedFaceNode(face);
                            faceNode.setParent(scene);
                            //faceNode.setFaceRegionsRenderable(faceRegionsRenderable);
                            faceNode.setFaceMeshTexture(myTexture[spinner.getSelectedItemPosition()]);

                            myface = face;
                            faceNodeMap.put(face, faceNode);

                            //add light bulb above head
                            ViewRenderable.builder().setView(this, R.layout.idea_view).build()
                                    .thenAccept (miau->{
                                        lightBulb = new Node();
                                        Vector3 localPosition = new Vector3();
                                        //lift the light bulb to be just above your head.
                                        localPosition.set(0.0f, 0.17f, 0.0f);
                                        lightBulb.setLocalPosition(localPosition);
                                        lightBulb.setParent(faceNode);
                                        lightBulb.setRenderable(miau);
                                    });
                        }
                    }



                    // Remove any AugmentedFaceNodes associated with an AugmentedFace that stopped tracking.
                    Iterator<Map.Entry<AugmentedFace, AugmentedFaceNode>> iter =
                            faceNodeMap.entrySet().iterator();
                    while (iter.hasNext()) {
                        Map.Entry<AugmentedFace, AugmentedFaceNode> entry = iter.next();
                        AugmentedFace face = entry.getKey();
                        if (face.getTrackingState() == TrackingState.STOPPED) {
                            AugmentedFaceNode faceNode = entry.getValue();
                            faceNode.setParent(null);
                            iter.remove();
                        }
                    }
                });
    }

    @Override
    protected void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            try {
                final Uri imageUri = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                if(reqCode == 0) {
                    selectedImage = restrictMaxBitmap(selectedImage);
                    selectedImage = adjustOpacity(selectedImage, 160);
                    selectedImage = Bitmap.createScaledBitmap(selectedImage, 2048, 2048, false);
                    selectedImage = flipBitmapHorizontally(selectedImage);
                    selectedImage = cropFace(selectedImage, drawableToBitmap(R.drawable.myfacemask));
                    System.out.println(selectedImage.getWidth());
                    if (faceNode != null && myface != null) {
                        Texture.builder()
                                .setSource(selectedImage)
                                .build()
                                .thenAccept(texture -> {
                                    faceMeshTexture = texture;
                                    faceNode.setFaceMeshTexture(faceMeshTexture);
                                    faceNodeMap.put(myface, faceNode);
                                });
                    }
                }
                else{
                    changeHoverObject(selectedImage);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(AugmentedFacesActivity.this, "Something went wrong", Toast.LENGTH_LONG).show();
            }

        }else {
            Toast.makeText(AugmentedFacesActivity.this, "You haven't picked Image",Toast.LENGTH_LONG).show();
        }
    }

    private Bitmap restrictMaxBitmap(Bitmap bitmap){
        if(bitmap.getWidth()>=4096)
            bitmap = Bitmap.createBitmap(bitmap, 0,0,4000, bitmap.getHeight());
        if(bitmap.getHeight()>=4096)
            bitmap = Bitmap.createBitmap(bitmap, 0,0,bitmap.getWidth(), 4000);
        return bitmap;
    }

    private Bitmap adjustOpacity(Bitmap bitmap, int opacity)
    {
        Bitmap mutableBitmap = bitmap.isMutable()
                ? bitmap
                : bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        int colour = (opacity & 0xFF) << 24;
        canvas.drawColor(colour, PorterDuff.Mode.DST_IN);
        return mutableBitmap;
    }

    private Bitmap cropFace(Bitmap sourceImage, Bitmap destinationImage){
        sourceImage = sourceImage.isMutable()
                ? sourceImage
                : sourceImage.copy(Bitmap.Config.ARGB_8888, true);
        destinationImage = destinationImage.isMutable()
                ? destinationImage
                : destinationImage.copy(Bitmap.Config.ARGB_8888, true);

        Paint paint = new Paint();
        Canvas canvas = new Canvas(destinationImage);
        canvas.drawBitmap(destinationImage, 0, 0, paint);

        PorterDuff.Mode mode = PorterDuff.Mode.SRC_OUT;// choose a mod
        paint.setXfermode(new PorterDuffXfermode(mode));

        canvas.drawBitmap(sourceImage, 0, 0, paint);

        return destinationImage;
    }

    private Bitmap drawableToBitmap(int drawable){
        return BitmapFactory.decodeResource(getResources(), drawable);
    }

    private Bitmap flipBitmapHorizontally(Bitmap bitmap){
        Matrix matrix = new Matrix();
        matrix.postScale(-1, 1, bitmap.getWidth()/2f, bitmap.getHeight()/2f);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private void changeHoverObject(Bitmap bitmap){
        View mView = getLayoutInflater().inflate(R.layout.idea_view, null);
        ImageView iv = (ImageView) mView.findViewById(R.id.light_bulb);
        iv.setImageBitmap(bitmap);
        ViewRenderable.builder().setView(AugmentedFacesActivity.this, mView).build()
                .thenAccept (miau->{
                    lightBulb.setRenderable(miau);
                });
    }


    private String generateFilename() {
        String date =
                new SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.getDefault()).format(new Date());
        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES) + File.separator + "Sceneform/" + date + "_screenshot.jpg";
    }

    private void saveBitmapToDisk(Bitmap bitmap, String filename) throws IOException {

        File out = new File(filename);
        if (!out.getParentFile().exists()) {
            out.getParentFile().mkdirs();
        }
        try (FileOutputStream outputStream = new FileOutputStream(filename);
             ByteArrayOutputStream outputData = new ByteArrayOutputStream()) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputData);
            outputData.writeTo(outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException ex) {
            throw new IOException("Failed to save bitmap to disk", ex);
        }
    }

    private void takePhoto(ArSceneView sceneView) {
        final String filename = generateFilename();
        /*ArSceneView view = fragment.getArSceneView();*/
        // Create a bitmap the size of the scene view.
        final Bitmap bitmap = Bitmap.createBitmap(sceneView.getWidth(), sceneView.getHeight(),
                Bitmap.Config.ARGB_8888);

        // Create a handler thread to offload the processing of the image.
        final HandlerThread handlerThread = new HandlerThread("PixelCopier");
        handlerThread.start();
        // Make the request to copy.
        PixelCopy.request(sceneView, bitmap, (copyResult) -> {
            if (copyResult == PixelCopy.SUCCESS) {
                try {
                    saveBitmapToDisk(bitmap, filename);
                } catch (IOException e) {
                    Toast toast = Toast.makeText(AugmentedFacesActivity.this, e.toString(),
                            Toast.LENGTH_LONG);
                    toast.show();
                    return;
                }

                File myfile = new File(filename);
                Uri fileUri = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ?
                        FileProvider.getUriForFile(this,getPackageName() + ".provider", myfile) : Uri.fromFile(myfile);

                notifyGalleryChange(myfile);
                loading.setVisibility(View.INVISIBLE);
                Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                        "Photo saved", Snackbar.LENGTH_LONG);
                snackbar.setAction("Open in Photos", v -> {
                    openImage(fileUri);
                });
                snackbar.show();
            } else {
                Log.d("DrawAR", "Failed to copyPixels: " + copyResult);
                Toast toast = Toast.makeText(AugmentedFacesActivity.this,
                        "Failed to copyPixels: " + copyResult, Toast.LENGTH_LONG);
                toast.show();
            }
            handlerThread.quitSafely();
        }, new Handler(handlerThread.getLooper()));
    }

    private void openImage(Uri fileUri){
        final Intent intent = new Intent(Intent.ACTION_VIEW)//
                .setDataAndType(fileUri, "image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    private void notifyGalleryChange(File file){
        MediaScannerConnection.scanFile(this, new String[] {
                file.getAbsolutePath()},
                null, new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                    }
                });
    }


    /**
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device.
     *
     * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     *
     * <p>Finishes the activity if Sceneform can not run
     */
    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (ArCoreApk.getInstance().checkAvailability(activity)
                == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
            Log.e(TAG, "Augmented Faces requires ARCore.");
            Toast.makeText(activity, "Augmented Faces requires ARCore", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }
}



