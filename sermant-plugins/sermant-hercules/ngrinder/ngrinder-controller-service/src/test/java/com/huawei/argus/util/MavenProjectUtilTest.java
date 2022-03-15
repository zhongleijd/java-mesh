package com.huawei.argus.util;

import org.junit.Assert;
import org.junit.Test;

public class MavenProjectUtilTest {
    @Test
    public void test_get_pom_xml_template() {
        MavenProject.MavenFile pomFile = MavenProject.getPomFile();
        Assert.assertTrue(pomFile.getContent().contains("</project>"));
        Assert.assertEquals("pom.xml", pomFile.getPath());
    }

    @Test
    public void test_get_application_properties_template() {
        MavenProject.MavenFile applicationPropertiesFile = MavenProject.getApplicationPropertiesFile();
        Assert.assertTrue(applicationPropertiesFile.getContent().contains("add config here"));
        Assert.assertEquals("src/main/resources/application.properties", applicationPropertiesFile.getPath());
    }
}
