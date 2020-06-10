<div class="homepage_info_box"><h3>News</h3></div>
<ul class="widget-body-text" style="list-style: none inside none; padding: 0; margin-left: 0">
<g:each in="${newsItems}" var="news">
    <li style="text-indent: -1.5em; padding-left: 1.5em">
        <i class="icon icon-common icon-new">&nbsp;</i><a
        href="${serverURL}/content/news/${news.key}">${news.value}</a></li>
</g:each>
</ul>
