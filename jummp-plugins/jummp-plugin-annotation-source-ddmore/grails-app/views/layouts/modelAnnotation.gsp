<g:applyLayout name="main">
    <head>
        <g:javascript src="underscore-min.js" plugin="jummp-plugin-web-application"/>
        <g:javascript src="handlebars.min.js" plugin="jummp-plugin-web-application"/>
        <g:javascript src="backbone-min.js" plugin="jummp-plugin-web-application"/>
        <g:javascript src="backbone.marionette.min.js"/>
        <g:javascript src="annotationEditor.js" />
        <g:external dir="css" file="annotationEditor.css" />
        <g:layoutHead/>
    </head>
    <body>
        <div id='container'>
            <g:layoutBody/>
        </div>
    </body>
</g:applyLayout>
