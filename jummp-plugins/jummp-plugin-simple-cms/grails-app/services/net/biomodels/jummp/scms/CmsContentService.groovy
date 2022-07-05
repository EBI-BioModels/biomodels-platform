package net.biomodels.jummp.scms

import grails.transaction.Transactional
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.scms.CmsContentTransportCommand as CCTC

@Transactional
class CmsContentService {

    Map fromCommandObject(CCTC cmd) {
        cmd.content = cmd.content.decodeHTML()
        User createdBy = User.findByUsername(cmd.createdBy)
        User lastChangedBy = User.findByUsername(cmd.lastChangedBy)
        CmsContent content = findByTransportCommand(cmd)
        if (content) {
            // save an existing content
            content.aliasURI = cmd.aliasURI
            content.title = cmd.title
            content.description = cmd.description
            content.content = cmd.content

            content.parent = content

            content.createdBy = createdBy
            content.createdOn = cmd.createdOn
            content.lastChangedBy = lastChangedBy
            content.lastChangedOn = cmd.lastChangedOn
        } else {
            // create a new content
            content = new CmsContent(aliasURI: cmd.aliasURI,
                title: cmd.title, description: cmd.description, content: cmd.content, parent: null,
                createdBy: createdBy, createdOn: cmd.createdOn,
                lastChangedBy: lastChangedBy, lastChangedOn: cmd.lastChangedOn)
        }
        Map result = [:]
        if (content.save(flush: true)) {
            result.put("message", "Success")
        } else {
            result.put("message", "Failure")
        }
        result.put("content", content)
        result
    }

    CmsContent findByTransportCommand(CCTC cmd) {
        CmsContent content = null
        if (cmd?.id || cmd?.id != -1) {
            content = CmsContent.get(cmd.id)
        } else if (cmd?.aliasURI) {
            content = CmsContent.executeQuery("from CmsContent as c where c.aliasURI = :aliasURI",
                [aliasURI: cmd.aliasURI])?.first()
        }
        content
    }
}
