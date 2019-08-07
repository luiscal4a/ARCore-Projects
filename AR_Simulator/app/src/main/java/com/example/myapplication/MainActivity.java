package com.example.myapplication;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListPopupWindow;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static java.lang.Math.atan2;

/**
 * This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;
    private float x=0f, y=0f, z=0f;
    private ArFragment arFragment;
    private AnchorNode myanchornode;
    TransformableNode mytranode = null;

    private SeekBar sb_size;
    private Spinner spn_model;

    private HitResult myhit;
    private float mySize = 70f;
    private float mytravel=0.01f, distance_x=0f, distance_z=0f, myangle=0f;


    int[] sfb_source = {R.raw.wheelchair, R.raw.stroller, R.raw.cart};
    String[] arr_models = {"Wheelchair", "Stroller", "Shopping cart"};
    private ModelRenderable[] renderable_models = new ModelRenderable[sfb_source.length];

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    // CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        Button r_left = (Button)findViewById(R.id.r_left);
        Button r_right = (Button)findViewById(R.id.r_right);
        Button accelerate = (Button)findViewById(R.id.accelerate);
        spn_model = (Spinner) findViewById(R.id.spn_model);
        sb_size = (SeekBar) findViewById(R.id.sb_size);
        List<AnchorNode> anchorNodes = new ArrayList<>();

        sb_size.setEnabled(false);

        // Keep immersive view on spinner opening
        Field popup = null;
        try {
            popup = Spinner.class.getDeclaredField("mPopup");
            popup.setAccessible(true);
            ListPopupWindow popupWindow = (ListPopupWindow) popup.get(spn_model);
            popupWindow.setModal(false);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }



        sb_size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mySize = progress;
                myanchornode.setLocalScale(new Vector3(progress/70f, progress/70f, progress/70f));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        accelerate.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mytranode != null) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        myangle = set(mytranode.getLocalRotation());
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                    }
                    forward(myanchornode);
                }
                return true;

            }
        });

        r_left.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(mytranode != null){
                    Quaternion q1 = mytranode.getLocalRotation();
                    Quaternion q2 = Quaternion.axisAngle(new Vector3(0, 1f, 0f), .5f);
                    mytranode.setLocalRotation(Quaternion.multiply(q1, q2));
                    myangle = set(mytranode.getLocalRotation());
                }

                return true;
            }

        });

        r_right.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(mytranode != null){
                    myangle+=0.01f;
                    Quaternion q1 = mytranode.getLocalRotation();
                    Quaternion q2 = Quaternion.axisAngle(new Vector3(0, 1f, 0f), -.5f);
                    mytranode.setLocalRotation(Quaternion.multiply(q1, q2));
                    myangle = set(mytranode.getLocalRotation());
                }

                return true;
            }

        });

        for(int i = 0 ; i < sfb_source.length ; i++) {
            int finalI = i;
            ModelRenderable.builder()
                    .setSource(this, sfb_source[i])
                    .build()
                    .thenAccept(renderable -> renderable_models[finalI] = renderable)
                    .exceptionally(
                            throwable -> {
                                Toast toast =
                                        Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.CENTER, 0, 0);
                                toast.show();
                                return null;
                            });
        }


        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if (renderable_models[spn_model.getSelectedItemPosition()] == null) {
                        return;
                    }

                    distance_x=0f;
                    distance_z=0f;
                    myangle=0f;

                    myhit = hitResult;

                    // Create the Anchor.
                    Anchor anchor = hitResult.createAnchor();

                    AnchorNode anchorNode = new AnchorNode(anchor);


                    anchorNode.setParent(arFragment.getArSceneView().getScene());
                    anchorNodes.add(anchorNode);
                    sb_size.setEnabled(true);

                    myanchornode = anchorNode;

                    // Create the transformable andy and add it to the anchor.
                    TransformableNode andy;
                    if(mytranode == null)
                        andy = new TransformableNode(arFragment.getTransformationSystem());
                    else andy = mytranode;

                    andy.setParent(anchorNode);
                    andy.setRenderable(renderable_models[spn_model.getSelectedItemPosition()]);
                    andy.select();

                    mytranode = andy;
                    mytranode.setLocalRotation(new Quaternion(0f, 0f, 0f, 1f));
                    myanchornode.setLocalScale(new Vector3(mySize/70f, mySize/70f, mySize/70f));
                });

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_spinner_dropdown_item,arr_models);

        spn_model.setAdapter(adapter);
        spn_model.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(mytranode != null)
                    mytranode.setRenderable(renderable_models[i]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

    }

    void ascend(AnchorNode an, float x, float y, float z){
        Anchor anchor =  myhit.getTrackable().createAnchor(
                myhit.getHitPose().compose(Pose.makeTranslation(x/100f, z/100f, y/100f)));

        an.setAnchor(anchor);
    }

    Quaternion rotate(AnchorNode an, float angle) {
        //mytranode.setLocalRotation(Quaternion.axisAngle(new Vector3(0, 1f, 0), angle));

        return mytranode.getLocalRotation();
    }


    public float set(Quaternion q1) {
        Vector3 angles = new Vector3();
        double sqw = q1.w*q1.w;
        double sqx = q1.x*q1.x;
        double sqy = q1.y*q1.y;
        double sqz = q1.z*q1.z;
        double unit = sqx + sqy + sqz + sqw; // if normalised is one, otherwise is correction factor
        double test = q1.x*q1.y + q1.z*q1.w;
        if (test > 0.499*unit) { // singularity at north pole
            angles.x = (float) (2 * atan2(q1.x,q1.w));
            angles.y = (float) Math.PI/2;
            angles.z = 0;
            return angles.x;
        }
        if (test < -0.499*unit) { // singularity at south pole
            angles.x = (float) (-2 * atan2(q1.x,q1.w));
            angles.y = (float) (-Math.PI/2);
            angles.z = 0;
            return angles.x;
        }
        angles.x = (float) atan2(2*q1.y*q1.w-2*q1.x*q1.z , sqx - sqy - sqz + sqw);
        angles.y = (float) Math.asin(2*test/unit);
        angles.z = (float) atan2(2*q1.x*q1.w-2*q1.y*q1.z , -sqx + sqy - sqz + sqw);
        return angles.x;
    }


    void forward(AnchorNode an){
        distance_x+=Math.sin(myangle)*mytravel;
        distance_z+=Math.cos(myangle)*mytravel;

        Anchor anchor =  myhit.getTrackable().createAnchor(
                myhit.getHitPose().compose(Pose.makeTranslation(-distance_x, 0f, -distance_z)));

        an.setAnchor(anchor);
    }

    float getMetersBetweenAnchors(Anchor anchor1, Anchor anchor2) {
        float[] distance_vector = anchor1.getPose().inverse()
                .compose(anchor2.getPose()).getTranslation();
        float totalDistanceSquared = 0;
        for(int i=0; i<3; ++i)
            totalDistanceSquared += distance_vector[i]*distance_vector[i];
        return (float) Math.sqrt(totalDistanceSquared);
    }

    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
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
}
