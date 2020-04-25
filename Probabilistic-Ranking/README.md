# Project 1: Probabilistic Ranking

To run the program, please use the following command:
java -jar HW1.jar BM25 | LM | RM1 | RM3 IndexPath QueriesPath Output


Function Description:
The search function is adpoted from the demo search file.
1. BM25: set the searcher's similarity to BM25Similarity and search for top 1000 results.
2. LM: set the searcher's similarity to LMDirichletSimilarity and search for top 1000 results.
3. RM1: use LM to search the top 1000 results and use the top k results to get P(t|q, R). Sorting the terms according to P(t|q, R) and get top n terms to expanded the query and rerank the top 1000 results.
4. RM3: use LM to search the top 1000 results and use the top k results to get P_RM1(t|q). Then calculate P_RM3(t|q) as (1-lambda)*P_MLE(t|q) + lambda*P_RM1(t|q). Sorting the terms according to P_RM3(t|q) and get top n terms to expanded the query and rerank the top 1000 results.

The parameters are tuned to get better results.
For map: k = 50, n = 5, lambda = 0.9 achieves better performance.

For P5(Precision after 5 docs retrieved): k = 5, n = 5, lambda = 0.8 achieves better performance.



         