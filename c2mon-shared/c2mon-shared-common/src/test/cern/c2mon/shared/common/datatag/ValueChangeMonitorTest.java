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

package cern.c2mon.shared.common.datatag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Test;

import cern.c2mon.shared.common.datatag.ValueChangeMonitor.OPERATOR;
import cern.c2mon.shared.util.parser.SimpleXMLParser;

/**
 * JUnit test of class <code>ValueChangeMonitorTest</code>
 * 
 * @author wbuczak
 */
public class ValueChangeMonitorTest {

    SimpleXMLParser parser;

    static final String vcm1 = "<value-change-monitor step=\"M0.99\" timeWindow=\"3\" />";
    static final String vcm2 = "<value-change-monitor step=\"L2.0\" />";
    static final String vcm3 = "<value-change-monitor timeWindow=\"3\" />";
    static final String vcm4 = "<value-change-monitor step=\"1.0\" timeWindow=\"3\" />";

    @Before
    public void setUp() throws Exception {
        parser = new SimpleXMLParser();
    }

    @Test
    public void testBasicInterface() {
        ValueChangeMonitor m1 = ValueChangeMonitor.fromConfigXML(parser.parse(vcm1).getDocumentElement());
        assertEquals(vcm1, m1.toConfigXML());
        assertEquals(OPERATOR.MORE,m1.getOperator());
        assertEquals(0.99, m1.getStep(),0.0f);
        assertEquals(3, m1.getTimeWindow().intValue());
        assertTrue(m1.hasStep());
        assertTrue(m1.hasTimeWindow());        
        
        ValueChangeMonitor m2 = ValueChangeMonitor.fromConfigXML(parser.parse(vcm2).getDocumentElement());
        assertEquals(vcm2, m2.toConfigXML());
        assertEquals(OPERATOR.LESS,m2.getOperator());
        assertEquals(2.0, m2.getStep(),0.0f);
        assertTrue (m2.hasStep());
        assertFalse(m2.hasTimeWindow());
        
        ValueChangeMonitor m3 = ValueChangeMonitor.fromConfigXML(parser.parse(vcm3).getDocumentElement());
        assertEquals(vcm3, m3.toConfigXML());
        assertEquals(OPERATOR.UNDEFINED,m3.getOperator());
        assertFalse (m3.hasStep());
        assertTrue(m3.hasTimeWindow());
               
        ValueChangeMonitor m4 = ValueChangeMonitor.fromConfigXML(parser.parse(vcm4).getDocumentElement());
        assertEquals(vcm4, m4.toConfigXML());
        assertEquals(OPERATOR.EQUALS,m4.getOperator());
        assertTrue (m4.hasStep());
        assertTrue(m4.hasTimeWindow());        
    }

}
