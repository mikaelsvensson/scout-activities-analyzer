package scout.analyzer.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
class CategoryMapping {
    @XmlElement
    public int category_id;
    @XmlElement
    public int activity_version_id;
}
