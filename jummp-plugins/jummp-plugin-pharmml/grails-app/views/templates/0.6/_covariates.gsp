<g:each in="${covariates}">
    <g:if test="${it.continuous != null}">
        <p class='bold default'>Continuous covariate <span class='italic'>${it.symbId}</span></p>
        <div class="spaced-top-bottom">${it.continuous.dist}</div>
        <g:if test="${it.continuous.transf}">
            <g:each in="${it.continuous.transf}" var="t">
                <div>${t}</div>
            </g:each>
        </g:if>
    </g:if>
    <g:if test="${it.categorical != null}">
        <p class='bold default'>Categorical covariate <span class='italic'>${it.symbId}</span></p>
        <div class="spaced-top-bottom">Categories: ${it.categorical}</div>
    </g:if>
</g:each>
