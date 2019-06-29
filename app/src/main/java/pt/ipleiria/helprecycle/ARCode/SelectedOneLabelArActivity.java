package pt.ipleiria.helprecycle.ARCode;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import pt.ipleiria.helprecycle.ARCode.Helpers.AugmentedImageNode;
import pt.ipleiria.helprecycle.ARCode.Helpers.CameraPermissionHelper;
import pt.ipleiria.helprecycle.ARCode.Helpers.FullScreenHelper;
import pt.ipleiria.helprecycle.ARCode.Helpers.SnackbarHelper;
import pt.ipleiria.helprecycle.MainActivity;
import pt.ipleiria.helprecycle.R;
import pt.ipleiria.helprecycle.common.Singleton;

public class SelectedOneLabelArActivity extends AppCompatActivity {

    private static final String TAG = SelectedOneLabelArActivity.class.getSimpleName();

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private ArSceneView arSceneView;

    //Related to ARCore, to verify if its needed to install
    private boolean installRequested;

    private Session session;

    //Class created to act like a Toast.
    private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();

    private boolean shouldConfigureSession = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);

        arSceneView = findViewById(R.id.surfaceview);

        installRequested = false;

        initializeSceneView();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (session == null) {
            Exception exception = null;
            String message = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(this)) {
                    CameraPermissionHelper.requestCameraPermission(this);
                    return;
                }

                session = new Session(/* context = */ this);
            } catch (UnavailableArcoreNotInstalledException
                    | UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (Exception e) {
                message = "This device does not support AR";
                exception = e;
            }

            if (message != null) {
                messageSnackbarHelper.showError(this, message);
                Log.e(TAG, "Exception creating session", exception);
                return;
            }

            shouldConfigureSession = true;
        }

        if (shouldConfigureSession) {
            configureSession();
            shouldConfigureSession = false;
            arSceneView.setupSession(session);
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session.resume();
            arSceneView.resume();
        } catch (CameraNotAvailableException e) {
            // In some cases (such as another camera app launching) the camera may be given to
            // a different app instead. Handle this properly by showing a message and recreate the
            // session at the next iteration.
            messageSnackbarHelper.showError(this, "Camera not available. Please restart the app.");
            session = null;
            return;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            arSceneView.pause();
            session.pause();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(
                    this, "Camera permissions are needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
    }

    private void initializeSceneView() {
        arSceneView.getScene().setOnUpdateListener((this::onUpdateFrame));
    }

    private void addObject(AugmentedImageNode node){

        //Destroy the 3rd child, which is a node that was already created. This is to avoid creating multiple and slowing down the Cellphone.
        if(arSceneView.getScene().getChildren().size() == 3){
            arSceneView.getScene().removeChild(arSceneView.getScene().getChildren().get(2));
        }
        arSceneView.getScene().addChild(node);
    }

    private void onUpdateFrame(FrameTime frameTime) {
        Frame frame = arSceneView.getArFrame();
        Collection<AugmentedImage> updatedAugmentedImages =
                frame.getUpdatedTrackables(AugmentedImage.class);

        //For each image recognized
        for (AugmentedImage augmentedImage : updatedAugmentedImages) {
            if (augmentedImage.getTrackingState() == TrackingState.TRACKING) {
                String filename = "arrow.sfb";
                AugmentedImageNode node = null;
                switch(augmentedImage.getName()){
                    case "yellow":
                        if(augmentedImage.getName().equals(Singleton.getInstance().getAnswer().toLowerCase())) {
                            node = new AugmentedImageNode(this, filename);
                            node.setImageArrow(augmentedImage);
                            Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show();
                        }
                        else{
                            Toast.makeText(this, "Not Yellow", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case "blue":
                        if(augmentedImage.getName().equals(Singleton.getInstance().getAnswer().toLowerCase())) {
                            node = new AugmentedImageNode(this, filename);
                            node.setImageArrow(augmentedImage);
                            Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(this, "Not Blue", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case "green":
                        if(augmentedImage.getName().equals(Singleton.getInstance().getAnswer().toLowerCase())) {
                            node = new AugmentedImageNode(this, filename);
                            node.setImageArrow(augmentedImage);
                            Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(this, "Not Green", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    default:
                        break;
                }
                if(node != null){
                    addObject(node);
                }
            }
        }
    }

    private void configureSession() {
        Config config = new Config(session);
        if (!setupAugmentedImageDb(config)) {
            messageSnackbarHelper.showError(this, "Could not setup augmented image database");
            Toast.makeText(this, "Error. Please go back and re-enter.", Toast.LENGTH_SHORT).show();
        }
        config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
        session.configure(config);
    }

    private boolean setupAugmentedImageDb(Config config) {
        AugmentedImageDatabase augmentedImageDatabase;
        HashMap<String, Bitmap> augmentedImageBitmaps = loadAugmentedImage();
        if (augmentedImageBitmaps == null) {
            return false;
        }

        augmentedImageDatabase = new AugmentedImageDatabase(session);
        for (Map.Entry<String, Bitmap> entry : augmentedImageBitmaps.entrySet()) {
            augmentedImageDatabase.addImage(entry.getKey(), entry.getValue());
        }

        config.setAugmentedImageDatabase(augmentedImageDatabase);

        return true;
    }

    private HashMap<String, Bitmap> loadAugmentedImage() {
        try (InputStream amarelo = getAssets().open("yellow_plastic.jpg"); InputStream azul = getAssets().open("blue_paper.jpg"); InputStream verde = getAssets().open("green_glass.jpg")) {
            HashMap<String, Bitmap> map = new HashMap<>();
            map.put("yellow", BitmapFactory.decodeStream(amarelo));
            map.put("blue", BitmapFactory.decodeStream(azul));
            map.put("green", BitmapFactory.decodeStream(verde));
            return map;
        } catch (IOException e) {
            Log.e(TAG, "IO exception loading augmented image bitmap.", e);
            Toast.makeText(this, "Error. Please go back and re-enter.", Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    public void backButton(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        Singleton.getInstance().resetAnswer();
        this.finish();
    }
}
