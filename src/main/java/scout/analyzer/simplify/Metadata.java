package scout.analyzer.simplify;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Map;
import java.util.Set;

@XmlRootElement
public class Metadata {
    @XmlElement(name = "v")
    @XmlElementWrapper(name = "commonWords")
    public Set<String> commonWords;
    @XmlElement
    @XmlJavaTypeAdapter(TranslationsAdapter.class)
    public Map<String, String> translations;
}
