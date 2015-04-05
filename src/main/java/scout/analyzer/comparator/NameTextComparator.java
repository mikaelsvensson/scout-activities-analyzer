package scout.analyzer.comparator;

import scout.analyzer.model.Activity;
import scout.analyzer.WordHistogram;

public class NameTextComparator extends AbstractWordsHistogramComparator {
    @Override
    public double compare(Activity rev1, Activity rev2) {
        WordHistogram rev1Histogram = getHistogram(rev1.activity_id, rev1.name_words());
        WordHistogram rev2Histogram = getHistogram(rev2.activity_id, rev2.name_words());
        return rev1Histogram.compare(rev2Histogram);
    }

    @Override
    public String toString() {
        return "name";
    }
}
