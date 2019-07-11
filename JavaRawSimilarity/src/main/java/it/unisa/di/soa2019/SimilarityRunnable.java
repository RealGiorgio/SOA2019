package it.unisa.di.soa2019;

import java.io.IOException;
import java.util.*;

public class SimilarityRunnable implements Runnable {

    private boolean linesCmp=false;
    private List<String> lines, lines1, lines2;
    private Main main;
    private long threadID;

    public SimilarityRunnable(Main main, List<String> lines){
        this.main = main;
        this.lines = lines;
    }

    public SimilarityRunnable(Main main, List<String> lines1, List<String> lines2){
        this.main = main;
        this.lines1 = lines1;
        this.lines2 = lines2;
        linesCmp = true;
    }

    @Override
    public void run() {
        threadID = Thread.currentThread().getId();
//        System.out.println("Thread " + threadID % 4 + " running");
        try {
            if(linesCmp){
                main.callbackSimilarity(calcLinesCmp());
            } else {
                main.callbackSimilarity(calcSimilarityLines());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private StringBuffer calcLinesCmp() {
        Map<Long, Map<String, Double>> mapSimilarity1 = new HashMap<>();
        Map<Long, Map<String, Double>> mapSimilarity2 = new HashMap<>();
        String[] values;
        StringBuffer inputBuffer = new StringBuffer();

        for (String line: lines1) {
            values = line.split(",");
            Long groupID = Long.parseLong(values[0]);
            Map<String, Double> mapWords = new HashMap<>();

            for (int i = 1; i < values.length - 1; i++) {
                mapWords.put(values[i].split(":")[0], Double.parseDouble(values[i].split(":")[1]) / Double.parseDouble(values[values.length - 1]));
            }

            mapSimilarity1.put(groupID, mapWords);
        }

        for (String line: lines2) {
            values = line.split(",");
            Long groupID = Long.parseLong(values[0]);
            Map<String, Double> mapWords = new HashMap<>();

            for (int i = 1; i < values.length - 1; i++) {
                mapWords.put(values[i].split(":")[0], Double.parseDouble(values[i].split(":")[1]) / Double.parseDouble(values[values.length - 1]));
            }

            mapSimilarity2.put(groupID, mapWords);
        }

        Set<Long> groupIDs1 = mapSimilarity1.keySet();
        Set<Long> groupIDs2 = mapSimilarity2.keySet();
        String line;
        Iterator<Long> iteratorI = mapSimilarity1.keySet().iterator();
        Iterator<Long> iteratorJ = mapSimilarity2.keySet().iterator();
        for (int i = 0; i < groupIDs1.size(); i++) {
            Long groupI = iteratorI.next();
            for (int j = 0; j < groupIDs2.size(); j++) {
                Long groupJ = iteratorJ.next();
                Double similarity = new Double(0);
                Map<String, Double> mapI = mapSimilarity1.get(groupI);
                Map<String, Double> mapJ = mapSimilarity2.get(groupJ);
                Iterable<String> wordsI = mapI.keySet();

                StringBuilder lineBuilder = new StringBuilder();
                lineBuilder.append("(").append(groupI).append(",").append(groupJ).append(")").append("\t");

                for (String word : wordsI) {
                    if (mapJ.containsKey(word)) {
                        similarity += mapI.get(word) * mapJ.get(word);
                    }
                }
                if (similarity != 0) {
                    lineBuilder.append(similarity);
                    line = lineBuilder.toString();
                    inputBuffer.append(line);
                    inputBuffer.append('\n');
                }
            }
            iteratorJ = mapSimilarity2.keySet().iterator();
        }

        return inputBuffer;
    }

    public StringBuffer calcSimilarityLines() {
        Map<Long, Map<String, Double>> mapSimilarity = new HashMap<>();
        String[] values;
        StringBuffer inputBuffer = new StringBuffer();

        for (String line: lines) {
            values = line.split(",");
            Long groupID = Long.parseLong(values[0]);
            Map<String, Double> mapWords = new HashMap<>();

            for (int i = 1; i < values.length - 1; i++) {
                mapWords.put(values[i].split(":")[0], Double.parseDouble(values[i].split(":")[1]) / Double.parseDouble(values[values.length - 1]));
            }

            mapSimilarity.put(groupID, mapWords);
        }

        Set<Long> groupIDs = mapSimilarity.keySet();
        String line;
        Iterator<Long> iteratorI = mapSimilarity.keySet().iterator();
        Iterator<Long> iteratorJ = mapSimilarity.keySet().iterator();
        for (int i = 0; i < groupIDs.size(); i++) {
            Long groupI = iteratorI.next();
            for (int j = 0; j <= i; j++) {
                iteratorJ.next();
            }
            for (int j = i + 1; j < groupIDs.size(); j++) {
                Long groupJ = iteratorJ.next();
                Double similarity = new Double(0);
                Map<String, Double> mapI = mapSimilarity.get(groupI);
                Map<String, Double> mapJ = mapSimilarity.get(groupJ);
                Iterable<String> wordsI = mapI.keySet();

                StringBuilder lineBuilder = new StringBuilder();
                lineBuilder.append("(").append(groupI).append(",").append(groupJ).append(")").append("\t");

                for (String word : wordsI) {
                    if (mapJ.containsKey(word)) {
                        similarity += mapI.get(word) * mapJ.get(word);
                    }
                }
                if (similarity != 0) {
                    lineBuilder.append(similarity);
                    line = lineBuilder.toString();
                    inputBuffer.append(line);
                    inputBuffer.append('\n');
                }
            }
            iteratorJ = mapSimilarity.keySet().iterator();
        }

        return inputBuffer;
    }
}
