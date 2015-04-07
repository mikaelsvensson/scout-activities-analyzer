package scout.analyzer;

import org.w3c.dom.Document;
import scout.analyzer.comparator.*;
import scout.analyzer.model.Activities;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Pattern;

public class RelatedActivitiesFinder {

    private static final NumberFormat NUMBER_FORMAT;

    static {
        NUMBER_FORMAT = NumberFormat.getNumberInstance();
        NUMBER_FORMAT.setMaximumFractionDigits(2);
        NUMBER_FORMAT.setMinimumFractionDigits(2);
    }

    private RelatedActivitiesFinder(Configuration configuration) throws JAXBException, IOException, TransformerException, ParserConfigurationException {

        Report report = new Report();

        System.out.println("Loading data from " + configuration.activitiesURL);
        Activities activities1 = Activities.get(configuration.activitiesURL);
        List<scout.analyzer.model.Activity> activities = activities1.activities;
        if (configuration.simplifyVocabulary) {
            System.out.println("Simplifying vocabulary");
            Simplifier.Metadata metadata = activities1.simplifyVocabulary(
                    configuration.simplifyRules,
                    configuration.minimumWordGroupSize);
            transformReport(metadata, "/simplifications.xsl", configuration.simplifierMetadataOutputFile);
        }

        LinkedHashMap<RevisionComparator, Double> comparators = new LinkedHashMap<>();
        comparators.put(new AllTextComparator(), configuration.comparatorFactorAllText);
        comparators.put(new NameTextComparator(), configuration.comparatorFactorName);
        comparators.put(new MaterialTextComparator(), configuration.comparatorFactorMaterials);
        comparators.put(new IntroductionTextComparator(), configuration.comparatorFactorIntroduction);
        comparators.put(new CategoryComparator(), configuration.comparatorFactorCategories);
        comparators.put(new AgeComparator(), configuration.comparatorFactorAge);
        comparators.put(new ParticipantCountComparator(), configuration.comparatorFactorParticipantCount);
        comparators.put(new TimeComparator(), configuration.comparatorFactorTime);

        report.comparatorValuesLabels = createComparatorValuesLabels(comparators.keySet());

        Map<String, Double[]> comparisons = new HashMap<>();

        int activityCount = activities.size();
        System.out.println("Calculating related activities");
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

        System.out.println("Generating reports");

        for (int i = 0; i < activityCount; i++) {
            scout.analyzer.model.Activity revision = activities.get(i);

            Report.Activity relation = new Report.Activity(
                    Util.join(revision.name_words()),
                    Util.join(revision.all_words(), 150));
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
                        Util.join(activities.get(entry.getKey()).all_words(), 150)));
            }
        }

        transformReport(report, "/report.xsl", configuration.outputFile);
        transformReport(report, "/report-simpletext.xsl", configuration.simpleReportOutputFile);
    }

    private void transformReport(Object o, String xslResourceName, String outputFileName) throws ParserConfigurationException, JAXBException, TransformerException, FileNotFoundException {
        Marshaller reportMarshaller = null;
        reportMarshaller = JAXBContext.newInstance(o.getClass()).createMarshaller();
        reportMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        if (xslResourceName != null) {
            Transformer transformer = TransformerFactory.newInstance().newTransformer(new StreamSource(getClass().getResourceAsStream(xslResourceName)));
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            reportMarshaller.marshal(o, doc);
            transformer.transform(new DOMSource(doc), new StreamResult(new FileOutputStream(outputFileName)));
        } else {
            reportMarshaller.marshal(o, new File(outputFileName));
        }
    }

    private static Configuration loadConfiguration(String outputFileName) throws ParserConfigurationException, JAXBException, TransformerException, FileNotFoundException {
        Unmarshaller reportMarshaller = JAXBContext.newInstance(Configuration.class).createUnmarshaller();
        return (Configuration) reportMarshaller.unmarshal(new File(outputFileName));
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
            if (args != null) {
                for (String arg : args) {
                    System.out.println("Calculating related activities using configuration file " + arg);
                    new RelatedActivitiesFinder(loadConfiguration(arg));
                }
            } else {
                System.out.println("Calculating related activities using default configuration");
                new RelatedActivitiesFinder(new Configuration());
            }
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
        public boolean simplifyVocabulary = true;
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
        @XmlElement
        public String outputFile = "report.html";
        @XmlElement
        public String simplifierMetadataOutputFile;
        @XmlElement
        public String simpleReportOutputFile;
        @XmlElement(name = "simplifyRule")
        public Simplifier.SimplifyRule[] simplifyRules = new Simplifier.SimplifyRule[]{
                new Simplifier.SimplifyRule(1, Pattern.compile("(\\p{IsAlphabetic}{4,})(ing|ingen|ings|ingens|ingar|ingarna)")),
                new Simplifier.SimplifyRule(1, Pattern.compile("(\\p{IsAlphabetic}{4,})[lt](iga|igt)")),
                new Simplifier.SimplifyRule(1, Pattern.compile("(\\p{IsAlphabetic}{4,})(nare|nskt|nens|nens)")),
                new Simplifier.SimplifyRule(1, Pattern.compile("(\\p{IsAlphabetic}{4,})(nar|are|ade|skt|ens|nen)")),
                new Simplifier.SimplifyRule(1, Pattern.compile("(\\p{IsAlphabetic}{4,})(na|re)")),
                new Simplifier.SimplifyRule(1, Pattern.compile("(\\p{IsAlphabetic}{4,})[aeis][nrtsk]")),
                new Simplifier.SimplifyRule(1, Pattern.compile("(\\p{IsAlphabetic}{4,})[aens]")),
                new Simplifier.SimplifyRule(0, Pattern.compile("(\\p{IsAlphabetic}{4,})"))
        };
        @XmlElement
        public int minimumWordGroupSize = 2;
        @XmlElement
        public String activitiesURL;
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

        @XmlElement(name = "v")
        @XmlElementWrapper(name = "comparatorValuesLabels")
        String[] comparatorValuesLabels;

        @XmlElement
        List<Activity> activities = new ArrayList<>();

    }

}
