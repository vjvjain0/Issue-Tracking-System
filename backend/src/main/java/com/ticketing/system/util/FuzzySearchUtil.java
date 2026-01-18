package com.ticketing.system.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FuzzySearchUtil {

    // Common stop words that shouldn't drive matching decisions
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "a", "an", "the", "is", "are", "was", "were", "be", "been", "being",
        "have", "has", "had", "do", "does", "did", "will", "would", "could",
        "should", "may", "might", "must", "shall", "can", "need", "dare",
        "ought", "used", "to", "of", "in", "for", "on", "with", "at", "by",
        "from", "as", "into", "through", "during", "before", "after",
        "above", "below", "between", "under", "again", "further", "then",
        "once", "here", "there", "when", "where", "why", "how", "all", "each",
        "few", "more", "most", "other", "some", "such", "no", "nor", "not",
        "only", "own", "same", "so", "than", "too", "very", "just", "also",
        "now", "and", "but", "or", "if", "because", "until", "while",
        "it", "its", "this", "that", "these", "those", "i", "me", "my",
        "we", "our", "you", "your", "he", "him", "his", "she", "her",
        "they", "them", "their", "what", "which", "who", "whom"
    ));

    /**
     * Check if a word is a stop word (common word that doesn't carry much meaning)
     */
    private static boolean isStopWord(String word) {
        return word.length() <= 2 || STOP_WORDS.contains(word.toLowerCase());
    }

    /**
     * Calculate the Levenshtein distance between two strings.
     */
    public static int levenshteinDistance(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();
        
        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    costs[j] = j;
                } else if (j > 0) {
                    int newValue = costs[j - 1];
                    if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
                        newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                    }
                    costs[j - 1] = lastValue;
                    lastValue = newValue;
                }
            }
            if (i > 0) {
                costs[s2.length()] = lastValue;
            }
        }
        return costs[s2.length()];
    }

    /**
     * Calculate similarity ratio between two strings (0.0 to 1.0).
     */
    public static double similarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        if (s1.isEmpty() && s2.isEmpty()) {
            return 1.0;
        }
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) {
            return 1.0;
        }
        int distance = levenshteinDistance(s1, s2);
        return 1.0 - ((double) distance / maxLen);
    }

    /**
     * Find the best matching word similarity for a query word against target words.
     */
    private static double findBestWordMatch(String queryWord, String[] targetWords, double threshold) {
        double bestScore = 0.0;
        
        for (String targetWord : targetWords) {
            if (targetWord.length() < 2) continue;
            
            // Exact match
            if (queryWord.equalsIgnoreCase(targetWord)) {
                return 1.0;
            }
            
            // Check containment with strict ratio
            if (targetWord.toLowerCase().contains(queryWord.toLowerCase()) || 
                queryWord.toLowerCase().contains(targetWord.toLowerCase())) {
                int minLen = Math.min(queryWord.length(), targetWord.length());
                int maxLen = Math.max(queryWord.length(), targetWord.length());
                double containRatio = (double) minLen / maxLen;
                if (containRatio >= 0.75) {
                    bestScore = Math.max(bestScore, 0.9);
                    continue;
                }
            }
            
            // Calculate similarity
            double sim = similarity(queryWord, targetWord);
            
            // For short words (3 chars or less), require higher similarity
            if (queryWord.length() <= 3) {
                if (sim >= threshold + 0.15) {
                    bestScore = Math.max(bestScore, sim);
                }
            } else {
                if (sim >= threshold) {
                    bestScore = Math.max(bestScore, sim);
                }
            }
        }
        
        return bestScore;
    }

    /**
     * Check if query fuzzy matches the target text.
     * Requires content words (non-stop-words) to match well.
     */
    public static boolean fuzzyMatches(String query, String target, double threshold) {
        if (query == null || target == null) {
            return false;
        }
        
        query = query.toLowerCase().trim();
        target = target.toLowerCase();
        
        // First check for exact substring match of the entire query
        if (target.contains(query)) {
            return true;
        }
        
        // Split into words
        String[] queryWords = query.split("[\\s,.!?;:]+");
        String[] targetWords = target.split("[\\s,.!?;:]+");
        
        // Categorize query words into content words and stop words
        int contentWordCount = 0;
        int contentWordsMatched = 0;
        int stopWordCount = 0;
        int stopWordsMatched = 0;
        
        for (String queryWord : queryWords) {
            if (queryWord.isEmpty()) continue;
            
            boolean isStop = isStopWord(queryWord);
            double bestMatch = findBestWordMatch(queryWord, targetWords, threshold);
            boolean matched = bestMatch >= threshold;
            
            if (isStop) {
                stopWordCount++;
                if (matched) stopWordsMatched++;
            } else {
                contentWordCount++;
                if (matched) contentWordsMatched++;
            }
        }
        
        // If no content words in query, fall back to overall matching
        if (contentWordCount == 0) {
            if (stopWordCount == 0) return false;
            // For stop-word-only queries, require all to match
            return stopWordsMatched == stopWordCount;
        }
        
        // Calculate content word match ratio
        double contentMatchRatio = (double) contentWordsMatched / contentWordCount;
        
        // Require at least 60% of content words to match
        // AND at least 1 content word must match
        if (contentMatchRatio < 0.6 || contentWordsMatched < 1) {
            return false;
        }
        
        // For queries with multiple content words, require stronger matching
        if (contentWordCount >= 2) {
            // Need at least 2 content words to match, or all of them if only 2
            if (contentWordCount == 2) {
                return contentWordsMatched == 2;
            } else {
                // For 3+ content words, need at least 60% AND minimum 2
                return contentWordsMatched >= 2 && contentMatchRatio >= 0.6;
            }
        }
        
        return true;
    }

    /**
     * Calculate a relevance score for a fuzzy match.
     * Higher score = better match. Returns 0 if not a good match.
     */
    public static double calculateRelevanceScore(String query, String title, String description) {
        if (query == null || (title == null && description == null)) {
            return 0.0;
        }
        
        query = query.toLowerCase().trim();
        String[] queryWords = query.split("[\\s,.!?;:]+");
        
        double titleScore = 0.0;
        double descScore = 0.0;
        int contentWordsInTitle = 0;
        int contentWordsInDesc = 0;
        int totalContentWords = 0;
        
        double threshold = getDefaultThreshold();
        
        for (String queryWord : queryWords) {
            if (queryWord.isEmpty()) continue;
            
            boolean isContentWord = !isStopWord(queryWord);
            if (isContentWord) totalContentWords++;
            
            // Check title
            if (title != null) {
                String[] titleWords = title.toLowerCase().split("[\\s,.!?;:]+");
                double titleBest = findBestWordMatch(queryWord, titleWords, threshold);
                if (titleBest >= threshold) {
                    // Content words score higher
                    double wordScore = isContentWord ? titleBest * 1.5 : titleBest * 0.5;
                    titleScore += wordScore;
                    if (isContentWord) contentWordsInTitle++;
                }
            }
            
            // Check description
            if (description != null) {
                String[] descWords = description.toLowerCase().split("[\\s,.!?;:]+");
                double descBest = findBestWordMatch(queryWord, descWords, threshold);
                if (descBest >= threshold) {
                    double wordScore = isContentWord ? descBest * 0.75 : descBest * 0.25;
                    descScore += wordScore;
                    if (isContentWord) contentWordsInDesc++;
                }
            }
        }
        
        // Penalize if not enough content words match
        if (totalContentWords > 0) {
            int maxContentMatches = Math.max(contentWordsInTitle, contentWordsInDesc);
            double contentRatio = (double) maxContentMatches / totalContentWords;
            if (contentRatio < 0.5) {
                return 0.0; // Not a good match
            }
        }
        
        // Title matches weighted 2x
        return (titleScore * 2.0) + descScore;
    }

    /**
     * Get the default fuzzy matching threshold.
     */
    public static double getDefaultThreshold() {
        return 0.70;
    }
}
