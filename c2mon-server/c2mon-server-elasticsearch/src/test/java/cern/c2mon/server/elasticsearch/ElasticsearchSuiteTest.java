package cern.c2mon.server.elasticsearch;

import cern.c2mon.server.elasticsearch.alarm.AlarmDocumentConverterTests;
import cern.c2mon.server.elasticsearch.alarm.AlarmDocumentIndexerTests;
import cern.c2mon.server.elasticsearch.config.ElasticsearchProperties;
import cern.c2mon.server.elasticsearch.supervision.SupervisionEventDocumentIndexerTests;
import cern.c2mon.server.elasticsearch.supervision.SupervisionEventDocumentTests;
import cern.c2mon.server.elasticsearch.tag.TagDocumentConverterTests;
import cern.c2mon.server.elasticsearch.tag.TagDocumentIndexerTests;
import cern.c2mon.server.elasticsearch.tag.config.TagConfigDocumentConverterTests;
import cern.c2mon.server.elasticsearch.tag.config.TagConfigDocumentIndexerTests;
import cern.c2mon.server.elasticsearch.util.EmbeddedElasticsearchManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    ElasticsearchModuleIntegrationTests.class,
    IndexManagerTests.class,
    IndexNameManagerTests.class,
    AlarmDocumentConverterTests.class,
    AlarmDocumentIndexerTests.class,
    SupervisionEventDocumentIndexerTests.class,
    SupervisionEventDocumentTests.class,
    TagDocumentConverterTests.class,
    TagDocumentIndexerTests.class,
    TagConfigDocumentConverterTests.class,
    TagConfigDocumentIndexerTests.class
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
