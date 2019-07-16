<div class="row">
    <div class="small-12 medium-6 large-6 columns" id="filter">
        Search models by keywords: <input type="text" name="searchByKeywords" id="searchWithKeywords"
                                     class="on-page-search"/>
    </div>
    <div class="small-12 medium-6 large-6 columns">
        Jump to a group of genus: <br/> |
        <g:each in="${('A'..'Z').collect {it}}" var="letter">
            <a href="#${letter}">${letter}</a> |
        </g:each>
    </div>
</div>
