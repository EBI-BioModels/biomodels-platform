<%@ page import="groovy.time.TimeCategory" %>
<style>
    .badge {
        display: inline;
        padding: 0.25em;
        background-color: red;
        font-size: small;
        font-weight: bold;
        text-align: center;
    }
    .alert {
        background-color: red !important;
        color: white;
        margin: 0;
    }
</style>
<ul class="widget-body-text" style="list-style: none inside none; padding: 0; margin-left: 0">
<g:each in="${newsItems}" var="news">
    <li style="text-indent: -1.5em; padding-left: 1.5em">
        <%
            // TODO: move the following stuff to the backend or convert them into the business treatment
            String strDatePublished = news.value.take(10)
            Date datePublished = new Date().parse("dd/MM/yyyy", strDatePublished)
            Integer nbDays = 0
            use(TimeCategory) {
                def duration = new Date() - datePublished
                nbDays = duration.days
            }
        %>
        <g:if test="${nbDays <= 90}">
            <i class="icon icon-common icon-new" style="color: red">&nbsp;</i></g:if>
        <g:else>
            <i class="icon icon-common icon-new" style="color: #007c82">&nbsp;</i></g:else>
        <a href="${serverURL}/content/news/${news.key}">${news.value}</a>
        <g:if test="${nbDays <= 90}">&nbsp;<span class="badge alert">New</span></g:if></li>
</g:each>
<a href="${g.createLink(controller: 'jummp', action: 'fetchNews')}">Read more...</a>
</ul>
