<%
	def imagePath="/images"
%>
<th>
	<a href="${createLink(controller: 'search', action: action, 
			params: [query: query,  sortDir: sortDirection=="asc"? "desc":"asc", sortBy: sortColumn])}">
	<b><g:message code="${msgCode}"/></b>
		<g:if test="${sortBy==sortColumn && sortDirection=="asc"}">
			<g:img dir="${imagePath}" 
			contextPath="" file="ascend.gif"
			absolute="true" 
			alt="Sort Descending"/>
		</g:if>
		<g:if test="${sortBy==sortColumn && sortDirection=="desc"}">
			<g:img dir="${imagePath}" 
			contextPath="" file="descend.gif" 
			absolute="true" 
			alt="Sort Ascending"/>
		</g:if>
	</a>
</th>
