package com.example.arbuilder;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.material.snackbar.Snackbar;
import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.collision.CollisionShape;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.ar.sceneform.rendering.Color;
import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;
    private ArFragment arFragment;
    private float upDistance = 0f;
    private ModelRenderable ballRenderable;
    private AnchorNode myanchornode;

    private TextView text;
    private VerticalSeekBar sk_height_control;
    private ImageButton btn_clear, btn_new;
    private Button btn_color;
    private ProgressBar loading;

    private int color = android.graphics.Color.RED;
    float data1 = 0.25f, data2 = 0.25f, data3 = 0.25f;
    float[] scale = {0.25f, 0.25f, 0.25f};

    private String[] objects = {"Cube", "Sphere", "Cylinder", "Cone", "Pyramid", "Ring"};
    private String[][] req_data = {{"Longitude", "Height", "Depth"}, {"Radius"},{"Radius", "Height"},
            {"Radius", "Height"}, {"Side", "Height"}, {"Radius"}};

    private boolean[] bool_scale = {false, false, false, true, true, true};
    private float[] local_scale = {1f, 1f, 1f};

    private int[] int_externalModels = {R.raw.cone, R.raw.pyramid, R.raw.ring};
    private ModelRenderable[] rd_externalModels = new ModelRenderable[int_externalModels.length];

    List<AnchorNode> anchorNodes = new ArrayList<>();

    private TransformableNode transformableNode;

    private ArSceneView sceneView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }
        try
        {
            this.getSupportActionBar().hide();
        }
        catch (NullPointerException e){}

        setContentView(R.layout.activity_main);


        if (ContextCompat.checkSelfPermission( MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions( MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
        if (ContextCompat.checkSelfPermission( MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions( MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 2);
        }



        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        text = (TextView) findViewById(R.id.text);

        sk_height_control = (VerticalSeekBar) findViewById(R.id.sk_height_control);
        btn_new = (ImageButton) findViewById(R.id.btn_new);
        btn_clear = (ImageButton) findViewById(R.id.btn_clear);
        ImageButton btn_raise = (ImageButton) findViewById(R.id.btn_raise);
        loading = (ProgressBar) findViewById(R.id.loading);


        ImageButton mybtn = (ImageButton) findViewById(R.id.take_photo);

        sk_height_control.setEnabled(false);
        sk_height_control.setMax(500);

        btn_raise.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(sk_height_control.isEnabled()) {
                    sk_height_control.raiseScaled();
                }
                else
                    Toast.makeText(MainActivity.this, "Tap plane to place object",
                            Toast.LENGTH_SHORT).show();
            }
        });

        mybtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loading.setVisibility(View.VISIBLE);
                takePhoto(sceneView);
            }
        });


        btn_new.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newObjectDialog();
            }
        });

        btn_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetLayout();
            }
        });


        sk_height_control.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                upDistance = progress;
                ascend(myanchornode, upDistance);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        for(int i = 0; i<int_externalModels.length ; i++) {
            int finalI = i;
            ModelRenderable.builder()
                    .setSource(this, int_externalModels[finalI])
                    .build()
                    .thenAccept(renderable -> rd_externalModels[finalI] = renderable)
                    .exceptionally(
                            throwable -> {
                                Toast toast =
                                        Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.CENTER, 0, 0);
                                toast.show();
                                return null;
                            });
        }


        makeMaterial(0, color);

        sceneView = arFragment.getArSceneView();


        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if (ballRenderable == null) {
                        return;
                    }

                    // Create the Anchor.
                    Anchor anchor = hitResult.createAnchor();

                    AnchorNode anchorNode = new AnchorNode(anchor);


                    anchorNode.setParent(arFragment.getArSceneView().getScene());

                    myanchornode = anchorNode;
                    anchorNodes.add(anchorNode);

                    sk_height_control.setEnabled(true);
                    sk_height_control.setProgress(0);


                    // Create the transformable andy and add it to the anchor.
                    TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());
                    andy.setParent(anchorNode);
                    andy.setRenderable(ballRenderable);
                    andy.select();
                    andy.setLocalScale(new Vector3(local_scale[0], local_scale[1], local_scale[2]));
                    andy.getScaleController().setEnabled(false);
                    andy.setOnTapListener(new Node.OnTapListener() {
                        @Override
                        public void onTap(HitTestResult hitTestResult, MotionEvent motionEvent) {
                            transformableNode = andy;
                            sk_height_control.setProgress(0);
                        }
                    });
                    transformableNode = andy;
                });
    }

    /**
     * Function to raise an object perpendicular to the ArPlane a specific distance
     * @param an anchor belonging to the object that should be raised
     * @param up distance in centimeters the object should be raised vertically
     */
    private void ascend(AnchorNode an, float up) {
        /*Anchor anchor = myhit.getTrackable().createAnchor(
                myhit.getHitPose().compose(Pose.makeTranslation(0, up / 100f, 0)));

        an.setAnchor(anchor);*/
        if(transformableNode != null)
            transformableNode.setLocalPosition(new Vector3(0,up / 100f,0));
    }

    private void materialDialog(){
        new ColorPickerDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle("ColorPicker Dialog")
                .setPreferenceName("MyColorPickerDialog")
                .setPositiveButton("Ok",
                        new ColorEnvelopeListener() {
                            @Override
                            public void onColorSelected(ColorEnvelope envelope, boolean fromUser) {
                                color = envelope.getColor();

                                System.out.println("Guacamole"+envelope.getArgb().length);
                                btn_color.setBackgroundColor(color);
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                .attachAlphaSlideBar(true) // default is true. If false, do not show the AlphaSlideBar.
                .attachBrightnessSlideBar(true)  // default is true. If false, do not show the BrightnessSlideBar.
                .show();
    }


    /**
     * Check whether the device supports the tools required to use the measurement tools
     * @param activity
     * @return boolean determining whether the device is supported or not
     */
    private boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
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

    private void newObjectDialog() {
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
        View mView = getLayoutInflater().inflate(R.layout.dialog_new_object, null);

        EditText[] et_data = {mView.findViewById(R.id.et_data1), mView.findViewById(R.id.et_data2),
                mView.findViewById(R.id.et_data3)};

        TextView[] tv_data = {mView.findViewById(R.id.tv_data1), mView.findViewById(R.id.tv_data2),
                mView.findViewById(R.id.tv_data3)};

        LinearLayout[] ll_data = {mView.findViewById(R.id.ll_data1), mView.findViewById(R.id.ll_data2),
                mView.findViewById(R.id.ll_data3)};

        btn_color = (Button) mView.findViewById(R.id.btn_color);
        Spinner spn_object = (Spinner) mView.findViewById(R.id.sp_object);
        mBuilder.setTitle("Create new object");

        btn_color.setBackgroundColor(color);


        ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_spinner_dropdown_item,objects);

        spn_object.setAdapter(adapter);

        spn_object.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                for(int j = 0; j<ll_data.length ; j++){
                    if(j < req_data[i].length){
                        ll_data[j].setVisibility(View.VISIBLE);
                        tv_data[j].setText(req_data[i][j]);
                    }
                    else
                        ll_data[j].setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        btn_color.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                materialDialog();
            }
        });

        mBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                int position = spn_object.getSelectedItemPosition();
                boolean bool_filled = true;
                for(int j = 0; j < req_data[position].length ; j++){
                    if(!isEmpty(et_data[j]))
                        scale[j] = Float.parseFloat(et_data[j].getText().toString());
                    else
                        bool_filled = false;
                }
                if(bool_filled) {
                    makeMaterial(position, color);
                    if(bool_scale[position]) {
                        Arrays.fill(local_scale, scale[0]);
                        for (int j = 1; j < req_data[position].length; j++) {
                            local_scale[j] = scale[j];
                        }
                    }
                    else
                        Arrays.fill(local_scale, 1f);
                }
                else
                    Toast.makeText(MainActivity.this,
                            "You did not fill all the required fields", Toast.LENGTH_SHORT).show();
            }
        });

        mBuilder.setView(mView);
        AlertDialog dialog = mBuilder.create();

        dialog.show();
    }

    private boolean isEmpty(EditText et){
        return et.getText().toString().equals("");
    }

    /**
     * Set layout to its initial state
     */
    private void resetLayout(){
        sk_height_control.setProgress(10);
        sk_height_control.setEnabled(false);
        emptyAnchors();
    }

    private void emptyAnchors(){
        for (AnchorNode n : anchorNodes) {
            arFragment.getArSceneView().getScene().removeChild(n);
            n.getAnchor().detach();
            n.setParent(null);
            n = null;
        }
    }

    private void makeMaterial(int option, int mycolor){
        MaterialFactory.makeTransparentWithColor(this, new Color(mycolor))
            .thenAccept(
                material -> {
                    if(option == 0)
                        ballRenderable =
                                ShapeFactory.makeCube(new Vector3(data1,data2,data3), new Vector3(0, 0, 0), material);
                    else if(option == 1)
                        ballRenderable =
                                ShapeFactory.makeSphere(data1, new Vector3(0, 0, 0), material);
                    else if(option == 2)
                        ballRenderable =
                                ShapeFactory.makeCylinder(data1, data2, new Vector3(0, 0, 0), material);

                    else {
                        int index = option -3;
                        if(index < rd_externalModels.length){
                            ballRenderable = rd_externalModels[index].makeCopy();
                            ballRenderable.setMaterial(material);
                        }
                    }
                });
    }

    private String generateFilename() {
        String date =
                new SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.getDefault()).format(new Date());
        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES) + File.separator + "AR_Builder/" + date + "_screenshot.jpg";
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
                    Toast toast = Toast.makeText(MainActivity.this, e.toString(),
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
                Toast toast = Toast.makeText(MainActivity.this,
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
}

