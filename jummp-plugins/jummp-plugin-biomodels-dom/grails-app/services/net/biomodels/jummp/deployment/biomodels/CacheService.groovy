package net.biomodels.jummp.deployment.biomodels

import net.biomodels.jummp.models.JummpEntry
import net.biomodels.jummp.utils.FileUtils
import net.biomodels.jummp.utils.TimeUtils

import java.nio.file.Files

/**
 * Cache data service
 * Please keep in mind that this cache will save into both memory (RAM) and Storage Disk
 * Because of improving speed
 * So, only caching a lightweight data, and caching when really needed
 */
class CacheService {

    private static Map<String, JummpEntry<Long, Serializable>> cached = new HashMap<>()

    /**
     * Reload all cached files from cacheDir
     * @param cacheDir
     */
    static void loadCachedFiles(File cacheDir) {
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
    static boolean hasCache(String name, File cacheDir) {
        if (cached.containsKey(name)) {
            if (cached.get(name).getKey() > TimeUtils.currentTimestamp) {
                return true
            }
            removeCache(name, cacheDir)
        }
        return false
    }

    static void removeCache(String name, File cacheDir) {
        if (cached.containsKey(name)) {
            cached.remove(name)
            File cache = new File(cacheDir, name)
            Files.deleteIfExists(cache.toPath())
        }
    }

    /**
     * Get back cache object
     * @param name
     * @return
     */
    static Serializable getCache(String name) {
        return cached.get(name).getValue();
    }

    /**
     * Set a new cache
     * @param name key
     * @param value
     * @param expired number of second from now when the cache will be expire
     */
    static void setCache(String name, Serializable value , int expired, File cacheDir) {
        if (hasCache(name, cacheDir)) {
            cached.get(name).setValue(value);
        } else {
            cached.put(name, new JummpEntry<Long, Serializable>(TimeUtils.currentTimestamp + expired, value))
        }
        FileUtils.writeObjectToFile(new File(cacheDir, name), cached.get(name))
    }
}
