package net.biomodels.jummp.deployment.biomodels

import net.biomodels.jummp.models.JummpEntry
import net.biomodels.jummp.utils.FileUtils
import net.biomodels.jummp.utils.TimeUtils
import org.apache.commons.lang.NullArgumentException
import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode

import javax.annotation.PostConstruct
import java.lang.ref.SoftReference
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap

/**
 * Cache data service
 * Please keep in mind that this cache will save into both memory (RAM) and Storage Disk
 * Because of improving speed
 * So, only caching a lightweight data, and caching when really needed
 */
@Scope(value = "application", proxyMode = ScopedProxyMode.TARGET_CLASS)
class CacheService {
    static transactional = false

    private Map<String, SoftReference<JummpEntry<Long, ? extends Serializable>>> cached = new ConcurrentHashMap<>()

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
                JummpEntry<Long, ? extends Serializable> cache =
                    FileUtils.loadObjectFromFile(fileEntry, JummpEntry.class) as JummpEntry<Long, ? extends Serializable>
                cached.put(fileEntry.getName(), new SoftReference<>(cache))
            }
        }
    }

    /**
     * Check whether we have cache for the given name or not
     * We also check if the time of cache was expired then remove it
     * Note: We moved this function to private since we can't keep the state of soft reference
     * Call getCache(String name) instead
     *
     * @param name
     * @return
     */
    private boolean hasCache(String name) {
        if (cached.containsKey(name)) {
            if (cached.get(name).get() != null && cached.get(name).get().getKey() > TimeUtils.currentTimestamp) {
                return true
            }
            removeCache(name)
        }
        return false
    }

    synchronized void removeCache(String name) {
        if (cached.containsKey(name)) {
            cached.remove(name)
            File cache = new File(getCacheDir(), name)
            Files.deleteIfExists(cache.toPath())
        }
    }

    /**
     * Get back the cache object
     * Return null if the cache is not exist or expired
     * @param name
     * @return
     */
    def <T extends Serializable> T getCache(String name) {
        if (hasCache(name)) {
            JummpEntry<Long, ? extends Serializable> cache = cached.get(name).get()
            if (cache != null) {
                return cache.value as T
            }
        }
        return null
    }

    /**
     * Set a new cache
     * @param name key non null
     * @param value non null
     * @param expired number of second from now when the cache will be expire
     */
    synchronized void setCache(String name, Serializable value , int expired) {
        if (name == null) {
            throw new NullArgumentException("name")
        }
        if (!hasCache(name)) {
            if (value == null) {
                throw new NullArgumentException("value")
            }
            JummpEntry<Long, ? extends Serializable> cache = new JummpEntry<Long, ? extends Serializable>(
                TimeUtils.currentTimestamp + expired, value)
            cached.put(name, new SoftReference<>(cache))
            FileUtils.writeObjectToFile(new File(getCacheDir(), name), cache)
        }
    }
}
