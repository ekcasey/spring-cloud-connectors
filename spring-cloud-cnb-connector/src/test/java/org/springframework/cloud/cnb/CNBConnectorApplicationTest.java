package org.springframework.cloud.cnb;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.cloud.CloudException;
import org.springframework.cloud.service.ServiceInfo;
import org.springframework.cloud.service.common.MysqlServiceInfo;

/**
 * 
 * @author Ramnivas Laddad
 * @author Scott Frederick
 *
 */
public class CNBConnectorApplicationTest extends AbstractCNBConnectorTest {
	
	@Test
	public void isInMatchingEnvironment() {
		when(mockEnvironment.getEnvValue("CNB_BINDINGS")).thenReturn("some/bindings/dir");
		assertTrue(testCloudConnector.isInMatchingCloud());
		
		when(mockEnvironment.getEnvValue("CNB_BINDINGS")).thenReturn(null);
		assertFalse(testCloudConnector.isInMatchingCloud());
	}
	
	private String getBindingsDir(String name, String... uri) {
		return "";
	}

	@Test
	public void serviceInfosWithNoBindings() {
		when(mockEnvironment.getEnvValue("CNB_BINDINGS")).thenReturn(null);
		List<ServiceInfo> serviceInfos = testCloudConnector.getServiceInfos();
		assertNotNull(serviceInfos);
		assertEquals(0, serviceInfos.size());
	}

	@Test
	public void serviceInfosWithEmptyBindingsDir() {
		when(mockEnvironment.getEnvValue("CNB_BINDINGS")).thenReturn(getTestBindingsPath("test-no-bindings"));
		List<ServiceInfo> serviceInfos = testCloudConnector.getServiceInfos();
		assertNotNull(serviceInfos);
		assertEquals(0, serviceInfos.size());
	}

	@Test(expected = CloudException.class)
	public void servicesInfosWithInvlaidBindingDir() {
		when(mockEnvironment.getEnvValue("CNB_BINDINGS")).thenReturn("not/exist");
		testCloudConnector.getServiceInfos();
	}

	@Test
	public void serviceInfosWithEmptyBinding() {
		when(mockEnvironment.getEnvValue("CNB_BINDINGS")).
				thenReturn(getTestBindingsPath("test-empty-binding"));
		List<ServiceInfo> serviceInfos = testCloudConnector.getServiceInfos();
		assertNotNull(serviceInfos);
		assertEquals(1, serviceInfos.size());
	}

	@Test
	public void serviceInfosWithEmptyBindingSecret() {
		when(mockEnvironment.getEnvValue("CNB_BINDINGS")).
				thenReturn(getTestBindingsPath("test-binding-empty-secret"));
		List<ServiceInfo> serviceInfos = testCloudConnector.getServiceInfos();
		assertNotNull(serviceInfos);
		assertEquals(1, serviceInfos.size());
	}

	@Test
	public void serviceInfosWithCNBInfoCreator() {
		when(mockEnvironment.getEnvValue("CNB_BINDINGS")).
				thenReturn(getTestBindingsPath("test-binding-post-processed"));
		List<ServiceInfo> serviceInfos = testCloudConnector.getServiceInfos();
		assertNotNull(serviceInfos);
		assertEquals(1, serviceInfos.size());
		assertThat(serviceInfos.get(0), instanceOf(MysqlServiceInfo.class));
		assertEquals("mysql://username:password@db.example.com/db", ((MysqlServiceInfo) serviceInfos.get(0)).getUri());
	}
}
