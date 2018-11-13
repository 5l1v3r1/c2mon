package cern.c2mon.server.elasticsearch;

import cern.c2mon.server.elasticsearch.client.ElasticsearchClientRest;
import cern.c2mon.server.elasticsearch.client.ElasticsearchClientTransport;
import cern.c2mon.server.elasticsearch.util.EmbeddedElasticsearchManager;
import cern.c2mon.server.elasticsearch.util.IndexUtils;
import org.elasticsearch.node.NodeValidationException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for implementations of {@link IndexManager}.
 */
@RunWith(Parameterized.class)
public class IndexManagerTests {
    private static final String NAME = "Test Name";
    private static final String UPDATED_NAME = NAME + " Updated";
    private static final String TEST_JSON = "{\"id\":\"1000\",\"name\":\"" + NAME + "\", \"description\":\"Test description\"}";
    private static final String TEST_JSON_2 = "{\"id\":\"1000\",\"name\":\"" + UPDATED_NAME + "\", \"description\":\"Test description\"}";

    /**
     * Parameters setup. Must include an instance of each of the {@link IndexManager} implementations.
     *
     * @return list of instances of each of the {@link IndexManager} implementations.
     * @throws NodeValidationException
     */
    @Parameters
    public static Collection<IndexManager> getIndexManagerClass() throws NodeValidationException {
        return Arrays.asList(
                new IndexManagerRest(new ElasticsearchClientRest(ElasticsearchSuiteTest.getProperties())),
                new IndexManagerTransport(new ElasticsearchClientTransport(ElasticsearchSuiteTest.getProperties())));
    }

    private String indexName;
    private IndexManager indexManager;

    /**
     * Constructor for injecting parameters.
     *
     * @param indexManager instance for current test set execution.
     */
    public IndexManagerTests(IndexManager indexManager) {
        this.indexManager = indexManager;
    }

    @Before
    public void setUp() {
        indexName = "test_index";

        if (indexManager != null) {
            indexManager.purgeIndexCache();
        }
    }

    @After
    public void tearDown() {
        EmbeddedElasticsearchManager.getEmbeddedNode().deleteIndex(indexName);
        EmbeddedElasticsearchManager.getEmbeddedNode().refreshIndices();
    }

    @Test
    public void createTest() throws NodeValidationException, IOException {
        String mapping = loadMapping("mappings/test.json");

        indexManager.create(indexName, mapping);

        assertTrue("Index should have been created.",
                IndexUtils.doesIndexExist(indexName, ElasticsearchSuiteTest.getProperties()));
    }

    @Test
    public void indexTestWithoutId() throws NodeValidationException, IOException {
        String mapping = loadMapping("mappings/test.json");

        indexManager.create(indexName, mapping);

        EmbeddedElasticsearchManager.getEmbeddedNode().refreshIndices();

        indexManager.index(indexName, TEST_JSON,"1");

        EmbeddedElasticsearchManager.getEmbeddedNode().refreshIndices();

        assertEquals("Index should have one document inserted.", 1,
                EmbeddedElasticsearchManager.getEmbeddedNode().fetchAllDocuments(indexName).size());
    }

    @Test
    public void indexTestWithId() throws NodeValidationException, IOException {
        String mapping = loadMapping("mappings/test.json");

        indexManager.create(indexName, mapping);

        EmbeddedElasticsearchManager.getEmbeddedNode().refreshIndices();

        indexManager.index(indexName, TEST_JSON,"1", "1");

        EmbeddedElasticsearchManager.getEmbeddedNode().refreshIndices();

        assertEquals("Index should have one document inserted.", 1,
                EmbeddedElasticsearchManager.getEmbeddedNode().fetchAllDocuments(indexName).size());
    }

    @Test
    public void existsTestWithoutRouting() throws NodeValidationException, IOException {
        String mapping = loadMapping("mappings/test.json");

        indexManager.create(indexName, mapping);

        EmbeddedElasticsearchManager.getEmbeddedNode().refreshIndices();

        assertTrue("'exists()' method should report index as exiting.", indexManager.exists(indexName));
    }

    @Test
    public void existsTestWithCachePurging() throws NodeValidationException, IOException {
        String mapping = loadMapping("mappings/test.json");

        indexManager.create(indexName, mapping);

        EmbeddedElasticsearchManager.getEmbeddedNode().refreshIndices();

        indexManager.purgeIndexCache();

        assertTrue("'exists()' method should check index existence on the server once cache is purged.",
                indexManager.exists(indexName));
    }

    @Test
    public void existsTestWithRouting() throws NodeValidationException, IOException {
        String mapping = loadMapping("mappings/test.json");

        indexManager.create(indexName, mapping);

        EmbeddedElasticsearchManager.getEmbeddedNode().refreshIndices();

        assertTrue("'exists()' method should report index as exiting.",
                indexManager.exists(indexName, "1"));
    }

    @Test
    public void updateExistingIndexNonExistingDocument() throws IOException {
        String mapping = loadMapping("mappings/test.json");

        indexManager.create(indexName, mapping);

        EmbeddedElasticsearchManager.getEmbeddedNode().refreshIndices();

        indexManager.update(indexName, TEST_JSON_2, "1");

        EmbeddedElasticsearchManager.getEmbeddedNode().refreshIndices();

        List<String> indexData = EmbeddedElasticsearchManager.getEmbeddedNode().fetchAllDocuments(indexName);
        assertEquals("Upsert should create document which does not exist.", 1, indexData.size());

        JSONObject jsonObject = new JSONObject(indexData.get(0));

        assertEquals("Updated document should have updated values.", UPDATED_NAME, jsonObject.getString("name"));
    }

    @Test
    public void updateExistingIndexExistingDocument() throws IOException {
        String mapping = loadMapping("mappings/test.json");

        indexManager.create(indexName, mapping);

        EmbeddedElasticsearchManager.getEmbeddedNode().refreshIndices();

        indexManager.index(indexName, TEST_JSON, "1", "1");

        EmbeddedElasticsearchManager.getEmbeddedNode().refreshIndices();

        indexManager.update(indexName, TEST_JSON_2, "1");

        EmbeddedElasticsearchManager.getEmbeddedNode().refreshIndices();

        List<String> indexData = EmbeddedElasticsearchManager.getEmbeddedNode().fetchAllDocuments(indexName);

        JSONObject jsonObject = new JSONObject(indexData.get(0));

        assertEquals("Updated document should have updated values.", UPDATED_NAME, jsonObject.getString("name"));
    }

    @Test
    public void updateNonExistingIndex() throws UnknownHostException {
        indexManager.update(indexName, TEST_JSON_2, "1");

        EmbeddedElasticsearchManager.getEmbeddedNode().refreshIndices();

        assertEquals("Index should be created if updating non-existing index.", 1,
                EmbeddedElasticsearchManager.getEmbeddedNode().fetchAllDocuments(indexName).size());
    }

    private String loadMapping(String source) throws IOException {
        return new BufferedReader(new InputStreamReader(new ClassPathResource(source).getInputStream()))
                .lines()
                .collect(Collectors.joining(""));
    }
}