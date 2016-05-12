<%--
 Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
 Deutsches Krebsforschungszentrum (DKFZ)

 This file is part of Jummp.

 Jummp is free software; you can redistribute it and/or modify it under the
 terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
 WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
--%>











<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="layout" content="main"/>
        <title>Login</title>
    </head>
    <body>

    <div id='login'>
        <form action='${postUrl}' method='POST' id='loginForm' class='cssform' autocomplete='on'>
            <table>
                <tr><p>${flash.message}</p></tr>
                <tbody>
                    <tr>
                    	<td class="name">
                                <label for="username">
                                    <g:message code="login.form.label"/>
                                </label>
                        </td>
                        <td>
                            <input type='text' name='j_username' id='username'/>
                        </td>
                    </tr>
                    <tr>
  	                <td class="name">
                               <label for="password">
                                    <g:message code="login.form.password"/>
                                </label>
                        </td>
                    	<td>
                            <input type='password' name='j_password' id='password'/>
                    	</td>
                   </tr>
                    <tr>
                        <td>&nbsp;</td>
                    	<td>
                    		<button>LOGIN</button>
                    	</td>
                   </tr>
                   <tr>
                       <td>&nbsp;</td>
                    	<td>
                    		<a href="${grailsApplication.config.grails.serverURL}/forgotpassword">
	  	     					Forgot password?
	  	     				</a>
                    	</td>
                   </tr>
                    <g:if test="${grailsApplication.config.jummp.security.anonymousRegistration}">
                    <tr>
                        <td>&nbsp;</td>
                        <td>
                        <a href="${grailsApplication.config.grails.serverURL}/registration">
                            Register
                        </a>
                        </td>
                    </tr>
                    </g:if>

                </tbody>
            </table>
        </form>
    </div>
    <script type='text/javascript'>
    <!-- TODO: move out of HTML page //-->
    	$("#loginForm input").focus(function() {
    		if ($(this).data("reset") === undefined) {
    		$(this).val("");
    		$(this).data("reset", true);
    		}
    	});
    	$("#loginForm input").keyup(function(event) {
    	// magic value 13 is enter
    	if (event.which == 13) {
        	$("#loginForm").submit();
        	}
        });
        $("#login div.loginButton button").click(function() {
        	$("#loginForm").submit();
        });
    </script>
    </body>
</html>
<content tag="title">
	Login
</content>
<content tag="contexthelp">
		login
	</content>
