package cern.c2mon.server.configuration.handler.transacted;

import cern.c2mon.cache.actions.alive.AliveTagCacheObjectFactory;
import cern.c2mon.cache.api.C2monCache;
import cern.c2mon.server.cache.loading.AliveTagDAO;
import cern.c2mon.server.common.alive.AliveTag;
import cern.c2mon.server.configuration.impl.ProcessChange;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class AliveTagConfigHandler extends BaseConfigHandlerImpl<AliveTag> {

  @Inject
  public AliveTagConfigHandler(C2monCache<AliveTag> aliveTimerCache, AliveTagDAO dao, AliveTagCacheObjectFactory aliveTagFactory) {
    super(aliveTimerCache, dao, aliveTagFactory, () -> null);
  }

  /**
   * Given a ControlTag id, returns a create event for sending
   * to the DAQ layer if necessary. Returns null if no event needs
   * sending to the DAQ layer for this particular ControlTag.
   *
   * <p>Currently, only alive tags with a DataTagAddress are sent
   * to the DAQ layer. All other cases only need an update to the
   * Equipment itself.
   *
   * <p>Created ControlTags are only sent to the DAQ layer once they
   * are referenced by some Equipment (and hence also belong to a given
   * DAQ!). Updates to ControlTags can be sent immediately
   *
   * @param configId the id of the configuration
   * @param controlTagId the id of the ControlTag that needs creating on the DAQ layer
   * @param equipmentId the id of the Equipment this control tag is attached to (compulsory)
   * @param processId the id of the Process to reconfigure
   * @return the change event including the process id
   */
  public ProcessChange getCreateEvent(final Long configId, final Long controlTagId, final Long equipmentId, final Long processId) {
    ProcessChange processChange = null;

    AliveTag aliveTimer = cache.get(controlTagId);

//    TODO (Alex) Turn this on when we have ControlTag events
//    if (aliveTimer.getAddress != null) {
//      DataTagAdd dataTagAdd = new DataTagAdd(configId, equipmentId, dataTagFacade.generateSourceDataTag(aliveTimer));
//      processChange = new ProcessChange(processId, dataTagAdd);
//    }

//        if (processFacade.getProcessIdFromControlTag(controlTag.getId()) != null) {
//          processChange = new ProcessChange(processFacade.getProcessIdFromControlTag(controlTag.getId()));
//        }

    return processChange;
  }
}
