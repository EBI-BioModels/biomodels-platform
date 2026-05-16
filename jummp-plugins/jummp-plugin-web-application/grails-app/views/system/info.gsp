<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 27/04/2022
  Time: 16:16
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>System Information | BioModels</title>
    <script>
        $(document).ready(function() {
            $('#foundation-version').text("Foundation CSS framework: " + Foundation.version);
            $('#jquery-version').text("jQuery: " + jQuery.fn.jquery);
        });
    </script>
    <style>
        h3 {
            color: #0d5aa7;
            font-weight: bolder;
        }
    </style>
</head>

<body>
    <h2>System Information</h2>
    <h3>App</h3>
    <p>App Version <g:meta name="app.version"/></p>
    <h3>Java, Groovy and Grails Framework</h3>
    <p>JVM: ${System.getProperty('java.version')}</p>
    <p>Built with Grails <g:meta name="app.grails.version"/> and Groovy ${GroovySystem.version}</p>
    <h3>JavaScript, jQuery and CSS</h3>
    <p id="foundation-version"></p>
    <p id="jquery-version"></p>
</body>
</html>
