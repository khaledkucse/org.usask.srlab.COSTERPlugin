package org.usask.srlab.coster.core.infer;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.usask.srlab.coster.core.COSTER;
import org.usask.srlab.coster.core.model.APIElement;
import org.usask.srlab.coster.core.model.OLDEntry;
import org.usask.srlab.coster.core.model.TestResult;
import org.usask.srlab.coster.core.utils.InferUtil;
import org.usask.srlab.coster.core.utils.ParseUtil;
import org.usask.srlab.coster.core.utils.TrainUtil;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ImportStmtComplete {
    private static final Logger logger = LogManager.getLogger(ImportStmtComplete.class.getName()); // logger variable for loggin in the file
    private static final DecimalFormat df = new DecimalFormat(); // Decimal formet variable for formating decimal into 2 digits

    private static void print(Object s){System.out.println(s.toString());}

    public static List<String> complete(String dictPath) {
        print("Collecting Jar files...");
        logger.info("Collecting Jar Files...");
        String[] jarPaths = ParseUtil.collectJarFiles(new File(COSTER.getJarRepoPath()));
        print("Collecting code snippet...");
        logger.info("Collecting code snippet...");
        
        TrainUtil.getSingletonTrainUtilInst().dictonaryCheckup(dictPath);
        ArrayList<String> snippetPath = ParseUtil.collectSOSnippets(new File(COSTER.getRepositoryPath()));
        String[] sourcefilePaths = new String[snippetPath.size()];
        sourcefilePaths = snippetPath.toArray(sourcefilePaths);
        logger.info("Total Number of Code Snippet: " + sourcefilePaths.length);

        print("Extracting data from the code snippets...");
        logger.info("Extracting data from the code snippets...");
        List<APIElement> testCases = InferUtil.collectSODataset(sourcefilePaths,jarPaths,COSTER.getRepositoryPath(),"");
        for(String eachSourceFile:sourcefilePaths)
        	new File(eachSourceFile).delete();
        
        print("Inferring Test Cases...");
        logger.info("Inferring Test Cases...");
        List<TestResult> testResults = new ArrayList<>();
        for(APIElement eachCase:testCases){
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
                    InferUtil.setCandidates(candidateList);
                    candidateList = InferUtil.generateList();
                    TestResult eachTestResult = new TestResult(eachCase, candidateList, 0);
                    testResults.add(eachTestResult);

                }
            }
        }
        List<String> returnedFQNs = new ArrayList<>();   
        for(TestResult eachTestResult:testResults) {
        	List<OLDEntry> recommendations = eachTestResult.getRecommendations();
        	for(OLDEntry eachEntry:recommendations)
        		if(!returnedFQNs.contains(eachEntry.getFqn()))
        			returnedFQNs.add(eachEntry.getFqn());
        }
        return returnedFQNs;
    }
}
