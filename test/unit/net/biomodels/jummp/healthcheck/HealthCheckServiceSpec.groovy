/*
 * Copyright (C) 2010-2020 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

package net.biomodels.jummp.healthcheck

import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.gorm.Domain
import grails.test.mixin.hibernate.HibernateTestMixin
import net.biomodels.jummp.plugins.security.Person
import net.biomodels.jummp.plugins.security.User
import spock.lang.FailsWith
import spock.lang.Specification

import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Future

@TestFor(HealthCheckService)
@TestMixin(HibernateTestMixin) // because we need a working sessionFactory/transactionManager
@Domain([User, Person]) // for invoking withSession{} or executeQuery() methods
class HealthCheckServiceSpec extends Specification {
    def "should return a health check status"() {
        when:
        HealthCheck[] checks = service.status

        then:
        1 == checks.length
        checks[0].status == HealthCheck.STATUS.OK
    }

    @FailsWith(reason = """The DataSource used for tests is DriverManagerDataSource,
which does not use connection pooling, but uses 1 connection per request.
""", value = AssertionError)
    def "should catch database access errors"() {
        int JOBS = 100
        given: "a pool of async health checkers larger than the db pool"
        def pool = Executors.newFixedThreadPool(JOBS)
        AsyncHealthCheck.service = service

        when: "we launch the health checks"
        Future<HealthCheck[]>[] checks = new Future<HealthCheck[]>[JOBS]
        for (int i = 0; i < JOBS; ++i) {
            Future<HealthCheck[]> asyncCheck = pool.submit(new AsyncHealthCheck())
            checks[i] = asyncCheck
        }
        // start all checkers at the same time
        AsyncHealthCheck.startLatch.countDown()

        and: "check for the db status reported by checkers"
        boolean foundError = false
        for (int i = 0; i < checks.length; i++) {
            Future<HealthCheck[]> task = checks[i]
            HealthCheck[] result = task.get()
            if (result[0].status == HealthCheck.STATUS.ERROR) {
                foundError = true
                break // we found what we were looking for
            }
        }

        then: "at least one check has reported an error"
        foundError

        cleanup:
        pool.shutdownNow()
        if (!pool.isShutdown()) {
            // don't use fail() because it would overlap with the AssertionError caused
            // by foundError being false instead of true
            throw new IllegalStateException("Health checker thread pool did not shut down")
        }
    }
}

// Instances of this class invoke healthCheckService.getStatus()
// They are meant to be ran on a thread pool
class AsyncHealthCheck implements Callable<HealthCheck[]> {
    static HealthCheckService service
    static CountDownLatch startLatch = new CountDownLatch(1)

    @Override
    HealthCheck[] call() throws Exception {
        startLatch.await() // block until all jobs have been submitted
        service.status
    }
}
