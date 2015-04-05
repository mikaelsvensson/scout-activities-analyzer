package scout.analyzer.comparator;

import scout.analyzer.model.Activity;

public class CategoryComparator implements RevisionComparator {
    @Override
    public double compare(Activity rev1, Activity rev2) {
        if (rev1.getCategories().size() > 0 && rev2.getCategories().size() > 0) {
            int max = rev1.getCategories().size() + rev2.getCategories().size();
            int actual = 0;
            for (Integer cat1 : rev1.getCategories()) {
                if (rev2.getCategories().contains(cat1)) {
                    actual += 2;
                }
            }
            return 1.0 * actual / max;
        } else {
            return 0;
        }
    }

    @Override
    public String toString() {
        return "category";
    }
}
