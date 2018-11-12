package cern.c2mon.server.elasticsearch;

import cern.c2mon.server.elasticsearch.config.BaseElasticsearchIntegrationTest;
import cern.c2mon.server.elasticsearch.config.ElasticsearchProperties;
import cern.c2mon.server.elasticsearch.tag.TagDocument;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.TestPropertySource;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Justin Lewis Salmon
 */
public class IndexNameManagerTest extends BaseElasticsearchIntegrationTest {

  @Autowired
  private IndexNameManager indexNameManager;

  @Test
  public void monthlyIndex() {
    indexNameManager.getProperties().setIndexType("M");

    TagDocument document = new TagDocument();
    document.put("timestamp", 1448928000000L);

    String index = indexNameManager.indexFor(document);
    assertEquals("c2mon-tag_2015-12", index);
  }

  @Test
  public void weeklyIndex() {
    indexNameManager.getProperties().setIndexType("W");

    TagDocument document = new TagDocument();
    document.put("timestamp", 1448928000000L);

    String index = indexNameManager.indexFor(document);
    assertEquals("c2mon-tag_2015-W49", index);
  }

  @Test
  public void dailyIndex() {
    indexNameManager.getProperties().setIndexType("D");

    TagDocument document = new TagDocument();
    document.put("timestamp", 1448928000000L);

    String index = indexNameManager.indexFor(document);
    assertEquals("c2mon-tag_2015-12-01", index);
  }
}
