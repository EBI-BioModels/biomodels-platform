<script type="text/javascript">
    window.onload = function() {
        if (window.jQuery) {
            $('.go-to-top').click(function () {
                $('body').animate({"scrollTop": "0px"}, 1000);
            });
        } else {
            alert("jQuery is not loaded");
        }
    }
</script>
<p>Each &quot;Model of the Month&quot; is an article which explores the biological significance of a particular model from BioModels Database. Every article includes elements such as:</p>

<ul>
    <li>the description of the model itself and its results</li>
    <li>the biological background of the model</li>
    <li>a brief description of the biological processes that are encoded as a mathematical model</li>
    <li>the biological role of each of the model elements</li>
    <li>the diseases that are caused due to the malfunction of these elements</li>
    <li>the presenters' own view on the model</li>
</ul>

<p>Direct access to the models of the month for a given year:
    <g:each in="${years}" var="year">
        <a href="#year_${year}" title="Access to the news of the year: ${year}">${year}</a> |
    </g:each>
</p>
