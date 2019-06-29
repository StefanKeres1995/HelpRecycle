package pt.ipleiria.helprecycle.common;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

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

    public String getAnswer(){
        return answer;
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

    /**
     * All labels confidences are interpreted to find the correct label
     * First get ml raw material labels and add their confidence to a list
     * Crosscheck ml's other labels with the tensorflow labels to interpret their materials and add their confidence to the same list
     * Get the materials from tensorflow labels and add them to a different list
     * Compare the medium highest medium values from each list to determine if they both have the same answer or which is higher
     * both could have nothing so the labels can return the "NOTHING" answer
     * */

    public String joinLabels (){


        List<Float> yellowConfidenceList = new LinkedList<>();
        List<Float> blueConfidenceList = new LinkedList<>();
        List<Float> greenConfidenceList = new LinkedList<>();

        for (String mlKey: getMlLabels().keySet()) {
            //first check the material itself
            switch (mlKey.toUpperCase()){
                case "PLASTIC":
                case "METAL":
                    yellowConfidenceList.add(getMlLabels().get(mlKey));
                    break;
                case "PAPER":
                    blueConfidenceList.add(getMlLabels().get(mlKey));
                    break;
                case "GLASS":
                    greenConfidenceList.add(getMlLabels().get(mlKey));
                    break;
                default:
                    break;
            }

            //check by crosschecking ml kit labels with tensorflow to get their materials
            for (String tfKey: getTfLabels().keySet()) {
                if (tfKey.toLowerCase().equals(mlKey.toLowerCase())){
                    switch (getTfList().get(mlKey.toLowerCase())){
                        case "PLASTIC":
                        case "METAL":
                            yellowConfidenceList.add(getMlLabels().get(mlKey));
                            break;
                        case "PAPER":
                            blueConfidenceList.add(getMlLabels().get(mlKey));
                            break;
                        case "GLASS":
                            greenConfidenceList.add(getMlLabels().get(mlKey));
                            break;
                        default:
                            break;

                    }

                }
            }
        }
        HashMap<String, Float> mlResponseMap = new HashMap<>();

        mlResponseMap.put( "YELLOW", mediumConfidence(yellowConfidenceList));
        mlResponseMap.put("BLUE", mediumConfidence(blueConfidenceList));
        mlResponseMap.put("GREEN", mediumConfidence(greenConfidenceList));

        String mlAnswer = sortHashMapByValues(mlResponseMap);



        //get tensorflow confidences from tensroflow results
        yellowConfidenceList = new LinkedList<>();
        blueConfidenceList = new LinkedList<>();
        greenConfidenceList = new LinkedList<>();

        for (String tfKey: getTfLabels().keySet()
        ) {
            switch (getTfList().get(tfKey)){
                case "PLASTIC":
                case "METAL":
                    yellowConfidenceList.add(getTfLabels().get(tfKey));
                    break;
                case "PAPER":
                    blueConfidenceList.add(getTfLabels().get(tfKey));
                    break;
                case "GLASS":
                    greenConfidenceList.add(getTfLabels().get(tfKey));
                    break;
                default:
                    break;
            }
        }
        //reset responseMap


        HashMap<String, Float> tfResponseMap = new HashMap<>();
        tfResponseMap.put( "YELLOW", mediumConfidence(yellowConfidenceList));
        tfResponseMap.put("BLUE", mediumConfidence(blueConfidenceList));
        tfResponseMap.put("GREEN", mediumConfidence(greenConfidenceList));

        String tfAnswer = sortHashMapByValues(tfResponseMap);

        //check if they are the same answer
        if (mlAnswer.equals(tfAnswer)){
            setAnswer(mlAnswer);
            return mlAnswer;
        }
        else if(mlResponseMap.containsKey(mlAnswer) && tfResponseMap.containsKey(tfAnswer)) {
            //both answers are different from nothing
            if (mlResponseMap.get(mlAnswer) > tfResponseMap.get(tfAnswer) ){
                //ml has better confidence
                setAnswer(mlAnswer);
                return mlAnswer;
            }
            else {
                //tf has better confidence
                setAnswer(tfAnswer);
                return tfAnswer;
            }
        } else {
            if (mlResponseMap.containsKey(mlAnswer)){
                //ml exists and tf not
                setAnswer(mlAnswer);
                return mlAnswer;
            }
            else {
                //tf exists and ml not
                setAnswer(tfAnswer);
                return tfAnswer;
            }
        }



    }

    public String sortHashMapByValues(HashMap<String, Float> passedMap) {
        float highestValue = -1;
        String highestKey = "NOTHING";
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


    private float mediumConfidence (List<Float> valuesList){
        if (!valuesList.isEmpty()){
            float sum = 0;
            for (Float value: valuesList) {
                sum += value;
            }
            return sum / valuesList.size();
        }
        return 0;
    }
}
