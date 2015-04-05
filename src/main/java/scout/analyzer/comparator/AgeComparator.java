package scout.analyzer.comparator;

import scout.analyzer.model.Activity;

public class AgeComparator extends AbstractRangeComparator {
    @Override
    public double compare(Activity rev1, Activity rev2) {
        return compare(rev1.age_min, rev1.age_max, rev2.age_min, rev2.age_max, 8, 25);
    }

    @Override
    public String toString() {
        return "age";
    }
}
