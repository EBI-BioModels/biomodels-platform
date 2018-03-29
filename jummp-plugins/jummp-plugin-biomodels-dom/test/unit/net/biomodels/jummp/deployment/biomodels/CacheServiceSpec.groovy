package net.biomodels.jummp.deployment.biomodels

import grails.test.mixin.TestFor
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(CacheService)
class CacheServiceSpec extends Specification {

    String cacheDir = File.createTempDir().getAbsolutePath()

    def setup() {
        grailsApplication.config.jummp.cache.dir = cacheDir
    }

    def cleanup() {
        File testDir = new File(cacheDir)
        for (File testFile : testDir.listFiles()) {
            if (testFile.isFile()) {
                testFile.delete()
            }
        }
    }

    void "get object not exist"() {
        when: 'check whether it has cache for a non exist object'
        String cacheName = 'non-exist'
        then: 'must return false'
        null == service.getCache(cacheName)
    }

    void "create a new cache"() {
        when: 'create a new cache'
        String cacheName = 'cache-exists'
        service.setCache(cacheName, 1, 100)
        then: 'must create a new cache file'
        File file = new File(cacheDir, cacheName)
        file.isFile()
        and: 'must return the same object value'
        1 == service.getCache(cacheName)
    }

    void "get object that exist"() {
        when: 'create new cache and get it back'
        String cacheName = 'cache-exists-2'
        service.setCache(cacheName, 1, 5)
        then: 'must return that object exists'
        null != service.getCache(cacheName)
        when: 'time expired'
        Thread.sleep(5000)
        then: 'must return that object no longer exists'
        null == service.getCache(cacheName)
        and: 'must remove the cache file'
        File file = new File(cacheDir, cacheName)
        !file.isFile()
    }
}
package net.biomodels.jummp.deployment.biomodels

import grails.test.mixin.TestFor
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(CacheService)
class CacheServiceSpec extends Specification {

    String cacheDir = File.createTempDir().getAbsolutePath()

    def setup() {
        grailsApplication.config.jummp.cache.dir = cacheDir
    }

    def cleanup() {
        File testDir = new File(cacheDir)
        for (File testFile : testDir.listFiles()) {
            if (testFile.isFile()) {
                testFile.delete()
            }
        }
    }

    void "get object not exist"() {
        when: 'check whether it has cache for a non exist object'
        String cacheName = 'non-exist'
        then: 'must return false'
        null == service.getCache(cacheName)
    }

    void "create a new cache"() {
        when: 'create a new cache'
        String cacheName = 'cache-exists'
        service.setCache(cacheName, 1, 100)
        then: 'must create a new cache file'
        File file = new File(cacheDir, cacheName)
        file.isFile()
        and: 'must return the same object value'
        1 == service.getCache(cacheName)
    }

    void "get object that exist"() {
        when: 'create new cache and get it back'
        String cacheName = 'cache-exists-2'
        service.setCache(cacheName, 1, 5)
        then: 'must return that object exists'
        null != service.getCache(cacheName)
        when: 'time expired'
        Thread.sleep(5000)
        then: 'must return that object no longer exists'
        null == service.getCache(cacheName)
        and: 'must remove the cache file'
        File file = new File(cacheDir, cacheName)
        !file.isFile()
    }
}
package net.biomodels.jummp.deployment.biomodels

import grails.test.mixin.TestFor
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(CacheService)
class CacheServiceSpec extends Specification {

    String cacheDir = File.createTempDir().getAbsolutePath()

    def setup() {
        grailsApplication.config.jummp.cache.dir = cacheDir
    }

    def cleanup() {
        File testDir = new File(cacheDir)
        for (File testFile : testDir.listFiles()) {
            if (testFile.isFile()) {
                testFile.delete()
            }
        }
    }

    void "get object not exist"() {
        when: 'check whether it has cache for a non exist object'
        String cacheName = 'non-exist'
        then: 'must return false'
        null == service.getCache(cacheName)
    }

    void "create a new cache"() {
        when: 'create a new cache'
        String cacheName = 'cache-exists'
        service.setCache(cacheName, 1, 100)
        then: 'must create a new cache file'
        File file = new File(cacheDir, cacheName)
        file.isFile()
        and: 'must return the same object value'
        1 == service.getCache(cacheName)
    }

    void "get object that exist"() {
        when: 'create new cache and get it back'
        String cacheName = 'cache-exists-2'
        service.setCache(cacheName, 1, 5)
        then: 'must return that object exists'
        null != service.getCache(cacheName)
        when: 'time expired'
        Thread.sleep(5000)
        then: 'must return that object no longer exists'
        null == service.getCache(cacheName)
        and: 'must remove the cache file'
        File file = new File(cacheDir, cacheName)
        !file.isFile()
    }
}
package net.biomodels.jummp.deployment.biomodels

import grails.test.mixin.TestFor
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(CacheService)
class CacheServiceSpec extends Specification {

    String cacheDir = File.createTempDir().getAbsolutePath()

    def setup() {
        grailsApplication.config.jummp.cache.dir = cacheDir
    }

    def cleanup() {
        File testDir = new File(cacheDir)
        for (File testFile : testDir.listFiles()) {
            if (testFile.isFile()) {
                testFile.delete()
            }
        }
    }

    void "get object not exist"() {
        when: 'check whether it has cache for a non exist object'
        String cacheName = 'non-exist'
        then: 'must return false'
        null == service.getCache(cacheName)
    }

    void "create a new cache"() {
        when: 'create a new cache'
        String cacheName = 'cache-exists'
        service.setCache(cacheName, 1, 100)
        then: 'must create a new cache file'
        File file = new File(cacheDir, cacheName)
        file.isFile()
        and: 'must return the same object value'
        1 == service.getCache(cacheName)
    }

    void "get object that exist"() {
        when: 'create new cache and get it back'
        String cacheName = 'cache-exists-2'
        service.setCache(cacheName, 1, 5)
        then: 'must return that object exists'
        null != service.getCache(cacheName)
        when: 'time expired'
        Thread.sleep(5000)
        then: 'must return that object no longer exists'
        null == service.getCache(cacheName)
        and: 'must remove the cache file'
        File file = new File(cacheDir, cacheName)
        !file.isFile()
    }
}
package net.biomodels.jummp.deployment.biomodels

import grails.test.mixin.TestFor
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(CacheService)
class CacheServiceSpec extends Specification {

    String cacheDir = File.createTempDir().getAbsolutePath()

    def setup() {
        grailsApplication.config.jummp.cache.dir = cacheDir
    }

    def cleanup() {
        File testDir = new File(cacheDir)
        for (File testFile : testDir.listFiles()) {
            if (testFile.isFile()) {
                testFile.delete()
            }
        }
    }

    void "get object not exist"() {
        when: 'check whether it has cache for a non exist object'
        String cacheName = 'non-exist'
        then: 'must return false'
        null == service.getCache(cacheName)
    }

    void "create a new cache"() {
        when: 'create a new cache'
        String cacheName = 'cache-exists'
        service.setCache(cacheName, 1, 100)
        then: 'must create a new cache file'
        File file = new File(cacheDir, cacheName)
        file.isFile()
        and: 'must return the same object value'
        1 == service.getCache(cacheName)
    }

    void "get object that exist"() {
        when: 'create new cache and get it back'
        String cacheName = 'cache-exists-2'
        service.setCache(cacheName, 1, 5)
        then: 'must return that object exists'
        null != service.getCache(cacheName)
        when: 'time expired'
        Thread.sleep(5000)
        then: 'must return that object no longer exists'
        null == service.getCache(cacheName)
        and: 'must remove the cache file'
        File file = new File(cacheDir, cacheName)
        !file.isFile()
    }
}
