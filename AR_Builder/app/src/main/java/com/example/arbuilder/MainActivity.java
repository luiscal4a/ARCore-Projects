package com.example.arbuilder;


import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;
    private ArFragment arFragment;
    private float upDistance = 0f;
    private ModelRenderable ballRenderable;
    private AnchorNode myanchornode;
    private DecimalFormat form_numbers = new DecimalFormat("#0.00 m");

    private HitResult myhit;

    private TextView text;
    private SeekBar sk_height_control;
    private Button btn_clear, btn_new;
    private Button btn_color;

    private int color = android.graphics.Color.RED;
    float data1 = 0.25f, data2 = 0.25f, data3 = 0.25f;

    private String[] objects = {"Cube", "Sphere", "Cylinder"};

    List<AnchorNode> anchorNodes = new ArrayList<>();

    private TransformableNode transformableNode;


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


        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        text = (TextView) findViewById(R.id.text);

        sk_height_control = (SeekBar) findViewById(R.id.sk_height_control);
        btn_new = (Button) findViewById(R.id.btn_new);
        btn_clear = (Button) findViewById(R.id.btn_clear);

        sk_height_control.setEnabled(false);


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

        makeMaterial(objects[0], color);


        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if (ballRenderable == null) {
                        return;
                    }
                    myhit = hitResult;

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

        EditText et_data1 = (EditText) mView.findViewById(R.id.et_data1);
        EditText et_data2 = (EditText) mView.findViewById(R.id.et_data2);
        EditText et_data3 = (EditText) mView.findViewById(R.id.et_data3);

        TextView tv_data1 = (TextView) mView.findViewById(R.id.tv_data1);
        TextView tv_data2 = (TextView) mView.findViewById(R.id.tv_data2);
        TextView tv_data3 = (TextView) mView.findViewById(R.id.tv_data3);

        LinearLayout ll_data1 = (LinearLayout) mView.findViewById(R.id.ll_data1);
        LinearLayout ll_data2 = (LinearLayout) mView.findViewById(R.id.ll_data2);
        LinearLayout ll_data3 = (LinearLayout) mView.findViewById(R.id.ll_data3);

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
                if(i == 0){
                    ll_data1.setVisibility(View.VISIBLE);
                    ll_data2.setVisibility(View.VISIBLE);
                    ll_data3.setVisibility(View.VISIBLE);
                    tv_data1.setText("Longitude");
                    tv_data2.setText("Height");
                    tv_data3.setText("Depth");
                }
                else if(i == 1){
                    ll_data1.setVisibility(View.VISIBLE);
                    ll_data2.setVisibility(View.GONE);
                    ll_data3.setVisibility(View.GONE);
                    tv_data1.setText("Radius");
                }
                else if(i == 2){
                    ll_data1.setVisibility(View.VISIBLE);
                    ll_data2.setVisibility(View.VISIBLE);
                    ll_data3.setVisibility(View.GONE);
                    tv_data1.setText("Radius");
                    tv_data2.setText("Height");
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
                String str_data1 = et_data1.getText().toString();
                String str_data2 = et_data2.getText().toString();
                String str_data3 = et_data3.getText().toString();
                switch (position){
                    case 0:
                        if(!str_data1.equals("")) {
                            data1 = Float.parseFloat(str_data1);
                            if (!str_data2.equals("")) {
                                data2 = Float.parseFloat(str_data2);
                                if (!str_data3.equals("")){
                                    data3 = Float.parseFloat(str_data3);
                                    makeMaterial("Cube", color);
                                }
                            }
                        }
                        break;

                    case 1:
                        if(!str_data1.equals("")) {
                            data1 = Float.parseFloat(str_data1);
                            makeMaterial("Sphere", color);
                        }
                        break;

                    case 2:
                        if(!str_data1.equals("")) {
                            data1 = Float.parseFloat(str_data1);
                            if(!str_data2.equals("")) {
                                data2 = Float.parseFloat(str_data2);
                                makeMaterial("Cylinder", color);
                            }
                        }
                        break;
                }
            }
        });

        mBuilder.setView(mView);
        AlertDialog dialog = mBuilder.create();

        dialog.show();
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

    private void makeMaterial(String option, int mycolor){
        MaterialFactory.makeTransparentWithColor(this, new Color(mycolor))
            .thenAccept(
                material -> {
                    if(option.equals(objects[0]))
                        ballRenderable =
                                ShapeFactory.makeCube(new Vector3(data1,data2,data3), new Vector3(0, 0, 0), material);
                    else if(option.equals(objects[1]))
                        ballRenderable =
                                ShapeFactory.makeSphere(data1, new Vector3(0, 0, 0), material);
                    else
                        ballRenderable =
                                ShapeFactory.makeCylinder(data1, data2, new Vector3(0, 0, 0), material);
                });
    }
}

