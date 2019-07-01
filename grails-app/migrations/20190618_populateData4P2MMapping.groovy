import net.biomodels.jummp.deployment.biomodels.P2MMapping

databaseChangeLog = {
    changeSet(author: "tnguyen (written)", id: "1560871943092-1") {
        grailsChange {
            change {
                def userDir = System.properties["user.dir"]
                File csvFile = new File(userDir, "modelMapping.csv")
                csvFile.eachLine { String line ->
                    def split = line.split(",")
                    String rep = split[0]
                    String parts = split[1]
                    String strMembers = parts.substring(1, parts.length() - 2)
                    List members = strMembers.split(";")
                    members.each { String mem ->
                        def p2m = P2MMapping.findOrSaveByRepresentativeAndMember(rep, mem)
                        p2m.save(flush: true)
                    }
                }
            }
        }
    }
}
