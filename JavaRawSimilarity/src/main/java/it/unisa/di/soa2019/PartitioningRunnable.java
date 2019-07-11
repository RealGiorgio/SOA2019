package it.unisa.di.soa2019;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;
import org.tartarus.snowball.ext.italianStemmer;
import org.tartarus.snowball.ext.russianStemmer;
import org.tartarus.snowball.ext.spanishStemmer;

import java.io.*;
import java.util.*;

public class PartitioningRunnable implements Runnable {

    private File inputFile;
    private long threadID;
    private boolean calcMap = false;
    private boolean mapCmp = false;
    private Main main;
    private List<List<String>> wordGroupList;
    private Map<Long, List<String>> mapPartitioningList;
    private Map<Long, Map<String, Long>> mapPartitioning;
    private Map<Long, Map<String, Long>> mapPartitioning2;

    public PartitioningRunnable(Main main, File inputFile) {
        this.main = main;
        calcMap = true;
        mapPartitioning = new HashMap<>();
        this.inputFile = inputFile;
    }

    public PartitioningRunnable(Main main, Map<Long, Map<String, Long>> mapPartitioning, Map<Long, Map<String, Long>> mapPartitioning2) {
        this.main = main;
        this.mapPartitioning = mapPartitioning;
        this.mapPartitioning2 = mapPartitioning2;
        mapCmp = true;
    }


    @Override
    public void run() {
        threadID = Thread.currentThread().getId();
//        System.out.println("Thread " + threadID + " running");
        try {
            if (calcMap) {
                main.callback(calcMap());
            }

            if (mapCmp) {
                mapCmp(mapPartitioning2);
                main.callback();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void mapCmp(Map<Long, Map<String, Long>> mapPartitioning2) {
        Map<String, Long> mapGroup;

        for (Long groupID : mapPartitioning2.keySet()) {
            if (mapPartitioning.containsKey(groupID)) {
                mapGroup = mapPartitioning.get(groupID);
            } else {
                mapGroup = new HashMap<>();
                mapPartitioning.put(groupID, mapGroup);
            }
            for (String word : mapPartitioning2.get(groupID).keySet()) {
                if (mapGroup.containsKey(word)) {
                    if (mapGroup.get(word) != null)
                        try {
                            mapGroup.put(word, mapGroup.get(word) + mapPartitioning2.get(groupID).get(word));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                } else {
                    mapGroup.put(word, mapPartitioning2.get(groupID).get(word));
                }
            }
        }
    }

    private Map<Long, Map<String, Long>> calcMap() throws IOException {
        String line;
        int numLine = 0;
        String[] values;
        BufferedReader bufferedPart = new BufferedReader(new FileReader(inputFile));
        Map<String, Long> mapGroup;
        mapPartitioning = new HashMap<>();

        HashMap<String, SnowballStemmer> stemmersMap;
        SnowballStemmer genericStemmer = new englishStemmer();

        stemmersMap = new HashMap<>(4);
        stemmersMap.put("it", new italianStemmer());
        stemmersMap.put("ru", new russianStemmer());
        stemmersMap.put("es", new spanishStemmer());
        stemmersMap.put("en", new englishStemmer());

        while ((line = bufferedPart.readLine()) != null) {
            if ((numLine % main.getNumberOfProcessors()) == (threadID % main.getNumberOfProcessors())) {
                values = line.split(",");
                String lang;

                if (values.length > 4) {
                    if (values.length > 5) {
                        lang = values[values.length - 1];
                        for (int i = 4; i < values.length - 1; i++) {
                            values[3] = values[3] + values[i]; // TODO use string builder here
                        }
                    } else {
                        try {
                            lang = values[4];
                        } catch (Exception e) {
                            e.printStackTrace();
                            lang = "unknown";
                        }
                    }

                    String[] words = values[3].replaceAll("\\n", " ").replaceAll("[^\b a-zA-Z0-9'а-яА-Я]", "").trim().toLowerCase().split(" ");
                    String groupIDstring = values[2];
                    Long groupID;
                    if (words != null && words.length >= 1) {
                        try {
                            groupID = Long.parseLong(groupIDstring);
                        } catch (NumberFormatException e) {
//                            e.printStackTrace();
                            numLine++;
                            continue;
                        }

                        if (mapPartitioning.containsKey(groupID)) {
                            mapGroup = mapPartitioning.get(groupID);
                        } else {
                            mapGroup = new HashMap<>();
                            mapPartitioning.put(groupID, mapGroup);
                        }

                        for (String word : words) {
                            if (word.length() == 0) {
                                continue;
                            }

                            SnowballStemmer currentStemmer = null;
                            if (stemmersMap.containsKey(lang)) {
                                currentStemmer = stemmersMap.get(lang);
                            } else {
                                currentStemmer = genericStemmer;
                            }
                            if (StopWords.getInstance().isStopWord(word)) {
                                continue;
                            }
                            currentStemmer.setCurrent(word);
                            currentStemmer.stem();
                            String stemmed = currentStemmer.getCurrent();

                            if (mapGroup.containsKey(stemmed)) {
                                mapGroup.put(stemmed, mapGroup.get(stemmed) + 1);
                            } else {
                                mapGroup.put(stemmed, new Long(1));
                            }
                        }
                    }
                }
            }
            numLine++;
        }
        return mapPartitioning;
    }
}
