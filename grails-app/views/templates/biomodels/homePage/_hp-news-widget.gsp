<div class="homepage_info_box"><h3>News</h3></div>
<ul class="widget-body-text">
<g:each in="${newsItems}" var="news">
    <li><a href="${serverURL}/content/news/${news.key}">${news.value}</a></li>
</g:each>
</ul>
