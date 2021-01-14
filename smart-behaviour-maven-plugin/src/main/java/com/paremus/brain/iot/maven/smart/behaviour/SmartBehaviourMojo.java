/*******************************************************************************
 * Copyright (C) 2021 Paremus Ltd
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package com.paremus.brain.iot.maven.smart.behaviour;
import static aQute.bnd.osgi.Constants.RUNBUNDLES;
import static aQute.bnd.osgi.Constants.RUNREQUIRES;
import static aQute.bnd.osgi.resource.CapReqBuilder.getRequirementsFrom;
import static aQute.bnd.osgi.resource.ResourceUtils.getIdentity;
import static eu.brain.iot.behaviour.namespace.SmartBehaviourManifest.BRAIN_IOT_DEPLOY_REQUIREMENTS;
import static eu.brain.iot.behaviour.namespace.SmartBehaviourManifest.BRAIN_IOT_DEPLOY_RESOURCES;
import static eu.brain.iot.behaviour.namespace.SmartBehaviourManifest.BRAIN_IOT_SMART_BEHEAVIOUR_SYMBOLIC_NAME;
import static eu.brain.iot.behaviour.namespace.SmartBehaviourManifest.BRAIN_IOT_SMART_BEHEAVIOUR_VERSION;
import static eu.brain.iot.behaviour.namespace.SmartBehaviourManifest.SMART_BEHAVIOUR_MAVEN_CLASSIFIER;
import static eu.brain.iot.behaviour.namespace.SmartBehaviourNamespace.SMART_BEHAVIOUR_NAMESPACE;
import static java.util.stream.Collectors.joining;
import static org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE;
import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.twdata.maven.mojoexecutor.MojoExecutor.Element;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.maven.lib.resolve.BndrunContainer;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.osgi.repository.SimpleIndexer;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.RequirementBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.version.MavenVersion;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;

@Mojo(name = "smart-behaviour", defaultPhase=LifecyclePhase.PACKAGE, requiresDependencyResolution=ResolutionScope.TEST)
public class SmartBehaviourMojo extends AbstractMojo {
	
	private static final Logger logger = LoggerFactory.getLogger(SmartBehaviourMojo.class);
	
	/**
	 * The name that should be used to represent this Smart Behaviour
	 */
	@Parameter(defaultValue="${project.groupId}.${project.artifactId}", required=false)
	private String name;

	/**
	 * The name that should be used to represent this Smart Behaviour
	 */
	@Parameter(defaultValue="${project.version}", required=false)
	private String version;
	
	/**
	 * Gather this project's compile and runtime dependencies into the target folder
	 */
	@Parameter(defaultValue="true", required=false)
	private boolean gatherDependencies = true;

	/**
	 * Gather this project's output jar into the target folder, only relevant when 
	 * <code>gatherDependencies</code> is <code>true</code>
	 */
	@Parameter(defaultValue="true", required=false)
	private boolean gatherOutputJar = true;
	
	/**
	 * The folder to use for building the artifact based on the gathered dependencies. 
	 * If <code>gatherDependencies</code> is <code>false</code> then this folder must
	 * be populated in some other way. 
	 */
	@Parameter(defaultValue="${project.build.directory}/smart-behaviour", required=false)
	private File targetFolder;

	/**
	 * A bndrun file to use as a source of initial requirements or a list of bundles to
	 * deploy. If no bndrun is supplied then the initial requirement will be
	 * Require-Capability: eu.brain.iot.behaviour;filter:="(consumed=*)"
	 */
	@Parameter(required=false)
	private File bndrun;
	
	/**
	 * If set to true then the run bundles from the bndrun file will be used rather than 
	 * the run requirements. Does nothing if no bndrun file is configured.
	 */
	@Parameter(required=false, defaultValue="false")
	private boolean useRunBundles = false;

	@Parameter( defaultValue = "${project}", readonly = true )
	private MavenProject mavenProject;

	@Parameter( defaultValue = "${session}", readonly = true )
	private MavenSession mavenSession;

	@Component
	private BuildPluginManager pluginManager;
	
	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
	private RepositorySystemSession repositorySession;
	
	@Component
	private ProjectDependenciesResolver resolver;
	
	@Component
	@SuppressWarnings("deprecation")
	private org.apache.maven.artifact.factory.ArtifactFactory	artifactFactory;
	
	@Component
	private RepositorySystem system;
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		
		// Check the symbolic name syntax
		if(!Verifier.SYMBOLICNAME.matcher(name).matches()) {
			throw new MojoExecutionException("The symbolic name " + name + " does not fit the required OSGi syntax");
		}
		
		// Check the version syntax
		try {
			version = new MavenVersion(version).getOSGiVersion().toString();
		} catch (Exception e) {
			throw new MojoExecutionException("The version " + version + " does not fit the required OSGi syntax");
		}
		
		targetFolder.mkdirs();
		
		if(gatherDependencies) {
			logger.info("Gathering dependencies");
			copyDependencies();
			
			if(gatherOutputJar) {
				copyOutputJar();
			}
		} else {
			logger.info("Skipping dependency collection due to plugin configuration");
		}
		
		Map<String, String> smartBehaviourProps;
		
		if(bndrun != null) {
			smartBehaviourProps = processBndrun();
		} else {
			smartBehaviourProps = new TreeMap<>();
			smartBehaviourProps.put(BRAIN_IOT_DEPLOY_REQUIREMENTS, inferRequirements());
		}
		
		generateJar(smartBehaviourProps);
	}

	private void copyDependencies() throws MojoExecutionException {
		executeMojo(
			    plugin(
			        groupId("org.apache.maven.plugins"),
			        artifactId("maven-dependency-plugin"),
			        version("3.1.1")
			    ),
			    goal("copy-dependencies"),
			    configuration(
			        element(name("outputDirectory"), targetFolder.getAbsolutePath()),
			        element(name("includeScope"), "runtime")
			    ),
			    executionEnvironment(
			        mavenProject,
			        mavenSession,
			        pluginManager
			    )
			);
	}

	private void copyOutputJar() throws MojoExecutionException {
		File file = mavenProject.getArtifact().getFile();
		if(file == null || !Files.exists(file.toPath())) {
			throw new MojoExecutionException("The output jar does not exist for " + mavenProject);
		}
		
		Path outputJar = file.toPath();
		
		try {
			Files.copy(outputJar, targetFolder.toPath().resolve(outputJar.getFileName()));
		} catch (IOException e) {
			throw new MojoExecutionException("Failed to copy the output jar " + outputJar, e);
		}
	}

	private Map<String, String> processBndrun() throws MojoExecutionException {
		logger.info("Processing {} for dependencies", bndrun);
		
		Map<String, String> smartBehaviourProps = new TreeMap<>();
		
		BndrunContainer container = new BndrunContainer.Builder(mavenProject, mavenSession, 
				repositorySession, resolver, artifactFactory, system)
					.build();
		
		if(useRunBundles) {
			try {
				container.execute(bndrun, "getRunBundles", 
						new File(mavenProject.getBuild().getDirectory()), 
						(a,b,c) -> {
							smartBehaviourProps.put(BRAIN_IOT_DEPLOY_RESOURCES, c.get(RUNBUNDLES));
							return 0;
						});
			} catch (Exception e) {
				throw new MojoExecutionException("Failed to look up the run bundles", e);
			}
		} else {
			try {
				container.execute(bndrun, "getRunBundles", 
						new File(mavenProject.getBuild().getDirectory()), 
						(a,b,c) -> {
							String runreqs = getRequirementsFrom(new Parameters(c.get(RUNREQUIRES))).stream()
								.map(r -> { 
										try {
											return ResourceUtils.toRequireCapability(r);
										} catch (Exception e) {
											throw new RuntimeException("Failed to process the run requirements from " + bndrun, e);
										}
									})
								.collect(joining(","));
							smartBehaviourProps.put(BRAIN_IOT_DEPLOY_REQUIREMENTS, runreqs);
							return 0;
						});
			} catch (Exception e) {
				throw new MojoExecutionException("Failed to look up the run requirements", e);
			}
		}
		return smartBehaviourProps;
	}

	private void generateJar(Map<String, String> smartBehaviourProps) throws MojoExecutionException {

		smartBehaviourProps.put(BRAIN_IOT_SMART_BEHEAVIOUR_SYMBOLIC_NAME, name);
		smartBehaviourProps.put(BRAIN_IOT_SMART_BEHEAVIOUR_VERSION, version);
		
		Element[] manifestEntries = smartBehaviourProps.entrySet().stream()
			.map(e -> element(name(e.getKey()), e.getValue()))
			.toArray(Element[]::new);
		
		executeMojo(
				plugin(
						groupId("org.apache.maven.plugins"),
						artifactId("maven-jar-plugin"),
						version("3.1.1")
						),
				goal("jar"),
				configuration(
						element(name("classesDirectory"), targetFolder.getAbsolutePath()),
						element(name("outputDirectory"), mavenProject.getBuild().getDirectory()),
						element(name("classifier"), SMART_BEHAVIOUR_MAVEN_CLASSIFIER),
						element(name("archive"),
								element(name("manifestEntries"), manifestEntries))
						),
				executionEnvironment(
						mavenProject,
						mavenSession,
						pluginManager
						)
				);
	}

	private String inferRequirements() throws MojoExecutionException {
		List<String> requirements = new ArrayList<>();
		
		try {
			for(Path p : Files.newDirectoryStream(targetFolder.toPath())) {
				SimpleIndexer indexer = new SimpleIndexer();
				indexer.files(Collections.singleton(p.toFile()));
				
				try {
					for (Resource resource : indexer.getResources()) {
						if(!resource.getCapabilities(SMART_BEHAVIOUR_NAMESPACE).isEmpty()) {
							List<Capability> capabilities = resource.getCapabilities(IDENTITY_NAMESPACE);
							if(!capabilities.isEmpty()) {
								Capability idCap = capabilities.get(0);
								
								RequirementBuilder rb = new RequirementBuilder(IDENTITY_NAMESPACE);
								
								Version version = ResourceUtils.getVersion(idCap);
								
								Attrs attrs = CapReqBuilder.clone(idCap).toAttrs();
								
								rb.addFilter(IDENTITY_NAMESPACE, getIdentity(idCap), 
										new VersionRange(version, version).toString(), attrs);
								
								requirements.add(ResourceUtils.toRequireCapability(
										rb.buildSyntheticRequirement()));
							}
						}
					}
				} catch (Exception e) {
					logger.warn("Failed to index the resource at {} when determinining the deployment requirements", p, e);
				}
			}
		} catch (Exception e) {
			throw new MojoExecutionException("A serious error occurred trying to determing the set of initial requirements", e);
		}
		
		if(requirements.isEmpty()) {
			throw new MojoExecutionException("No bundles providing smart behaviours could be found in " + targetFolder);
		}
		String requirement = requirements.stream().collect(joining(","));
		return requirement;
	}
}
