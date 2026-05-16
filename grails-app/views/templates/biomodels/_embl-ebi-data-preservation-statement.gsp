<script>
    $(document).ready(() => {
        const elixirRibbon = $("div.elixir-ribbon > div.row > div.column");
        if (elixirRibbon.length) {
            elixirRibbon.removeClass("column");
            elixirRibbon.addClass("columns small-12 medium-6 large-6");
            $('<div class="columns small-12 medium-6 large-6">' +
                '<h5><a href="https://www.ebi.ac.uk/licencing" target="_blank">EMBL-EBI Licencing</a><br/>' +
                '<a href="https://www.ebi.ac.uk/long-term-data-preservation" target="_blank">' +
                'EMBL-EBI Data Preservation</a></h5></div>').insertAfter(elixirRibbon);
        }
    });
</script>
