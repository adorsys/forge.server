package de.adorsys.forge.server;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jboss.forge.env.Configuration;
import org.jboss.forge.env.ConfigurationScope;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellPrompt;
import org.jboss.forge.shell.events.PostStartup;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.Option;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyBootstrap;


/**
 * 
 * @author sso
 *
 */
@Alias("forge-server")
@Singleton
public class ForgeServerPlugin implements Plugin {

	public static final String FORGE_SERVER_GIT_TEMPLATE_REPO_URL = "forge-server.gitTemplateRepoUrl";

	private static final String FORGE_SERVER_AUTOSTART = "forge-server.autostart";

	final ShellPrompt prompt;

	final Shell shell;

	final ForgeRestAPI forgeRestAPI;

	final Configuration config;

	@Inject
	public ForgeServerPlugin(
			ShellPrompt prompt, Shell shell, ForgeRestAPI forgeRestAPI, Configuration config) throws Exception {
		this.prompt = prompt;
		this.shell = shell;
		this.forgeRestAPI = forgeRestAPI;
		this.config = config;
	}

	private Server server;

	@Command("setup")
	public void setup(@Option(name="gitTemplateRepoUrl", required=true) String gitTemplateRepoUrl,
			@Option(name="autostart", required=true) Boolean autoStart) {
		Configuration scopedConfiguration = config.getScopedConfiguration(ConfigurationScope.USER);
		scopedConfiguration.setProperty(FORGE_SERVER_GIT_TEMPLATE_REPO_URL, gitTemplateRepoUrl);
		scopedConfiguration.setProperty(FORGE_SERVER_AUTOSTART, autoStart);
	}

	@Command("start")
	public void startServer() throws Exception {
		shell.println("Starting Forge Server");
		server = new Server(8081);

		ServletContextHandler handler = new ServletContextHandler();
		server.setHandler(handler);
		handler.setContextPath("/");
		handler.addEventListener(new ResteasyBootstrap());
		ServletHolder servletHolder = new ServletHolder("restAPIServlet", HttpServletDispatcher.class);

		JAXRSApplication.getServices().add(forgeRestAPI);

		handler.setInitParameter("javax.ws.rs.Application", "de.adorsys.forge.server.JAXRSApplication");
		handler.setInitParameter("resteasy.providers", "org.jboss.resteasy.plugins.providers.StringTextStar,org.jboss.resteasy.plugins.providers.StreamingOutputProvider");
		handler.addServlet(servletHolder, "/*");
		handler.start();
		server.start();
		shell.println("started Forge Server");
	}

	@Command("stop")
	public void shutDown() throws Exception {
		server.stop();
		server.join();
	}

	public void start(@Observes PostStartup ps) throws Exception {
		Configuration scopedConfiguration = config.getScopedConfiguration(ConfigurationScope.USER);
		boolean autoStart = scopedConfiguration.getBoolean(FORGE_SERVER_AUTOSTART, false);
		if (autoStart) {
			startServer();
		}
	}

}
