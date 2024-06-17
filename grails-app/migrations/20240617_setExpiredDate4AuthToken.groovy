import net.biomodels.jummp.plugins.security.AuthToken
databaseChangeLog = {
    // Make all existing tokens expired after 30 days since today.
    changeSet(author: "Tung Nguyen (manually created)", id: "1718635423-2") {
        grailsChange {
            change {
                List<AuthToken> allTokens = AuthToken.getAll()
                for (AuthToken token : allTokens) {
                    if (!token.expiredDate) {
                        def dt = new Date() + 30
                        token.expiredDate = dt
                        if (!token.save(flush: true)) {
                            println "Cannot update the expired date for the token, id = ${token.id}"
                        }
                    }
                }
            }
        }
    }
}
