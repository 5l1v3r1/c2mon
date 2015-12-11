package cern.c2mon.server.eslog.structure;

import cern.c2mon.server.cache.EquipmentCache;
import cern.c2mon.server.cache.ProcessCache;
import cern.c2mon.server.cache.SubEquipmentCache;
import cern.c2mon.server.common.control.ControlTag;
import cern.c2mon.server.common.datatag.DataTag;
import cern.c2mon.server.common.equipment.Equipment;
import cern.c2mon.server.common.process.Process;
import cern.c2mon.server.common.subequipment.SubEquipment;
import cern.c2mon.server.common.tag.Tag;
import cern.c2mon.server.eslog.structure.types.TagES;
import cern.c2mon.shared.common.datatag.DataTagQuality;
import cern.c2mon.shared.common.datatag.TagQualityStatus;
import cern.c2mon.shared.util.json.GsonFactory;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Converts the dataTags from the server to the ElasticSearch format.
 * @author Alban Marguet.
 */
@Slf4j
@Component
public class DataTagESLogConverter {
    private final ProcessCache processCache;
    private final EquipmentCache equipmentCache;
    private final SubEquipmentCache subEquipmentCache;
    private TagES tagES;
    private Gson gson;

    @Autowired
    public DataTagESLogConverter(final ProcessCache processCache, final EquipmentCache equipmentCache, final SubEquipmentCache subEquipmentCache, TagES tagES) {
        this.processCache = processCache;
        this.equipmentCache = equipmentCache;
        this.subEquipmentCache = subEquipmentCache;
        this.tagES = tagES;
        this.gson = GsonFactory.createGson();
    }


    /**
     * Converts all the properties of a Tag to create a TagES.
     * @param tag Tag object in C2MON.
     * @return TagES, ready to be logged to the ElasticSearch instance.
     */
    public TagES convertToTagES(Tag tag) {
        tagES.setMetadataProcess(getTagMetadataProcess(tag));
        tagES.setTagId(tag.getId());
        tagES.setTagName(tag.getName());
        tagES.setDataType(tag.getDataType());
        if (tag instanceof DataTag || tag instanceof ControlTag) {
            tagES.setTagTime(((DataTag) tag).getSourceTimestamp().getTime());
            tagES.setTagDaqTime(((DataTag) tag).getDaqTimestamp().getTime());
        }
        tagES.setTagServerTime(tag.getCacheTimestamp().getTime());
        int code = 0;
        if (tag.getDataTagQuality() != null) {
            for (TagQualityStatus status : tag.getDataTagQuality().getInvalidQualityStates().keySet()) {
                code = (int) (code + Math.pow(2, status.getCode()));
            }
        }
        tagES.setTagStatus(code);
        DataTagQuality quality = tag.getDataTagQuality();
        if (quality != null && quality.isInitialised()) {
            tagES.setQuality(gson.toJson(quality.getInvalidQualityStates()));
            if (tagES.getQuality() != null) {
                tagES.setQuality("{\"UNKNOWN_REASON\":\"Invalid quality String was too long: unable to store in ShortTermLog table.\"}");
            }
        }
        tagES.setTagValue(tag.getValue());
        tagES.setTagValueDesc(tag.getValueDescription());

        //log.info("converted to: " + tagES.toString());
        return tagES;
    }

    /**
     * Retrieve the ProcessName [, EquipmentName, SubEquipmentName] for a given Tag.
     * According to the hierarchy.
     * @param tag for which to get the metadata
     * @return List of names in the order ProcessName [, EquipmentName, SubEquipmentName].
     */
    public Map<String, String> getTagMetadataProcess(Tag tag) {
        Map<String, String> result = new HashMap<>();
        long processId = -1;
        long equipmentId = -1;
        long subEquipmentId = -1;
        String processName;
        String equipmentName;
        String subEquipmentName;


        if (!tag.getSubEquipmentIds().isEmpty() && tag.getSubEquipmentIds().size() >= 1) {
            subEquipmentId = tag.getSubEquipmentIds().iterator().next();

            equipmentId = subEquipmentCache.get(subEquipmentId).getParentId();

            processId = equipmentCache.get(equipmentId).getProcessId();

        } else if (!tag.getEquipmentIds().isEmpty() && tag.getEquipmentIds().size() >= 1) {
            equipmentId = tag.getEquipmentIds().iterator().next();

            processId = equipmentCache.get(equipmentId).getProcessId();

        } else if (!tag.getProcessIds().isEmpty() && tag.getProcessIds().size() >= 1) {
            processId = tag.getProcessIds().iterator().next();
        } else {
            log.info("no Process, Equipment or subEquipment");
            return result;
        }

        processName = getProcessName(processId);
        log.info("add process");
        result.put("Process", processName);

        if (equipmentId != -1) {
            equipmentName = getEquipmentName(equipmentId);
            if (equipmentName != null) {
                result.put("Equipment", equipmentName);
                log.info("add equipment");
            }
        }
        if (subEquipmentId != -1) {
            subEquipmentName = getSubEquipmentName(subEquipmentId);
            if (subEquipmentName != null) {
                result.put("SubEquipment", subEquipmentName);
                log.info("add subEquipment");
            }
        }

        return result;
    }

    /**
     * Retrieve the name of a Process according to the id.
     * @param processId of the tag.
     * @return processName
     */
    public String getProcessName(long processId) {
        String processName = null;

        Process process = processCache.get(processId);
        if (process != null) {
            processName = process.getName();
        }

        return processName;
    }

    /**
     * Retrieve the name of an Equipment according to the id.
     * @param equipmentId of the tag.
     * @return equipmentName
     */
    public String getEquipmentName(long equipmentId) {
        String equipmentName = null;

        Equipment equipment = equipmentCache.get(equipmentId);
        if (equipment != null) {
            equipmentName = equipment.getName();
        }

        return equipmentName;
    }

    /**
     * Retrieve the Name of a SubEquipment according to the id.
     * @param subEquipmentId of the tag.
     * @return subEquipmentName
     */
    public String getSubEquipmentName(long subEquipmentId) {
        String subEquipmentName = null;

        SubEquipment subEquipment = subEquipmentCache.get(subEquipmentId);
        if (subEquipment != null) {
            subEquipmentName = subEquipment.getName();
        }

        return subEquipmentName;
    }
}