package com.resumeai.trainer;

import opennlp.tools.namefind.*;
import opennlp.tools.util.*;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ModelTrainer {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: ModelTrainer <training-data.txt> <output-model.bin>");
            System.err.println("Example:");
            System.err.println("  ModelTrainer src/main/resources/opennlp-models/training-data.txt");
            System.err.println("              src/main/resources/opennlp-models/en-ner-skills.bin");
            System.exit(1);
        }

        String inputPath  = args[0];
        String outputPath = args[1];

        System.out.println("Training NER model...");
        System.out.println("  Input:  " + inputPath);
        System.out.println("  Output: " + outputPath);

        File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            System.err.println("ERROR: Training data file not found: " + inputPath);
            System.exit(1);
        }

        InputStreamFactory factory = () -> new FileInputStream(inputPath);

        try (ObjectStream<String> lineStream = new PlainTextByLineStream(factory, StandardCharsets.UTF_8);
             ObjectStream<NameSample> sampleStream = new NameSampleDataStream(lineStream)) {

            TrainingParameters params = TrainingParameters.defaultParams();
            params.put(TrainingParameters.ITERATIONS_PARAM, 150);
            params.put(TrainingParameters.CUTOFF_PARAM, 1);

            TokenNameFinderModel model = NameFinderME.train(
                    "en",
                    "skill",
                    sampleStream,
                    params,
                    new TokenNameFinderFactory()
            );

            File outputFile = new File(outputPath);
            outputFile.getParentFile().mkdirs();

            try (OutputStream modelOut = new BufferedOutputStream(new FileOutputStream(outputFile))) {
                model.serialize(modelOut);
            }

            System.out.println("Model trained and saved to: " + outputPath);
            System.out.println("Restart the application to activate AI mode.");
        }
    }
}
