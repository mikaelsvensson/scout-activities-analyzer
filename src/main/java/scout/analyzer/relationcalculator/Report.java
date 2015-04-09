package scout.analyzer.relationcalculator;

import scout.analyzer.model.Entity;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
public class Report {

    @XmlRootElement
    static class Activity extends Entity {
        @XmlElement
        String name;
        @XmlElement
        String description;
        @XmlElement
        List<Activity.Relation> relations = new ArrayList<>();

        private Activity() {
        }

        public Activity(String name, String description, int id) {
            this.name = name;
            this.description = description;
            this.id = id;
        }

        public void add(Activity.Relation relation) {
            relations.add(relation);
        }

        @XmlRootElement
        static class Relation extends Entity {
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
