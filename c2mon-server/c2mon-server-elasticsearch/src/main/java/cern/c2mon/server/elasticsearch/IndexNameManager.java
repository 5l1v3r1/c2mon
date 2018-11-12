package cern.c2mon.server.elasticsearch;

import cern.c2mon.server.elasticsearch.alarm.AlarmDocument;
import cern.c2mon.server.elasticsearch.config.ElasticsearchProperties;
import cern.c2mon.server.elasticsearch.supervision.SupervisionEventDocument;
import cern.c2mon.server.elasticsearch.tag.TagDocument;
import cern.c2mon.server.elasticsearch.tag.config.TagConfigDocument;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Manages index name definitions.
 */
@Component
public class IndexNameManager {

    @Getter
    private ElasticsearchProperties properties;

    /**
     * @param properties of Elasticsearch server the application is communicating with.
     */
    @Autowired
    public IndexNameManager(ElasticsearchProperties properties) {
        this.properties = properties;
    }

    /**
     * Generate an index for the given {@link TagDocument} based on its
     * timestamp.
     *
     * @param tag the tag to generate an index for
     *
     * @return the generated index name
     */
    public String indexFor(TagDocument tag) {
        String prefix = properties.getIndexPrefix() + "-tag_";
        return getIndexName(prefix, (Long) tag.get("timestamp"));
    }

    /**
     * Generate an index for the given {@link TagConfigDocument}.
     *
     * @param tag the tag to generate an index for
     *
     * @return the generated index name
     */
    public String indexFor(TagConfigDocument tag) {
        return properties.getTagConfigIndex();
    }

    /**
     * Generate an index for the given {@link AlarmDocument} based on its
     * timestamp.
     *
     * @param alarm the alarm to generate an index for
     *
     * @return the generated index name
     */
    public String indexFor(AlarmDocument alarm) {
        String prefix = properties.getIndexPrefix() + "-alarm_";
        return getIndexName(prefix, (Long) alarm.get("timestamp"));
    }

    /**
     * Generate an index for the given {@link SupervisionEventDocument}
     * based on its timestamp.
     *
     * @param supervisionEvent the supervision event to generate an index for
     *
     * @return the generated index name
     */
    public String indexFor(SupervisionEventDocument supervisionEvent) {
        String prefix = properties.getIndexPrefix() + "-supervision_";
        return getIndexName(prefix, (Long) supervisionEvent.get("timestamp"));
    }

    /**
     * Generate an index for the given prefix and timestamp, based on the current
     * time series indexing strategy.
     *
     * @param prefix    the index prefix
     * @param timestamp the timestamp which will be used to generate the index
     *
     * @return the generated index name
     */
    private String getIndexName(String prefix, long timestamp) {
        String indexType = properties.getIndexType();
        String dateFormat;

        switch (indexType.toLowerCase()) {
            case "d":
                dateFormat = "yyyy-MM-dd";
                break;
            case "w":
                dateFormat = "yyyy-'W'ww";
                break;
            case "m":
            default:
                dateFormat = "yyyy-MM";
                break;
        }

        return prefix + new SimpleDateFormat(dateFormat).format(new Date(timestamp));
    }
}
