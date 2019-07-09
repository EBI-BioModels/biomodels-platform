<div class="row">
    Filter your models by<br/>
    <div class="small-12 medium-6 large-6 columns" id="filter">
        searching keywords: <input type="text" name="searchByKeywords" id="searchWithKeywords" class="on-page-search"/>
    </div>
    <div class="small-12 medium-6 large-6 columns">
        choosing letters: <br/> |
        <g:each in="${('A'..'Z').collect {it}}" var="letter">
            <a href="#${letter}">${letter}</a> |
        </g:each>
    </div>
</div>
