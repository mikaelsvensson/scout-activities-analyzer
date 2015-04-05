package scout.analyzer.model;


import scout.analyzer.RelatedActivitiesFinder;
import scout.analyzer.Util;
import scout.analyzer.WordHistogram;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@XmlRootElement(name = "data")
public class Activities {

    @XmlElement(name = "row")
    public List<Activity> activities = new ArrayList<>();

    void simplifyVocabulary(RelatedActivitiesFinder.Configuration reportConfiguration) throws IOException, JAXBException {
        /**
         * Get list of common words.
         */
        Set<String> commonWords = getCommonWords();
        reportConfiguration.commonWords = commonWords;

        /**
         * Get list of similar words which should be translated to a common "base word".
         */
        Map<String, String> translations = getTranslations();
        reportConfiguration.translations = translations;

        for (Activity activity : activities) {
            activity.simplifyVocabulary(commonWords, translations);
        }
    }

    private Map<String, String> getTranslations() throws JAXBException, IOException {
        Map<String, String> translations = new TreeMap<>();
        Variations va = Util.loadXMLResource(Variations.class, "/variations.xml");
        for (Variation variation : va.all()) {
            String[] suffixes = variation.suffix.split("-");
            for (String prefix : variation.prefix) {
                for (int i = 1; i < suffixes.length; i++) {
                    translations.put(prefix + suffixes[i], prefix + suffixes[0]);
                }
            }
        }

        Map<String, String> wordCategories = getWordCategoriesMap();
        translations.putAll(wordCategories);
        return translations;
    }

    private Map<String, String> getWordCategoriesMap() throws IOException {

        Properties props = new Properties();
        props.load(getClass().getResourceAsStream("/rejected_word_categories.properties"));
        Set<String> rejectedCategories = props.stringPropertyNames();

        TreeSet<String> uniqueWords = new TreeSet<>();
        for (Activity revision : activities) {
            Collections.addAll(uniqueWords, revision.all_words());
        }

        FileWriter writer = new FileWriter("words.txt");
        Pattern commonEndings = Pattern.compile("(a|ar|are)$");
        int nextPossibleCategory = 0;
        Map<String, String> wordCategories = new HashMap<>();
        String[] words = uniqueWords.toArray(new String[uniqueWords.size()]);

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            writer.write(word);

//            if (!rejectedCategories.contains(word)) {
                String category = commonEndings.matcher(word).replaceFirst("");

                int followers = 0;
                if (i > nextPossibleCategory && category.length() >= 4) {
                    while (i + followers + 1 < words.length && words[i + followers + 1].startsWith(category)) {
                        followers++;
                    }
                }

                if (followers >= 2) {
                    nextPossibleCategory = i + followers;
                    writer.write(" <- ordkategori " + category + "/" + word + " (" + followers + " liknande) ");
                    for (int x = i + 1; x < i + 1 + followers; x++) {
                        wordCategories.put(words[x], word);
                        writer.write(words[x] + " ");
                    }
                }
//            }
            writer.write('\n');
        }
        writer.close();
        return wordCategories;
    }


    private Set<String> getCommonWords() throws IOException {
        Properties fixedWords = new Properties();
        fixedWords.load(getClass().getResourceAsStream("/common_words.properties"));
        Set<String> commonWords = new HashSet<>(fixedWords.stringPropertyNames());

        WordHistogram wordHistogram = new WordHistogram();
        for (Activity activity : activities) {
            for (String word : activity.all_words()) {
                wordHistogram.countWord(word);
            }
        }
        commonWords.addAll(wordHistogram.getTop(50).keySet());
        return commonWords;
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

        if (configuration.simplifyVocabulary) {
            job.simplifyVocabulary(configuration);
        }
        return job;
    }

}
