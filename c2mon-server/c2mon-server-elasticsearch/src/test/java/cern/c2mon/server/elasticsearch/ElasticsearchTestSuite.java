package cern.c2mon.server.elasticsearch;

import cern.c2mon.server.elasticsearch.alarm.AlarmDocumentConverterTests;
import cern.c2mon.server.elasticsearch.alarm.AlarmDocumentIndexerTests;
import cern.c2mon.server.elasticsearch.supervision.SupervisionEventDocumentIndexerTests;
import cern.c2mon.server.elasticsearch.supervision.SupervisionEventDocumentTests;
import cern.c2mon.server.elasticsearch.tag.BaseTagDocumentConverterTest;
import cern.c2mon.server.elasticsearch.tag.TagDocumentConverterTests;
import cern.c2mon.server.elasticsearch.tag.TagDocumentIndexerTests;
import cern.c2mon.server.elasticsearch.tag.config.TagConfigDocumentConverterTests;
import cern.c2mon.server.elasticsearch.tag.config.TagConfigDocumentIndexerTests;
import cern.c2mon.server.elasticsearch.util.EmbeddedElasticsearchManager;
import org.junit.AfterClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    ElasticsearchModuleIntegrationTest.class,
    IndexManagerTests.class,
    IndexNameManagerTests.class,
    AlarmDocumentConverterTests.class,
    AlarmDocumentIndexerTests.class,
    SupervisionEventDocumentIndexerTests.class,
    SupervisionEventDocumentTests.class,
    TagDocumentConverterTests.class,
    TagDocumentIndexerTests.class,
    TagConfigDocumentConverterTests.class,
    TagConfigDocumentIndexerTests.class,
    BaseTagDocumentConverterTest.class
})
public class ElasticsearchTestSuite {

  @AfterClass
  public static void cleanup() {
    EmbeddedElasticsearchManager.stop();
  }
}
