/*
 *
 *    Copyright (c) 2011 bitstrings.org - Pino Silvaggio
 *
 *    All rights reserved. This program and the accompanying materials
 *    are made available under the terms of the Eclipse Public License v1.0
 *    which accompanies this distribution, and is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.bitstrings.eclipse.m2e.connectors.xmlbeans;

import java.io.File;

import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.jdt.AbstractSourcesGenerationProjectConfigurator;
import org.eclipse.m2e.jdt.IClasspathDescriptor;


public class XmlBeansProjectConfigurator extends AbstractSourcesGenerationProjectConfigurator {
    @Override
    protected String getOutputFolderParameterName() {
        return "sourceGenerationDirectory";
    }

    @Override
    public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
        super.configure(request, monitor);
        for (MojoExecution mojoExecution : getMojoExecutions(request, monitor)) {
            Object configuration = mojoExecution.getPlugin().getConfiguration();
            Xpp3Dom conf = (Xpp3Dom) configuration;
            // Force the noJavac option to 'true' Eclipse will compile the code
            // so no point having XMLBeans do it.
            Xpp3Dom noJavac = conf.getChild("noJavac");
            if (noJavac == null) {
                noJavac = new Xpp3Dom("noJavac");
                conf.addChild(noJavac);
            }
            noJavac.setValue(Boolean.TRUE.toString());
        }
    }

    /**
     * Override the default, we want more control of the
     * classpath.addSourceEntry operation.
     */
    @Override
    protected File[] getSourceFolders(ProjectConfigurationRequest request, MojoExecution mojoExecution,
            IProgressMonitor monitor) throws CoreException {
        return new File[0];
    }

    /**
     * Configure a source folder for both the generated sources and the
     * generated resources that go in the 'class generation' directory (the all
     * important schema information).
     */
    @Override
    public void configureRawClasspath(ProjectConfigurationRequest request, IClasspathDescriptor classpath,
            IProgressMonitor monitor) throws CoreException {
        // Keep the same behaviour as the original.
        super.configureRawClasspath(request, classpath, monitor);

        IMavenProjectFacade facade = request.getMavenProjectFacade();
        for (MojoExecution mojoExecution : getMojoExecutions(request, monitor)) {
            File sourceDir = getParameterValue(request.getMavenProject(), getOutputFolderParameterName(), File.class,
                    mojoExecution, monitor);
            IPath sourcePath = getFullPath(facade, sourceDir);
            if (sourcePath != null) {
                classpath.addSourceEntry(sourcePath, facade.getOutputLocation(), true);
            }

            File genClassesDir = getParameterValue(request.getMavenProject(), "classGenerationDirectory", File.class,
                    mojoExecution, monitor);
            IPath genClassesPath = getFullPath(facade, genClassesDir);
            if (genClassesPath != null) {
                Path include = new Path("schemaorg_apache_xmlbeans/**");
                classpath.addSourceEntry(genClassesPath, facade.getOutputLocation(), new IPath[] { include }, null,
                        true);
            }
        }
    }

    @Override
    public AbstractBuildParticipant getBuildParticipant(IMavenProjectFacade projectFacade, MojoExecution execution,
            IPluginExecutionMetadata executionMetadata) {
        return new XmlBeansBuildParticipant(projectFacade, execution);
    }
}
