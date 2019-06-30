
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

    //Add an image related to the image Plastic.
    public void setImagePlastic(AugmentedImage image) {
        this.image = image;

        if (!modelFuture.isDone()) {
            CompletableFuture.allOf(modelFuture).thenAccept((Void aVoid) -> {
                setImagePlastic(image);
            }).exceptionally(throwable -> {
                Log.e(TAG, "Exception loading", throwable);
                return null;
            });
        }

        setAnchor(image.createAnchor(image.getCenterPose()));

        Node node = new Node();

        Pose pose = Pose.makeTranslation(0.0f, 0.0f, 0.010f);

        node.setParent(this);
        node.setLocalPosition(new Vector3(pose.tx(), pose.ty(), pose.tz()));
        node.setWorldRotation(new Quaternion(pose.qx(), pose.qy() + 180, pose.qz(), pose.qw()));
        node.setLocalScale(new Vector3(1f, 1f, 1f));
        node.setRenderable(modelFuture.getNow(null));
    }

    //Add an image related to the image Arrow.
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
        node.setLocalRotation(new Quaternion(pose.qx() + 180f, pose.qy(), pose.qz(), pose.qw()));
        node.setLocalScale(new Vector3(1f, 1f, 1f));
        node.setRenderable(modelFuture.getNow(null));
    }

    public AugmentedImage getImage() {
        return image;
    }

    //Add an image related to the image Glass.
    public void setImageGlass(AugmentedImage image) {
        this.image = image;

        if (!modelFuture.isDone()) {
            CompletableFuture.allOf(modelFuture).thenAccept((Void aVoid) -> {
                setImageGlass(image);
            }).exceptionally(throwable -> {
                Log.e(TAG, "Exception loading", throwable);
                return null;
            });
        }

        setAnchor(image.createAnchor(image.getCenterPose()));

        Node node = new Node();

        Pose pose = Pose.makeTranslation(0.0f, 0.0f, 0.010f);

        node.setParent(this);
        node.setLocalPosition(new Vector3(pose.tx(), pose.ty(), pose.tz()));
        node.setWorldRotation(new Quaternion(pose.qx(), pose.qy() + 180, pose.qz(), pose.qw()));
        node.setLocalScale(new Vector3(0.3f, 0.3f, 0.3f));
        node.setRenderable(modelFuture.getNow(null));
    }

    //Add an image related to the image Paper.
    public void setImagePaper(AugmentedImage image) {
        this.image = image;

        if (!modelFuture.isDone()) {
            CompletableFuture.allOf(modelFuture).thenAccept((Void aVoid) -> {
                setImageGlass(image);
            }).exceptionally(throwable -> {
                Log.e(TAG, "Exception loading", throwable);
                return null;
            });
        }

        setAnchor(image.createAnchor(image.getCenterPose()));

        Node node = new Node();

        Pose pose = Pose.makeTranslation(0.0f, 0.0f, 0.010f);

        node.setParent(this);
        node.setLocalPosition(new Vector3(pose.tx(), pose.ty(), pose.tz()));
        node.setWorldRotation(new Quaternion(pose.qx(), pose.qy() + 270, pose.qz() + 270, pose.qw()));
        node.setLocalScale(new Vector3(0.3f, 0.3f, 0.3f));
        node.setRenderable(modelFuture.getNow(null));
    }
}
