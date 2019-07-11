package it.unisa.di.soa2019;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


public class Main implements PartitioningInterface, SimilarityInterface {
    private int numberOfProcessors;
    private AtomicInteger callbacks;
    private AtomicInteger numThread;
    private PartitioningRunnable partitioningRunnable;
    private SimilarityRunnable similarityRunnable;
    private Map<Long, Map<String, Long>> mapPartitioning;
    private List<Map<Long, Map<String, Long>>> mapPartitioningList;
    private FileInputStream inputStreamIn = null;
    private Scanner scIn = null;
    private FileInputStream inputStreamOut = null;
    private Scanner scOut = null;
    private String inPath, outPath, partPath;
    private Double norma;
    private FileOutputStream fileSimOut;
    private Map<Integer, List<String>> mapSimilarityLines;

    private File filePart;
    private File inFile;

    public static void main(String[] args) {
        Main main = new Main();
        System.out.println("Main starts....\n");

        main.setup(args);
        main.threadPartitioningStart(main.getNumberOfProcessors());
    }

    public int getNumberOfProcessors() {
        return numberOfProcessors;
    }

    public String getPartPath() {
        return partPath;
    }

    public Main (){
        Runtime runtime = Runtime.getRuntime();
        numberOfProcessors = runtime.availableProcessors();
    }

    public void threadSimilarityStart(int numberOfProcessors) throws IOException {
        Map<Integer, List<String>> mapLines = similaritySplit();
        callbacks.set(0);

        numThread = new AtomicInteger(numberOfProcessors);
        for (int i = 0; i < numberOfProcessors; i++) {
            similarityRunnable = new SimilarityRunnable(this, new ArrayList<>(mapLines.get(i)));
            Thread thread = new Thread(similarityRunnable);
            thread.start();
        }

    }

    public void threadPartitioningStart(int numberOfProcessors) {
        numThread = new AtomicInteger(numberOfProcessors);
        for (int i = 0; i < numberOfProcessors; i++) {
            partitioningRunnable = new PartitioningRunnable(this, inFile);
            Thread thread = new Thread(partitioningRunnable);
            thread.start();
        }

    }

    private void threadPartitioningStart(Map<Long, Map<String, Long>> mapPartitioning, Map<Long, Map<String, Long>> mapPartitioning2) {
        partitioningRunnable = new PartitioningRunnable(this, mapPartitioning, mapPartitioning2);
        Thread thread = new Thread(partitioningRunnable);
        thread.start();
    }


    public Map<Integer, List<String>> similaritySplit() {
        mapSimilarityLines = new HashMap<>();
        String line;
        Integer numLine = 0;

        try {
            BufferedReader bufferedPart = new BufferedReader(new FileReader(filePart));

            while ((line = bufferedPart.readLine()) != null) {
                Integer threadID = numLine%numberOfProcessors;
                if(mapSimilarityLines.containsKey(numLine%numberOfProcessors)){
                    mapSimilarityLines.get(threadID).add(line);
                } else {
                    mapSimilarityLines.put(threadID, new ArrayList<>(Arrays.asList(line)));
                }
                numLine++;
            }
            bufferedPart.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return mapSimilarityLines;
    }

    public void setup(String[] args) {
        inPath = args[0];
        outPath = args[1];
        partPath = args[2];
        try {
            inFile = new File(inPath);
            callbacks = new AtomicInteger(0);
            mapPartitioningList = new ArrayList<>();
            mapPartitioning = new HashMap<>();
            inputStreamIn = new FileInputStream(inFile);
            scIn = new Scanner(inputStreamIn, "UTF-8");
            PrintWriter writer;
            filePart = new File(partPath);
            if(!filePart.exists()) {
                filePart.createNewFile();
            } else {
                filePart.delete();
                filePart.createNewFile();
            }
            File fileSim = new File(outPath);
            fileSim.createNewFile();
            writer = new PrintWriter(fileSim);
            writer.print("");
            writer.close();
            inputStreamOut = new FileInputStream(outPath);
            scOut = new Scanner(inputStreamOut, "UTF-8");
            fileSimOut = new FileOutputStream(outPath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void callback(Map<Long, Map<String, Long>> mapPartitioning) {
        callbacks.set(callbacks.get() + 1);
        mapPartitioningList.add(mapPartitioning);
        if (callbacks.get() == numThread.get()) {
            callbacks.set(0);
            numThread.set(mapPartitioningList.size());
            for (int i = 0; i < mapPartitioningList.size(); i++) {
                threadPartitioningStart(this.mapPartitioning, mapPartitioningList.get(i));
            }
        }
    }

    @Override
    public synchronized void callback() throws IOException {
        callbacks.set(callbacks.get() + 1);
        if (callbacks.get() == numThread.get()) {
            updatePartFile();
        }
    }


    public void updatePartFile() throws IOException {
        String[] values;
        String line;
        StringBuffer inputBuffer = new StringBuffer();
        List<Long> groupInFile = new ArrayList<>();

        BufferedReader bufferedPart = new BufferedReader(new FileReader(filePart));

        while ((line = bufferedPart.readLine()) != null) {
            norma = new Double(0);
            values = line.split(",");
            Long groupID = Long.parseLong(values[0]);
            groupInFile.add(groupID);

            if (mapPartitioning.containsKey(groupID)) {
                Map<String, Long> lineMap = new HashMap<>();
                for (int i = 1; i < values.length - 1; i++) {
                    lineMap.put(values[i].split(":")[0], Long.parseLong(values[i].split(":")[1]));
                }

                Map<String, Long> mapWords = mapPartitioning.get(groupID);
                Set<String> words = mapWords.keySet();
                for (String word : words) {
                    if (lineMap.containsKey(word)) {
                        lineMap.put(word, mapWords.get(word) + lineMap.get(word));
                    } else {
                        lineMap.put(word, new Long(1));
                    }
                }
                StringBuilder lineBuilder = new StringBuilder();
                lineBuilder.append(groupID);
                for (Map.Entry<String, Long> entry : lineMap.entrySet()) {
                    lineBuilder.append(",").append(entry.getKey()).append(":").append(entry.getValue());
                    norma += Math.pow(entry.getValue(), 2);
                }
                norma = Math.sqrt(norma);
                lineBuilder.append(",").append(norma);
                line = lineBuilder.toString();
            }
            inputBuffer.append(line);
            inputBuffer.append('\n');
        }
        for (Long groupID : mapPartitioning.keySet()) {
            norma = new Double(0);
            if (!groupInFile.contains(groupID)) {
                StringBuilder lineBuilder = new StringBuilder();
                lineBuilder.append(groupID);
                Map<String, Long> mapGroup = mapPartitioning.get(groupID);
                Set<String> words = mapGroup.keySet();
                for (String word : words) {
                    lineBuilder.append(",").append(word).append(":").append(mapGroup.get(word));
                    norma += Math.pow(mapGroup.get(word), 2);
                }
                norma = Math.sqrt(norma);
                lineBuilder.append(",").append(norma);
                line = lineBuilder.toString();
                inputBuffer.append(line);
                inputBuffer.append('\n');
            }
        }
        String inputStr = inputBuffer.toString();

        FileOutputStream filePartOut = new FileOutputStream(filePart);
        filePartOut.write(inputStr.getBytes());
        filePartOut.close();
        mapPartitioning = new HashMap<>();
        threadSimilarityStart(numberOfProcessors);
    }

    @Override
    public synchronized void callbackSimilarity(StringBuffer similarityInputStream) throws IOException {
        callbacks.set(callbacks.get() + 1);
        String inputStr = similarityInputStream.toString();
        fileSimOut.write(inputStr.getBytes());
        if (callbacks.get() == numThread.get()) {
//            numThread.set(binomialCoeff(mapSimilarityLines.size(), 2));
            callbacks.set(numThread.get()+1);
            for (int i = 0; i < mapSimilarityLines.size(); i++) {
                for (int j = i+1; j < mapSimilarityLines.size(); j++) {
                    similarityRunnable = new SimilarityRunnable(this, new ArrayList<>(mapSimilarityLines.get(i)), mapSimilarityLines.get(j));
                    Thread thread = new Thread(similarityRunnable);
                    thread.start();
                }
            }
        }
    }


//    public static int binomialCoeff(int n, int k) {
//        int C[] = new int[k + 1];
//
//        // nC0 is 1
//        C[0] = 1;
//
//        for (int i = 1; i <= n; i++) {
//            // Compute next row of pascal
//            // triangle using the previous row
//            for (int j = Math.min(i, k); j > 0; j--)
//                C[j] = C[j] + C[j - 1];
//        }
//        return C[k];
//    }
}
