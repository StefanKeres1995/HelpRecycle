package pt.ipleiria.helprecycle;

import java.util.HashMap;

public class Recycle {

    HashMap<String, String> recyclableMaterial = new HashMap<>();

    private static final String YELLOW = "Yellow";
    private static final String GREEN = "Green";
    private static final String BLUE = "Blue";
    private static final String BROWN = "Brown"; //food waste, kinda
    private static final String RED = "Red"; //batteries and others
    //private static final String TEXTILES = "Textile";

    public void createMaterialList() {

        //Yellow
        recyclableMaterial.put("Metal", "Yellow");
        recyclableMaterial.put("Plastic", "Yellow");

    }
}
