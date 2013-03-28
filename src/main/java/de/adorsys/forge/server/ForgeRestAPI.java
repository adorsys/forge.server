package de.adorsys.forge.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonGenerator.Feature;
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.jgit.api.Git;
import org.jboss.forge.ForgeEnvironment;
import org.jboss.forge.env.Configuration;
import org.jboss.forge.env.ConfigurationScope;
import org.jboss.forge.project.services.ResourceFactory;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.shell.Shell;

/**
 * 
 * @author sso
 */
@Path("/forge")
@Singleton
public class ForgeRestAPI {

	@Inject
	Shell shell;

	@Inject
	ForgeEnvironment environment;

	@Inject
	ResourceFactory resourceFactory;

	@Inject
	Configuration config;

	private final String tmpDir;

	private String currentJob;

	private final File templateDir;

	public ForgeRestAPI() {
		tmpDir = System.getProperties().getProperty("java.io.tmpdir");
		templateDir = new File(tmpDir, "jim-templates");
	}

	@GET
	@Path("status")
	public String status() {
		if (currentJob == null) {
			return "waiting for a job";
		}
		return "executing job " + currentJob;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response listTemplates() throws FileNotFoundException, IOException {
		updateTemplates();
		FileInputStream fis = new FileInputStream(getIndexJSONFile());
		try {
			String content = IOUtils.toString(fis, "UTF8");
			return Response.ok(content).build();
		} finally {
			fis.close();
		}
	}

	@POST
	@Path("{jobid:[a-zA-Z0-9\\-_]+}/{scriptpath:.+}")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.TEXT_PLAIN)
	public synchronized Response execute(@PathParam("scriptpath") final String scriptpath, final String properties,
			@PathParam("jobid") final String jobid, @QueryParam("giturl") final String gitUrl) throws Exception {
		final File scriptPath = new File(templateDir, scriptpath);
		if (!scriptPath.exists()) {
			return Response.status(Status.NOT_FOUND).entity("Script " + scriptpath + " not in repository").build();
		}
		StreamingOutput streamingOutput = new StreamingOutput() {

			@Override
			public void write(OutputStream output) throws IOException, WebApplicationException {
				shell.setOutputStream(output);
				File workingDir = new File(tmpDir, "forge-job-" + jobid);
				try {
					Git.cloneRepository().setURI(gitUrl).setDirectory(workingDir).call();
					Git git = Git.open(workingDir);

					Properties props = new Properties();
					props.load(new StringReader(properties));

					Set<Entry<Object, Object>> propEntries = props.entrySet();
					for (Entry<Object, Object> entry : propEntries) {
						environment.setProperty(String.valueOf(entry.getKey()), entry.getValue());
					}
					build(scriptPath, jobid, workingDir);

					git.add().addFilepattern(".").call();
					git.commit().setMessage("Generated project by Forge").call();
					shell.println("commit the files");

					git.push().call();
					shell.println("push to git");
				} catch (Exception e) {
					throw new WebApplicationException(e);
				} finally {
					FileUtils.deleteDirectory(workingDir);
					shell.setOutputStream(System.out);
					currentJob = null;
				}

			}
		};
		return Response.ok(streamingOutput).build();
	}

	public synchronized void updateTemplates() {
		try {
			if (templateDir.exists()) {
				shell.println("Updating Templates");
				Git git = Git.open(templateDir);
				git.pull().call();
			} else {
				shell.println("Checkout Templates");

				Configuration scopedConfiguration = config.getScopedConfiguration(ConfigurationScope.USER);
				String uri = scopedConfiguration.getString(ForgeServerPlugin.FORGE_SERVER_GIT_TEMPLATE_REPO_URL);
				Git.cloneRepository().setURI(uri).setDirectory(templateDir).call();
			}
			File file = getIndexJSONFile();
			if (!file.exists()) {
				shell.println("Create a initial index.json");
				ArrayList<ForgeTemplate> template = new ArrayList<ForgeTemplate>();
				ForgeTemplate forgeTemplate = new ForgeTemplate();
				forgeTemplate.setTitle("Test");
				forgeTemplate.setPath("test/test.fsh");
				forgeTemplate.setDescription("This is a configuration demo template");
				template.add(forgeTemplate);
				forgeTemplate = new ForgeTemplate();
				forgeTemplate.setTitle("Tes2t");
				forgeTemplate.setPath("test/test2.fsh");
				forgeTemplate.setDescription("This is a configuration demo template");
				template.add(forgeTemplate);
				new ObjectMapper().configure(Feature.QUOTE_FIELD_NAMES, false).writeValue(file, template);
				Git git = Git.open(templateDir);
				git.add().addFilepattern("index.json").call();
				git.commit().setMessage("Created initial index.json").call();
				git.push().call();
			}
		} catch (Exception e) {
			shell.println("Problem updating generation templates... continue");
		}
	}

	private File getIndexJSONFile() {
		File file = new File(templateDir, "index.json");
		return file;
	}

	private void build(File scriptPath, String jobid, File workingDir) throws Exception {
		currentJob = jobid;

		updateTemplates();

		shell.setAcceptDefaults(true);
		environment.setProperty("VERBOSE", "true");
		environment.setProperty("templatedir", templateDir.getAbsolutePath());
		Resource<File> workingDirResource = resourceFactory.getResourceFrom(workingDir);
		shell.setCurrentResource(workingDirResource);
		shell.println("run " + scriptPath.getAbsolutePath());
		shell.execute(scriptPath);
		shell.setCurrentResource(resourceFactory.getResourceFrom(new File(tmpDir)));
	}

}
