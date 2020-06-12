package org.usask.srlab.coster.core.infer;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;


import org.usask.srlab.coster.core.COSTER;
import org.usask.srlab.coster.core.model.APIElement;
import org.usask.srlab.coster.core.model.TestResult;
import org.usask.srlab.coster.core.model.OLDEntry;
import org.usask.srlab.coster.core.utils.EvaluationUtil;
import org.usask.srlab.coster.core.utils.InferUtil;
import org.usask.srlab.coster.core.utils.ParseUtil;


public class ExtrinsicInference {
    private static final Logger logger = LogManager.getLogger(ExtrinsicInference.class.getName()); // logger variable for loggin in the file
    private static final DecimalFormat df = new DecimalFormat(); // Decimal formet variable for formating decimal into 2 digits

    private static void print(Object s){System.out.println(s.toString());}


    public static void evaluation(){
        List<APIElement> testCases = new ArrayList<>();
        if(COSTER.getIsExtraction()) {
            print("Collecting Jar files...");
            logger.info("Collecting Jar Files...");
            String[] jarPaths = ParseUtil.collectJarFiles(new File(COSTER.getJarRepoPath()));
            print("Collecting StackOverflow code snippets...");
            logger.info("Collecting StackOverflow code snippets...");
            ArrayList<String> snippetPaths = ParseUtil.collectSOSnippets(new File(COSTER.getRepositoryPath()));
            String[] sourcefilePaths = new String[snippetPaths.size()];
            sourcefilePaths = snippetPaths.toArray(sourcefilePaths);
            logger.info("Total Number of StackOverflow Code Snippet: "+ sourcefilePaths.length);

            print("Extracting test data from the StackOverflow code snippets...");
            logger.info("Extracting test data from the StackOverflow code snippets...");
            testCases = InferUtil.collectSODataset(sourcefilePaths,jarPaths,COSTER.getRepositoryPath(),COSTER.getDatasetPath());
        }
        else{
            testCases = InferUtil.collectTestAPISFromDATASET(COSTER.getDatasetPath());
        }
        List<TestResult> testResults = new ArrayList<>();
        int count = 0;
        long totalInferenceTime = 0;
        print("Inferring test data...");
        logger.info("Inferring test data...");
        for(APIElement eachCase:testCases){
            long starttime = System.currentTimeMillis();
            if(InferUtil.isLibraryExists(eachCase.getActualFQN(),COSTER.getModelPath())) {
                String queryContext = StringUtils.join(eachCase.getContext()," ").replaceAll(",","");
                String queryAPIelement = eachCase.getName();
                List<OLDEntry> candidateList = InferUtil.collectCandidateList(queryContext, COSTER.getModelPath());
                for(OLDEntry eachCandidate:candidateList){
                    String candidateContext = eachCandidate.getContext();
                    String candidateFQN = eachCandidate.getFqn();
                    double contextSimialrityScore = InferUtil.calculateContextSimilarity(queryContext,candidateContext, COSTER.getContextSimilarity());
                    double nameSimilarityScore = InferUtil.calculateNameSimilarity(queryAPIelement,candidateFQN, COSTER.getNameSimilarity());
                    double recommendationScore = InferUtil.calculateRecommendationScore(eachCandidate.getScore(),contextSimialrityScore,nameSimilarityScore);
                    eachCandidate.setScore(recommendationScore);
                }
                if(COSTER.isFqnThreshold()) {
                    long inferenceTime = System.currentTimeMillis() - starttime;
                    totalInferenceTime += inferenceTime;
                    InferUtil.setCandidates(candidateList);
                    candidateList = InferUtil.generateList();
                    TestResult eachTestResult = new TestResult(eachCase, candidateList, inferenceTime);
                    testResults.add(eachTestResult);
                    count++;

                }
            }
            if(count%100 == 0){
                logger.info("Test Data Inferred: "+count+"/"+testCases.size()+" ("+df.format((count*100/testCases.size()))+"%)");
                print("Test Data Inferred: "+count+"/"+testCases.size()+" ("+df.format((count*100/testCases.size()))+"%)");
            }
        }

        logger.info("Test Data Inferred: "+count+"/"+testCases.size()+" ("+df.format((count*100/testCases.size()))+"%)");
        print("Test Data Inferred: "+count+"/"+testCases.size()+" ("+df.format((count*100/testCases.size()))+"%)");

        print("Average time for inference: "+ (double)(totalInferenceTime/testCases.size()) + "milliseconds");
        logger.info("Average time for inference: "+ (double)(totalInferenceTime/testCases.size()) + "milliseconds");

        logger.info("Calculating performance mesures...");
//        int totalCases = CompilableCodeExtraction.getTotalCase().get();
        EvaluationUtil evaluationUtil = new EvaluationUtil(testResults);
        print("Precision: "+String.format("%.2f",evaluationUtil.getPrecision()));
        print("Recall: "+String.format("%.2f",evaluationUtil.getRecall()));
        print("FScore: "+String.format("%.2f",evaluationUtil.getFscore()));
        logger.info("Precision: "+String.format("%.2f",evaluationUtil.getPrecision()));
        logger.info("Recall: "+String.format("%.2f",evaluationUtil.getRecall()));
        logger.info("FScore: "+String.format("%.2f",evaluationUtil.getFscore()));

        logger.info("Extrinsic Evaluation is done!!!");
        print("Extrinsic Evaluation is Done!!!");
    }
}
