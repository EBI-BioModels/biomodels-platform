/**
 * Copyright (C) 2010-2016 EMBL-European Bioinformatics Institute (EMBL-EBI),
 * Deutsches Krebsforschungszentrum (DKFZ)
 *
 * This file is part of Jummp.
 *
 * Jummp is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along
 * with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
 */


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
 * @short: Service responsible for caching data
 *
 * Allow concurrent request
 *
 * Please keep in mind that this cache will save data into both memory (RAM) and Storage Disk
 * Because of improving speed, only caching lightweight data when really needed circumstances
 *
 * @author: Vu Tu <tvu@ebi.ac.uk>
 */
@Scope(value = "application", proxyMode = ScopedProxyMode.TARGET_CLASS)
class CacheService {
    static transactional = false

    /**
     * The map that keep the cached object
     */
    private Map<String, SoftReference<JummpEntry<Long, ? extends Serializable>>> cached = new ConcurrentHashMap<>()

    /**
     * Dependency Injection of GrailsApplication
     */
    def grailsApplication

    @PostConstruct
    def init() {
        reloadCachedFiles(getCacheDir())
    }

    /**
     * Get cache dir path from config then create File object from it
     */
    File getCacheDir() {
        String cacheDirString = grailsApplication.config.jummp.cache.dir
        return new File(cacheDirString)
    }

    /**
     * Reload all cached files from cacheDir
     * @param cacheDir: the File that contain path to the cache dir
     */
    void reloadCachedFiles(File cacheDir) {
        for (final File fileEntry : cacheDir.listFiles()) {
            if (!fileEntry.isDirectory()) {
                reloadCachedFile(fileEntry)
            }
        }
    }

    /**
     * Reload a cache that saved in to a file
     * @param file: the cache file
     */
    private synchronized void reloadCachedFile(File file) {
        if (file.isFile()) {
            JummpEntry<Long, ? extends Serializable> cache =
                FileUtils.loadObjectFromFile(file, JummpEntry.class) as JummpEntry<Long, ? extends Serializable>
            cached.put(file.getName(), new SoftReference<>(cache))
        }
    }

    /**
     * Check whether we have cache for the given name or not
     * We also check if the time of cache was expired then remove it
     * Note: We moved this function to private since we can't keep the state of soft reference
     * Call getCache(String name) instead
     *
     * @param name: name of the cache
     * @return
     *          True if we have that cache and not expired
     *          False otherwise
     */
    private boolean hasCache(String name) {
        if (cached.containsKey(name)) {
            if (cached.get(name).get() != null) {
                if (cached.get(name).get().getKey() > TimeUtils.currentTimestamp) {
                    return true
                }
                removeCache(name)
            } else {
                File cacheFile = new File(getCacheDir(), name)
                if (cacheFile.isFile()) {
                    reloadCachedFile(cacheFile)
                    return hasCache(name)
                }
            }
        }
        return false
    }

    /**
     * Remove the cache from both memory and hard disk
     * @param name: the name of the cache
     */
    synchronized void removeCache(String name) {
        if (cached.containsKey(name)) {
            cached.remove(name)
            File cache = new File(getCacheDir(), name)
            Files.deleteIfExists(cache.toPath())
        }
    }

    /**
     * Get the cache object
     * Return null if the cache is not exist or expired
     * @param name: the name of the cache
     * @return: Object cached
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
     * @param name: the name of the cache, non null
     * @param value: the object of the cache, non null
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