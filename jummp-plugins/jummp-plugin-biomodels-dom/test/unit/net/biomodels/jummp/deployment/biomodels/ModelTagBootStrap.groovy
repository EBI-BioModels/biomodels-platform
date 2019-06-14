package net.biomodels.jummp.deployment.biomodels

import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.ModelFormat
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.plugins.security.Person
import net.biomodels.jummp.plugins.security.User

class ModelTagBootStrap {
    static void initialiseData() {
        /**
         * Create a person, a user, and a model format
         * Create two models and a few tags
         * Create a few associations between these models and tags
         */
        Person person = new Person(userRealName: "Elvis Nguyen")
        assert person.save()
        User user = new User(person: person, username: "elvis", email: 'elvis@itersdesktop.com', enabled: true, password:
            's3cr3t', accountExpired: false, accountLocked: false, passwordExpired: false)
        user.save()

        ModelFormat fmt = new ModelFormat(identifier: 'foo', name: 'foo', formatVersion: '1.0')
        fmt.save()

        Model m1 = new Model(vcsIdentifier: "aaa/123abc", name: "Model 1", submissionId: "M001")
        Model m2 = new Model(vcsIdentifier: "aaa/123def", name: "Model 2", submissionId: "M002")
        Model m3 = new Model(vcsIdentifier: "aaa/123def", name: "Model 2", submissionId: "M003")

        Revision r1 = new Revision(model: m1, vcsId: "1", revisionNumber: 1, owner: User.findByUsername("elvis"),
            minorRevision: false, name:"sample model 1", description: "sample model 1",
            comment: "this is a sample model", uploadDate: new Date(),
            format: ModelFormat.findByIdentifierAndFormatVersion("foo", "1.0"))
        Revision r2 = new Revision(model: m2, vcsId: "1", revisionNumber: 1, owner: User.findByUsername("elvis"),
            minorRevision: false, name:"sample model 2", description: "sample model 2",
            comment: "this is a sample model", uploadDate: new Date(),
            format: ModelFormat.findByIdentifierAndFormatVersion("foo", "1.0"))
        Revision r3 = new Revision(model: m2, vcsId: "1", revisionNumber: 1, owner: User.findByUsername("elvis"),
            minorRevision: false, name:"sample model 3", description: "sample model 3",
            comment: "this is a sample model", uploadDate: new Date(),
            format: ModelFormat.findByIdentifierAndFormatVersion("foo", "1.0"))

        m1.revisions = [r1] as Set
        m2.revisions = [r2] as Set
        m3.revisions = [r3] as Set

        assert [m1,m2,m3]*.save(flush: true)

        // Create some tags
        Tag tag = new Tag()
        tag.name = "Annotated"
        tag.description = "The is a fully annotated model"
        tag.userCreated = User.findByUsername("elvis")
        tag.dateCreated = new Date()
        tag.dateModified = new Date()
        assert tag.save()

        tag = new Tag()
        tag.name = "Reproducible"
        tag.description = "The is a reproducible model"
        tag.userCreated = User.findByUsername("elvis")
        tag.dateCreated = new Date()
        tag.dateModified = new Date()
        assert tag.save()

        tag = new Tag()
        tag.name = "Partially Annotated"
        tag.description = "The is a partially annotated model"
        tag.userCreated = User.findByUsername("elvis")
        tag.dateCreated = new Date()
        tag.dateModified = new Date()
        assert tag.save()

        assert 3 == Tag.count()

        // Map models and tags
        Tag tagAnnotated = Tag.findByName("Annotated")
        Tag tagPartiallyAnnotated = Tag.findByName("Partially Annotated")
        Tag tagReproducible = Tag.findByName("Reproducible")

        ModelTag modelTag1 = new ModelTag(model: m1, tag: tagAnnotated)
        ModelTag modelTag2 = new ModelTag(model: m1, tag: tagReproducible)
        modelTag1.save()
        modelTag2.save()

        ModelTag modelTag3 = new ModelTag(model: m2, tag: tagPartiallyAnnotated)
        ModelTag modelTag4 = new ModelTag(model: m2, tag: tagReproducible)
        modelTag3.save()
        modelTag4.save()

        assert ModelTag.count == 4
    }
}
