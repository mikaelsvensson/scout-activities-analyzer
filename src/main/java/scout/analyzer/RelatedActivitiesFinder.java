package scout.analyzer;

import org.w3c.dom.Document;
import scout.analyzer.comparator.*;
import scout.analyzer.model.Activities;
import scout.analyzer.model.Activity;
import scout.analyzer.model.Entity;

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
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.MessageFormat;
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

    private static final String RELATED_ACTIVITY_IDS_JSON_PREFIX = "{\"related_activity_ids\":[";

    private RelatedActivitiesFinder(Configuration configuration) throws JAXBException, IOException, TransformerException, ParserConfigurationException {

        System.out.println("Loading data from " + configuration.httpAllActivitiesURL);
        Activities activities1 = Activities.get(configuration.httpAllActivitiesURL);
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


        System.out.println("Calculating related activities");

        Map<String, Double[]> comparisons = calculateRelatedActivities(activities, comparators);

        System.out.println("Generating reports");

        Report report = buildReport(activities, comparators, comparisons, configuration.maxRelated);

        transformReport(report, "/report.xsl", configuration.outputFile);
        transformReport(report, "/report-simpletext.xsl", configuration.simpleReportOutputFile);

        if (configuration.httpSetRelatedActivitiesURL != null && configuration.httpSetRelatedActivitiesURL.length() > 0) {
            System.out.println("Calling REST API to set each activity's related activities.");

            setRemoteRelatedActivities(
                    configuration.httpSetRelatedActivitiesURL,
                    configuration.httpAuthorizationHeader,
                    report.activities);
        }
    }

    private Map<String, Double[]> calculateRelatedActivities(List<Activity> activities, LinkedHashMap<RevisionComparator, Double> comparators) {
        Map<String, Double[]> comparisons = new HashMap<>();
        for (int i = 0; i < activities.size(); i++) {
            for (int j = i + 1; j < activities.size(); j++) {
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
        return comparisons;
    }

    private Report buildReport(List<Activity> activities, LinkedHashMap<RevisionComparator, Double> comparators, Map<String, Double[]> comparisons, int maxRelated) {
        Report report = new Report();
        report.comparatorValuesLabels = createComparatorValuesLabels(comparators.keySet());
        for (int i = 0; i < activities.size(); i++) {
            Activity revision = activities.get(i);

            Report.Activity relation = new Report.Activity(
                    Util.join(revision.name_words()),
                    Util.join(revision.all_words(), 150),
                    revision.activity_id);
            report.activities.add(relation);

            TreeMap<Integer, Double[]> similar = new TreeMap<>();
            for (int j = 0; j < activities.size(); j++) {
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
            for (Map.Entry<Integer, Double[]> entry : similarEntries.subList(0, Math.min(similarEntries.size(), maxRelated))) {
                String[] parts = new String[entry.getValue().length];
                for (int j = 0; j < entry.getValue().length; j++) {
                    Double value = entry.getValue()[j];
                    parts[j] = NUMBER_FORMAT.format(value);
                }
                Activity activity = activities.get(entry.getKey());
                relation.add(new Report.Activity.Relation(
                        createComparatorValues(entry.getValue()),
                        Util.join(activity.name_words()),
                        Util.join(activity.all_words(), 150),
                        activity.activity_id));
            }
        }
        return report;
    }

    private void setRemoteRelatedActivities(String httpSetRelatedActivitiesURL, String httpAuthorizationHeader, List<Report.Activity> activities) throws IOException {
        for (Report.Activity activity : activities) {
            String jsonBody = getJsonBody(activity);
            URL url = new URL(MessageFormat.format(httpSetRelatedActivitiesURL, activity.id));
            System.out.println("Connect to " + url + " and send this: " + jsonBody);

            int responseCode = doHttpPost(url, httpAuthorizationHeader, jsonBody);
            System.out.println("API response: " + responseCode);
        }
    }

    private String getJsonBody(Report.Activity activity) {
        StringBuilder sb = new StringBuilder(RELATED_ACTIVITY_IDS_JSON_PREFIX);
        for (Report.Activity.Relation relation : activity.relations) {
            if (sb.length() > RELATED_ACTIVITY_IDS_JSON_PREFIX.length()) {
                sb.append(',');
            }
            sb.append(relation.id);
        }
        sb.append("]}");
        return sb.toString();
    }

    private int doHttpPost(URL url, String authorizationHeader, String jsonBody) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.addRequestProperty("Authorization", authorizationHeader);
        connection.addRequestProperty("Content-Encoding", "UTF-8");
        connection.addRequestProperty("Content-Type", "application/json");
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.connect();
        connection.getOutputStream().write(jsonBody.getBytes("UTF-8"));
        return connection.getResponseCode();
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
                new Simplifier.SimplifyRule(1, Pattern.compile("([a-zåäöA-ZÅÄÖ]{4,})(ing|ingen|ings|ingens|ingar|ingarna)")),
                new Simplifier.SimplifyRule(1, Pattern.compile("([a-zåäöA-ZÅÄÖ]{4,})[lt](iga|igt)")),
                new Simplifier.SimplifyRule(1, Pattern.compile("([a-zåäöA-ZÅÄÖ]{4,})(nare|nskt|nens|nens)")),
                new Simplifier.SimplifyRule(1, Pattern.compile("([a-zåäöA-ZÅÄÖ]{4,})(nar|are|ade|skt|ens|nen)")),
                new Simplifier.SimplifyRule(1, Pattern.compile("([a-zåäöA-ZÅÄÖ]{4,})(na|re)")),
                new Simplifier.SimplifyRule(1, Pattern.compile("([a-zåäöA-ZÅÄÖ]{4,})[aeis][nrtsk]")),
                new Simplifier.SimplifyRule(1, Pattern.compile("([a-zåäöA-ZÅÄÖ]{4,})[aens]")),
                new Simplifier.SimplifyRule(0, Pattern.compile("([a-zåäöA-ZÅÄÖ]{4,})"))
        };
        @XmlElement
        public int minimumWordGroupSize = 2;
        @XmlElement
        public String httpAllActivitiesURL;
        @XmlElement
        public String httpSetRelatedActivitiesURL;
        @XmlElement
        public String httpAuthorizationHeader;
    }

    @XmlRootElement
    public static class Report {

        @XmlRootElement
        private static class Activity extends Entity {
            @XmlElement
            String name;
            @XmlElement
            String description;
            @XmlElement
            List<Relation> relations = new ArrayList<>();

            private Activity() {
            }

            public Activity(String name, String description, int id) {
                this.name = name;
                this.description = description;
                this.id = id;
            }

            public void add(Relation relation) {
                relations.add(relation);
            }

            @XmlRootElement
            private static class Relation extends Entity {
                @XmlElement
                String name;
                @XmlElement
                String description;
                @XmlElement(name = "v")
                @XmlElementWrapper(name = "comparatorValues")
                String[] comparatorValues;

                private Relation() {
                }

                public Relation(String[] comparatorValues, String name, String description, int id) {
                    this.comparatorValues = comparatorValues;
                    this.name = name;
                    this.description = description;
                    this.id = id;
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
