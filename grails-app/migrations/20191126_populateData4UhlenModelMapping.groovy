import grails.util.Holders
import net.biomodels.jummp.deployment.biomodels.UhlenModelMapping

databaseChangeLog = {
    changeSet(author: "tnguyen (customised)", id: "1574781701449-1") {
        grailsChange {
            change {
                File csvFile = Holders.grailsApplication.mainContext.getResource("UhlenModelMapping.csv").file
                csvFile.eachLine { String line ->
                    def split = line.split(",")
                    String rep = split[0]
                    String parts = split[1]
                    String strMembers = parts.substring(1, parts.length() - 1)
                    List members = strMembers.split(";")
                    members.eachWithIndex { String mem, int i ->
                        UhlenModelMapping.findOrSaveByRepresentativeAndMember(rep, mem)
                        if (i%100 == 0) {
                            // clear session and save records after every 100 entries created
                            UhlenModelMapping.withSession { session ->
                                session.flush()
                                session.clear()
                            }
                        }
                    }
                    // clear session and save last records
                    UhlenModelMapping.withSession { session ->
                        session.flush()
                        session.clear()
                    }
                }
            }
        }
    }
}
