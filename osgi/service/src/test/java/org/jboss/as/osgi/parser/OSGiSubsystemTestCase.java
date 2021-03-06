/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.as.osgi.parser;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.subsystem.test.LegacyKernelServicesInitializer;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test the subsystem parser
 *
 * @author Thomas.Diesler@jboss.com
 * @author David Bosschaert
 */
public class OSGiSubsystemTestCase extends AbstractSubsystemBaseTest {

    private static final String SUBSYSTEM_XML_1_2 =
        "<subsystem xmlns='urn:jboss:domain:osgi:1.2' activation='lazy'>" +
        "  <!-- Some Comment -->" +
        "  <properties>" +
        "    <property name='prop1'>val1</property>" +
        "    <property name='prop2'>" +
        "       val2a," +
        "       val2b," +
        "    </property>" +
        "  </properties>" +
        "  <capabilities>" +
        "    <capability name='org.acme.module2' startlevel='1'/>" +
        "    <capability name='org.acme.module1'/>" +
        "  </capabilities>" +
        "</subsystem>";

    private static final String SUBSYSTEM_XML_1_2_EXPRESSIONS =
        "<subsystem xmlns='urn:jboss:domain:osgi:1.2' activation='${test.exp:lazy}'>" +
        "  <!-- Some Comment -->" +
        "  <properties>" +
        "    <property name='prop1'>${test.exp:val1}</property>" +
        "    <property name='prop2'>" +
        "       ${test.exp:val2a}," +
        "       ${test.exp:val2b}," +
        "    </property>" +
        "  </properties>" +
        "  <capabilities>" +
        "    <capability name='org.acme.module2' startlevel='${test.exp:1}'/>" +
        "    <capability name='org.acme.module1'/>" +
        "  </capabilities>" +
        "</subsystem>";

    private static final String SUBSYSTEM_XML_1_1 =
        "<subsystem xmlns='urn:jboss:domain:osgi:1.1' activation='lazy'>" +
        "  <!-- Some Comment -->" +
        "  <configuration pid='Pid1'>" +
        "    <property name='org.acme.key1' value='val 1'/>" +
        "  </configuration>" +
        "  <configuration pid='Pid2'>" +
        "    <property name='propname' value='propval'/>" +
        "  </configuration>" +
        "  <properties>" +
        "    <property name='prop1'>val1</property>" +
        "    <property name='prop2'>" +
        "       val2a," +
        "       val2b," +
        "    </property>" +
        "  </properties>" +
        "  <capabilities>" +
        "    <capability name='org.acme.module2' startlevel='1'/>" +
        "    <capability name='org.acme.module1'/>" +
        "  </capabilities>" +
        "</subsystem>";

    private static final String SUBSYSTEM_XML_1_0 =
            "<subsystem xmlns='urn:jboss:domain:osgi:1.0' activation='lazy'>" +
            "  <!-- Some Comment -->" +
            "  <configuration pid='Pid1'>" +
            "    <property name='org.acme.key1'>val 1</property>" +
            "  </configuration>" +
            "  <configuration pid='Pid2'>" +
            "    <property name='propname'>propval</property>" +
            "  </configuration>" +
            "  <properties>" +
            "    <property name='prop1'>val1</property>" +
            "    <property name='prop2'>" +
            "       val2a," +
            "       val2b," +
            "    </property>" +
            "  </properties>" +
            "  <modules>" +
            "    <module identifier='org.acme.module2' startlevel='1'/>" +
            "    <module identifier='org.acme.module1'/>" +
            "  </modules>" +
            "</subsystem>";

    public OSGiSubsystemTestCase() {
        super(OSGiExtension.SUBSYSTEM_NAME, new OSGiExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return SUBSYSTEM_XML_1_2_EXPRESSIONS;
    }

    @Test
    public void testParseEmptySubsystem() throws Exception {
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
                "</subsystem>";
        List<ModelNode> operations = parse(subsystemXml);

        // Check that we have the expected number of operations
        Assert.assertEquals(1, operations.size());

        // Check that each operation has the correct content
        ModelNode addSubsystem = operations.get(0);
        Assert.assertEquals(ModelDescriptionConstants.ADD, addSubsystem.get(ModelDescriptionConstants.OP).asString());
        PathAddress addr = PathAddress.pathAddress(addSubsystem.get(ModelDescriptionConstants.OP_ADDR));
        Assert.assertEquals(1, addr.size());
        PathElement element = addr.getElement(0);
        Assert.assertEquals(ModelDescriptionConstants.SUBSYSTEM, element.getKey());
        Assert.assertEquals(OSGiExtension.SUBSYSTEM_NAME, element.getValue());
    }

    @Test
    public void testParseSubsystemWithProperties() throws Exception {
        String subsystemXml =
            "<subsystem xmlns='urn:jboss:domain:osgi:1.2' activation='eager'>" +
            "  <properties>" +
            "    <property name='org.acme.myProperty'>" +
            "      hi ho" +
            "    </property>" +
            "    <property name='org.acme.myProperty2'>" +
            "      hi.ho" +
            "    </property>" +
            "  </properties>" +
            "</subsystem>";

        List<ModelNode> operations = parse(subsystemXml);
        Assert.assertEquals(3, operations.size());

        ModelNode addSubsystem = operations.get(0);
        Assert.assertEquals(ModelDescriptionConstants.ADD, addSubsystem.get(ModelDescriptionConstants.OP).asString());
        assertOSGiSubsystemAddress(addSubsystem.get(ModelDescriptionConstants.OP_ADDR));
        Assert.assertEquals("eager", addSubsystem.get(ModelConstants.ACTIVATION).asString());

        checkData(operations, 1, ModelConstants.PROPERTY, "org.acme.myProperty", ModelConstants.VALUE, "hi ho");
        checkData(operations, 2, ModelConstants.PROPERTY, "org.acme.myProperty2", ModelConstants.VALUE, "hi.ho");
    }

    @Test
    public void testParseSubsystemWithCapabilities() throws Exception {
        String subsystemXml =
            "<subsystem xmlns='urn:jboss:domain:osgi:1.2' activation='lazy'>" +
            "  <capabilities>" +
            "    <capability name='org.acme.module1'/>" +
            "    <capability name='org.acme.module2' startlevel='1'/>" +
            "  </capabilities>" +
            "</subsystem>";

        List<ModelNode> operations = parse(subsystemXml);
        Assert.assertEquals(3, operations.size());

        ModelNode addSubsystem = operations.get(0);
        Assert.assertEquals(ModelDescriptionConstants.ADD, addSubsystem.get(ModelDescriptionConstants.OP).asString());
        assertOSGiSubsystemAddress(addSubsystem.get(ModelDescriptionConstants.OP_ADDR));

        checkData(operations, 1, ModelConstants.CAPABILITY, "org.acme.module1",  ModelConstants.STARTLEVEL, "undefined");
        checkData(operations, 2, ModelConstants.CAPABILITY, "org.acme.module2", ModelConstants.STARTLEVEL, "1");
    }

    @Test
    public void testReadWriteEmptySubsystem() throws Exception {
        String subsystemXml =
            "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
            "</subsystem>";

        ModelNode testModel = new ModelNode();
        testModel.get(ModelDescriptionConstants.SUBSYSTEM, OSGiExtension.SUBSYSTEM_NAME).setEmptyObject();
        String triggered = outputModel(testModel);
        Assert.assertEquals(normalizeXML(subsystemXml), normalizeXML(triggered));
    }

    @Test
    public void testReadWriteNamespace10() throws Exception {
        KernelServices services = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(SUBSYSTEM_XML_1_0)
                .build();
        ModelNode model = services.readWholeModel();

        String marshalled = outputModel(model);
        Assert.assertEquals(normalizeXML(SUBSYSTEM_XML_1_2), normalizeXML(marshalled));
    }

    @Test
    public void testReadWriteNamespace11() throws Exception {
        KernelServices services = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(SUBSYSTEM_XML_1_1)
                .build();
        ModelNode model = services.readWholeModel();

        String marshalled = outputModel(model);
        Assert.assertEquals(normalizeXML(SUBSYSTEM_XML_1_2), normalizeXML(marshalled));
    }

    @Test
    public void testDescribeHandler() throws Exception {
        KernelServices servicesA = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(SUBSYSTEM_XML_1_2)
                .build();
        ModelNode modelA = servicesA.readWholeModel();
        ModelNode describeOp = new ModelNode();
        describeOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.DESCRIBE);
        describeOp.get(ModelDescriptionConstants.OP_ADDR).set(
                PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, OSGiExtension.SUBSYSTEM_NAME)).toModelNode());
        List<ModelNode> operations = checkResultAndGetContents(servicesA.executeOperation(describeOp)).asList();

        KernelServices servicesB = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setBootOperations(operations)
                .build();
        ModelNode modelB = servicesB.readWholeModel();

        compare(modelA, modelB);
    }
    @Test
    public void testTransformerAS712() throws Exception {
        testTransformers1_0_0(ModelTestControllerVersion.V7_1_2_FINAL, "org.jboss.as:jboss-as-osgi-service:7.1.2.Final");
    }

    @Test
    public void testTransformerAS713() throws Exception {
        testTransformers1_0_0(ModelTestControllerVersion.V7_1_3_FINAL, "org.jboss.as:jboss-as-osgi-service:7.1.3.Final", "org.jboss.osgi.framework:jbosgi-framework-core:1.3.1.CR1");
    }

    private void testTransformers1_0_0(ModelTestControllerVersion controllerVersion, String... mavenGAVs) throws Exception {
        ModelVersion modelVersion = ModelVersion.create(1, 0, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(SUBSYSTEM_XML_1_2);

        LegacyKernelServicesInitializer legacyInitializer = builder.createLegacyKernelServicesBuilder(AdditionalInitialization.MANAGEMENT, controllerVersion, modelVersion);
        for (String mavenGAV : mavenGAVs) {
            legacyInitializer.addMavenResourceURL(mavenGAV);
        }

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(mainServices.getLegacyServices(modelVersion).isSuccessfulBoot());

        ModelNode current = mainServices.readWholeModel();
        ModelNode legacy = mainServices.getLegacyServices(modelVersion).readWholeModel();

        checkSubsystemModelTransformation(mainServices, modelVersion);
        //Check that start level was removed
        ModelNode currentModule1 = current.get(ModelDescriptionConstants.SUBSYSTEM, getMainSubsystemName(), ModelConstants.CAPABILITY, "org.acme.module1");
        Assert.assertTrue(currentModule1.isDefined());
        Assert.assertTrue(currentModule1.has(ModelConstants.STARTLEVEL));
        Assert.assertFalse(currentModule1.hasDefined(ModelConstants.STARTLEVEL));

        ModelNode legacyModule1 = legacy.get(ModelDescriptionConstants.SUBSYSTEM, getMainSubsystemName(), ModelConstants.CAPABILITY, "org.acme.module1");
        Assert.assertTrue(legacyModule1.isDefined());
        Assert.assertFalse(legacyModule1.has(ModelConstants.STARTLEVEL));

        ModelNode transformedModule1 = mainServices.readTransformedModel(modelVersion).get(SUBSYSTEM, getMainSubsystemName(), ModelConstants.CAPABILITY, "org.acme.module1");
        Assert.assertTrue(transformedModule1.isDefined());
        Assert.assertFalse(legacyModule1.has(ModelConstants.STARTLEVEL));
    }

    @Test
    public void testRejectExpressionsAS712() throws Exception {
        testRejectExpressions1_0_0(ModelTestControllerVersion.V7_1_2_FINAL);
    }

    @Test
    public void testRejectExpressionsAS713() throws Exception {
        testRejectExpressions1_0_0(ModelTestControllerVersion.V7_1_3_FINAL);
    }

    private void testRejectExpressions1_0_0(ModelTestControllerVersion controllerVersion) throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());

        // create builder for legacy subsystem version
        ModelVersion version_1_0_0 = ModelVersion.create(1, 0, 0);
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, version_1_0_0)
                .addMavenResourceURL("org.jboss.as:jboss-as-osgi-service:" + controllerVersion.getMavenGavVersion());

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(version_1_0_0);

        org.junit.Assert.assertNotNull(legacyServices);
        org.junit.Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        org.junit.Assert.assertTrue(legacyServices.isSuccessfulBoot());

        List<ModelNode> xmlOps = builder.parseXml(SUBSYSTEM_XML_1_2_EXPRESSIONS);

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, version_1_0_0, xmlOps,
                new FailedOperationTransformationConfig()
                        .addFailedAttribute(PathAddress.pathAddress(OSGiRootResource.SUBSYSTEM_PATH),
                                new FailedOperationTransformationConfig.RejectExpressionsConfig(OSGiRootResource.ACTIVATION))
                        .addFailedAttribute(PathAddress.pathAddress(OSGiRootResource.SUBSYSTEM_PATH, FrameworkPropertyResource.PROPERTY_PATH),
                                new FailedOperationTransformationConfig.RejectExpressionsConfig(FrameworkPropertyResource.VALUE))
                        .addFailedAttribute(PathAddress.pathAddress(OSGiRootResource.SUBSYSTEM_PATH, FrameworkCapabilityResource.CAPABILITY_PATH),
                                new FailedOperationTransformationConfig.RejectExpressionsConfig(FrameworkCapabilityResource.STARTLEVEL)
                                        .setReadOnly(FrameworkCapabilityResource.STARTLEVEL))
        );
    }

    private void assertOSGiSubsystemAddress(ModelNode address) {
        PathAddress addr = PathAddress.pathAddress(address);
        PathElement element = addr.getElement(0);
        Assert.assertEquals(ModelDescriptionConstants.SUBSYSTEM, element.getKey());
        Assert.assertEquals(OSGiExtension.SUBSYSTEM_NAME, element.getValue());
    }

    private void checkData(List<ModelNode> operations, int idx, String addrKey, String addrVal, String valKey, String value) {
        ModelNode node = operations.get(idx);
        Assert.assertEquals(ModelDescriptionConstants.ADD, node.get(ModelDescriptionConstants.OP).asString());
        ModelNode address = node.get(ModelDescriptionConstants.OP_ADDR);
        assertOSGiSubsystemAddress(address);
        PathAddress pa = PathAddress.pathAddress(address);
        PathElement pe = pa.getElement(1);
        Assert.assertEquals(addrKey, pe.getKey());
        Assert.assertEquals(addrVal, pe.getValue());
        Assert.assertEquals(value, node.get(valKey).asString());
    }

    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.MANAGEMENT;
    }
}
