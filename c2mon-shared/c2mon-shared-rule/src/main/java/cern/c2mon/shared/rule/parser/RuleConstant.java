/******************************************************************************
 * Copyright (C) 2010-2016 CERN. All rights not expressly granted are reserved.
 * 
 * This file is part of the CERN Control and Monitoring Platform 'C2MON'.
 * C2MON is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the license.
 * 
 * C2MON is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with C2MON. If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/
package cern.c2mon.shared.rule.parser;

/**
 * A list of CONSTANTS that can be used inside a rule.
 * 
 * @author ekoufaki
 */
public enum RuleConstant {
  
  /** 
   * Can be used inside a rule to check against a Tag's Quality 
   * @see https://issues.cern.ch/browse/TIMS-836 
   */
  INVALID_KEYWORD("INVALID"),

  /** 
   * Used internally from the parser to mark tags that are INVALID
   * @see https://issues.cern.ch/browse/TIM-835
   */
  INTERNAL_INVALID("INTERNAL_INVALID");
  
  /**
   * @param text used inside a rule to represent this CONSTANT
   */
  private RuleConstant(final String text) {
      this.text = "$" + text;
  }

  /** Text used inside a rule to represent this CONSTANT */
  private final String text;

  @Override
  public String toString() {
      return text;
  }
  
  /**
   * @return The RuleConstant represented by the specified token, or null if no RuleConstant was found.
   * @param constant RuleConstant as a string
   */
  public static RuleConstant fromString(final String constant) {
    
    for (RuleConstant r: RuleConstant.values()) {
      if (r.toString().equals(constant)) {
        return r;
      }
    }
    return null;
  }
}
