package pt.ipleiria.helprecycle;

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
        List<String> labelsStr = new ArrayList<>();
        for (int i = 0; i < labels.size(); ++i) {
            FirebaseVisionCloudLabel label = labels.get(i);
            Log.d(TAG, "cloud label: " + label);
            if (label.getLabel() != null) {
                labelsStr.add((label.getLabel()));
            }
        }
        for (String lbl: labelsStr
        ) {
            Log.d("Label ", lbl);
        }

        CloudLabelGraphic cloudLabelGraphic = new CloudLabelGraphic(graphicOverlay, labelsStr);
        graphicOverlay.add(cloudLabelGraphic);
        graphicOverlay.postInvalidate();
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Cloud Label detection failed " + e);
    }
}
/*
class Materials {

    HashMap<String, String> recyclableMaterial = new HashMap<>();

    private static final String YELLOW = "Yellow";
    private static final String GREEN = "Green";
    private static final String BLUE = "Blue";
    private static final String BROWN = "Brown"; //food waste
    private static final String RED = "Red"; //batteries
    //private static final String TEXTILES = "Textile";

    public void createMaterialList {

        //Yellow
        recyclableMaterial.put("Metal", "Yellow");
        recyclableMaterial.put("Plastic", "Yellow");
        recyclableMaterial.put("Paperboard", "Yellow");
        recyclableMaterial.put("Cardboard", "Yellow");

    }
}
*/