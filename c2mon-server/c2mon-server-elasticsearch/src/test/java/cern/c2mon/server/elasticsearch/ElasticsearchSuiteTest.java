package cern.c2mon.server.elasticsearch;

import cern.c2mon.server.elasticsearch.alarm.AlarmDocumentConverterTest;
import cern.c2mon.server.elasticsearch.alarm.AlarmDocumentIndexerTest;
import cern.c2mon.server.elasticsearch.config.ElasticsearchProperties;
import cern.c2mon.server.elasticsearch.supervision.SupervisionEventDocumentIndexerTest;
import cern.c2mon.server.elasticsearch.supervision.SupervisionEventDocumentTest;
import cern.c2mon.server.elasticsearch.tag.TagDocumentConverterTest;
import cern.c2mon.server.elasticsearch.tag.TagDocumentIndexerTest;
import cern.c2mon.server.elasticsearch.tag.config.TagConfigDocumentConverterTest;
import cern.c2mon.server.elasticsearch.tag.config.TagConfigDocumentIndexerTest;
import cern.c2mon.server.elasticsearch.util.EmbeddedElasticsearchManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    ElasticsearchModuleIntegrationTest.class,
    IndexManagerTest.class,
    IndexNameManagerTest.class,
    AlarmDocumentConverterTest.class,
    AlarmDocumentIndexerTest.class,
    SupervisionEventDocumentIndexerTest.class,
    SupervisionEventDocumentTest.class,
    TagDocumentConverterTest.class,
    TagDocumentIndexerTest.class,
    TagConfigDocumentConverterTest.class,
    TagConfigDocumentIndexerTest.class
})
public class ElasticsearchSuiteTest {

  private static final ElasticsearchProperties properties = new ElasticsearchProperties();

  public static ElasticsearchProperties getProperties() {
    return properties;
  }

  @BeforeClass
  public static void setUpClass() {
    EmbeddedElasticsearchManager.start(properties);
  }

  @AfterClass
  public static void cleanup() {
    EmbeddedElasticsearchManager.stop();
  }
}
