package scout.analyzer;

import org.w3c.dom.Document;
import scout.analyzer.comparator.*;
import scout.analyzer.model.Activities;
import scout.analyzer.model.TranslationsAdapter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;

public class RelatedActivitiesFinder {

    private static final NumberFormat NUMBER_FORMAT;

    static {
        NUMBER_FORMAT = NumberFormat.getNumberInstance();
        NUMBER_FORMAT.setMaximumFractionDigits(2);
        NUMBER_FORMAT.setMinimumFractionDigits(2);
    }

    private Marshaller reportMarshaller = null;

    private RelatedActivitiesFinder(Configuration configuration) throws JAXBException, IOException, TransformerException, ParserConfigurationException {

        reportMarshaller = JAXBContext.newInstance(Report.class).createMarshaller();
        reportMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        Report report = new Report();

        List<scout.analyzer.model.Activity> activities = Activities.get(configuration).activities;

        LinkedHashMap<RevisionComparator, Double> comparators = new LinkedHashMap<>();
        comparators.put(new AllTextComparator(), configuration.comparatorFactorAllText);
        comparators.put(new NameTextComparator(), configuration.comparatorFactorName);
        comparators.put(new MaterialTextComparator(), configuration.comparatorFactorMaterials);
        comparators.put(new IntroductionTextComparator(), configuration.comparatorFactorIntroduction);
        comparators.put(new CategoryComparator(), configuration.comparatorFactorCategories);
        comparators.put(new AgeComparator(), configuration.comparatorFactorAge);
        comparators.put(new ParticipantCountComparator(), configuration.comparatorFactorParticipantCount);
        comparators.put(new TimeComparator(), configuration.comparatorFactorTime);

        report.configuration = configuration;

        report.comparatorValuesLabels = createComparatorValuesLabels(comparators.keySet());

        Map<String, Double[]> comparisons = new HashMap<>();

        int activityCount = 10;//activities.size();
        for (int i = 0; i < activityCount; i++) {
            for (int j = i + 1; j < activityCount; j++) {
                Double[] compare = new Double[1 + comparators.size()];
                compare[0] = 0.0;
                int x = 0;
                for (Map.Entry<RevisionComparator, Double> entry : comparators.entrySet()) {
                    Double factor = entry.getValue();
                    RevisionComparator comparator = entry.getKey();
                    compare[++x] = factor > 0 ? factor * comparator.compare(activities.get(i), activities.get(j)) : 0;
                    compare[0] += compare[x];
                }
                if (compare[0] > 0) {
                    comparisons.put(getKey(i, j), compare);
                }
            }
        }

        for (int i = 0; i < activityCount; i++) {
            scout.analyzer.model.Activity revision = activities.get(i);

            Report.Activity relation = new Report.Activity(
                    Util.join(revision.name_words()),
                    Util.join(revision.descr_introduction_words()));
            report.activities.add(relation);

            TreeMap<Integer, Double[]> similar = new TreeMap<>();
            for (int j = 0; j < activityCount; j++) {
                if (i != j) {
                    String key = getKey(i, j);
                    Double[] compare = comparisons.get(key);
                    if (compare != null) {
                        similar.put(j, compare);
                    }
                }
            }

            ArrayList<Map.Entry<Integer, Double[]>> similarEntries = new ArrayList<>(similar.entrySet());
            Collections.sort(similarEntries, new Comparator<Map.Entry<Integer, Double[]>>() {
                @Override
                public int compare(Map.Entry<Integer, Double[]> o1, Map.Entry<Integer, Double[]> o2) {
                    return o2.getValue()[0].compareTo(o1.getValue()[0]);
                }
            });
            for (Map.Entry<Integer, Double[]> entry : similarEntries.subList(0, Math.min(similarEntries.size(), configuration.maxRelated))) {
                String[] parts = new String[entry.getValue().length];
                for (int j = 0; j < entry.getValue().length; j++) {
                    Double value = entry.getValue()[j];
                    parts[j] = NUMBER_FORMAT.format(value);
                }
                relation.add(new Report.Activity.Relation(
                        createComparatorValues(entry.getValue()),
                        Util.join(activities.get(entry.getKey()).name_words()),
                        Util.join(activities.get(entry.getKey()).descr_introduction_words())));
            }
        }

        reportMarshaller.marshal(report, new File("report.xml"));
        Transformer transformer = TransformerFactory.newInstance().newTransformer(new StreamSource(getClass().getResourceAsStream("/report.xsl")));
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        reportMarshaller.marshal(report, doc);
        transformer.transform(new DOMSource(doc), new StreamResult(new FileOutputStream("report.html")));
    }

    private String[] createComparatorValuesLabels(Set<RevisionComparator> comparators) {
        String[] strings = new String[1 + comparators.size()];
        int x = 0;
        strings[x++] = "=";
        for (RevisionComparator comparator : comparators) {
            strings[x++] = comparator.toString();
        }
        return strings;
    }

    private String[] createComparatorValues(Double[] values) {
        String[] strings = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            strings[i] = NUMBER_FORMAT.format(values[i]);
        }
        return strings;
    }


    private String getKey(int i, int j) {
        return Math.min(i, j) + ";" + Math.max(i, j);
    }

    public static void main(String[] args) {
        try {
            new RelatedActivitiesFinder(new Configuration(5, true, 1.0, 0.5, 1.0, 2.0, 0.5, 0.1, 0.1, 0.1));
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ParserConfigurationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (TransformerException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    @XmlRootElement
    public static class Configuration {
        @XmlElement
        int maxRelated = 5;
        @XmlElement
        public
        boolean simplifyVocabulary = true;
        @XmlElement
        double comparatorFactorAllText = 1.0;
        @XmlElement
        double comparatorFactorName = 0.5;
        @XmlElement
        double comparatorFactorMaterials = 1.0;
        @XmlElement
        double comparatorFactorIntroduction = 2.0;
        @XmlElement
        double comparatorFactorCategories = 0.5;
        @XmlElement
        double comparatorFactorAge = 0.1;
        @XmlElement
        double comparatorFactorParticipantCount = 0.1;
        @XmlElement
        double comparatorFactorTime = 0.1;
        @XmlElement(name = "v")
        @XmlElementWrapper(name = "commonWords")
        public Set<String> commonWords;
        @XmlElement
        @XmlJavaTypeAdapter(TranslationsAdapter.class)
        public Map<String, String> translations;

        public Configuration() {
        }

        public Configuration(int maxRelated,
                             boolean simplifyVocabulary,
                             double comparatorFactorAllText,
                             double comparatorFactorName,
                             double comparatorFactorMaterials,
                             double comparatorFactorIntroduction,
                             double comparatorFactorCategories,
                             double comparatorFactorAge,
                             double comparatorFactorParticipantCount,
                             double comparatorFactorTime) {
            this.maxRelated = maxRelated;
            this.simplifyVocabulary = simplifyVocabulary;
            this.comparatorFactorAllText = comparatorFactorAllText;
            this.comparatorFactorName = comparatorFactorName;
            this.comparatorFactorMaterials = comparatorFactorMaterials;
            this.comparatorFactorIntroduction = comparatorFactorIntroduction;
            this.comparatorFactorCategories = comparatorFactorCategories;
            this.comparatorFactorAge = comparatorFactorAge;
            this.comparatorFactorParticipantCount = comparatorFactorParticipantCount;
            this.comparatorFactorTime = comparatorFactorTime;
        }

    }

    @XmlRootElement
    public static class Report {

        @XmlRootElement
        private static class Activity {
            @XmlElement
            String name;
            @XmlElement
            String description;
            @XmlElement
            List<Relation> relations = new ArrayList<>();

            private Activity() {
            }

            public Activity(String name, String description) {
                this.name = name;
                this.description = description;
            }

            public void add(Relation relation) {
                relations.add(relation);
            }

            @XmlRootElement
            private static class Relation {
                @XmlElement
                String name;
                @XmlElement
                String description;
                @XmlElement(name = "v")
                @XmlElementWrapper(name = "comparatorValues")
                String[] comparatorValues;

                private Relation() {
                }

                public Relation(String[] comparatorValues, String name, String description) {
                    this.comparatorValues = comparatorValues;
                    this.name = name;
                    this.description = description;
                }
            }
        }

        @XmlElement
        public Configuration configuration = new Configuration();

        @XmlElement(name = "v")
        @XmlElementWrapper(name = "comparatorValuesLabels")
        String[] comparatorValuesLabels;

        @XmlElement
        List<Activity> activities = new ArrayList<>();

    }

}
