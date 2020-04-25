/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//package org.apache.lucene.demo;


import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.LMSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;


/** Simple command-line based search demo. */
public class SearchFiles {

  private SearchFiles() {}

  /** Simple command-line based search demo. */
  public static void main(String[] args) throws Exception {
    String usage =
      "Usage:\tjava -jar HW1.jar [BM25 | LM | RM1 | RM3] IndexPath QueriesPath Output \n";

    String field = "Text";  
    if(args.length < 4) {
    	  System.out.println(usage);
    	  System.exit(0);
    }
    String index = null;  // index path
    String  queryPath = null; // query path 
    String  ranker = null;  // search function
    String output = null;   // output file 
    if(args.length == 4) {
    	ranker = args[0];
    	index = args[1];
    	queryPath = args[2];
    	output = args[3];
    }
    


    
    
    IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
    IndexSearcher searcher = new IndexSearcher(reader);
    Analyzer analyzer = new StandardAnalyzer();

    
    // set similarity function
    if (ranker.equalsIgnoreCase("BM25")) { 
        Similarity simBM25 = new BM25Similarity();
        searcher.setSimilarity(simBM25);
      } else {
    	  Similarity simLM = new LMDirichletSimilarity();
          searcher.setSimilarity(simLM);
      }


    ArrayList<ArrayList<String>> queries = new ArrayList<ArrayList<String>>();
    if (queryPath != null) {
    	// parse query file
    	queries = ParseFiles.parseQuery(queryPath);
    	
    } 
    QueryParser parser = new QueryParser(field, analyzer);
    ArrayList<String> topics = queries.get(1);  // get the query list
    ArrayList<String> topicnums = queries.get(0); // get the query number list
    String querynum;
    String query;
    FileWriter fw = new FileWriter(output);
    String res = "";
    for(int i  = 0; i < topics.size(); i++) {
    	query = topics.get(i);
    	querynum = topicnums.get(i);
    	
        if (query.length() == 0) {
          continue;  // in case of empty query
        }
        Query q = parser.parse(query);
//        System.out.println("Searching for: " + q.toString(field));
        

        TopDocs results = searcher.search(q, 1000);
        
        ScoreDoc[] hits = results.scoreDocs;
        
        int top = 50; // get  top results for term expanding
        if(hits.length < top)
        	top = hits.length;
        ScoreDoc[] topHits = new ScoreDoc[top];
       
        for(int k = 0; k<topHits.length; k++) {
        	topHits[k] = hits[k];
        }
       
        if(ranker.equals("RM1")) {
        	
        	Map<ScoreDoc, Double> docScore = RM1(hits, topHits, reader, analyzer, query, field);
        	ArrayList<ScoreDoc> reRankedDoc = reRank(docScore);	
        	// rerank 
        	for (int j = 0; j < reRankedDoc.size(); j++) {
        		hits[j] = reRankedDoc.get(j);
        	}
        }
        else if(ranker.equals("RM3")) {
        	Map<ScoreDoc, Double> docScore = RM3(hits, topHits, reader, analyzer, query, field);
        	ArrayList<ScoreDoc> reRankedDoc = reRank(docScore);
        	for (int j = 0; j < reRankedDoc.size(); j++) {
        		hits[j] = reRankedDoc.get(j);
        	}
        }

//        int numTotalHits = Math.toIntExact(results.totalHits.value);
//        System.out.println(numTotalHits + " total matching documents");
        
        for(int j = 0; j < hits.length; j++) {
        	Document doc = searcher.doc(hits[j].doc);
//        	System.out.println("explain: " +searcher.explain(q, hits[j].doc));
//        	 searcher.explain(q,hits[j].doc);
        	
        	// format and write output
            String docID = doc.get("DocId");
	    	res =  querynum + "\t" + "Q0" + "\t" + docID + "\t" + j + "\t" + hits[j].score + "\t" + "yda3\n";
	    	 
	    	fw.write(res);
        }  

    }
    fw.close();
    
  }
  
  
  
  public static  Map<ScoreDoc, Double> RM1(ScoreDoc[] results,ScoreDoc[] topResults, IndexReader reader, Analyzer analyzer, String query, String field) throws IOException {
	  List<String> queryTerms = new ArrayList<>();
      // tokenize the query
      try (TokenStream tokenStream  = analyzer.tokenStream(null, new StringReader(query))) {
        tokenStream.reset();  
        while (tokenStream.incrementToken()) {
      	  queryTerms.add(tokenStream.getAttribute(CharTermAttribute.class).toString());
        }
      } catch (IOException e) {
      	e.printStackTrace();
      }
      
      // get p(t|d) for each term
      Map<String, Map<ScoreDoc, Double>> ptd = calculatePtd1(topResults, reader, field);

      
      Map<String, Double> scoreMap = new HashMap<String, Double>();
      for(String term:ptd.keySet()) {
    	  Map<ScoreDoc, Double> docScore = ptd.get(term);
    	  double score = 0.0;
    	  for(ScoreDoc sd:docScore.keySet()) {
    		  score += docScore.get(sd)*sd.score; // use LM score as P(Q|D)
    	  }
    	  scoreMap.put(term, score);
      }
      

      int k = 5;   // get top 5 terms to expand the query
      List<String> expandedQuery = expandQuery(scoreMap, k);
      
      // caculate log(P(q|D,R)), where q is the selected terms, and is calculated 
      //using same score function as Lucene's LMDirichletSimilarity
      Map<ScoreDoc, Double> docScore = calculatePqd(results, reader, field, expandedQuery);
      for(ScoreDoc sd:docScore.keySet()) {
    	  double score = docScore.get(sd);
    	  score += sd.score; // add the original query score
    	  docScore.put(sd, score);
      }

      return docScore;
  
      
      
  }
      
  public static  ArrayList<ScoreDoc> reRank(Map<ScoreDoc, Double> rerankedDoc) { 
	  ArrayList<ScoreDoc> rerankedDocList= new ArrayList<ScoreDoc>();
      for(ScoreDoc sd:rerankedDoc.keySet()) {
      	double s = rerankedDoc.get(sd);
      	sd.score = (float)s; 
      	
      	rerankedDocList.add(sd);
      }
      
       // sort by score in descending order
      Collections.sort(rerankedDocList,new Comparator<ScoreDoc>() {
          public int compare(ScoreDoc sd1, ScoreDoc sd2) {
          	if(sd1.score < sd2.score)
          		return 1;
          	else if (sd1.score == sd2.score)
          		return 0;
          	else
          		return -1;
           }
        });
	return rerankedDocList;
	   
  }
  

  
  
  public static List<String> expandQuery(Map<String,Double> termScores, int k) { 
	  // sort the keys in ascending order by the values
	  List<String> sortedKeys = new ArrayList<String>();
	  termScores.entrySet().stream().sorted(Entry.comparingByValue())
	  .forEach(key->sortedKeys.add(key.getKey()));
	  List<String> topTerms = new ArrayList<String>();
//	   get the last k keys
	  for(int i = 0; i < k; i ++) {
		  topTerms.add(sortedKeys.get(sortedKeys.size() - 1 - i));
	  }

	  return topTerms;
	   
  }
  
 
 
  // this function calculate P(t|D)
  public static Map<String, Map<ScoreDoc, Double>> calculatePtd1(ScoreDoc[] results, IndexReader reader, String field) throws IOException {
	  Map<String, Map<ScoreDoc, Double>> termProb = new HashMap<String, Map<ScoreDoc, Double>>();
	  Map<ScoreDoc, Double> termCounts;
	  int docId;
	  Terms docterms;
	  ArrayList<String> stopWords = new ArrayList<>(List.of(
		        "a", "an", "and", "are", "as", "at", "be", "but", "by",
		        "for", "if", "in", "into", "is", "it",
		        "no", "not", "of", "on", "or", "such",
		        "that", "the", "their", "then", "there", "these",
		        "they", "this", "to", "was", "will", "with","i","he",
		        "we","you","she","who","were","us","ha","have","would",
		        "hi","our","from","her","has","said","been","which","its",
		        "says","his","may","your"
		    ));
	  for(ScoreDoc sd : results) {
		  docId = sd.doc;
		  docterms = reader.getTermVector(docId, field);
		  
//		  docterms.getSumTotalTermFreq();
		  Long docLen = docterms.getSumTotalTermFreq();
		  TermsEnum terms = docterms.iterator();
		  while (terms.next() != null) {
			  String term = terms.term().utf8ToString();
//			  System.out.println("term:"+term);
			  if(!stopWords.contains(term)) { // ignore stop words
				  long freq = terms.totalTermFreq();
				  
				  if (!termProb.containsKey(term)) {		 
					  termProb.put(term, new HashMap<ScoreDoc, Double>());
			      }
				  termCounts = termProb.get(term);
				  // put P(t|d) in the hashmap
		          termCounts.put(sd, ((double)freq) / docLen);
	        }
		  }
		  
	  }
	return termProb;
	  
  }
  
  // this function calculate query likehood
  public static Map<ScoreDoc, Double> calculatePqd(ScoreDoc[] results, IndexReader reader, String field, List<String> queryTerms) throws IOException {
	  int docId;
	  Terms docterms;
	  double mu = 2000;
	  
	  int collectionLen = 0;
	  Map<ScoreDoc, Map<String,Integer>> docTermFreq = new HashMap<ScoreDoc, Map<String,Integer>>();
	  Map<ScoreDoc,Integer> docLens = new HashMap<ScoreDoc, Integer>(); // store doclen for each doc
	  Map<String, Integer> termCFreq = new HashMap<String, Integer>(); // store queryterm collection freq
	  // this for loog obtain a (doc,termfreq) map for each document 
	  for(ScoreDoc sd : results) {
		  docId = sd.doc;
		  docterms = reader.getTermVector(docId, field); 
//		  docterms.getSumTotalTermFreq();
		  int docLen = (int)docterms.getSumTotalTermFreq();
		  docLens.put(sd, docLen);
		  TermsEnum terms = docterms.iterator();
		  Map<String, Integer> termFreq = new HashMap<String, Integer>();
		  while (terms.next() != null) {
			  String term = terms.term().utf8ToString();
			  int freq = (int)(terms.totalTermFreq());
			  termFreq.put(term,freq); 
			  if(!termCFreq.containsKey(term)) {
				  termCFreq.put(term, freq);
			  }
			  else {
				  freq += termCFreq.get(term);
				  termCFreq.put(term, freq);
			  }
	        }
		  docTermFreq.put(sd, termFreq);	  
	  }
	  
	  for(ScoreDoc sd:docLens.keySet()) {
		  collectionLen += docLens.get(sd);
	  }
	  
	  Map<ScoreDoc, Double> pqds = new HashMap<ScoreDoc, Double>();
	  // this for loop obtain p(Q|D) 
	  for(ScoreDoc sd : results) {
		  double pqd = (double)0.0;		  
		  int cCount = 0;  // freq in the doc collection
		  int docCount = 0;  // freq in a doc
		  Map<String, Integer> termFreq = docTermFreq.get(sd);
		  for(String queryTerm:queryTerms) {
			  if(termFreq.containsKey(queryTerm)) {
				  docCount = termFreq.get(queryTerm);
			  }
			 
			  if(termCFreq.containsKey(queryTerm)) {
				  cCount = termCFreq.get(queryTerm);
			  }
			  double pqid =  (double)docCount / docLens.get(sd);
			  double pqic = (double)cCount / collectionLen; 
			  
			 // this function is same with Lucene's LMDirichletSimilarity score function
			  pqd += (Math.log(1 + docCount /
				        (mu * (pqic))) +
				        Math.log(mu / (docLens.get(sd) + mu)));

		  }

		  pqds.put(sd,pqd);
		  
	  }
	  
	return pqds;
	  
  }
  
  
  public static Map<ScoreDoc, Double> RM3(ScoreDoc[] results,ScoreDoc[] topResults, IndexReader reader, Analyzer analyzer, String query, String field) throws IOException {
	  
	  ArrayList<String> queryTerms = new ArrayList<>();
      // tokenize the query
      try (TokenStream tokenStream  = analyzer.tokenStream(null, new StringReader(query))) {
        tokenStream.reset();  
        while (tokenStream.incrementToken()) {
      	  queryTerms.add(tokenStream.getAttribute(CharTermAttribute.class).toString());
        }
      } catch (IOException e) {
      	e.printStackTrace();
      }
      
      // get p(t|d) for each term
      Map<String, Map<ScoreDoc, Double>> ptd = calculatePtd1(topResults, reader, field);
      double lambda = 0.9;
      double max = -1;
      double min = 1000000;
      
      Map<String, Double> scoreMap = new HashMap<String, Double>();
      for(String term:ptd.keySet()) {
    	  Map<ScoreDoc, Double> docScore = ptd.get(term);
    	  double score = 0.0;
    	  for(ScoreDoc sd:docScore.keySet()) {
    		  score += docScore.get(sd)*sd.score;

    	  }
    	  if(score > max)
    		  max = score;
    	  if(score < min)
    		  min = score;
    	  scoreMap.put(term, score);
      }
      
      // get P_mle(t|q)
      Map<String, Double> ptq = calculatePtq(queryTerms);
      
      // normalize the rm1 term score to [0-1], and get rm3 term score
      for(String term:scoreMap.keySet()) { 
    	  double score = scoreMap.get(term);
    	  score = (score-min)/(max-min);
    	  double pmle = 0.0;
    	  if(ptq.containsKey(term)) {
    		  pmle = ptq.get(term);
    	  }
			
    	  
    	  score = (1-lambda)*pmle + lambda*score;
    	  scoreMap.put(term, score);
      }
           
      int k = 5;
      List<String> expandedQuery = expandQuery(scoreMap, k);
   
      Map<ScoreDoc, Double> docScore = calculatePqd(results, reader, field, expandedQuery);
      for(ScoreDoc sd:docScore.keySet()) {
    	  double score = docScore.get(sd);
    	  score += sd.score;
    	  docScore.put(sd, score);
      }

      return docScore;
  }
  public static Map<String, Double> calculatePtq(ArrayList<String> queries) {
	  Map<String, Double> ptd = new HashMap<String, Double>();
	  Map<String, Integer> count = new HashMap<String, Integer>();
	 for(String term:queries) {
		 if(count.containsKey(term)) {
			 count.put(term, count.get(term) + 1);
		 }
		 else
			 count.put(term, 1);
	 }
	 
	 // get P(t|q)
	 for(String term:queries) {
		 ptd.put(term, (double)count.get(term) / queries.size());
	 } 
	return ptd;
	  
  }  
  
  
  
}
