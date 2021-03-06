/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.ee.structure;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.filter.PathFilters;

import static org.jboss.as.ee.subsystem.GlobalModulesDefinition.ANNOTATIONS;
import static org.jboss.as.ee.subsystem.GlobalModulesDefinition.META_INF;
import static org.jboss.as.ee.subsystem.GlobalModulesDefinition.NAME;
import static org.jboss.as.ee.subsystem.GlobalModulesDefinition.SERVICES;
import static org.jboss.as.ee.subsystem.GlobalModulesDefinition.SLOT;

/**
 * Dependency processor that adds modules defined in the global-modules section of
 * the configuration to all deployments.
 *
 * @author Stuart Douglas
 */
public class GlobalModuleDependencyProcessor implements DeploymentUnitProcessor {

    private volatile ModelNode globalModules = new ModelNode();

    public GlobalModuleDependencyProcessor() {
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);

        final ModelNode globalMods = this.globalModules;

        if (globalMods.isDefined()) {
            for (final ModelNode module : globalMods.asList()) {
                final String name = module.get(NAME).asString();
                boolean annotations = module.get(ANNOTATIONS).asBoolean();
                boolean services = module.get(SERVICES).asBoolean();
                boolean metaInf = module.get(META_INF).asBoolean();

                String slot = module.get(SLOT).asString();
                final ModuleIdentifier identifier = ModuleIdentifier.create(name, slot);
                final ModuleDependency dependency = new ModuleDependency(Module.getBootModuleLoader(), identifier, false, false, services, false);

                if (metaInf) {
                    dependency.addImportFilter(PathFilters.getMetaInfSubdirectoriesFilter(), true);
                    dependency.addImportFilter(PathFilters.getMetaInfFilter(), true);
                }

                if(annotations) {
                    deploymentUnit.addToAttachmentList(Attachments.ADDITIONAL_ANNOTATION_INDEXES, identifier);
                }

                moduleSpecification.addSystemDependency(dependency);
            }
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }

    /**
     * Set the global modules configuration for the container.
     * @param globalModules a fully resolved (i.e. with expressions resolved and default values set) global modules configuration
     */
    public void setGlobalModules(final ModelNode globalModules) {
        this.globalModules = globalModules;
    }
}
