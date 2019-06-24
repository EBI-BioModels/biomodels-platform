package net.biomodels.jummp.core.user

import net.biomodels.jummp.plugins.security.Person

/**
 * Convenience class for adding customised methods to Person class.
 *
 * Relies on Groovy categories, a form of runtime meta-programming.
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
@Category(Person)
class PersonCategory {
    /**
     * Provides a lightweight command object that can be used outside Jummp's core.
     *
     * @return the PersonTransportCommand representation of a Person object.
     */
    PersonTransportCommand toCommandObject() {
        String userRealName = this.userRealName
        String institution = this.institution
        String orcid = this.orcid
        return new PersonTransportCommand(userRealName: userRealName,
            institution: institution, orcid: orcid)
    }
}
