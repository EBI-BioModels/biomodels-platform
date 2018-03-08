package net.biomodels.jummp.deployment.biomodels

import grails.util.Holders
import net.biomodels.jummp.models.JummpEntry
import net.biomodels.jummp.utils.TimeUtils

import java.nio.file.Files

class CacheService {

    private Map<String, JummpEntry<Long, Serializable>> cached = new HashMap<>()

    CacheService() {
        reloadCachedFiles(getCacheDir())
    }

    /**
     * Get cache dir path from config then create File object from it
     * @return
     */
    File getCacheDir() {
        String cacheDirString = Holders.grailsApplication.config.jummp.cache.dir
        return new File(cacheDirString)
    }

    /**
     * Reload all cached files from cacheDir
     * @param cacheDir
     */
    void reloadCachedFiles(File cacheDir) {
        for (final File fileEntry : cacheDir.listFiles()) {
            if (!fileEntry.isDirectory()) {
                cached.put(fileEntry.getName(), loadObjectFromFile(fileEntry))
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
                return true;
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
        writeObjectToFile(getCacheDir(), name, cached.get(name))
    }

    /**
     * Write object to file for future recover
     * @param name
     * @param object
     */
    void writeObjectToFile(File cacheDir, String name, Serializable object) {
        FileOutputStream cacheFile = new FileOutputStream(new File(cacheDir, name))
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(cacheFile)
        objectOutputStream.writeObject(object)
        objectOutputStream.close()
        cacheFile.close()
    }

    /**
     * Load object from file
     * @param file
     * @return
     */
    JummpEntry<Long, Serializable> loadObjectFromFile(File file) {
        return new FileInputStream(file).withObjectInputStream(getClass().classLoader) {
            is -> is.readObject() as JummpEntry<Long, Serializable>
        }
    }
}
