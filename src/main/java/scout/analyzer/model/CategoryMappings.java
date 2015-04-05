package scout.analyzer.model;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "data")
class CategoryMappings {

    @XmlElement(name = "row")
    public final List<CategoryMapping> mappings = new ArrayList<>();

}
