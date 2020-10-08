<span style="word-wrap: break-word">
    <g:if test="${modellingApproach.key == "OTHER"}">
        Other
    </g:if>
    <g:else>
        <a href="http://identifiers.org/mamo/${modellingApproach.key}" target="_blank">
            ${modellingApproach.value}</a>
    </g:else>
</span>
