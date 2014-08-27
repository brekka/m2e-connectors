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
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.bitstrings.eclipse.m2e.common.BuildHelper;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.sonatype.plexus.build.incremental.BuildContext;

public class XmlBeansBuildParticipant extends MojoExecutionBuildParticipant {
    private IMavenProjectFacade projectFacade;

    public XmlBeansBuildParticipant(IMavenProjectFacade projectFacade, MojoExecution execution) {
        super(execution, true);
        this.projectFacade = projectFacade;
    }

    @Override
    public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
        final IMaven maven = MavenPlugin.getMaven();
        final BuildContext buildContext = getBuildContext();
        final MavenSession mavenSession = getSession();
        final MojoExecution mojoExecution = getMojoExecution();

        if (Arrays.asList(
                BuildHelper.getModifiedFiles(mavenSession, mojoExecution, maven, buildContext, "schemaDirectory"))
                .isEmpty()) {
            return null;
        }

        final Set<IProject> result = super.build(kind, monitor);

        List<String> refresh = Arrays.asList("sourceGenerationDirectory", "classGenerationDirectory", "staleFile");
        for (String param : refresh) {
            File location = maven.getMojoParameterValue(projectFacade.getMavenProject(), getMojoExecution(), param,
                    File.class, null);
            if (location != null) {
                buildContext.refresh(location);
            }
        }

        return result;
    }
}
