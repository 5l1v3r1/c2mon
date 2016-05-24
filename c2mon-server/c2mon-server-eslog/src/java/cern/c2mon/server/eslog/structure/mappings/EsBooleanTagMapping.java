/******************************************************************************
 * Copyright (C) 2010-2016 CERN. All rights not expressly granted are reserved.
 * <p>
 * This file is part of the CERN Control and Monitoring Platform 'C2MON'.
 * C2MON is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the license.
 * <p>
 * C2MON is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for
 * more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with C2MON. If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/
package cern.c2mon.server.eslog.structure.mappings;

/**
 * EsMapping that a {@link cern.c2mon.server.eslog.structure.types.EsTagBoolean} will use in the ElasticSearch cluster.
 * valueBoolean = true --> valueNumeric = 1, valueString = "true".
 * valueBoolean = false --> valueNumeric = 0, valueString = "false".
 *
 * @author Alban Marguet.
 */
public class EsBooleanTagMapping extends EsTagMapping implements EsMapping {

  /**
   * Instantiate a {@link EsBooleanTagMapping} by setting its properties with {@param type}
   */
  public EsBooleanTagMapping(ValueType type) {
    super();
    setProperties(type);
  }

  /**
   * Initialize the mapping according that the valueType is boolean type.
   */
  @Override
  public void setProperties(ValueType valueType) {
    if(valueType.equals(ValueType.BOOLEAN)) {
      properties = new Properties(valueType);
    } else {
      throw new IllegalArgumentException("Type for EsTagBoolean must be boolean.");
    }
  }
}