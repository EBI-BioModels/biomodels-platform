package net.biomodels.jummp.plugin.webapp

import grails.test.mixin.Mock
import net.biomodels.jummp.core.ModelDelegateService
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.PermissionTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.model.Revision

import grails.test.mixin.TestFor
import spock.lang.Specification

@TestFor(net.biomodels.jummp.webapp.ModelController)
@Mock([Revision])
class ModelControllerSpec extends Specification {
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
            [ new PermissionTransportCommand(id: "0", name: "Me", read: true, write: true),
            new PermissionTransportCommand(id: "1", name: "Myself", read: true),
            new PermissionTransportCommand(id: "2", name: "I", read: true),
            ]
        }
        def springSecurityService = new Object()
        springSecurityService.metaClass.getCurrentUser = {
        	return null;
        }
        
        controller.modelDelegateService = mds.createMock()
        controller.springSecurityService = springSecurityService;

        when: "the access permissions of that model are checked"
        controller.request.parameters = [id: "MODEL123.4"]
        def model = controller.share()

        then: "the correct permissions are returned"
        model != null
        grails.converters.JSON perms = model.permissions
        String jsonPerms = perms.toString(false)
        String expected = """\
[{"disabledEdit":false,"id":"0","name":"Me","read":true,"show":true,"write":true},{"disabledEdit":false,"id":"1","name":"Myself","read":true,"show":true,"write":false},{"disabledEdit":false,"id":"2","name":"I","read":true,"show":true,"write":false}]"""
        jsonPerms == expected
    }
}
