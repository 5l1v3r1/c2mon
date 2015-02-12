package cern.c2mon.daq.dip;


import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import cern.c2mon.daq.common.logger.EquipmentLogger;
import cern.c2mon.daq.common.logger.EquipmentLoggerFactory;
import cern.c2mon.shared.common.datatag.ISourceDataTag;
import cern.c2mon.shared.common.datatag.address.DIPHardwareAddress;
import cern.dip.Dip;
import cern.dip.DipData;
import cern.dip.DipException;
import cern.dip.DipFactory;
import cern.dip.DipSubscription;
import cern.dip.DipSubscriptionListener;

/**
 * Junit test of the DipAlivePublisher class.
 *
 * @author Mark Brightwell
 *
 */
public class DipAlivePublisherTest {

  /**
   * Test publication topic.
   */
  private static final String TEST_DIP_PUBLICATION_NAME = "dip/ts/TIM/DIPDAQ_TEST_ALIVE";

  /**
   * Mocks
   */
  private ISourceDataTag mockAliveTag;
  private DIPHardwareAddress mockAddress;
  private EquipmentLoggerFactory equipmentLoggerFactoryMock;

  /**
   * Logger needed for Publisher initialization.
   */
  private EquipmentLogger equipmentLogger;

  /**
   * Field used for recording number of listener calls in test.
   */
  private int listenerCalls = 0;

  /**
   * Initialized fields above.
   */
  @Before
  public void setUp() {
    mockAliveTag = EasyMock.createMock(ISourceDataTag.class);
    this.equipmentLogger = new EquipmentLogger("Test equipmentLogger", "Test equipmentLogger", "DipAlivePublisher");
    mockAddress = EasyMock.createMock(DIPHardwareAddress.class);
    this.equipmentLoggerFactoryMock = EasyMock.createMock(EquipmentLoggerFactory.class);
  }

  /**
   * Checks the publication is arriving at correct intervals
   * (no check on actual value of publication, just reception of messages).
   * @throws InterruptedException
   * @throws DipException
   */
  @Test
  public void testPublishAlive() throws InterruptedException, DipException {

    DipSubscriptionListener listener = new TestListener();
    DipFactory dipFactory = Dip.create("DAQ_ALIVE_TEST-" + System.currentTimeMillis());
    DipSubscription dipSubscription = dipFactory.createDipSubscription(TEST_DIP_PUBLICATION_NAME, listener);

    EasyMock.expect(mockAliveTag.getHardwareAddress()).andReturn(mockAddress);
    EasyMock.expectLastCall().times(2);
    EasyMock.expect(mockAddress.getItemName()).andReturn(TEST_DIP_PUBLICATION_NAME);
    EasyMock.expect(this.equipmentLoggerFactoryMock.getEquipmentLogger(DipAlivePublisher.class)).andReturn(this.equipmentLogger).times(1);

    EasyMock.replay(mockAliveTag, mockAddress, this.equipmentLoggerFactoryMock);

    DipAlivePublisher publisher = new DipAlivePublisher("Test DIP alive", mockAliveTag, 5000, this.equipmentLoggerFactoryMock);
    publisher.start();
    //wait for first 2 publications
    Thread.sleep(8000);
    publisher.stop();

    EasyMock.verify(mockAliveTag);
    EasyMock.verify(mockAddress);
    Assert.assertEquals(2, listenerCalls);
    listenerCalls = 0;
  }

  /**
   * For recording calls to listener.
   * @author Mark Brightwell
   *
   */
  public class TestListener implements DipSubscriptionListener {

    @Override
    public void connected(DipSubscription arg0) {
    }

    @Override
    public void disconnected(DipSubscription arg0, String arg1) {
    }

    @Override
    public void handleException(DipSubscription arg0, Exception arg1) {
    }

    @Override
    public void handleMessage(DipSubscription arg0, DipData arg1) {
      listenerCalls++;
    }

  }

}
