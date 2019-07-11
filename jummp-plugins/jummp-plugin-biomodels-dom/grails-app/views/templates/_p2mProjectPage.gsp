<h2>Path2Models project page</h2>

<p>
    Path2Models is a collection of models automatically generated from pathway resources such as
    <a href="http://www.kegg.jp/" title="KEGG: Kyoto Encyclopedia of Genes and Genomes">KEGG</a>,
    <a href="https://cgap.nci.nih.gov/Pathways/BioCarta_Pathways" title="BioCarta">BioCarta</a>,
    <a href="http://www.metacyc.org/" title="MetaCyc">MetaCyc</a>,
    <a href="https://wiki.nci.nih.gov/pages/viewpage.action?pageId=315491760" title="Pathway Interaction Database">PID</a> and
    <a href="http://sabio.villa-bosch.de/" title="SABIO-RK">SABIO-RK</a> and hosted in BioModels database.
</p>

<p>About 140K models in Path2Models project are grouped taxonomically into 814 bundles of models,
typically one per genus. Each bundle of models contains one representative entry, typically a genome
scale model
for one of the organisms under that genus. The description of a representative SBML model contains the
BioModels accessions of all models under this bundle along with metadata information. The other
models of
the
group are available as a COMBINE archive alongside the main file.
All requests to access a model inside a COMBINE archive will be redirected to the representative model.
</p>
<h3>Browse models</h3>
<p>One can also browse those models through <a href="${createLink(controller: "p2m", action: "browse")}">the dedicated
browse page</a>
    .</p>
<h3>Search models</h3>

<p>We will support and adopt searching models into the primary search system later.</p>

<h3>Download all models</h3>
<ul>
    <li><a href="ftp://ftp.ebi.ac.uk/pub/databases/biomodels/auto_generated/path2models/latest/"
           title="Access to the FTP server where the models archives are hosted">Archives of all models</a> (from the latest release)
    </li>
</ul>

<h3>Additional information</h3>

<p>
    Those automatically generated models are only partially parameterized. In the case of KEGG signaling pathways for which no mechanistic details are provided, the models (with qual constructs) contain only topological relationships together with interaction signs. No logical rules specify the effects of (combined) interactions, and these models should be seen as scaffolds to be further parameterized before use in simulation. This can be done either by considering default, yet biologically meaningful, logical functions (e.g., requiring the presence of at least one activator and absence of all inhibitors) [<a
    href="http://identifiers.org/pubmed/14636597"
    title="A structure-based anatomy of the E. coli metabolome">Nobeli2003</a>], by doing further manual refinement of the model (e.g., by literature mining), or by using dedicated experimental data to identify the functions [<a
    href="http://identifiers.org/pubmed/22871648"
    title="State-time spectrum of signal transduction logic models">MacNamara2012</a>].
</p>

<p>
    Several simulation tools now support the SBML Level 3 qual package, including <a href="http://ginsim.org/"
                                                                                     title="Gene Interaction Network simulation">GINsim</a>, <a
    href="http://www.cellnopt.org/"
    title="A flexible pipeline to model protein signalling networks trained to data using various logic formalisms">CellNOpt</a> and <a
    href="http://www.thecellcollective.org/"
    title="The Cell Collective platform">The Cell Collective platform</a>. CellNOpt provides a pipeline to generate logical rules by pruning a general scaffold with all possible rules so as to find the submodel that best describes the data. This can be done using various formalisms [<a
    href="http://identifiers.org/pubmed/23079107"
    title="CellNOptR: a flexible toolkit to train protein signaling networks to data using multiple logic formalisms">Terfve2012</a>] of increasing detail, depending of the data at hand. The Cell Collective platform includes Bio-Logic Builder to facilitate the conversion of biological knowledge into a computational model [<a
    href="http://identifiers.org/pubmed/22871178"
    title="The Cell Collective: toward an open and collaborative approach to systems biology">Helikar2012</a>]. GINsim provides complementary features that allow performing multiple analyses of logical models using powerful algorithms [<a
    href="http://identifiers.org/pubmed/22144167"
    title="Logical modelling of gene regulatory networks with GINsim">Chaouiya2012</a>]. Therefore, relying on a combined use of these tools, one could use the Path2Models qualitative models by training them against data of, for instance, a cell type of interest, and subsequently analyzing the resulting models.
</p>

<h3>References</h3>

<p>
    For more information about the initial effort behind this branch, please refer to the <a
    href="http://biomodels-net.github.io/path2models/"
    title="Path2Models project github page">Github project page of path2models</a>. This project has now been
completed. Models generated from other efforts are added to this branch.
</p>

<p>
    If you use models generated by the Path2Models project, please cite:
</p>
<dl style="padding-left:1em;">
    <dt>
        <strong>Finja Büchel, Nicolas Rodriguez, Neil Swainston, Clemens Wrzodek, Tobias Czauderna, Roland Keller, Florian Mittag, Michael Schubert, Mihai Glont, Martin Golebiewski, Martijn van Iersel, Sarah Keating, Matthias Rall, Michael Wybrow, Henning Hermjakob, Michael Hucka, Douglas B Kell, Wolfgang Müller, Pedro Mendes, Andreas Zell, Claudine Chaouiya, Julio Saez-Rodriguez, Falk Schreiber, Laibe, Camille, Andreas Dräger and Nicolas Le Novère</strong>
    </dt>
    <dd style="margin-left:0;">
        <div
            class="pubtitle">Path2Models: large-scale generation of computational models from biochemical pathway maps.</div>
    </dd>
    <dd style="margin-left:0;">
        <div class="pubjournal"><em>BMC Systems Biology</em> 2013, 7:116</div>

    </dd>
    <dd style="margin-left:0;">
        [<a href="http://identifiers.org/pubmed/24180668"
            title="Access to the publication: 24180668">Access to this publication</a>]
    </dd>
</dl>

<p>If you use models generated from the <a href="https://wiki.nci.nih.gov/pages/viewpage.action?pageId=315491760"
                                           title="PID">Pathway Interaction Database</a>, please cite:</p>
<dl style="padding-left:1em;">
    <dt>
        <strong>Finja Büchel, Clemens Wrzodek, Florian Mittag, Andreas Dräger, Johannes Eichner, Nicolas Rodriguez, Nicolas Le Novère, and Andreas Zell</strong>
    </dt>
    <dd style="margin-left:0;">
        <div class="pubtitle">Qualitative translation of relations from BioPAX to SBML qual.</div>
    </dd>
    <dd style="margin-left:0;">
        <div class="pubjournal"><em>Bioinformatics</em> 2012, 28(20):2648-2653</div>

    </dd>
    <dd style="margin-left:0;">
        [<a href="http://identifiers.org/pubmed/22923304"
            title="Access to the publication: 22923304">Access to this publication</a>]
    </dd>
</dl>

<p>
    For general BioModels Database reference information,
    please see our <a href="citation" title="How can I quote BioModels Database?">citation page</a>.
</p>
