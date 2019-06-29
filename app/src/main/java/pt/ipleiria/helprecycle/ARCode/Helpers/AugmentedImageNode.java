
package pt.ipleiria.helprecycle.ARCode.Helpers;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;

import java.util.concurrent.CompletableFuture;

public class AugmentedImageNode extends AnchorNode {

    private static final String TAG = "AugmentedImageNode";

    private AugmentedImage image;
    private static CompletableFuture<ModelRenderable> modelFuture;

    public AugmentedImageNode(Context context, String filename) {
        // Upon construction, start loading the modelFuture
        if (modelFuture == null) {
            modelFuture = ModelRenderable.builder().setRegistryId("modelFuture")
                    .setSource(context, Uri.parse(filename))
                    .build();
        }
    }

    /**
     * Called when the AugmentedImage is detected and should be rendered. A Sceneform node tree is
     * created based on an Anchor created from the image.
     *
     * @param image captured by your camera
     */
    public void setImage(AugmentedImage image) {
        this.image = image;

        if (!modelFuture.isDone()) {
            CompletableFuture.allOf(modelFuture).thenAccept((Void aVoid) -> {
                setImage(image);
            }).exceptionally(throwable -> {
                Log.e(TAG, "Exception loading", throwable);
                return null;
            });
        }

        setAnchor(image.createAnchor(image.getCenterPose()));

        Node node = new Node();

        Pose pose = Pose.makeTranslation(0.0f, 0.0f, 0.010f);

        node.setParent(this);
        node.setLocalPosition(new Vector3(pose.tx(), pose.ty(), pose.tz() + 0.5f));
//        node.setLocalPosition(new Vector3(, 0f, 0f));
        node.setLocalRotation(new Quaternion(pose.qx(), pose.qy(), pose.qz(), pose.qw()));
//        node.setLocalRotation(new Quaternion(0f, 0f, 0f, 0f));
        node.setLocalScale(new Vector3(1f, 1f, 1f));
        node.setRenderable(modelFuture.getNow(null));
    }

    public void setImageArrow(AugmentedImage image){
        this.image = image;

        if (!modelFuture.isDone()) {
            CompletableFuture.allOf(modelFuture).thenAccept((Void aVoid) -> {
                setImageArrow(image);
            }).exceptionally(throwable -> {
                Log.e(TAG, "Exception loading", throwable);
                return null;
            });
        }

        setAnchor(image.createAnchor(image.getCenterPose()));

        Node node = new Node();

        Pose pose = Pose.makeTranslation(0.0f, 0.0f, 0.010f);

        node.setParent(this);
        node.setLocalPosition(new Vector3(pose.tx(), pose.ty(), pose.tz() - 0.1f));
        node.setLocalRotation(new Quaternion(pose.qx() + 270f, pose.qy() + 180f, pose.qz(), pose.qw()));
        node.setLocalScale(new Vector3(1f, 1f, 1f));
        node.setRenderable(modelFuture.getNow(null));
    }

    public AugmentedImage getImage() {
        return image;
    }
}
