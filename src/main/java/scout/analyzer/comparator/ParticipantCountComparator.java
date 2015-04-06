package scout.analyzer.comparator;

import scout.analyzer.model.Activity;

public class ParticipantCountComparator extends AbstractRangeComparator {
    @Override
    public double compare(Activity rev1, Activity rev2) {
        return compare(rev1.participants_min, rev1.participants_max, rev2.participants_min, rev2.participants_max, 1, 20);
    }

    @Override
    public String toString() {
        return "grpsz";
    }
}
