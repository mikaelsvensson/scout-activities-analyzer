package scout.analyzer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

public class Util {
    public static <C> C loadXMLResource(Class<C> cls, String resourceName) throws JAXBException {
        JAXBContext ctx1 = JAXBContext.newInstance(cls);
        Unmarshaller unmarshaller1 = ctx1.createUnmarshaller();
        return (C) unmarshaller1.unmarshal(Util.class.getResourceAsStream(resourceName));
    }

    static String join(Object[] strings) {
        if (strings != null && strings.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (Object s : strings) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(s);
            }
            return sb.toString();
        } else {
            return "";
        }
    }
}
