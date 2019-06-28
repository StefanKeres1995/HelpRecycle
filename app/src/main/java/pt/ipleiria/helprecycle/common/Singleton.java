package pt.ipleiria.helprecycle.common;

import java.util.HashMap;

public class Singleton {

    private HashMap<String, Float> tfLabels;
    private HashMap<String, Float> mlLabels;
    private HashMap<String, Float> labels;

    private HashMap<String, String> tfList;

    public String answer = "NOTHING";

    public Boolean tfArrived = false;
    public Boolean mlArrived = false;

    private static final Singleton ourInstance = new Singleton();

    public static Singleton getInstance() {
        return ourInstance;
    }

    private Singleton() {
        labels = new HashMap<>();
    }


    public HashMap<String, Float> getLabels() {
        return labels;
    }

    public void setAnswer(String answer) {
        resetAnswer();
        this.answer = answer;
    }

    public void resetAnswer(){
        answer = "NOTHING";
        setTfArrived(false);
        setMlArrived(false);
    }

    public Boolean checkIfBothMLArrived(){
        if (tfArrived && mlArrived){
            return true;
        }
        return false;
    }

    public HashMap<String, Float> getTfLabels() {
        return tfLabels;
    }

    public String setTfLabels(HashMap<String, Float> tfLabels) {
        this.tfLabels = tfLabels;
        setTfArrived(true);
        if(checkIfBothMLArrived()){
            return joinLabels();
        }
        return "NOTHING";
    }

    public HashMap<String, Float> getMlLabels() {
        return mlLabels;
    }

    public String setMlLabels(HashMap<String, Float> mlLabels) {
        this.mlLabels = mlLabels;
        setMlArrived(true);
        if(checkIfBothMLArrived()){
            return joinLabels();
        }
        return "NOTHING";
    }

    public void setTfArrived(Boolean tfArrived) {
        this.tfArrived = tfArrived;
    }

    public void setMlArrived(Boolean mlArrived) {
        this.mlArrived = mlArrived;
    }

    public HashMap<String, String> getTfList() {
        return tfList;
    }

    public void setTfList(HashMap<String, String> tfList) {
        this.tfList = tfList;
    }

    public String joinLabels (){

        float yellowConfidence = 0;
        float greenConfidence = 0;
        float blueConfidence = 0;

        for (String mlKey: getMlLabels().keySet()) {
            for (String tfKey: getTfLabels().keySet()) {
                if (tfKey.toLowerCase().equals(mlKey.toLowerCase())){
                    switch (getTfList().get(mlKey.toLowerCase())){
                        case "PLASTIC":
                        case "METAL":
                            yellowConfidence = yellowConfidence + getMlLabels().get(mlKey);
                            break;
                        case "PAPER":
                            blueConfidence = blueConfidence + getMlLabels().get(mlKey);
                            break;
                        case "GLASS":
                            greenConfidence = greenConfidence + getMlLabels().get(mlKey);
                            break;
                        default:
                            break;

                    }

                }
            }
        }
        HashMap<String, Float> responseMap = new HashMap<>();
        responseMap.put( "YELLOW", yellowConfidence);
        responseMap.put("BLUE", blueConfidence);
        responseMap.put("GREEN", greenConfidence);

        String answer = sortHashMapByValues(responseMap);
        setAnswer(answer);
        return answer;
    }

    public String sortHashMapByValues(HashMap<String, Float> passedMap) {
        float highestValue = -1;
        String highestKey = "EMPTY";
        for (String key: passedMap.keySet()){
            if(highestValue <= passedMap.get(key)){
                highestValue = passedMap.get(key);
                highestKey = key;
            }
        }
        if(highestValue == 0){
            return "NOTHING";
        }
        return highestKey;
    }
}
