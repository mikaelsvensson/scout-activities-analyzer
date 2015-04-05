package scout.analyzer.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.*;

@XmlRootElement(name = "row")
public class Activity {
    @XmlElement
    private int id;
    @XmlElement
    public int status;
    @XmlElement
    private String name;
    @XmlElement
    private String descr_material;
    @XmlElement
    private String descr_introduction;
    @XmlElement
    private String descr_prepare;
    @XmlElement
    private String descr_main;
    @XmlElement
    private String descr_safety;
    @XmlElement
    private String descr_notes;
    @XmlElement
    public int age_min;
    @XmlElement
    public int age_max;
    @XmlElement
    public int participants_min;
    @XmlElement
    public int participants_max;
    @XmlElement
    public int time_min;
    @XmlElement
    public int time_max;
    @XmlElement
    public boolean featured;
    @XmlElement
    public String created_at;
    @XmlElement
    public String updated_at;
    @XmlElement
    public int activity_id;

    private String[] name_words;
    private String[] descr_introduction_words;
    private String[] descr_safety_words;
    private String[] descr_main_words;
    private String[] descr_material_words;
    private String[] descr_notes_words;
    private String[] descr_prepare_words;
    private String[] all_words;
    private Collection<Integer> categories;

    private String[] getWordSequence(String text) {
        if (text != null) {
            return text.toLowerCase().split("[^\\p{IsAlphabetic}]+");
        } else {
            return null;
        }
    }

    public String[] descr_introduction_words() {
        if (descr_introduction_words == null) {
            descr_introduction_words = getWordSequence(descr_introduction);
            descr_introduction = null;
        }
        return descr_introduction_words;
    }

    public String[] name_words() {
        if (name_words == null) {
            name_words = getWordSequence(name);
            name = null;
        }
        return name_words;
    }

    String[] descr_main_words() {
        if (descr_main_words == null) {
            descr_main_words = getWordSequence(descr_main);
            descr_main = null;
        }
        return descr_main_words;
    }

    public String[] descr_material_words() {
        if (descr_material_words == null) {
            descr_material_words = getWordSequence(descr_material);
            descr_material = null;
        }
        return descr_material_words;
    }

    String[] descr_notes_words() {
        if (descr_notes_words == null) {
            descr_notes_words = getWordSequence(descr_notes);
            descr_notes = null;
        }
        return descr_notes_words;
    }

    String[] descr_prepare_words() {
        if (descr_prepare_words == null) {
            descr_prepare_words = getWordSequence(descr_prepare);
            descr_prepare = null;
        }
        return descr_prepare_words;
    }

    String[] descr_safety_words() {
        if (descr_safety_words == null) {
            descr_safety_words = getWordSequence(descr_safety);
            descr_safety = null;
        }
        return descr_safety_words;
    }

    public String[] all_words() {
        if (all_words == null) {
            int count = 0;
            String[][] allStrings = new String[][]{
                    name_words(),
                    descr_introduction_words(),
                    descr_main_words(),
                    descr_material_words(),
                    descr_notes_words(),
                    descr_prepare_words(),
                    descr_safety_words()};
            for (String[] strings : allStrings) {
                count += strings.length;
            }
            all_words = new String[count];
            int pos = 0;
            for (String[] strings : allStrings) {
                System.arraycopy(strings, 0, all_words, pos, strings.length);
                pos += strings.length;
            }
        }
        return all_words;
    }

    public void setCategories(List<CategoryMapping> categories) {
        this.categories = new ArrayList<>();
        for (CategoryMapping category : categories) {
            if (category.activity_version_id == id) {
                this.categories.add(category.category_id);
            }
        }
    }

    public Collection<Integer> getCategories() {
        return categories;
    }

    public void simplifyVocabulary(Set<String> commonWords, Map<String, String> translations) {
        all_words = null;
//        for (int i = 0; i < name_words().length; i++) {
//            name_words[i] = fixVariation(name_words[i], commonWords, translations);
//        }
        for (int i = 0; i < descr_introduction_words().length; i++) {
            descr_introduction_words[i] = fixVariation(descr_introduction_words[i], commonWords, translations);
        }
        for (int i = 0; i < descr_safety_words().length; i++) {
            descr_safety_words[i] = fixVariation(descr_safety_words[i], commonWords, translations);
        }
        for (int i = 0; i < descr_main_words().length; i++) {
            descr_main_words[i] = fixVariation(descr_main_words[i], commonWords, translations);
        }
        for (int i = 0; i < descr_material_words().length; i++) {
            descr_material_words[i] = fixVariation(descr_material_words[i], commonWords, translations);
        }
        for (int i = 0; i < descr_notes_words().length; i++) {
            descr_notes_words[i] = fixVariation(descr_notes_words[i], commonWords, translations);
        }
        for (int i = 0; i < descr_prepare_words().length; i++) {
            descr_prepare_words[i] = fixVariation(descr_prepare_words[i], commonWords, translations);
        }
    }

    private static String fixVariation(String word, Set<String> commonWords, Map<String, String> translations) {
        if (commonWords.contains(word)) {
            return "";
        } else if (translations.containsKey(word)) {
            return translations.get(word);
        } else {
            return word;
        }
    }

}