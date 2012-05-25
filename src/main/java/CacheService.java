import org.infinispan.Cache;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

/**
 * Created with IntelliJ IDEA.
 * User: torben
 * Date: 25.05.12
 * Time: 11:09
 * To change this template use File | Settings | File Templates.
 */
@Stateless
public class CacheService {
    @Produces
    @Dependent
    @Resource(lookup = "java:jboss/infinispan/cache/my-cache/mapper")
    private static Cache<String, String> cache;

    public void put(String key, String value) {
        System.out.println("*** inserting " + key + "/" + value + " using " + System.getProperty("jboss.node.name"));
        cache.put(key, value);
    }

    public String get(String key) {
        System.out.println("*** retrieving " + key  + " using " + System.getProperty("jboss.node.name"));
        return cache.get(key);
    }
}
