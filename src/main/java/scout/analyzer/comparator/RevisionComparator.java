package scout.analyzer.comparator;

import scout.analyzer.model.Activity;

public interface RevisionComparator {
    double compare(Activity rev1, Activity rev2);
}
