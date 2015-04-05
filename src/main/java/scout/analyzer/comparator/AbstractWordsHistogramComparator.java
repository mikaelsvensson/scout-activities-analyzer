package scout.analyzer.comparator;

import scout.analyzer.WordHistogram;

import java.util.HashMap;
import java.util.Map;

abstract class AbstractWordsHistogramComparator implements RevisionComparator {
    private final Map<Integer, WordHistogram> histograms = new HashMap<>();

    WordHistogram getHistogram(int key, String[] words) {
        if (!histograms.containsKey(key)) {
            histograms.put(key, getHistogram(words));
        }
        return histograms.get(key);
    }

    private WordHistogram getHistogram(String[] words) {
        WordHistogram histogram = new WordHistogram();

        for (String word : words) {
            if (word != null && word.length() > 0) {
                histogram.countWord(word);
            }
        }
        return histogram;
    }
}
