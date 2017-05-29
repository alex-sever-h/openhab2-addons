package org.no.rules;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.CSVLoader;

public class Predictor {

    private RandomForest rf;
    private Instances trainData;

    public double[] predict(String input, int classIndex) throws Exception {

        if (rf == null) {
            throw new Exception("Not trained. Run training first.");
        }

        InputStream stream = new ByteArrayInputStream(input.getBytes());

        CSVLoader csvLoader = new CSVLoader();

        csvLoader.setSource(new BufferedInputStream(stream));

        csvLoader.setNoHeaderRowPresent(true);

        Instances trainData1 = new Instances(csvLoader.getDataSet());

        trainData1.setClassIndex(classIndex);

        trainData.add(0, trainData1.firstInstance());

        double[] results = rf.distributionForInstance(trainData.firstInstance());

        trainData.remove(0);

        return results;
    }

    public void train(String[] inputData, int classIndex) throws Exception {
        StringBuffer sb = new StringBuffer();
        System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\n");
        for (String s : inputData) {
            sb.append(s);
            sb.append('\n');
        }
        System.out.println("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\n");

        InputStream stream = new ByteArrayInputStream(sb.toString().getBytes());
        System.out.println("cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc\n");

        CSVLoader csvLoader = new CSVLoader();
        System.out.println("dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd\n");

        csvLoader.setSource(new BufferedInputStream(stream));

        csvLoader.setNoHeaderRowPresent(true);

        rf = new RandomForest();

        trainData = new Instances(csvLoader.getDataSet());

        trainData.setClassIndex(classIndex);

        // You can set the options here
        String[] options = new String[2];
        options[0] = "-R";
        // rf.setOptions(options);

        rf.buildClassifier(trainData);

        double[] results = rf.distributionForInstance(trainData.firstInstance());

        System.out.println("Results: " + results);
    }
}
