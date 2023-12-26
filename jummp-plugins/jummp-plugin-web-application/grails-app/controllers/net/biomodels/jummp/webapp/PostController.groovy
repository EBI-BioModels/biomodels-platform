package net.biomodels.jummp.webapp

import grails.converters.JSON
import grails.converters.XML
import grails.plugin.springsecurity.annotation.Secured
import grails.transaction.Transactional
import net.biomodels.jummp.CommonController
import net.biomodels.jummp.model.Post
import net.biomodels.jummp.plugins.security.User

import static org.springframework.http.HttpStatus.NOT_FOUND
import static org.springframework.http.HttpStatus.OK

@Transactional(readOnly = true)
class PostController extends CommonController {
    def springSecurityService
    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE", createNewPost: "POST"]

    // Admin can use all list of user's Post
    @Secured("permitAll")
    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond Post.list(params), [formats:['xml', 'json']]
    }

    @Secured('ROLE_USER')
    @Transactional
    def createNewPost(String content) {
        if (content == null) {
            notFound()
            return
        }
        User currentUser = (User)springSecurityService.currentUser
        Post post = new Post(content: content, postBy : currentUser)
        post.save(flush: true)
        //respond post, [formats:['xml', 'json']]
        Map map = [content: post.content, postBy: post.postBy.username, id: post.id]
        withFormat {
            json { render map as JSON }
            xml { render map as XML }
            '*' { render status: 415, view: "/errors/error415" }
        }
    }


    @Transactional
    def delete(Long id) {

        Post post = Post.get(id)
        if(!post) {
            notFound()
            return
        }
        post.delete(flush:true)
        '*'{ render status:  OK}
    }

    protected void notFound() {
        request.withFormat {
            '*'{ render status: NOT_FOUND }
        }
    }
}
