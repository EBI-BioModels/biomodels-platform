<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 07/09/2020
  Time: 21:52
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<%
    String style = session['branding.style']
    String faqURI = g.createLink(uri: '/faq', absolute: true)
    String contactURI = g.createLink(uri: '/contact', absolute: true)
    def createMsgArgs = [contactURI, faqURI]
    def isUpdate = false
    String modelSubmit = g.createLink(controller: "model", action: "submit")
%>
<head>
    <meta name="layout" content="${style}/main" />
    <title>Submission Guidelines and Agreement | BioModels</title>
    <script type="text/javascript">
        $(document).ready(function() {
            $('#creatorExample').click(function () {
                $("#dialog").dialog({
                    width: 659,
                    height: 755
                });
            });
        });
        $(document).delegate("#Continue", "click", function () {
            window.location.href = "${modelSubmit}";
        });
        $(document).delegate("#Cancel", "click", function () {
            window.location.href = "${serverURL}";
        });
    </script>
</head>
<body>
<g:render template="/templates/model/upload/submission-guidelines-and-agreement"
          model="[style: style, isUpdate: isUpdate, faqURI: faqURI,
                  contactURI: contactURI, createMsgArgs: createMsgArgs]"/>
</body>
