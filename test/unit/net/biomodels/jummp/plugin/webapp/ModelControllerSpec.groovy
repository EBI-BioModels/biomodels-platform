package net.biomodels.jummp.plugin.webapp

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.web.FiltersUnitTestMixin
import net.biomodels.jummp.core.ModelDelegateService
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.PermissionTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.utils.redis.RedisService
import net.biomodels.jummp.webapp.ModelController
import spock.lang.Specification

@TestFor(ModelController)
@TestMixin(FiltersUnitTestMixin)
@Mock([Revision])
class ModelControllerSpec extends Specification {
    // JBM-705 added a mandatory redisService lookup to CommonController.setConfiguration(), which
    // runs as part of every controller bean's initialisation (not just when an action touches
    // Redis). @TestFor's generated `controller` getter mocks (and so initialises) the controller
    // bean in an instance-level @Before fixture, which runs before Spock's own setup() - so the
    // bean has to be registered at the class level (setupSpec()), not per-test, or it is still
    // missing when the controller bean gets created.
    def setupSpec() {
        defineBeans {
            redisService(RedisService)
        }
    }

    def setup() {
    }

    def cleanup() {
    }

    void "share throws exception when called without a model revision id"() {
        when:
        controller.share()

        then:
        controller.actionUri == '/errors/error403'
    }

    void "share can handle rubbish input"() {
        when: 'I access a model identifier with an invalid identifier'
        controller.request.parameters = [id: "1"]
        controller.share()

        then: 'I get an error.'
        controller.actionUri == '/errors/error403'
    }

    void "share returns a model if there exists a Revision with the supplied id "() {
        given: "a model is shared with me, myself and I"
        def mds = mockFor(ModelDelegateService)
        mds.demand.getRevisionFromParams() { String mId, String rId ->
            new RevisionTransportCommand( id: Integer.parseInt(rId), name: "mock model version",
                        model: new ModelTransportCommand(submissionId: "${mId}.${rId}")
            )
        }
        mds.demand.getPermissionsMap() { id ->
            // PermissionTransportCommand.id is a primitive int - passing a single-character
            // String here (as this used to) doesn't parse as a number: Groovy's map constructor
            // coerces a one-char String into an int property via its Unicode code point, so
            // id: "0"/"1"/"2" silently became 48/49/50 (the ASCII codes of '0'/'1'/'2'), not 0/1/2.
            [ new PermissionTransportCommand(id: 0, name: "Me", read: true, write: true),
            new PermissionTransportCommand(id: 1, name: "Myself", read: true),
            new PermissionTransportCommand(id: 2, name: "I", read: true),
            ]
        }
        def springSecurityService = new Object()
        springSecurityService.metaClass.getCurrentUser = {
            return null
        }

        controller.modelDelegateService = mds.createMock()
        controller.springSecurityService = springSecurityService

        when: "the access permissions of that model are checked"
        controller.request.parameters = [id: "MODEL123.4"]
        def model = controller.share()

        then: "the correct permissions are returned"
        model != null
        grails.converters.JSON perms = model.permissions
        String jsonPerms = perms.toString(false)
        // id is a primitive int (unquoted in JSON, not the earlier "0"/"1"/"2" strings), and
        // PermissionTransportCommand has since gained a username field, serialised alongside it.
        String expected = """\
[{"disabledEdit":false,"id":0,"name":"Me","read":true,"show":true,"username":null,"write":true},\
{"disabledEdit":false,"id":1,"name":"Myself","read":true,"show":true,"username":null,"write":false},\
{"disabledEdit":false,"id":2,"name":"I","read":true,"show":true,"username":null,"write":false}]"""
        jsonPerms == expected
    }

    void "test show action is filtered"() {
        given: "given parameters and mock objects to the show action"
        params.controller = "Model"
        String modelId = "<em>MODEL12345</em>"
        params.id = modelId
        params.revisionId = "1"

        def modelDelegateService = mockFor(ModelDelegateService)
        modelDelegateService.metaClass.getRevisionFromParams = { String id, String revisionId ->
            return null
        }
        controller.modelDelegateService = modelDelegateService

        // the id/revisionId encoding under test happens in ParameterFilters.before, not in the
        // show action itself - withFilters wraps the call so that filter actually runs.
        // ParameterFilters (grails-app/conf/ParameterFilters.groovy) is in the default package;
        // a bare reference from this named-package spec resolves as a runtime property lookup
        // rather than a compile-time class literal (there is no import syntax that crosses the
        // default-package boundary), so it is looked up reflectively instead.
        mockFilters(Class.forName("ParameterFilters"))

        when: "the show action is called"
        withFilters(controller: "Model", action: "show") {
            controller.show()
        }

        then: "the id param is encoded"
        params.id == "&lt;em&gt;MODEL12345&lt;/em&gt;"
    }
}
