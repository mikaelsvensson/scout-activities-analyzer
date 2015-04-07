package scout.analyzer.model;

import javax.xml.bind.annotation.XmlElement;

public class Entity {
    @XmlElement
    public int id;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Entity)) return false;

        Entity entity = (Entity) o;

        if (id != entity.id) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
