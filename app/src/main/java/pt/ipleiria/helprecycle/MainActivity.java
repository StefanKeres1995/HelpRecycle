package pt.ipleiria.helprecycle;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.gms.common.annotation.KeepName;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import pt.ipleiria.helprecycle.ARCode.ArActivity;
import pt.ipleiria.helprecycle.common.CSVFile;
import pt.ipleiria.helprecycle.common.GraphicOverlay;
import pt.ipleiria.helprecycle.common.Singleton;
import pt.ipleiria.helprecycle.common.VisionImageProcessor;

@KeepName
public final class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final String CLOUD_LABEL_DETECTION = "Cloud Label";

    private static final String SIZE_PREVIEW = "w:max"; // Available on-screen width.
    private static final String SIZE_1024_768 = "w:1024"; // ~1024*768 in a normal ratio
    private static final String SIZE_640_480 = "w:640"; // ~640*480 in a normal ratio

    private static final String KEY_IMAGE_URI = "com.googletest.firebase.ml.demo.KEY_IMAGE_URI";
    private static final String KEY_IMAGE_MAX_WIDTH =
            "com.googletest.firebase.ml.demo.KEY_IMAGE_MAX_WIDTH";
    private static final String KEY_IMAGE_MAX_HEIGHT =
            "com.googletest.firebase.ml.demo.KEY_IMAGE_MAX_HEIGHT";
    private static final String KEY_SELECTED_SIZE =
            "com.googletest.firebase.ml.demo.KEY_SELECTED_SIZE";

    private static final int REQUEST_IMAGE_CAPTURE = 1001;
    private static final int REQUEST_CHOOSE_IMAGE = 1002;
    private static final int ASK_FOR_PERMISSIONS_CAMERA_EXTERNAL = 1000;

    private Button getImageButton;
    private ImageView preview;
    private GraphicOverlay graphicOverlay;
    private String selectedMode = CLOUD_LABEL_DETECTION;
    private String selectedSize = SIZE_PREVIEW;

    boolean isLandScape;

    private Uri imageUri;
    // Max width (portrait mode)
    private Integer imageMaxWidth;
    // Max height (portrait mode)
    private Integer imageMaxHeight;
    private Bitmap bitmapForDetection;
    private VisionImageProcessor imageProcessor;

    //--------------------TENSORFLOW------------------------

    // presets for rgb conversion
    private static final int RESULTS_TO_SHOW = 6;
    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128.0f;

    // options for model interpreter
    private final Interpreter.Options tfliteOptions = new Interpreter.Options();
    // tflite graph
    private Interpreter tflite;
    // holds all the possible labels for model
    private List<String> labelList;
    // holds the selected image data as bytes
    private ByteBuffer imgData = null;
    // holds the probabilities of each label for quantized graphs
    private byte[][] labelProbArrayB = null;
    // selected classifier information received from extras
    private String chosen = "inception_quant.tflite";
    // input image dimensions for the Inception Model
    private int DIM_IMG_SIZE_X = 299;
    private int DIM_IMG_SIZE_Y = 299;
    private int DIM_PIXEL_SIZE = 3;

    // int array to hold image data
    private int[] intValues;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // initialize array that holds image data
        intValues = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];
        //initilize graph and labels
        try{
            tflite = new Interpreter(loadModelFile(), tfliteOptions);
            labelList = loadLabelList();
        } catch (Exception ex){
            ex.printStackTrace();
        }

        // initialize byte array. The size depends if the input data needs to be quantized or not
        imgData =
                ByteBuffer.allocateDirect(
                        DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE);

        imgData.order(ByteOrder.nativeOrder());

        // initialize probabilities array. The datatypes that array holds the input data needs to be quantized
        labelProbArrayB= new byte[1][labelList.size()];



        getImageButton = (Button) findViewById(R.id.getImageButton);
        getImageButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Menu for selecting either: a) take new photo b) select from existing
                        PopupMenu popup = new PopupMenu(MainActivity.this, view);
                        popup.setOnMenuItemClickListener(
                                new PopupMenu.OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(MenuItem menuItem) {
                                        switch (menuItem.getItemId()) {
                                            case R.id.select_images_from_local:
                                                startChooseImageIntentForResult();
                                                return true;
                                            case R.id.take_photo_using_camera:
                                                startCameraIntentForResult();
                                                return true;
                                            default:
                                                return false;
                                        }
                                    }
                                });

                        MenuInflater inflater = popup.getMenuInflater();
                        inflater.inflate(R.menu.camera_button_menu, popup.getMenu());
                        popup.show();
                    }
                });
        preview = (ImageView) findViewById(R.id.previewPane);
        if (preview == null) {
            Log.d(TAG, "Preview is null");
        }
        graphicOverlay = (GraphicOverlay) findViewById(R.id.previewOverlay);
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null");
        }

        createImageProcessor();

        isLandScape =
                (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);

        if (savedInstanceState != null) {
            imageUri = savedInstanceState.getParcelable(KEY_IMAGE_URI);
            imageMaxWidth = savedInstanceState.getInt(KEY_IMAGE_MAX_WIDTH);
            imageMaxHeight = savedInstanceState.getInt(KEY_IMAGE_MAX_HEIGHT);
            selectedSize = savedInstanceState.getString(KEY_SELECTED_SIZE);

            if (imageUri != null) {
                tryReloadAndDetectInImage();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(KEY_IMAGE_URI, imageUri);
        if (imageMaxWidth != null) {
            outState.putInt(KEY_IMAGE_MAX_WIDTH, imageMaxWidth);
        }
        if (imageMaxHeight != null) {
            outState.putInt(KEY_IMAGE_MAX_HEIGHT, imageMaxHeight);
        }
        outState.putString(KEY_SELECTED_SIZE, selectedSize);
    }

    private void startCameraIntentForResult() {
        // Clean up last time's image
        imageUri = null;
        preview.setImageBitmap(null);

        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    ASK_FOR_PERMISSIONS_CAMERA_EXTERNAL);
        }else {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "New Picture");
            values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");
            imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private void startChooseImageIntentForResult() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CHOOSE_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            tryReloadAndDetectInImage();
        } else if (requestCode == REQUEST_CHOOSE_IMAGE && resultCode == RESULT_OK) {
            // In this case, imageUri is returned by the chooser, save it.
            imageUri = data.getData();
            tryReloadAndDetectInImage();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ASK_FOR_PERMISSIONS_CAMERA_EXTERNAL) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                if(grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    startCameraIntentForResult();
                }else{
                    Toast.makeText(this, "WRITE permission denied", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void tryReloadAndDetectInImage() {
        try {
            if (imageUri == null) {
                return;
            }

            // Clear the overlay first
            graphicOverlay.clear();

            Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

            // Get the dimensions of the View
            Pair<Integer, Integer> targetedSize = getTargetedWidthHeight();

            int targetWidth = targetedSize.first;
            int maxHeight = targetedSize.second;

            // Determine how much to scale down the image
            float scaleFactor =
                    Math.max(
                            (float) imageBitmap.getWidth() / (float) targetWidth,
                            (float) imageBitmap.getHeight() / (float) maxHeight);

            //read all tensorflow labels from the csv
            InputStream inputStream = getResources().openRawResource(R.raw.label_values);
            CSVFile csvFile = new CSVFile(inputStream);
            HashMap<String, String> tensorflowMapValues = csvFile.read();

            Singleton.getInstance().setTfList(tensorflowMapValues);

            //----------------ML KIT ------------------
            Bitmap resizedBitmap =
                    Bitmap.createScaledBitmap(
                            imageBitmap,
                            (int) (imageBitmap.getWidth() / scaleFactor),
                            (int) (imageBitmap.getHeight() / scaleFactor),
                            true);

            preview.setImageBitmap(resizedBitmap);
            bitmapForDetection = resizedBitmap;

            imageProcessor.process(bitmapForDetection, graphicOverlay);


            //-----------------------TensorFlow-------------------
            // get current bitmap from imageView
            Bitmap bitmap_orig = ((BitmapDrawable)preview.getDrawable()).getBitmap();
            // resize the bitmap to the required input size to the CNN
            Bitmap bitmap = getResizedBitmap(bitmap_orig, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y);
            // convert bitmap to byte array
            convertBitmapToByteBuffer(bitmap);
            // pass byte data to the graph

            tflite.run(imgData, labelProbArrayB);

            // prepare tf results
            printTopKLabels();

        } catch (IOException e) {
            Log.e(TAG, "Error retrieving saved image");
        }
    }

    // Returns max image width, always for portrait mode. Caller needs to swap width / height for
    // landscape mode.
    private Integer getImageMaxWidth() {
        if (imageMaxWidth == null) {
            // Calculate the max width in portrait mode. This is done lazily since we need to wait for
            // a UI layout pass to get the right values. So delay it to first time image rendering time.
            if (isLandScape) {
                imageMaxWidth =
                        ((View) preview.getParent()).getHeight() - findViewById(R.id.controlPanel).getHeight();
            } else {
                imageMaxWidth = ((View) preview.getParent()).getWidth();
            }
        }

        return imageMaxWidth;
    }

    // Returns max image height, always for portrait mode. Caller needs to swap width / height for
    // landscape mode.
    private Integer getImageMaxHeight() {
        if (imageMaxHeight == null) {
            // Calculate the max width in portrait mode. This is done lazily since we need to wait for
            // a UI layout pass to get the right values. So delay it to first time image rendering time.
            if (isLandScape) {
                imageMaxHeight = ((View) preview.getParent()).getWidth();
            } else {
                imageMaxHeight =
                        ((View) preview.getParent()).getHeight() - findViewById(R.id.controlPanel).getHeight();
            }
        }

        return imageMaxHeight;
    }

    // Gets the targeted width / height.
    private Pair<Integer, Integer> getTargetedWidthHeight() {
        int targetWidth;
        int targetHeight;

        switch (selectedSize) {
            case SIZE_PREVIEW:
                int maxWidthForPortraitMode = getImageMaxWidth();
                int maxHeightForPortraitMode = getImageMaxHeight();
                targetWidth = isLandScape ? maxHeightForPortraitMode : maxWidthForPortraitMode;
                targetHeight = isLandScape ? maxWidthForPortraitMode : maxHeightForPortraitMode;
                break;
            case SIZE_640_480:
                targetWidth = isLandScape ? 640 : 480;
                targetHeight = isLandScape ? 480 : 640;
                break;
            case SIZE_1024_768:
                targetWidth = isLandScape ? 1024 : 768;
                targetHeight = isLandScape ? 768 : 1024;
                break;
            default:
                throw new IllegalStateException("Unknown size");
        }
        return new Pair<>(targetWidth, targetHeight);
    }

    private void createImageProcessor() {
        switch (selectedMode) {
            case CLOUD_LABEL_DETECTION:
                imageProcessor = new CloudImageLabelingProcessor();
                break;
            default:
                throw new IllegalStateException("Unknown selectedMode: " + selectedMode);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_gps, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.item_gps:
                //Intent gpsActivity = new Intent(this, GPSActivity.class);
                Intent gpsActivity = new Intent (this, MapsActivity.class);
                startActivity(gpsActivity);
            default:
                return super.onOptionsItemSelected(item);
        }
    }



/***
//------------------------------------TENSORFLOW----------------------------------------------
 **/
    // priority queue that will hold the top results from the CNN
    private PriorityQueue<Map.Entry<String, Float>> sortedLabels =
            new PriorityQueue<>(
                    RESULTS_TO_SHOW,
                    new Comparator<Map.Entry<String, Float>>() {
                        @Override
                        public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
                            return (o1.getValue()).compareTo(o2.getValue());
                        }
                    });


    // loads tflite grapg from file
    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd(chosen);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    // loads the labels from the label txt file in assets into a string array
    private List<String> loadLabelList() throws IOException {
        List<String> labelList = new ArrayList<String>();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(this.getAssets().open("labels.txt")));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    // resizes bitmap to given dimensions
    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }

    // converts bitmap to byte array which is passed in the tflite graph
    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (imgData == null) {
            return;
        }
        imgData.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        // loop through all pixels
        int pixel = 0;
        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                final int val = intValues[pixel++];
                // get rgb values from intValues where each int holds the rgb values for a pixel.
                // if quantized, convert each rgb value to a byte, otherwise to a float

                imgData.put((byte) ((val >> 16) & 0xFF));
                imgData.put((byte) ((val >> 8) & 0xFF));
                imgData.put((byte) (val & 0xFF));
            }
        }
    }


    private void printTopKLabels() {
        // add all results to priority queue
        for (int i = 0; i < labelList.size(); ++i) {
            sortedLabels.add(
                    new AbstractMap.SimpleEntry<>(labelList.get(i), (labelProbArrayB[0][i] & 0xff) / 255.0f));

            if (sortedLabels.size() > RESULTS_TO_SHOW) {
                sortedLabels.poll();
            }
        }

        // get top results from priority queue
        final int size = sortedLabels.size();

        //populate results
        HashMap<String, Float> tfResults = new HashMap<>();
        for (int i = 0; i < size; ++i) {
            Map.Entry<String, Float> label = sortedLabels.poll();
            tfResults.put(label.getKey(), label.getValue());
        }

        //only does different than nothing when either both
        Singleton.getInstance().setTfLabels(tfResults);

        // This will display the correct answer on the screen
        ///TODO change from CloudLabelGraphic to accept a string
        ArrayList<String> answerList = new ArrayList<>();
        answerList.add(Singleton.getInstance().getAnswer());
        CloudLabelGraphic cloudLabelGraphic = new CloudLabelGraphic(graphicOverlay, answerList);
        graphicOverlay.add(cloudLabelGraphic);
        graphicOverlay.postInvalidate();


        do{
            try {
                Toast.makeText(this, "Waiting for MLKit.", Toast.LENGTH_SHORT).show();
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }while(!Singleton.getInstance().checkIfBothMLArrived());


        Intent intent = new Intent(getBaseContext(), ArActivity.class);
        startActivity(intent);
    }
}