<span style="word-wrap: break-word">
    <g:if test="${modellingApproach.key == "OTHER"}">
        Other
    </g:if>
    <g:else>
        <a href="${modellingApproach.value[1]}" target="_blank">
            ${modellingApproach.value[0]}</a>
    </g:else>
</span>
