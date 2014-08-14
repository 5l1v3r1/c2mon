package cern.c2mon.server.client.request;

import static junit.framework.Assert.assertTrue;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import org.apache.activemq.command.ActiveMQTextMessage;
import org.junit.Test;

import cern.c2mon.shared.client.alarm.AlarmValue;
import cern.c2mon.shared.client.command.CommandReport;
import cern.c2mon.shared.client.command.CommandTagHandle;
import cern.c2mon.shared.client.configuration.ConfigurationReport;
import cern.c2mon.shared.client.device.DeviceClassNameResponse;
import cern.c2mon.shared.client.device.TransferDevice;
import cern.c2mon.shared.client.process.ProcessNameResponse;
import cern.c2mon.shared.client.request.ClientRequest;
import cern.c2mon.shared.client.request.ClientRequestImpl;
import cern.c2mon.shared.client.request.JsonRequest;
import cern.c2mon.shared.client.supervision.SupervisionEvent;
import cern.c2mon.shared.client.tag.TagConfig;
import cern.c2mon.shared.client.tag.TagUpdate;
import cern.c2mon.shared.client.tag.TagValueUpdate;

public class ClientRequestMessageConverterTest {

  @Test
  public void testSupervisionMessageConversion() {
    JsonRequest<SupervisionEvent> request = new ClientRequestImpl<SupervisionEvent>(SupervisionEvent.class);

    TextMessage message = new ActiveMQTextMessage();
    try {
      message.setText(request.toJson());
      ClientRequest receivedRequest = ClientRequestMessageConverter.fromMessage(message);

      assertTrue(receivedRequest.getRequestType() == ClientRequest.RequestType.SUPERVISION_REQUEST);
    }
    catch (JMSException e) {
      assertTrue(e.getMessage(), false);
    }
  }

  @Test
  public void testActiveAlarmsMessageConversion() {
    JsonRequest<AlarmValue> request = new ClientRequestImpl<AlarmValue>(
        ClientRequest.ResultType.TRANSFER_ACTIVE_ALARM_LIST,
        ClientRequest.RequestType.ACTIVE_ALARMS_REQUEST,
        10000);

    TextMessage message = new ActiveMQTextMessage();
    try {
      message.setText(request.toJson());
      ClientRequest receivedRequest = ClientRequestMessageConverter.fromMessage(message);

      assertTrue(receivedRequest.getRequestType() == ClientRequest.RequestType.ACTIVE_ALARMS_REQUEST);
      assertTrue(receivedRequest.getResultType() == ClientRequest.ResultType.TRANSFER_ACTIVE_ALARM_LIST);
      assertTrue(receivedRequest.getTimeout() == 10000);
    }
    catch (JMSException e) {
      assertTrue(e.getMessage(), false);
    }
  }

  @Test
  public void testTransferTagMessageConversion() {
    JsonRequest<TagUpdate> request = new ClientRequestImpl<TagUpdate>(TagUpdate.class);

    TextMessage message = new ActiveMQTextMessage();
    try {
      message.setText(request.toJson());
      ClientRequest receivedRequest = ClientRequestMessageConverter.fromMessage(message);

      assertTrue(receivedRequest.getRequestType() == ClientRequest.RequestType.TAG_REQUEST);
      assertTrue(receivedRequest.getResultType() == ClientRequest.ResultType.TRANSFER_TAG_LIST);
    }
    catch (JMSException e) {
      assertTrue(e.getMessage(), false);
    }
  }


  @Test
  public void testTransferTagValueMessageConversion() {
    JsonRequest<TagValueUpdate> request = new ClientRequestImpl<TagValueUpdate>(TagValueUpdate.class);

    TextMessage message = new ActiveMQTextMessage();
    try {
      message.setText(request.toJson());
      ClientRequest receivedRequest = ClientRequestMessageConverter.fromMessage(message);

      assertTrue(receivedRequest.getRequestType() == ClientRequest.RequestType.TAG_REQUEST);
      assertTrue(receivedRequest.getResultType() == ClientRequest.ResultType.TRANSFER_TAG_VALUE_LIST);
    }
    catch (JMSException e) {
      assertTrue(e.getMessage(), false);
    }
  }

  @Test
  public void testAlarmValueMessageConversion() {
    JsonRequest<AlarmValue> request = new ClientRequestImpl<AlarmValue>(AlarmValue.class);

    TextMessage message = new ActiveMQTextMessage();
    try {
      message.setText(request.toJson());
      ClientRequest receivedRequest = ClientRequestMessageConverter.fromMessage(message);

      assertTrue(receivedRequest.getRequestType() == ClientRequest.RequestType.ALARM_REQUEST);
      assertTrue(receivedRequest.getResultType() == ClientRequest.ResultType.TRANSFER_ALARM_LIST);
    }
    catch (JMSException e) {
      assertTrue(e.getMessage(), false);
    }
  }

  @Test
  public void testTagConfigMessageConversion() {
    JsonRequest<TagConfig> request = new ClientRequestImpl<TagConfig>(TagConfig.class);

    TextMessage message = new ActiveMQTextMessage();
    try {
      message.setText(request.toJson());
      ClientRequest receivedRequest = ClientRequestMessageConverter.fromMessage(message);

      assertTrue(receivedRequest.getRequestType() == ClientRequest.RequestType.TAG_CONFIGURATION_REQUEST);
      assertTrue(receivedRequest.getResultType() == ClientRequest.ResultType.TRANSFER_TAG_CONFIGURATION_LIST);
    }
    catch (JMSException e) {
      assertTrue(e.getMessage(), false);
    }
  }

  @Test
  public void testConfigurationReportMessageConversion() {
    JsonRequest<ConfigurationReport> request = new ClientRequestImpl<ConfigurationReport>(ConfigurationReport.class);

    TextMessage message = new ActiveMQTextMessage();
    try {
      message.setText(request.toJson());
      ClientRequest receivedRequest = ClientRequestMessageConverter.fromMessage(message);

      assertTrue(receivedRequest.getRequestType() == ClientRequest.RequestType.APPLY_CONFIGURATION_REQUEST);
      assertTrue(receivedRequest.getResultType() == ClientRequest.ResultType.TRANSFER_CONFIGURATION_REPORT);
    }
    catch (JMSException e) {
      assertTrue(e.getMessage(), false);
    }
  }

  @Test
  public void testCommandTagHandleMessageConversion() {
    JsonRequest<CommandTagHandle> request = new ClientRequestImpl<CommandTagHandle>(CommandTagHandle.class);

    TextMessage message = new ActiveMQTextMessage();
    try {
      message.setText(request.toJson());
      ClientRequest receivedRequest = ClientRequestMessageConverter.fromMessage(message);

      assertTrue(receivedRequest.getRequestType() == ClientRequest.RequestType.COMMAND_HANDLE_REQUEST);
      assertTrue(receivedRequest.getResultType() == ClientRequest.ResultType.TRANSFER_COMMAND_HANDLES_LIST);
      assertTrue(receivedRequest.requiresObjectResponse());
    }
    catch (JMSException e) {
      assertTrue(e.getMessage(), false);
    }
  }

  @Test
  public void testExecuteCommandMessageConversion() {
    JsonRequest<CommandReport> request = new ClientRequestImpl<CommandReport>(CommandReport.class);

    TextMessage message = new ActiveMQTextMessage();
    try {
      message.setText(request.toJson());
      ClientRequest receivedRequest = ClientRequestMessageConverter.fromMessage(message);

      assertTrue(receivedRequest.getRequestType() == ClientRequest.RequestType.EXECUTE_COMMAND_REQUEST);
      assertTrue(receivedRequest.getResultType() == ClientRequest.ResultType.TRANSFER_COMMAND_REPORT);
    }
    catch (JMSException e) {
      assertTrue(e.getMessage(), false);
    }
  }

  @Test
  public void testProcessNamesMessageConversion() {

    ClientRequestImpl<ProcessNameResponse> request = new ClientRequestImpl<ProcessNameResponse>(ProcessNameResponse.class);

    TextMessage message = new ActiveMQTextMessage();
    try {
      message.setText(request.toJson());
      ClientRequest receivedRequest = ClientRequestMessageConverter.fromMessage(message);

      assertTrue(receivedRequest.getRequestType() == ClientRequest.RequestType.PROCESS_NAMES_REQUEST);
      assertTrue(receivedRequest.getResultType() == ClientRequest.ResultType.TRANSFER_PROCESS_NAMES);
    }
    catch (JMSException e) {
      assertTrue(e.getMessage(), false);
    }
  }

  @Test
  public void testDeviceClassNamesMessageConversion() {
    ClientRequestImpl<DeviceClassNameResponse> request = new ClientRequestImpl<>(DeviceClassNameResponse.class);

    TextMessage message = new ActiveMQTextMessage();
    try {
      message.setText(request.toJson());
      ClientRequest receivedRequest = ClientRequestMessageConverter.fromMessage(message);

      assertTrue(receivedRequest.getRequestType() == ClientRequest.RequestType.DEVICE_CLASS_NAMES_REQUEST);
      assertTrue(receivedRequest.getResultType() == ClientRequest.ResultType.TRANSFER_DEVICE_CLASS_NAMES);
    }
    catch (JMSException e) {
      assertTrue(e.getMessage(), false);
    }
  }

  @Test
  public void testDevicesMessageConversion() {
    ClientRequestImpl<TransferDevice> request = new ClientRequestImpl<>(TransferDevice.class);

    TextMessage message = new ActiveMQTextMessage();
    try {
      message.setText(request.toJson());
      ClientRequest receivedRequest = ClientRequestMessageConverter.fromMessage(message);

      assertTrue(receivedRequest.getRequestType() == ClientRequest.RequestType.DEVICE_REQUEST);
      assertTrue(receivedRequest.getResultType() == ClientRequest.ResultType.TRANSFER_DEVICE_LIST);
    }
    catch (JMSException e) {
      assertTrue(e.getMessage(), false);
    }
  }
}
