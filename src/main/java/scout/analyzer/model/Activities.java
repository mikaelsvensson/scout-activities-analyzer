package scout.analyzer.model;


import scout.analyzer.RelatedActivitiesFinder;
import scout.analyzer.Simplifier;
import scout.analyzer.Util;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.util.*;

@XmlRootElement(name = "data")
public class Activities {

    @XmlElement(name = "row")
    public List<Activity> activities = new ArrayList<>();

    public Simplifier.Metadata simplifyVocabulary(Simplifier.SimplifyRule[] simplifyRules, int minimumWordGroupSize) throws IOException, JAXBException {
        Simplifier simplifier = new Simplifier(getAllWords(), simplifyRules, minimumWordGroupSize);

        for (Activity activity : activities) {
            activity.simplifyVocabulary(simplifier);
        }

        return simplifier.getMetadata();
    }


    private List<String> getAllWords() {
        ArrayList<String> allWords = new ArrayList<>();
        for (Activity revision : activities) {
            Collections.addAll(allWords, revision.all_words());
        }
        return allWords;
    }

    public static Activities get(RelatedActivitiesFinder.Configuration configuration) throws JAXBException, IOException {
        Activities job = Util.loadXMLResource(Activities.class, "/activity_versions.xml");
        CategoryMappings categoryMappings = Util.loadXMLResource(CategoryMappings.class, "/activity_versions_categories.xml");
        Map<Integer, Activity> activities = new HashMap<>();
        for (Activity activity : job.activities) {
            activity.setCategories(categoryMappings.mappings);
        }
        for (Activity activity : job.activities) {
            activities.put(activity.activity_id, activity);
        }
        job.activities = new ArrayList<>(activities.values());

        return job;
    }

}
