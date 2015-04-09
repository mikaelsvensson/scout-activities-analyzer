package scout.analyzer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class Util {
    public static <C> C loadXMLResource(Class<C> cls, String resourceName) throws JAXBException, MalformedURLException {
        JAXBContext ctx1 = JAXBContext.newInstance(cls);
        Unmarshaller unmarshaller1 = ctx1.createUnmarshaller();
        if (new File(resourceName).isFile()) {
            return (C) unmarshaller1.unmarshal(new File(resourceName));
        } else {
            return (C) unmarshaller1.unmarshal(new URL(resourceName));
        }
    }

    public static String join(Object[] strings) {
        return join(strings, Integer.MAX_VALUE);
    }

    public static String join(Object[] strings, int desiredLength) {
        if (strings != null && strings.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (Object s : strings) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(s);
                if (desiredLength > 0 && sb.length() > desiredLength) {
                    sb.append("...");
                    break;
                }
            }
            return sb.toString();
        } else {
            return "";
        }
    }
}
