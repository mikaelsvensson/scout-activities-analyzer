package scout.analyzer.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "variations")
class Variations {
    @XmlElement(name = "variation")
    @XmlElementWrapper(name = "verbs")
    private List<Variation> verbs;
    @XmlElement(name = "variation")
    @XmlElementWrapper(name = "nouns")
    private List<Variation> nouns;
    @XmlElement(name = "variation")
    @XmlElementWrapper(name = "other")
    private List<Variation> other;

    public List<Variation> all() {
        ArrayList<Variation> list = new ArrayList<>();
        list.addAll(nouns);
        list.addAll(other);
        list.addAll(verbs);
        return list;
    }
}
