package io.leopard.maven.plugins.subproject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Resource;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.WriterFactory;

/**
 * include子项目一起打包.
 *
 * @author <a href="hctan@163.com">阿海</a>
 * @goal add-sources
 * @execute phase="generate-sources"
 * @threadSafe true
 * @requiresDependencyResolution runtime
 * @since 1.0
 */
// @Mojo(name = "add-sources", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
// * @goal add-sources
// * @execute phase="generate-sources"
// @Mojo(name = "add-sources", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class SubprojectMojo extends AbstractMojo {

	private static List<String> resourcesExclude = new ArrayList<String>();

	static {
		resourcesExclude.add("test.txt");
	}
	/**
	 * @parameter
	 * @required
	 * @readonly
	 */
	private String[] sources;

	/**
	 * @parameter
	 * @required
	 * @readonly
	 */
	private SubprojectDependency[] dependencies;

	/**
	 * The Maven project.
	 *
	 * @parameter default-value="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;

	/**
	 * @component
	 */
	private ArtifactFactory artifactFactory;
	private Map<String, Artifact> dependencyArtifactMap = new HashMap<String, Artifact>();

	protected String getKey(String groupId, String artifactId) {
		return groupId + ":" + artifactId;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		project.getDependencyArtifacts().add(artifactFactory.createArtifact("io.leopard.data4j", "data4j-memdb", "0.0.1-SNAPSHOT", "compile", "jar"));

		{
			// @SuppressWarnings("unchecked")
			Set<Artifact> set = project.getDependencyArtifacts();
			for (Artifact artifact : set) {
				String groupId = artifact.getGroupId();
				String artifactId = artifact.getArtifactId();
				dependencyArtifactMap.put(this.getKey(groupId, artifactId), artifact);
			}
		}
		File root = project.getBasedir().getParentFile();

		// System.err.println("subproject add-source:" + project.getBasedir().toString());
		// System.err.println("subproject add-source:" + StringUtils.join(sources, ","));
		for (String source : sources) {
			File moduleDir = new File(root, source);

			File subDir = new File(moduleDir, "src/main/java");
			// System.err.println("subDir:" + subDir.getAbsolutePath());
			this.project.addCompileSourceRoot(subDir.getAbsolutePath());
			// if (getLog().isInfoEnabled()) {
			// getLog().info("Source directory: " + subDir + " added.");
			// }
			{
				Resource resource = new Resource();
				resource.setDirectory(moduleDir.getAbsolutePath() + "/src/main/resources");
				resource.setExcludes(resourcesExclude);
				project.addResource(resource);
			}

			this.addDependencyArtifacts(new File(moduleDir, "pom.xml"));
			// <dependency>
			// <groupId>redis.clients</groupId>
			// <artifactId>jedis</artifactId>
			// <version>2.5.1</version>
			// </dependency>
		}

		try {
			this.updatePom();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void updatePom() throws IOException {
		Map<String, Dependency> dependencyMap = new HashMap<String, Dependency>();

		{
			List<Dependency> dependencyList = project.getOriginalModel().getDependencies();
			for (Dependency dependency : dependencyList) {
				String key = this.getKey(dependency.getGroupId(), dependency.getArtifactId());
				dependencyMap.put(key, dependency);
			}
		}
		if (dependencies != null) {
			for (SubprojectDependency dependency : dependencies) {
				String key = this.getKey(dependency.getGroupId(), dependency.getArtifactId());
				Dependency dependency2 = dependencyMap.get(key);
				if (dependency2 != null) {
					dependency2.setScope(dependency.getScope());
				}
				// System.err.println("dependency:" + dependency + " artifact:" + artifact);
			}
			// throw new RuntimeException();
		}
		File buildDir = new File(project.getBuild().getDirectory());
		buildDir.mkdirs();
		File pomFile = new File(buildDir, "pom-subproject.xml");
		Writer writer = WriterFactory.newXmlWriter(pomFile);
		new MavenXpp3Writer().write(writer, project.getOriginalModel());
		writer.close();
		project.setFile(pomFile);
	}

	@SuppressWarnings("unchecked")
	protected void addDependencyArtifacts(File pomFile) {
		Model model;
		try {
			InputStream input = FileUtils.openInputStream(pomFile);
			model = new MavenXpp3Reader().read(input);
		}
		catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		List<Dependency> dependencies = model.getDependencies();
		if (dependencies != null) {
			for (Dependency dependency : dependencies) {
				String groupId = dependency.getGroupId();
				String artifactId = dependency.getArtifactId();
				String version = dependency.getVersion();
				String scope = dependency.getScope();
				String type = dependency.getType();

				if (version != null && scope != null) {
					if (!dependencyArtifactMap.containsKey(this.getKey(groupId, artifactId))) {
						Dependency dependency2 = new Dependency();
						dependency2.setGroupId(groupId);
						dependency2.setArtifactId(artifactId);
						dependency2.setScope(scope);
						dependency2.setVersion(version);
						dependency2.setType(type);
						// project.getOriginalModel().getDependencies().add(dependency2);

						project.getDependencyArtifacts().add(artifactFactory.createArtifact(groupId, artifactId, version, scope, type));
					}
				}
			}
		}

		// project.getDependencyArtifacts().add(artifactFactory.createArtifact("redis.clients", "jedis", "2.5.1", "compile", "jar"));

	}
}
