<%@ page contentType="text/html;charset=UTF-8" %>
<h3>Filter your results</h3>
<g:each in="${facets}" var="facet">
    <h4>${facet.label}</h4>
    <p>
        <g:each in="${facet.facetValues}" var="fv">
            <input type="checkbox" value="${fv.value}">
            <a href="${grailsApplication.config.grails.serverURL}/search?query=${query}+${fv.label}"><span>${fv.label} (${fv.count})</span></a>
            <br/>
        </g:each>
    </p>
</g:each>
