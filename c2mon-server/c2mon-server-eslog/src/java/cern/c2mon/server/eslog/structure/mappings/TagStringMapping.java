package cern.c2mon.server.eslog.structure.mappings;

/**
 * @author Alban Marguet.
 */
public class TagStringMapping extends TagESMapping implements Mapping {

    public TagStringMapping(String type) {
        super();
        setProperties(type);
    }

    @Override
    public void setProperties(String tagValueType) {
        if (tagValueType.compareTo(stringType) != 0) {
            throw new IllegalArgumentException("Type for TagString must be string.");
        } else {
            this.properties = new Properties(tagValueType);
        }
    }
}