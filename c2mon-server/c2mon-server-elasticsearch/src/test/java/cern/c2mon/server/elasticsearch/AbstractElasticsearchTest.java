package cern.c2mon.server.elasticsearch;

import cern.c2mon.server.cache.config.CacheModule;
import cern.c2mon.server.cache.dbaccess.config.CacheDbAccessModule;
import cern.c2mon.server.cache.loading.config.CacheLoadingModule;
import cern.c2mon.server.common.config.CommonModule;
import cern.c2mon.server.elasticsearch.config.ElasticsearchModule;
import cern.c2mon.server.elasticsearch.junit.CachePopulationRule;
import cern.c2mon.server.elasticsearch.util.EmbeddedElasticsearchManager;
import cern.c2mon.server.supervision.config.SupervisionModule;
import org.junit.After;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration(classes = {
        CommonModule.class,
        CacheModule.class,
        CacheDbAccessModule.class,
        CacheLoadingModule.class,
        SupervisionModule.class,
        ElasticsearchModule.class,
        CachePopulationRule.class
})
@RunWith(SpringJUnit4ClassRunner.class)
public class AbstractElasticsearchTest {

    protected String indexName;

    @After
    public void tearDown() {
        EmbeddedElasticsearchManager.getEmbeddedNode().deleteIndex(indexName);
        EmbeddedElasticsearchManager.getEmbeddedNode().refreshIndices();
    }
}
