package pt.ipleiria.helprecycle;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.cloud.FirebaseVisionCloudDetectorOptions;
import com.google.firebase.ml.vision.cloud.label.FirebaseVisionCloudLabel;
import com.google.firebase.ml.vision.cloud.label.FirebaseVisionCloudLabelDetector;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import pt.ipleiria.helprecycle.common.FrameMetadata;
import pt.ipleiria.helprecycle.common.GraphicOverlay;
import pt.ipleiria.helprecycle.common.Singleton;

public class CloudImageLabelingProcessor  extends VisionProcessorBase<List<FirebaseVisionCloudLabel>> {
    private static final String TAG = "CloudImgLabelProcessor";

    private final FirebaseVisionCloudLabelDetector detector;

    public CloudImageLabelingProcessor() {
        FirebaseVisionCloudDetectorOptions options =
                new FirebaseVisionCloudDetectorOptions.Builder()
                        .setMaxResults(10)
                        .setModelType(FirebaseVisionCloudDetectorOptions.STABLE_MODEL)
                        .build();

        detector = FirebaseVision.getInstance().getVisionCloudLabelDetector(options);
    }

    @Override
    protected Task<List<FirebaseVisionCloudLabel>> detectInImage(FirebaseVisionImage image) {
        return detector.detectInImage(image);
    }

    @Override
    protected void onSuccess(
            @Nullable Bitmap originalCameraImage,
            @NonNull List<FirebaseVisionCloudLabel> labels,
            @NonNull FrameMetadata frameMetadata,
            @NonNull GraphicOverlay graphicOverlay) {
        graphicOverlay.clear();
        Log.d(TAG, "cloud label size: " + labels.size());
        HashMap<String, Float> labelsStr = new HashMap<>();
        for (int i = 0; i < labels.size(); ++i) {
            FirebaseVisionCloudLabel label = labels.get(i);
            Log.d(TAG, "cloud label: " + label);
            if (label.getLabel() != null) {
                labelsStr.put(label.getLabel(), label.getConfidence());
            }
        }
        if( Singleton.getInstance().setMlLabels(labelsStr) != "NOTHING"){





            //CALL Zera
            Intent intent = new Intent();
            //...
        }

        // This will display the correct answer on the screen
        ///TODO change from CloudLabelGraphic to accept a string
        ArrayList<String> answerList = new ArrayList<>();
        answerList.add(Singleton.getInstance().getAnswer());
        CloudLabelGraphic cloudLabelGraphic = new CloudLabelGraphic(graphicOverlay, answerList);
        graphicOverlay.add(cloudLabelGraphic);
        graphicOverlay.postInvalidate();





    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Cloud Label detection failed " + e);
    }



}
