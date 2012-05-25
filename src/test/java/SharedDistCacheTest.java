import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created with IntelliJ IDEA.
 * User: torben
 * Date: 25.05.12
 * Time: 11:07
 * To change this template use File | Settings | File Templates.
 */
public class SharedDistCacheTest extends Arquillian {

    @EJB
    private CacheService service;

    @Resource(mappedName = "java:jboss/datasources/PGDS")
    private DataSource dataSource;

    @Deployment(name = "jboss1")
    @TargetsContainer("jboss1")
    public static JavaArchive createDeploymentJBoss1() {
        return getDeployment();
    }

    @Deployment(name = "jboss2")
    @TargetsContainer("jboss2")
    public static JavaArchive createDeploymentJBoss2() {
        return getDeployment();
    }

    private static JavaArchive getDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "test.jar")
              .addClass(CacheService.class)
              .addAsManifestResource("MANIFEST.MF");
    }

    @Test(groups = "init")
    @OperateOnDeployment("jboss1")
    public void clearDatabase() throws SQLException {
        Connection c = dataSource.getConnection("test", "test");
        Statement stmt = c.createStatement();
        stmt.execute("delete from ispn_entry_mapper where id in ('key1', 'key2')");
    }

    @Test(groups = "first", dependsOnGroups = "init")
    @OperateOnDeployment("jboss1")
    public void persistJBoss1() throws InterruptedException, SQLException {
        service.put("key1", "v1");

        Assert.assertEquals("v1", service.get("key1"));
        String key1 = getDatabaseValue("key1");
        Assert.assertNotNull(key1, "key1 was not persisted");
        Assert.assertTrue(key1.contains("v1"));

    }

    @Test(groups = "first", dependsOnGroups = "init")
    @OperateOnDeployment("jboss2")
    public void persistJBoss2() throws InterruptedException, SQLException {
        service.put("key2", "v2");

        Assert.assertEquals("v2", service.get("key2"));
        String key2 = getDatabaseValue("key2");
        Assert.assertNotNull(key2, "key2 was not persisted");
        Assert.assertTrue(key2.contains("v2"));

    }

    @Test(groups = "second", dependsOnGroups = "first")
    @OperateOnDeployment("jboss1")
    public void changeKeyOnJBoss1() throws SQLException, InterruptedException {
        service.put("key1", "mv1");

        Assert.assertEquals("mv1", service.get("key1"));
        String key1 = getDatabaseValue("key1");
        Assert.assertNotNull(key1, "key1 was not persisted");
        Assert.assertTrue(key1.contains("mv1"), "value not persisted in database: " + key1);
    }

    @Test(groups = "second", dependsOnGroups = "first")
    @OperateOnDeployment("jboss2")
    public void changeKeyOnJBoss2() throws SQLException, InterruptedException {
        service.put("key1", "mv2");

        Assert.assertEquals("mv2", service.get("key1"));
        String key1 = getDatabaseValue("key1");
        Assert.assertNotNull(key1, "key1 was not persisted");
        Assert.assertTrue(key1.contains("mv2"), "value not persisted in database: " + key1);
    }

    private String getDatabaseValue(String key) throws SQLException, InterruptedException {
        Assert.assertNotNull(dataSource);

        // allow distribution + persisting
        Thread.sleep(2000);

        Connection c = dataSource.getConnection("test", "test");
        Statement stmt = c.createStatement();
        ResultSet rs = stmt.executeQuery("select data from ispn_entry_mapper where id = '" + key + "'");

        if (!rs.next()) {
            return null;
        }

        String result = new String(rs.getBytes("data"));
        rs.close();
        return result;
    }
}
