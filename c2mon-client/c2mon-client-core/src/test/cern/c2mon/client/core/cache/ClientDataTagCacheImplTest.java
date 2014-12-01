package cern.c2mon.client.core.cache;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.jms.JMSException;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import cern.c2mon.client.common.listener.DataTagUpdateListener;
import cern.c2mon.client.common.tag.ClientDataTag;
import cern.c2mon.client.core.manager.CoreSupervisionManager;
import cern.c2mon.client.core.tag.ClientDataTagImpl;
import cern.c2mon.client.jms.JmsProxy;
import cern.c2mon.client.jms.RequestHandler;
import cern.c2mon.shared.client.tag.TagMode;
import cern.c2mon.shared.client.tag.TagUpdate;
import cern.c2mon.shared.client.tag.TagValueUpdate;
import cern.c2mon.shared.client.tag.TransferTagImpl;
import cern.c2mon.shared.common.datatag.DataTagQuality;
import cern.c2mon.shared.common.datatag.DataTagQualityImpl;
import cern.c2mon.shared.rule.RuleFormatException;

/**
 * Don't forget to add all required environment variables before you start your
 * test! The environment variable are specified in the <code>pom.xml</code>
 *
 * @author Matthias Braeger
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "classpath:cern/c2mon/client/core/cache/c2mon-cache-test.xml" })
public class ClientDataTagCacheImplTest {
  /**
   * Component to test
   */
  @Autowired
  private ClientDataTagCacheImpl cache;
  @Autowired
  private JmsProxy jmsProxyMock;
  @Autowired
  private RequestHandler requestHandlerMock;
  @Autowired
  private CoreSupervisionManager supervisionManagerMock;
  @Autowired
  private CacheSynchronizer cacheSynchronizer;
  @Autowired
  private CacheController cacheController;

  @Before
  public void init() {
   EasyMock.reset(jmsProxyMock, supervisionManagerMock, requestHandlerMock);
   cacheController.getLiveCache().clear();
   cacheController.getHistoryCache().clear();
  }

  @Test
  public void testEmptyCache() {
    assertEquals(0, cache.getAllSubscribedDataTags().size());
  }


  /**
   * Adds two tags into the cache and subscribes them to a <code>DataTagUpdateListener</code>.
   * @throws Exception
   */
  @Test
  public void testAddDataTagUpdateListener() throws Exception {
    // Test setup
    Set<Long> tagIds = new HashSet<Long>();
    tagIds.add(1L);
    tagIds.add(2L);
    Collection<TagUpdate> serverUpdates = new ArrayList<TagUpdate>(tagIds.size());
    for (Long tagId : tagIds) {
      serverUpdates.add(createValidTransferTag(tagId));
      prepareClientDataTagCreateMock(tagId);
    }
    EasyMock.expect(requestHandlerMock.requestTags(tagIds)).andReturn(serverUpdates);
    EasyMock.expect(requestHandlerMock.requestTagValues(tagIds)).andReturn(new ArrayList<TagValueUpdate>(serverUpdates));
    DataTagUpdateListener listener = EasyMock.createMock(DataTagUpdateListener.class);

    // run test
    EasyMock.replay(jmsProxyMock, requestHandlerMock);
    Collection<ClientDataTag> cachedTags = cache.getAllSubscribedDataTags();
    assertEquals(0, cachedTags.size());
    cache.addDataTagUpdateListener(tagIds, listener);

    cachedTags = cache.getAllSubscribedDataTags();
    assertEquals(2, cachedTags.size());

    Thread.sleep(500);

    // check test success
    EasyMock.verify(jmsProxyMock, requestHandlerMock);
  }


  @Test
  public void testUnsubscribeAllDataTags() throws Exception {
    // test setup
    Set<Long> tagIds = new HashSet<Long>();
    tagIds.add(1L);
    tagIds.add(2L);
    Collection<TagUpdate> serverUpdates = new ArrayList<TagUpdate>(tagIds.size());
    for (Long tagId : tagIds) {
      serverUpdates.add(createValidTransferTag(tagId));
      ClientDataTagImpl cdtMock = prepareClientDataTagCreateMock(tagId);
      jmsProxyMock.unregisterUpdateListener(cdtMock);
      supervisionManagerMock.removeSupervisionListener(cdtMock);
    }
    EasyMock.expect(requestHandlerMock.requestTags(tagIds)).andReturn(serverUpdates);
    EasyMock.expect(requestHandlerMock.requestTagValues(tagIds)).andReturn(new ArrayList<TagValueUpdate>(serverUpdates));
    DataTagUpdateListener listener1 = EasyMock.createMock(DataTagUpdateListener.class);
    DataTagUpdateListener listener2 = EasyMock.createMock(DataTagUpdateListener.class);

    // run test
    EasyMock.replay(jmsProxyMock, requestHandlerMock);
    cache.addDataTagUpdateListener(tagIds, listener1);
    Collection<ClientDataTag> cachedTags = cache.getAllSubscribedDataTags();
    assertEquals(2, cachedTags.size());
    // NOT Registered listener
    cache.unsubscribeAllDataTags(listener2);
    cachedTags = cache.getAllSubscribedDataTags();
    assertEquals(2, cachedTags.size());
    // Registered listener
    cache.unsubscribeAllDataTags(listener1);
    cachedTags = cache.getAllSubscribedDataTags();
    assertEquals(0, cachedTags.size());

    Thread.sleep(2000);

    // check test success
    EasyMock.verify(jmsProxyMock, requestHandlerMock);
  }


  @Test
  public void testContainsTag() throws Exception {
    // Test setup
    Set<Long> tagIds = new HashSet<Long>();
    tagIds.add(1L);
    tagIds.add(2L);
    Collection<TagUpdate> serverUpdates = new ArrayList<TagUpdate>(tagIds.size());
    for (Long tagId : tagIds) {
      serverUpdates.add(createValidTransferTag(tagId));
      prepareClientDataTagCreateMock(tagId);
    }
    EasyMock.expect(requestHandlerMock.requestTags(tagIds)).andReturn(serverUpdates);
    EasyMock.expect(requestHandlerMock.requestTagValues(tagIds)).andReturn(new ArrayList<TagValueUpdate>(serverUpdates));
    supervisionManagerMock.refreshSupervisionStatus();
    EasyMock.expectLastCall();
    DataTagUpdateListener listener = EasyMock.createMock(DataTagUpdateListener.class);

    // run test
    EasyMock.replay(jmsProxyMock, supervisionManagerMock, requestHandlerMock);
    cache.addDataTagUpdateListener(tagIds, listener);
    assertTrue(cache.containsTag(1L));
    assertTrue(cache.containsTag(2L));
    assertFalse(cache.containsTag(23423L));

    Thread.sleep(500);

    // check test success
    EasyMock.verify(jmsProxyMock, supervisionManagerMock, requestHandlerMock);
  }

  @Test
  public void testHistoryMode() throws Exception {
    // Test setup
    Set<Long> tagIds = new HashSet<Long>();
    tagIds.add(1L);
    tagIds.add(2L);
    Collection<TagUpdate> serverUpdates = new ArrayList<TagUpdate>(tagIds.size());
    for (Long tagId : tagIds) {
      serverUpdates.add(createValidTransferTag(tagId));
      prepareClientDataTagCreateMock(tagId);
    }
    EasyMock.expect(requestHandlerMock.requestTags(tagIds)).andReturn(serverUpdates);
    EasyMock.expect(requestHandlerMock.requestTagValues(tagIds)).andReturn(new ArrayList<TagValueUpdate>(serverUpdates));
    DataTagUpdateListener listener = EasyMock.createMock(DataTagUpdateListener.class);

    // run test
    EasyMock.replay(jmsProxyMock, requestHandlerMock);
    cache.addDataTagUpdateListener(tagIds, listener);
    cache.setHistoryMode(true);
    for (Long tagId : tagIds) {
      assertTrue(cache.containsTag(tagId));
    }
    assertFalse(cache.containsTag(23423L));

    assertTrue(cache.isHistoryModeEnabled());
    cache.addDataTagUpdateListener(tagIds, listener);
    Collection<ClientDataTag> cachedTags = cache.getAllSubscribedDataTags();
    assertEquals(2, cachedTags.size());

    Thread.sleep(500);

    // check test success
    EasyMock.verify(jmsProxyMock, requestHandlerMock);
    cache.setHistoryMode(false);
  }


  private ClientDataTagImpl prepareClientDataTagCreateMock(final Long tagId) throws RuleFormatException, JMSException {
    ClientDataTagImpl cdtMock = new ClientDataTagImpl(tagId);
    cdtMock.update(createValidTransferTag(tagId));
    supervisionManagerMock.addSupervisionListener(cdtMock, cdtMock.getProcessIds(), cdtMock.getEquipmentIds(), cdtMock.getSubEquipmentIds());
    EasyMock.expect(jmsProxyMock.isRegisteredListener(cdtMock)).andReturn(false);
    jmsProxyMock.registerUpdateListener(cdtMock, cdtMock);

    return cdtMock;
  }

  private TagUpdate createValidTransferTag(final Long tagId) {
    return createValidTransferTag(tagId, Float.valueOf(1.234f));
  }

  private TagUpdate createValidTransferTag(final Long tagId, Object value) {
    DataTagQuality tagQuality = new DataTagQualityImpl();
    tagQuality.validate();
    TagUpdate tagUpdate =
      new TransferTagImpl(
          tagId,
          value,
          "test value desc",
          (DataTagQualityImpl) tagQuality,
          TagMode.TEST,
          new Timestamp(System.currentTimeMillis() - 10000L),
          new Timestamp(System.currentTimeMillis() - 5000L),
          new Timestamp(System.currentTimeMillis()),
          "Test description",
          "My.data.tag.name",
          "My.jms.topic");

    return tagUpdate;
  }
}
