package org.amjonota;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Utils {

    private static final int MAX_SUMMARY_WORDS = 10;
    private static final float DAMPING = 0.85f;
    private static final int ITERATIONS = 30;

    private Utils() {}

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 8;
    }

    public static boolean isNonEmpty(String val) {
        return val != null && !val.isBlank();
    }

    public static String summarize(String text) {
        if (text == null || text.isBlank()) return "";

        String[] sentences = text.split("(?<=[.!?])\\s+|\\r?\\n+");
        if (sentences.length <= 1) {
            String trimmed = text.trim();
            String[] words = trimmed.split("\\s+");
            if (words.length <= MAX_SUMMARY_WORDS) return trimmed;
            return String.join(" ", Arrays.copyOf(words, MAX_SUMMARY_WORDS));
        }

        String[][] sentenceWords = new String[sentences.length][];
        for (int i = 0; i < sentences.length; i++) {
            sentenceWords[i] = sentences[i].toLowerCase().split("[^a-z0-9]+");
        }

        int n = sentences.length;
        float[][] scores = new float[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                float sim = similarity(sentenceWords[i], sentenceWords[j]);
                scores[i][j] = sim;
                scores[j][i] = sim;
            }
        }

        float[] ranks = new float[n];
        Arrays.fill(ranks, 1.0f / n);

        for (int iter = 0; iter < ITERATIONS; iter++) {
            float[] newRanks = new float[n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (i == j) continue;
                    float rowSum = 0;
                    for (int k = 0; k < n; k++) rowSum += scores[i][k];
                    if (rowSum > 0) newRanks[i] += scores[j][i] * ranks[j] / rowSum;
                }

                newRanks[i] = (1 - DAMPING) + DAMPING * newRanks[i];
            }

            ranks = newRanks;
        }

        float[] finalRanks = ranks.clone();
        Integer[] indices = new Integer[n];

        for (int i = 0; i < n; i++) {
            indices[i] = i;
        }

        Arrays.sort(indices, (i, j) -> Float.compare(finalRanks[j], finalRanks[i]));

        StringBuilder sb = new StringBuilder();
        int wordCount = 0;
        Arrays.sort(indices);

        for (int idx : indices) {
            String[] words = sentences[idx].trim().split("\\s+");
            if (wordCount + words.length > MAX_SUMMARY_WORDS && wordCount > 0) break;
            if (sb.length() > 0) sb.append(" ");
            sb.append(sentences[idx].trim());
            wordCount += words.length;
            if (wordCount >= MAX_SUMMARY_WORDS) break;
        }

        return sb.toString();
    }

    private static float similarity(String[] a, String[] b) {
        Set<String> setA = new HashSet<>();
        for (String w : a) if (w.length() > 2) setA.add(w);
        int overlap = 0;
        for (String w : b) {
            if (w.length() > 2 && setA.contains(w)) overlap++;
        }

        int denom = (int) (Math.log10(setA.size() + 1) * Math.log10(b.length + 1));
        return denom == 0 ? 0 : (float) overlap / denom;
    }
}
