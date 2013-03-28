package de.adorsys.forge.server;

import static junit.framework.Assert.*;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URLEncoder;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.forge.test.AbstractShellTest;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Ignore;
import org.junit.Test;

public class ForgeServerPluginTest extends AbstractShellTest {
	@Deployment
	public static JavaArchive getDeployment() {
		return AbstractShellTest.getDeployment().addPackages(true, ForgeServerPlugin.class.getPackage());
	}

	@Test
	@Ignore
	public void testGit() throws Exception {
		getShell().execute("forge-server");

		String encode = URLEncoder.encode("https://sso@fisheye.adorsys.de/git/showcase.wellengang.git", "UTF8");

		String uri = "http://localhost:8081/forge/foobar/" + encode;
		HttpResponse response = Request.Post(uri).bodyString("forge-server;", ContentType.TEXT_PLAIN)
				.execute().returnResponse();
		assertEquals(200, response.getStatusLine().getStatusCode());
		InputStream content = response.getEntity().getContent();
		IOUtils.copy(content, new FileOutputStream("target/forge.zip"));
	}

}
