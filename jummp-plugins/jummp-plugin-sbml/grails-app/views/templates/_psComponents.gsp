<%@ page import="net.biomodels.jummp.core.constants.BioModels" %>
<g:if test="${components}">
    <div id="Components" class="row">
        <div class="small-12 columns">
            <div class="row">
                <div class="pull-element-right round-border small-12 medium-4 large-2 columns">
                    <strong>Legends</strong><br/>
                    <span class="legend-green-block">
                    </span>
                    <span>
                        : Variable used inside SBML models</span>
                </div>

                <div class="small-12 medium-4 large-2 columns">
                    <button id="expand-all"><i class="icon icon-common icon-plus"></i> Expand All</button><br/>
                    <button id="collapse-all"><i class="icon icon-common icon-collapse"></i> Collapse All</button>
                </div>
            </div>
            <br/>

            <div class="container row">
                <div class="small-12 medium-4 large-8 columns">
                    <div class="header"><span>Species</span>
                    </div>


                    <div class="content">
                        <g:if test="${components.species?.size() > 0}">
                            <table>
                                <th>Species</th>
                                <th>Initial Concentration/Amount</th>
                                <g:each var="a" in="${components.species}">
                                    <tr style="text-align: center">
                                        <td>${a.value.speciesAnnotationShow}</td>
                                        <td>${a.value.initialData}</td>
                                    </tr>
                                </g:each>
                            </table>
                        </g:if>
                        <g:else>
                            No records to display
                        </g:else>
                    </div>

                    <div class="header"><span>Reactions</span>

                    </div>

                    <div class="content small-12 medium-4 large-8 columns">
                        <g:if test="${components.reactions?.size() > 0}">
                            <table style="width: 100%">
                                <th>Reactions</th>
                                <th>Rate</th>
                                <th>Parameters</th>
                                <g:each var="a" in="${components.reactions}">
                                    <tr style="text-align: center">
                                        <td>${a.value.reaction}</td>
                                        <td>${a.value.rate}</td>
                                        <td>${a.value.parameters}</td>
                                    </tr>
                                </g:each>
                            </table>
                        </g:if>
                        <g:else>
                            No records to display
                        </g:else>
                    </div>
                </div>
            </div>
        </div>

    </div>
    <br/>
    <div class="row">

        <div class="columns small-12 medium-12 large-12">
            Above are the first reactions and species of this model. To get more results, please search for
            <a href="${BioModels.BM_ROOT_URL}/parameterSearch/index?query=${perennialId}&start=0&size=100&sort=model%3Aascending&is_curated=true" target="_blank">${perennialId}</a> on Parameters Search Portal.
        </div>
    </div>
    <br/>
    <script>
        $(function() {
            $(".header").first().next().slideDown(500);

            $("#expand-all").on("click", function() {
                $(".header").next().slideDown(500);
            });
            $("#collapse-all").on("click", function() {
                $(".header").next().slideUp(500);
            });
        });

        $(".header").on("click", function() {
            const $header = $(this);
            // getting the next element
            const $content = $header.next();
            // open the content needed - toggle the slide if it is visible, slide up, if not slided own.
            $content.slideToggle(500, function () {
            });
        });
    </script>
</g:if>
