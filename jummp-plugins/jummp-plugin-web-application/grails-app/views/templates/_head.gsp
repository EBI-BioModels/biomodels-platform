<g:javascript src="toastr.min.js" contextPath="${grailsApplication.config.grails.serverURL}"/>
<link rel="stylesheet"
      href="${resource(contextPath: "${grailsApplication.config.grails.serverURL}", dir: 'css', file: 'toastr.min.css')}"/>
<script type="javascript">
    toastr.options = {
        "progressBar": true
    }
</script>
