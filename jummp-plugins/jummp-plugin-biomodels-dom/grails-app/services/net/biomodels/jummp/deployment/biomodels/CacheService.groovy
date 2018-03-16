package net.biomodels.jummp.deployment.biomodels

import net.biomodels.jummp.models.JummpEntry
import net.biomodels.jummp.utils.FileUtils
import net.biomodels.jummp.utils.TimeUtils
import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode

import javax.annotation.PostConstruct
import java.nio.file.Files

/**
 * Cache data service
 * Please keep in mind that this cache will save into both memory (RAM) and Storage Disk
 * Because of improving speed
 * So, only caching a lightweight data, and caching when really needed
 */
@Scope(value = "application", proxyMode = ScopedProxyMode.TARGET_CLASS)
class CacheService {
    static transactional = false

    //Todo: Apply soft reference
    private Map<String, JummpEntry<Long, Serializable>> cached = new HashMap<>()

    def grailsApplication

    @PostConstruct
    def init() {
        reloadCachedFiles(getCacheDir())
    }

    /**
     * Get cache dir path from config then create File object from it
     * @return
     */
    File getCacheDir() {
        String cacheDirString = grailsApplication.config.jummp.cache.dir
        return new File(cacheDirString)
    }

    /**
     * Reload all cached files from cacheDir
     * @param cacheDir
     */
    void reloadCachedFiles(File cacheDir) {
        for (final File fileEntry : cacheDir.listFiles()) {
            if (!fileEntry.isDirectory()) {
                JummpEntry<Long, Serializable> cache =
                    FileUtils.loadObjectFromFile(fileEntry, JummpEntry.class) as JummpEntry<Long, Serializable>
                cached.put(fileEntry.getName(), cache)
            }
        }
    }

    /**
     * Check whether we have cache for the given name or not
     * We also check if the time of cache was expired then remove it
     *
     * @param name
     * @return
     */
    boolean hasCache(String name) {
        if (cached.containsKey(name)) {
            if (cached.get(name).getKey() > TimeUtils.currentTimestamp) {
                return true
            }
            removeCache(name)
        }
        return false
    }

    void removeCache(String name) {
        if (cached.containsKey(name)) {
            cached.remove(name)
            File cache = new File(getCacheDir(), name)
            Files.deleteIfExists(cache.toPath())
        }
    }

    /**
     * Get back cache object
     * @param name
     * @return
     */
    Serializable getCache(String name) {
        return cached.get(name).getValue();
    }

    /**
     * Set a new cache
     * @param name key
     * @param value
     * @param expired number of second from now when the cache will be expire
     */
    void setCache(String name, Serializable value , int expired) {
        if (hasCache(name)) {
            cached.get(name).setValue(value);
        } else {
            cached.put(name, new JummpEntry<Long, Serializable>(TimeUtils.currentTimestamp + expired, value))
        }
        FileUtils.writeObjectToFile(new File(getCacheDir(), name), cached.get(name))
    }
}
