package com.brko.service.ml.service.impl;

import com.brko.service.ml.models.PfspStopWords;
import com.brko.service.ml.models.PfspWord2Vec;
import com.brko.service.ml.service.TextComparatorService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.DocumentPreprocessor;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.math3.util.Pair;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

/**
 * Created by ppetrov on 9/20/2016.
 */
@Service
public class TextComparatorServiceImpl implements TextComparatorService {

    final Logger logger = Logger.getLogger(this.getClass().getName());

    private PfspWord2Vec pfspWord2VecModel;

    @PostConstruct
    public void loadWord2VecModel() {
        pfspWord2VecModel = new PfspWord2Vec();

        try (ZipFile word2VecZip = new ZipFile("C:\\Users\\ppetrov\\Documents\\ppt_private\\diplomska\\word2vec.zip");
             InputStream inputStream = word2VecZip.getInputStream(word2VecZip.entries().nextElement());
             BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {

            String parts [] = br.readLine().split(" ");
            int numWords = Integer.parseInt(parts[0]);
            int vectorSize = Integer.parseInt(parts[1]);
            logger.info(String.format("Loading %s words with vectors. Vector size is %s.", numWords, vectorSize));

            for (int t=0;t<numWords;t++) {

                String line = br.readLine();
                parts = line.split("<->");

                String word = parts[0];

                String[] vectorParts = parts[1].split(" ");
                float[] vector = new float[vectorParts.length];
                for (int i = 0; i < vector.length; i++) {
                    vector[i] = Float.parseFloat(vectorParts[i]);
                }

                pfspWord2VecModel.addWordVecToModel(word, vector);
            }

            logger.info("PfspWord2Vec model is loaded.");
        } catch (Exception e) {
            logger.error(e);
        }
    }

    public double computeDistance(String paperText, String noteText) {
        List<String> paperWords = getWordsFromText(paperText);
        List<String> noteWords = getWordsFromText(noteText);

        List<Pair<String, Double>> noteNbowPairs = getNbowPairsForListOfWords(noteWords);
        List<Pair<String, Double>> paperNbowPairs = getNbowPairsForListOfWords(paperWords);

        double [][] costMatrix = new double[noteNbowPairs.size()][paperNbowPairs.size()];
        for (int i=0;i<noteNbowPairs.size();i++) {
            for (int j=0;j<paperNbowPairs.size();j++) {
                double cost = pfspWord2VecModel
                        .distance(noteNbowPairs.get(i).getFirst(), paperNbowPairs.get(j).getFirst());
                costMatrix[i][j] = cost;
            }
        }

        return computeDistanceWithLinearProgramming(costMatrix, noteNbowPairs, paperNbowPairs);
    }

    private double computeDistanceWithLinearProgramming(
            double[][] costMatrix,
            List<Pair<String, Double>> noteNbowPairs,
            List<Pair<String, Double>> paperNbowPairs) {
        return 0;
    }

    private double[][] createTransportationMatrix(
            List<String> noteWords,
            Map<String, Double> noteNbowMap,
            List<String> paperWords,
            Map<String, Double> paperNbowMap) {

        double [][] transportationMatrix = new double[noteNbowMap.size()][paperNbowMap.size()];
        for (int i=0;i<noteWords.size(); i++) {
            for (int j=0;j<paperWords.size();j++) {
                transportationMatrix[i][j] = noteNbowMap.get(noteWords.get(i)) * paperNbowMap.get(paperWords.get(j));
            }
        }
        return transportationMatrix;
    }

    private List<Pair<String, Double>> getNbowPairsForListOfWords(List<String> words) {
        Map<String, Double> nBow = Maps.newHashMap();

        for (String word : words) {
            if (!nBow.containsKey(word)) {
                nBow.put(word, 0.0);
            }
            nBow.put(word, nBow.get(word) + 1.0);
        }

        for (String word : nBow.keySet()) {
            nBow.put(word, nBow.get(word) / words.size());
        }

        return nBow
                .entrySet()
                .stream()
                .map(entry -> new Pair<String, Double>(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private List<String> getWordsFromText(String text) {
        DocumentPreprocessor documentPreprocessor = new DocumentPreprocessor(new StringReader(text));

        List<String> textWords = Lists.newArrayList();
        for (List<HasWord> sentence : documentPreprocessor) {
            textWords.addAll(getWordsFromSentance(sentence));
        }

        return textWords;
    }

    private Collection<? extends String> getWordsFromSentance(List<HasWord> sentence) {
        return sentence
                .stream()
                .filter(hasWord -> BooleanUtils.isFalse(PfspStopWords.isStopWord(hasWord.word())))
                .map(HasWord::word)
                .collect(Collectors.toList());
    }
}
