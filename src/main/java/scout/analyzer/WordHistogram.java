package scout.analyzer;

import java.util.*;

public class WordHistogram {
    private final Map<String, Integer> wordCounts = new HashMap<>();

    public void countWord(String word) {
        if (!wordCounts.containsKey(word)) {
            wordCounts.put(word, 1);
        } else {
            wordCounts.put(word, wordCounts.get(word) + 1);
        }
    }

    public Set<String> getWords() {
        return wordCounts.keySet();
    }

    Map<String, Double> getPercentages() {
        int sum = 0;
        for (Integer count : wordCounts.values()) {
            sum += count;
        }
        Map<String, Double> map = new HashMap<>();
        for (Map.Entry<String, Integer> entry : wordCounts.entrySet()) {
            map.put(entry.getKey(), 1.0 * entry.getValue() / sum);
        }
        return map;
    }

    public double compare(WordHistogram that) {
        double sum = 0;
        Map<String, Double> thisPercentages = getPercentages();
        Map<String, Double> thatPercentages = that.getPercentages();
        for (Map.Entry<String, Double> entry : thisPercentages.entrySet()) {
            Double thisPercent = entry.getValue();
            Double thatPercent = thatPercentages.get(entry.getKey());
            if (thatPercent != null) {
                sum += (thisPercent + thatPercent) / 2;
            }
        }
        return sum;
    }

    public Map<String, Integer> getTop(int limit) {
        ArrayList<Map.Entry<String, Integer>> entries = new ArrayList<>(wordCounts.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });
        Map<String, Integer> topWords = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(entries.size(), limit); i++) {
            Map.Entry<String, Integer> entry = entries.get(i);
            topWords.put(entry.getKey(), entry.getValue());
        }
        return topWords;
    }
}
