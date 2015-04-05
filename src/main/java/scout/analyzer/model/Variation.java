package scout.analyzer.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
class Variation {
    @XmlAttribute
    public String suffix;
    @XmlElement
    public List<String> prefix;
}
