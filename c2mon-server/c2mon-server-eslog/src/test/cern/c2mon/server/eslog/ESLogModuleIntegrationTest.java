package cern.c2mon.server.eslog;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

/**
 * Integration test with the core modules.
 * 
 * @author Alban Marguet.
 */
@Slf4j
@ContextConfiguration({"classpath:cern/c2mon/server/eslog/config/server-eslog-integration.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore("Works alone but not in the continuity of all the tests (issue with the used ports for the cluster setup in memory)")
public class ESLogModuleIntegrationTest {
  private static Node clusterNode;
  private static Client clusterClient;

  @BeforeClass
  public static void initCluster() {
    log.info("@BeforeClass");
    String clusterName = "testCluster";
    String home = "../../resources/elasticsearch";
    String host = "localhost";

    clusterNode = nodeBuilder()
        .settings(Settings.settingsBuilder()
            .put("path.home", home)
            .put("cluster.name", clusterName)
            .put("node.local", true)
            .put("node.name", "ClusterNode")
            .put("node.data", true)
            .put("node.master", true)
            .put("http.enabled", false)
            .put("transport.host", host)
            .put("transport.tcp.port", 8300)
            .build())
        .node();

    clusterNode.start();
    clusterClient = clusterNode.client();
    log.info("Node created with home " + home + " in cluster " + clusterName + ".");
    clusterClient.admin().cluster().prepareHealth().setWaitForGreenStatus().execute().actionGet();
  }

  @AfterClass
  public static void tidyUpCluster() {
    clusterClient.close();
    clusterNode.close();
  }

  @Test
  public void testModuleStartup() {
    String[] indices = clusterClient.admin().indices().prepareGetIndex().get().indices();
    log.info("indices in the cluster:");
    for (String index : indices) {
      log.info(index);
    }
  }
}