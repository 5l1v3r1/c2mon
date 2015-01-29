package cern.c2mon.daq.tools;

import cern.c2mon.daq.common.logger.EquipmentLogger;
import cern.c2mon.daq.common.logger.EquipmentLoggerFactory;
import cern.c2mon.shared.common.datatag.SourceDataTag;
import cern.c2mon.shared.common.type.TypeConverter;

/**
 * Class with all possible validations for Data Tag Values
 * 
 * @author vilches
 */
public class DataTagValueValidator {
	/**
	 * The logger of this class
	 */
	protected EquipmentLogger equipmentLogger;
	
	/**
     * The maximum allowed difference between the system's timestamp and the equipment's timestamp
     */
    private static final int MAX_MSECONDS_DIFF = 300000; // 5 minutes

	/**
	 * Creates a new Data Tag Value Validator which uses the provided equipment logger to log its results.
	 * 
	 * @param equipmentLogger The equipment logger to use
	 */
	public DataTagValueValidator(final EquipmentLoggerFactory equipmentLoggerFactory) {
		this.equipmentLogger = equipmentLoggerFactory.getEquipmentLogger(getClass());
	}

	/**
	 * This method is responsible for checking if new value received from data source fits in a proper range
	 * 
	 * @param sdt the source data tag object
	 * @param value new value of SourceDataTag to be checked
	 * @return True if the value is in range else false.
	 */
	@SuppressWarnings("unchecked")
	public boolean isInRange(final SourceDataTag sdt, final Object value) {
		this.equipmentLogger.trace("isInRange - entering isInRange()..");

		boolean isInRange = true;
		Object convertedValue;
		if (sdt.getMinValue() != null) {
		  // Convert value before comparing (we assume if we get here the value is convertible)
		  convertedValue = TypeConverter.cast(value.toString(), sdt.getDataType());
			if (sdt.getMinValue().compareTo(convertedValue) > 0) {
				this.equipmentLogger.trace("\tisInRange - out of range : " + convertedValue + " is less than the authorized minimum value "
						+ sdt.getMinValue());
				isInRange = false;
			}
		}

		if (isInRange) {
			if (sdt.getMaxValue() != null) {
			  // Convert value before comparing (we assume if we get here the value is convertible)
	      convertedValue = TypeConverter.cast(value.toString(), sdt.getDataType());
				if (sdt.getMaxValue().compareTo(convertedValue) < 0) {
					this.equipmentLogger.trace("\tisInRange - out of range : " + convertedValue + " is greater than the authorized maximum value " 
					    + sdt.getMaxValue());
					isInRange = false;
				}
			}
		}

		this.equipmentLogger.trace("isInRange - leaving isInRange(). Is value in range?: " + isInRange);

		return isInRange;
	}

	/**
	 * This method checks whether the equipment's time is too far in the future or not. For doing that, the equipment's
	 * time is compared to the system's time
	 * 
	 * @param timestamp Time sent from the equipment in ms.
	 * @return Whether the equipment's timestamp is inside the indicated time range or not
	 */
	public boolean isTimestampValid(final long timestamp) {
		this.equipmentLogger.trace("entering isTimestampValid()..");
		boolean isValid = true;
		long diff = (timestamp - System.currentTimeMillis());
		if (diff > MAX_MSECONDS_DIFF) {
			isValid = false;
		}
		this.equipmentLogger.trace("leaving isTimestampValid().. Result: " + isValid);
		return isValid;
	}
	
	/**
     * Checks if the tagValue is convertible to the value of the current tag.
     * 
     * @param tag The tag to check.
     * @param tagValue The value to check.
     * @return True if the value is convertible else false.
     */
    public boolean isConvertible(final SourceDataTag tag, final Object tagValue) {
        this.equipmentLogger.trace("entering isConvertable()..");
        String value = tagValue.toString();
        this.equipmentLogger.trace("isConvertible - Tag #" + tag.getId() + " Casting " + value + " to " + tag.getDataType());
        boolean isConvertable = TypeConverter.cast(value, tag.getDataType()) != null;
        this.equipmentLogger.trace("leaving isConvertable().. Result: " + isConvertable);
        return isConvertable;
    }
}
