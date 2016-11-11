<%@
    page contentType="text/html;charset=UTF-8"
    expressionCodec = "none"
%>

<g:if test="${models}">
    <g:if test="${actionName == 'search'}">
        <h3>Filter your results</h3>
        <g:each in="${facets}" var="facet">
            <h4>${facet.label}</h4>
            <p>
                <g:each in="${facet.facetValues}" var="fv">
                    <input type="checkbox" value="${fv.value}">
                    <%
                        boolean isAsked = query.contains("${facet.id}:${fv.value}")
                    %>
                    <g:if test="${isAsked}">
                        <span>${fv.label} (${fv.count})</span>
                    </g:if>
                    <g:else>
                    <a href="${grailsApplication.config.grails.serverURL}/search?query=${query} and ${facet.id}:${fv.value}">
                        <span>${fv.label} (${fv.count})</span></a>
                    </g:else>
                    <br/>
                </g:each>
            </p>
        </g:each>
    </g:if>
    <g:elseif test="${actionName == 'list'}">
        <h3>Filter your models</h3>
        <g:each in="${facets}" var="facet">
            <h4>${facet}</h4>
            <p>Will be realised soon!!!</p>
        </g:each>
    </g:elseif>
</g:if>
<g:else>
    <p></p>
</g:else>
