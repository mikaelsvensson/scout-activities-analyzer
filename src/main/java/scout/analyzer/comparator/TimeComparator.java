package scout.analyzer.comparator;

import scout.analyzer.model.Activity;

public class TimeComparator extends AbstractRangeComparator {
    @Override
    public double compare(Activity rev1, Activity rev2) {
        return compare(rev1.time_min, rev1.time_max, rev2.time_min, rev2.time_max, 1, 120);
    }

    @Override
    public String toString() {
        return "time";
    }
}
