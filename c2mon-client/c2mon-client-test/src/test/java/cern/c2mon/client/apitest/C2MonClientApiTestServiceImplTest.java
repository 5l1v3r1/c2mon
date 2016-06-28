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
package cern.c2mon.client.apitest;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import cern.c2mon.client.apitest.MetricDef;
import cern.c2mon.client.apitest.service.C2MonClientApiTestService;


//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration({ "classpath:resources/application-context-test.xml" })
public class C2MonClientApiTestServiceImplTest {

	@Autowired
	C2MonClientApiTestService service;

	//@Test
	public void getAllDeviceRuleMetricsForProcess() throws Exception {

		List<MetricDef> def = service.getProcessMetrics("P_CLIC_01");

		assertEquals(2, def.size());
		
		//assertEquals("TESTDEVICE1:STATUS", def.get(0).getName());
		//assertEquals(100, def.get(0).getRuleTagId());
		
		//assertEquals("TESTDEVICE2:STATUS", def.get(1).getName());
		//assertEquals(222, def.get(1).getRuleTagId());
	}
}
