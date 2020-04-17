<div class="homepage_info_box"><h3>News</h3></div>
<ul style="font-size: 87%">
<g:each in="${newsItems}" var="news">
    <li><a href="https://www.ebi.ac.uk/biomodels/content/news/${news.key}">${news.value}</a></li>
</g:each>
</ul>
