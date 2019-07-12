import grails.util.Holders
import net.biomodels.jummp.deployment.biomodels.P2MMapping

databaseChangeLog = {
    changeSet(author: "tnguyen (customised)", id: "1560871943092-1") {
        grailsChange {
            change {
                File csvFile = Holders.grailsApplication.mainContext.getResource("modelMapping.csv").file
                csvFile.eachLine { String line ->
                    def split = line.split(",")
                    String rep = split[0]
                    String parts = split[1]
                    String strMembers = parts.substring(1, parts.length() - 2)
                    List members = strMembers.split(";")
                    members.eachWithIndex { String mem, int i ->
                        P2MMapping.findOrSaveByRepresentativeAndMember(rep, mem)
                        if (i.mod(100) == 0) {
                            // clear session and save records after every 100 entries created
                            P2MMapping.withSession { session ->
                                session.flush()
                                session.clear()
                            }
                        }
                    }
                    // clear session and save last records
                    P2MMapping.withSession { session ->
                        session.flush()
                        session.clear()
                    }
                }
            }
        }
    }
}
