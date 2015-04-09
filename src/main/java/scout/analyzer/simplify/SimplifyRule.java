package scout.analyzer.simplify;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.regex.Pattern;

@XmlRootElement
public class SimplifyRule {
    @XmlAttribute(name = "pattern")
    String patternExpr;
    Pattern pattern;
    @XmlAttribute
    int matchGroupIndex;

    public SimplifyRule() {
    }

    public SimplifyRule(int matchGroupIndex, Pattern pattern) {
        this.matchGroupIndex = matchGroupIndex;
        this.pattern = pattern;
    }

    public Pattern getPattern() {
        if (pattern == null) {
            pattern = Pattern.compile(patternExpr);
        }
        return pattern;
    }
}
