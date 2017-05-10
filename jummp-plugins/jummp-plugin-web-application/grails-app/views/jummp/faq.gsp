<%--
 Copyright (C) 2010-2017 EMBL-European Bioinformatics Institute (EMBL-EBI),
 Deutsches Krebsforschungszentrum (DKFZ)

 This file is part of Jummp.

 Jummp is free software; you can redistribute it and/or modify it under the
 terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
 WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
--%>






<head>
    <meta name="layout" content="${session['branding.style']}/main" />
</head>
<body>
<h2>Frequently Asked Questions</h2>

<ul>
    <li class="faq_title">What is BioModels Database and how does it differ from other resources?
        <ul class="faq_subheading">
            <li><a href="#WHAT_IS_BIOMDB">What is BioModels Database?</a></li>
            <li><a href="#DIFFER_MOD">How does BioModels Database differ from other databases of models?</a></li>
            <li><a href="#DIFFER_PATH">How does BioModels Database differ from other biological pathway databases?</a></li>
            <li><a href="#DIFF_REACTOME">How does BioModels Database differs from Reactome?</a></li>
        </ul>
    </li>
    <li class="faq_title">Citations and contact information
        <ul class="faq_subheading">
            <li><a href="#QUOTE_BIOMDB">How to cite BioModels Database?</a></li>
            <li><a href="#QUOTE_MODEL">How to cite a model present in BioModels Database?</a></li>
            <li><a href="#TEAM_CONTACT">How to contact the team behind BioModels Database?</a></li>
            <li><a href="#ORIGINAL_AUTHOR">I wish to contact the original author(s) of a model, but the listed email does not work. What should I do?</a></li>
        </ul>
    </li>
    <li class="faq_title">Questions dealing with model correctness, reuse and distribution
        <ul class="faq_subheading">
            <li><a href="#MODEL_RELIABILITY">How reliable are the models hosted in BioModels Database?</a></li>
            <li><a href="#REPORT_ERROR">What should I do if I find an error in a model?</a></li>
            <li><a href="#MODEL_REUSE">What are the conditions of use and distribution for unmodified models originating from BioModels Database?</a></li>
            <li><a href="#MODEL_MODIFY">What are the conditions of use and distribution for modified models that originated from BioModels Database?</a></li>
            <li><a href="#MODEL_CONVERSION">Can I convert a model from BioModels Database into another SBML version?</a></li>
        </ul>
    </li>
    <li class="faq_title">Questions dealing with the provided features of BioModels Database
        <ul class="faq_subheading">
            <li><a href="#BROWSE_BIOMDB">How to browse and search BioModels Database?</a></li>
            <li><a href="#TAB_INFO">What information can be found using the model tabs?</a></li>
            <li><a href="#DOWNLOAD_MODEL">How to download a model from BioModels Database?</a></li>
            <li><a href="#RUN_MODEL">What options are available for simulating a model?</a></li>
            <li><a href="#SUBMODEL_GENERATION">How to generate a sub-model from a larger one?</a></li>
            <li><a href="#SBGN">Does BioModels Database provide SBGN maps?</a></li>
            <li><a href="#MATLAB">Why doesn't BioModels Database export models under the MATLAB format?</a></li>
        </ul>
    </li>
    <li class="faq_title">Questions dealing with model submission
        <ul class="faq_subheading">
            <li><a href="#MODEL_SUBMISSION">How to submit a model?</a></li>
            <li><a href="#SUBMIT_BEFORE_PAPER">Can I submit a model before it is described in a published paper?</a></li>
            <li><a href="#ACCESS_AFTER_SUBMISSION">Will a model be publicly accessible immediately after its submission?</a></li>
            <li><a href="#REVIEWER_ACCESS">Can reviewers access unpublished models?</a></li>
            <li><a href="#SUPPORTED_FORMATS">What are the model encoding formats supported?</a></li>
            <li><a href="#MODEL_UPDATE">How to submit a revised version of a model?</a></li>
            <li><a href="#CELLML_CONV">Why does my SBML model contain no species or reactions after a conversion from CellML?</a></li>
            <li><a href="#SUBMIT">Why do I receive error messages when trying to submit a model?</a></li>
        </ul>
    </li>
    <li class="faq_title">Questions dealing with the curation of the models
        <ul class="faq_subheading">
            <li><a href="#MODEL_CURATION">What checks are performed to ensure model correctness?</a></li>
            <li><a href="#MIRIAM">What is MIRIAM?</a></li>
            <li><a href="#MIRIAM_COMPLIANCE">How is MIRIAM compliance ensured?</a></li>
            <li><a href="#ANNOTATION">What is annotation, and what purpose does it serve in a model?</a></li>
            <li><a href="#ANNOTATORS">Who annotates the models?</a></li>
            <li><a href="#ANNOT_IN_SBML">How are annotations stored in SBML?</a></li>
            <li><a href="#FULL_ANNOTATION">When is a model completely annotated?</a></li>
            <li><a href="#CURATION_TOOLS">What are the tools used by the curators of BioModels Database?</a></li>
            <li><a href="#NON_CURA_BRANCH">What is the non-curated branch of BioModels Database?</a></li>
        </ul>
    </li>
    <li class="faq_title">Questions dealing with the underlying software infrastructure
        <ul class="faq_subheading">
            <li><a href="#BIOMDB_SOFTWARE">What are the tools used to develop and run the software infrastructure behind BioModels Database?</a></li>
            <li><a href="#BIOMDB_SOFTWARE_REUSE">Can I install my own version of BioModels Database?</a></li>
        </ul>
    </li>
    <li class="faq_title">Miscellaneous questions
        <ul class="faq_subheading">
            <li><a href="#ID_SCHEME">What is the naming and identifier scheme used in BioModels Database?</a></li>
            <li><a href="#MODEL_AUTHORS">Who are the authors of a model?</a></li>
            <li><a href="#MODEL_SUBMITTER">Who is the submitter of a model?</a></li>
            <li><a href="#MODEL_CREATORS">Who are the encoders of a model?</a></li>
            <li><a href="#ERROR_REACTION">Why do I get an error message stating some reaction modifiers are not declared?</a></li>
            <li><a href="#SESSION_EXPIRED">Why do I see a "session has expired" message when browsing the resource?</a></li>
        </ul>
    </li>
</ul>


<!-- ####################################################################### -->
<!-- What is BioModels Database and how does it differ from other resources? -->
<!-- ####################################################################### -->

<h3 id="WHAT_IS_BIOMDB">What is Biomodels Database?</h3>
<p>
    BioModels Database is a repository of computational models of biological processes. It hosts models described in
    peer-reviewed scientific literature and models generated automatically from pathway resources (Path2Models).
    A large number of models collected from literature are manually
    <a href="#MODEL_CURATION" title="More about model curation (in this FAQ)">curated</a> and
    <a href="#ANNOTATION" title="More about annotation (in this FAQ)">semantically enriched with cross-references</a>
    from external data resources (such as publications, databases of compounds and pathways, ontologies, etc.).
    The resource allows scientific community to store, search and retrieve mathematical models of their interest.
    In addition, features such as generation of sub-models, online simulation, conversion of models into different
    representational formats, and
    <a href="webservices" title="Programmatic access via web services">programmatic access</a>
    via web services, are provided.</p>
<p>
    All models are provided under the terms of the <a href="http://creativecommons.org/publicdomain/zero/1.0/" title="Creative Commons CC0">Creative Commons CC0 Public Domain Dedication</a>, cf. our <a href="http://www.ebi.ac.uk/biomodels-main/termsofuse" title="BioModels Database: terms of use">terms of use</a>. This means that the models are available freely for use, modification and distribution, to all users.
</p>
<p>
    Users can browse and search the content of the repository, and download models in <a href="http://www.sbml.org/" title="SBML">SBML</a> format, as well as various other formats, such as XPP, VCML, SciLab, Octave, BioPAX, PNG, SVG, ... A human readable summary of each model is also available in PDF.
</p>
<p>
    More information can be found in the associated <a href="citation" title="BioModels Database publications">publications</a>.
</p>

<h3 id="DIFFER_MOD">How does BioModels Database differ from other databases of models?</h3>
<p>
    BioModels Database is more than just a <em>repository</em> of models, it is a true <em>database</em>. The models, their controlled annotation and all related information is stored in a set of <a href="http://www.mysql.com" title="MySQL">MySQL</a> tables. This allows users to search not only for particular models based on their internal components elements, but also based on the extensive additional annotation. In addition, this annotation allows the exploration of the relevant linked resources, thereby facilitating the understanding of the concepts upon which the model is founded.
</p>

<h3 id="DIFFER_PATH">Hoes does BioModels Database differ from other biological pathway databases?</h3>
<p>
    BioModels Database is <strong>not</strong> a database of biochemical pathways. The current state of the field of Computational Systems Biology means that these models are largely dominating the resource at the moment, but the scope of BioModels Database itself is larger than just biochemical events. A quantitative model also differs from a pathway in several respects:
</p>
<ul class="faq_list">
    <li>A pathway need not contain quantitative information on the amount of objects, their behaviour, nor on their location.</li>
    <li>A pathway is static, while a model can be instantiated into dynamic simulations.</li>
    <li>A formal model can merge several biochemical reactions into one, or conversely, can contain reactions without counterparts in the corresponding biological context. The purpose is that the <em>simulations</em> performed with the model produce quantitative results commensurate with the available experimental knowledge.</li>
</ul>

<h3 id="DIFF_REACTOME">How does BioModels Database differ from Reactome?</h3>
<p>
    <a href="http://www.reactome.org/" title="Reactome">Reactome</a> is a database of reactions and pathways, not a database of quantitative models. The SBML files exported by Reactome (or KEGG for that matter) do not contain any quantitative information, whether quantities (amount or concentration of species) or kinetics. Reactome aims to describe the human cellular pathways accurately and in great details, not to distribute abstract quantitative description of their functions.
</p>


<!-- ################################# -->
<!-- Citations and contact information -->
<!-- ################################# -->

<h3 id="QUOTE_BIOMDB">How to cite BioModels Database?</h3>
<p>
    Please, have a look at the <a href="citation" title="How to quote BioModels Database?">citation information</a> page.
</p>

<h3 id="QUOTE_MODEL">How to cite a model present in BioModels Database?</h3>
<p>
    The best way to cite a model present in BioModels Database is to state the reference publication associated with the model. You can also mention the model's identifier (of the form "BIOMD" or "MODEL" followed by 10 digits).
</p>

<h3 id="TEAM_CONTACT">How to contact the team behind BioModels Database?</h3>
<p>
    The easiest way to contact the team developing and maintaining the software infrastructure and the content of BioModels Database is to use the following email address: <b>biomodels-net-support</b> AT <b>lists.sf.net</b>.
</p>
<p>
    You can also refer to the information provided on the <a href="contact" title="contact us page">contact us</a> page.
</p>

<h3 id="ORIGINAL_AUTHOR">I wish to contact the original author(s) of a model, but the listed email does not work. What should I do?</h3>
<p>
    This will happen occasionally as people move on to new roles or to new positions in different institutions. There is no easy solution for this, so we would suggest in the first instance to write to the BioModels Database curation team, which can have them look into it on your behalf. Please provide as much information as you have available, and avenues you may have explored already. The curation team can be contacted via: <b>biomodels-cura</b> AT <b>ebi.ac.uk</b>.
</p>


<!-- ################################################################ -->
<!-- Questions dealing with model correctness, reuse and distribution -->
<!-- ################################################################ -->

<h3 id="MODEL_RELIABILITY">How reliable are the models hosted in BioModels Database?</h3>
<p>
    Before being publicly available on BioModels Database, a model passes through a curation pipeline. This ensures its syntactic correctness, semantic soundness, and its correspondence with its reference publication, both in terms of model structure and simulation results. Consequently the structure of a model would not normally change, while its annotation is expected to improve constantly over time.
</p>

<h3 id="REPORT_ERROR">What should I do if I find an error in a model?</h3>
<p>
    The models present in BioModels Database have already been extensively checked and corrected. However, it remains possible that some errors may have crept through our rigorous curation pipeline. If you discover any errors with a specific model, or have any potential concerns, please do <a href="#TEAM_CONTACT" title="How to contact the team behind BioModels Database?">contact us</a>. Comments and bugs for specific models may be submitted directly from the menu bar at the top of the web page describing each model, where you will find a "Submit Model Comment/Bug" link.
</p>

<h3 id="MODEL_REUSE">What are the conditions of use and distribution for unmodified models originating from BioModels Database?</h3>
<p>
    You can use and freely distribute the models present in BioModels Database in their current form. Please refer to the <a href="termsofuse">legal terms of use</a> for more details.
</p>

<h3 id="MODEL_MODIFY">What are the conditions of use and distribution for modified models that originated from BioModels Database?</h3>
<p>
    You can modify and freely distribute a modified version a model, in whole or part, that is present in BioModels Database. The modified model <em>must</em> be renamed, and all references to the BioModels Database identifier and any mention of the copyright holders of the model must be removed. This is to prevent any confusion over precisely which is the original model, and which are modified versions of that model. Please refer to the <a href="termsofuse">legal terms of use</a> for more details.
</p>

<h3 id="MODEL_CONVERSION">Can I convert a model from BioModels Database into another SBML version?</h3>
<p>
    Yes, you can convert a model coming from BioModels Database from the original SBML Level/Version to another, and distribute the converted version freely. In these instances, the converted model is regarded as a modified version. Hence, the converted model <em>must</em> be renamed, and all references to the BioModels Database identifier and any mention of the Copyright Holders of the model must be removed. This is to prevent any confusion over precisely which is the original model, and which are modified versions of that model.Please refer to the <a href="termsofuse">legal terms of use</a> for details.
</p>
<p>
    For models from the curated branch, you can download them in multiple Levels and Versions of SBML via the "Download SBML" link present on top of each model page.
</p>


<!-- ################################################################# -->
<!-- Question dealing with the provided features of BioModels Database -->
<!-- ################################################################# -->

<h3 id="BROWSE_BIOMDB">How to browse and search BioModels Database?</h3>
<p>
    There are two main ways in which the BioModels Database can be browsed:
</p>
<ol class="faq_list">
    <li>The list of all available models is provided for both the <a href="publmodels" title="List of all curated models">curated</a> and <a href="noncuramodels" title="List of all non-curated models">non-curated</a> branch. Links to those lists are available on the home page as well as on the top menu bar of each page.</li>
    <li>The alternative to the list-based browsing is to use the <a href="gotree" title="Browse models via GO tree">tree view of deposited models</a>, which utilises a pruned sub-tree of the <a href="http://www.geneontology.org/">Gene Ontology</a>. In this way, models can be located within which there are elements annotated with a given GO term. Link to this view is available from the home page as well as the always visible top menu.</li>
</ol>
<p>
    Once you have identified a model of interest, you can click on the link (on its identifier) to reach the model description page. Access to further information is offered here through a variety of <a href="#TAB_INFO"><em>tabs</em></a>.
</p>


<h3 id="TAB_INFO">What information can be found using the model tabs?</h3>
<p>
    Here is a brief description of the information available through each of the tabs available on each curated model description page:
</p>
<ol>
    <li><b>Model</b>: model creation information, citation details and abstract, original model submitted, and curator's notes.</li>
    <li><b>Overview</b>: this view is organised into sections providing general 'model' information, such as creation time and publication link; 'mathematical expression' information for reactions listed in the model; 'physical entities' present in the model such as compartments and entities and 'global parameters' that are use in the model. This tab also gives access to the <a href="#SUBMODEL_GENERATION">sub-model generation feature</a>.</li>
    <li><b>Math</b>: all reactions, rules and events are listed in this panel. Each is accompanied by mathematical expressions to illustrate the kinetic information.</li>
    <li><b>Physical entities</b>: entities, initial amounts and additional entity level annotation are provided here. Links to external resources through which more detailed information can be obtained is provided additionally.</li>
    <li><b>Parameters</b>: global and local parameters are listed in this panel.</li>
    <li><b>Curation</b>: an representative plot of the results generated by a simulation of the model is present in this panel. Additional curator's comments and clarifications may also be available.</li>
</ol>


<h3 id="DOWNLOAD_MODEL">How to download a model from BioModels Database?</h3>
<p>
    There are several way to download the models:
</p>
<ul class="faq_list">
    <li>Each model can be downloaded from its own description page, via a "Download SBML" link in the top menu.</li>
    <li>All the models can be downloaded in one single archive. Several archives are available: two per release (one with only the SBML files, one with the SBML and all the export files) and one automatically generated every week (only contains the SBML files). The archives are available on the <a href="ftp://ftp.ebi.ac.uk/pub/databases/biomodels/" title="BioModels Database archives via FTP">FTP server</a>.</li>
    <li>SBML version of the models can be obtained using the <a href="static-pages.do?page=ws">Web Services</a>.</li>
</ul>

<h3 id="RUN_MODEL">What options are available for simulating a model?</h3>
<p>
    Several options are available to simulate the models:
</p>
<p>
    All models in the curated branch can be simulated via the embedded online simulation feature. This can be launched via the "BioModels Online Simulation" item in the "Actions" top menu. Simulations are performed using the <a href="http://www.tbi.univie.ac.at/~raim/odeSolver/" title="SBML ODE Solver Library">SBML ODE Solver Library (SOSlib)</a> and run on the EBI computing cluster. Both the numerical results and a graph are provided to the user once the simulation is complete.
</p>
<img src="http://www.ebi.ac.uk/biomodels/FAQ/img/simulate_model.png" title="Access to the BioModels Database online simulation tool" alt="screenshot presenting the access to the BioModels Database online simulation tool" class="screenshot" />
<p>
    Many models stored in the curated branch have been converted to the <a href="http://jjj.biochem.sun.ac.za/index.html">JWS Online</a> format and can be simulated with its mathematica-based Java applet. This option can be launched via the "BioModels Online Simulation" item in the "Actions" top menu. Also refer to the full <a href="http://jjj.biochem.sun.ac.za/biomodels/" title="list of models from BioModels Database which can be simulated in JWS Online">list of models</a> from BioModels Database which can be simulated in that way.
</p>
<img src="http://www.ebi.ac.uk/biomodels/FAQ/img/jws_simulate_model.png" title="Access to the BioModels Database online simulation tool" alt="screenshot presenting the access to the BioModels Database online simulation tool" class="screenshot" />
<p>
    Models are exported into several different formats, that can be used with various simulators. Some of the formats are specific for particular software tools, such as <a href="http://www.math.pitt.edu/~bard/xpp/xpp.html" title="XPP-Aut">XPP-Aut</a> or <a href="http://www.scilab.org" title="SciLab">SciLab</a>. Other formats, such as <a href="http://www.sbml.org/" title="Systems Biology Markup Language">SBML</a> and <a href="http://www.cellml.org" title="CellML">CellML</a> are common standard formats and can be used with multiple compliant software tools. Once a model has been selected, it can be downloaded using the relevant link from the "Download SBML" or "Other formats" menus, and saved locally. The model can then be simulated using your simulation tool of choice.
</p>
<p>
    Below are screenshots of <a href="BIOMD0000000005" title="Access to the model BIOMD0000000005">Tyson1991_CellCycle_6var</a> model simulated using JWS Online, SciLab, XPP-aut, COPASI and CellDesigner.
</p>
<img src="http://www.ebi.ac.uk/biomodels/doc/JWS.png" alt="JWS Online screenshot" class="screenshot" />
<img src="http://www.ebi.ac.uk/biomodels/doc/SciLab.png" alt="SciLab screenshot" class="screenshot" />
<img src="http://www.ebi.ac.uk/biomodels/doc/XPP.png" alt="XPP screenshot" class="screenshot" />
<img src="http://www.ebi.ac.uk/biomodels/doc/COPASI.png" alt="COPASI screenshot" class="screenshot" />
<img src="http://www.ebi.ac.uk/biomodels/doc/CellDesigner.png" alt="CellDesigner screenshot" class="screenshot" />

<h3 id="SUBMODEL_GENERATION">How to generate a sub-model from a larger one?</h3>
<p>
    BioModels Database provides a feature which allows users to extract components from large scale models into smaller sub-models.
</p>
<p>
    From the "Overview" tab, one can select reactions, entities and parameters (via the check-box alongside them) and generate a sub-model containing these specific elements. Once all the elements of interest have been selected, please click one the "Create a submodel with selected elements" link at the top of the tabbed display. After generation, the newly generated sub-model will be available from a newly created tab and can be downloaded in SBML format.
</p>
<img src="http://www.ebi.ac.uk/biomodels/FAQ/img/submodel.png" alt="sub-model creation screenshot" class="screenshot" />
<p>
    Be aware that the generated sub-models might contain more elements than the ones selected. This is due to the fact that the generation algorithm ensures that the  sub-model is coherent and valid SBML.
</p>

<h3 id="SBGN">Does BioModels Database provide SBGN maps?</h3>
<p>
    BioModels Database provides graphical representations of the models in various formats (PNG, SVG and a dynamic visualisation via a Java applet). Those are available from the "Actions" menu.
</p>
<img src="http://www.ebi.ac.uk/biomodels/FAQ/img/graphical_representation.png" title="Access to the BioModels Database online simulation tool" alt="screenshot presenting the access to the BioModels Database online simulation tool" class="screenshot" />
<p>
    Due to workforce limitations, and the volume of work that generating SBGN exports necessitate, the layout of most of these maps is not currently fully compliant with the specification of <a href="http://www.sbgn.org/Documents/Specifications#Process_Diagram" title="SBGN Specifications of Process_Diagram">SBGN Process Diagrams</a> Level 1 Version 1. However, the BioModels.net team has been involved in SBGN development since it's inception, and is fully committed to support it as much, and as quickly, as possible.
</p>

<h3 id="MATLAB">Why doesn't BioModels Database export models under MATLAB format?</h3>
<p>
    BioModels Database developers are doing their best to provide a variety of export formats. BioModels Database is a public resource, and is totally committed to support open standard formats. Therefore, our first priority is the support of free (non-proprietary) software. In addition, BioModels Database is entirely funded by taxpayers' money, and we feel it is inappropriate to develop software to interface with commercial tools as a priority.
</p>
<p>
    That said, we do provide exports for Octave, which uses a m-file similar to the one used by MATLAB, so you should be able to use it in this environment. Additionally, there exist several packages offering SBML support for MATLAB. See the <a href="http://sbml.org/Software/SBMLToolbox" title="SBML Toolbox">SBML Toolbox</a>, the <a href="http://www.fcc.chalmers.se/sys/products/systems-biology-toolbox-for-matlab" title="Systems Biology Toolbox">Systems Biology Toolbox</a>, and <a href="http://www.mathworks.com/products/simbiology/" title="simbiology">simbiology</a>, the toolbox developed by MathWorks.
</p>


<!-- ####################################### -->
<!-- Questions dealing with model submission -->
<!-- ####################################### -->

<h3 id="MODEL_SUBMISSION">How to submit a model?</h3>
<p>
    Anybody can submit a model to BioModels Database. Even if you are not the actual author or encoder of a model you can submit it for inclusion in BioModels Database. If you do so, you will be recorded as the <em>submitter</em>, not as an author or an encoder. However, we do encourage you to contact the original authors to be sure they are happy with the submission.
</p>
<p>
    If a model is encoded in <a href="http://sbml.org/" title="Systems Biology Markup Language (SBML)">SBML</a> or <a href="http://www.cellml.org/" title="CellML language">CellML</a>, the submission is entirely processed online, via the <a href="submit" title="Model submission page">submission page</a>. If the model is encoded using a different format, please read the section of this FAQ about the <a href="#SUPPORTED_FORMATS" title="Could BioModels Database accept models encoded using my own software-specific format?">supported formats</a>. If the model can be converted into SBML or CellML, please <a href="contact" title="Contact BioModels.net Team">contact us</a>.
</p>
<p>
    Once the model has been submitted, the submitter will receive an email notification with a unique, stable and perennial <i>submission identifier</i>. This identifier is composed of the string "MODEL" followed by 10 digits, for example <b>MODEL1003250000</b> (these digits are actually based on the submission date and time). This identifier can be used to reference the model in publications, and once the model is publicly available on BioModels Database, users can directly access the model using this identifier.
</p>
<p>
    BioModels Database currently only publishes models which have been described in a peer reviewed scientific publication. Therefore, authors are encouraged to submit models before publication of the associated paper (and will receive an identifier that they can used in the publication), but the models will only be publicly available on BioModels Database once the paper has been published.
</p>
<p>
    In order to access a model, knowing it submission identifier, you can use the URL: <b>http://www.ebi.ac.uk/biomodels-main/</b> followed by the identifier. For example: <a href="http://www.ebi.ac.uk/biomodels-main/MODEL1002160000" title="Access to the model: MODEL1002160000">http://www.ebi.ac.uk/biomodels-main/MODEL1002160000</a>.
</p>

<h3 id="SUBMIT_BEFORE_PAPER">Can I submit a model before it is described in a published paper?</h3>
<p>
    Yes. Models can be submitted prior to the publication of the associated paper(s). It is actually strongly advised to do so: at the submission time, each model is assigned a unique and perennial identifier which allows users to access and retrieve it. This identifier can be used by authors as a reference in their publications.
</p>
<p>
    Please refer to the section of this FAQ about the <a href="#ACCESS_AFTER_SUBMISSION" title="Will a model be publicly accessible immediately after its submission?">access to a model after its submission</a> for more information about when a model will be publicly available from BioModels Database.
</p>

<h3 id="ACCESS_AFTER_SUBMISSION">Will a model be publicly accessible immediately after its submission?</h3>
<p>
    No. Models are not directly visible and retrievable by the public as soon as they are submitted to BioModels Database. From submission to their public release, all models undergo various automated and manually performed curation and annotation steps to ensure a consistent level of quality and accuracy.
</p>
<p>
    Moreover, all models are only made publicly available from BioModels Database after the publication of the corresponding papers. If you submitted a model before the publication of the associated paper, please <a href="contact" title="Contact BioModels.net Team">keep us informed</a> of the status of your publication. That way, we can make the model available from BioModels Database as quickly as possible.
</p>
<p>
    Finally, new models are usually made publicly available during "releases". There are between 2 to 4 releases of the database a year. Please <a href="contact" title="Contact BioModels.net Team">contact us</a> if you need your model to be available earlier.
</p>

<h3 id="REVIEWER_ACCESS">Can reviewers access unpublished models?</h3>
<p>
    BioModels Database can provide access to unpublished models to reviewers.
</p>
<p>
    This is not automatic, so the model submitter needs to request it (for example emailing us at <b>biomodels-cura</b> AT <b>ebi.ac.uk</b> or mentioning it in the comment field of the submission form). In this case, we will provide the submitter with a URL which can be used to download the model encoded in SBML. If security is a concern, we can supply a protected access, where reviewers will be asked for a password to access the model.
</p>
<p>
    If you have more specific needs, please don't hesitate to <a href="contact" title="Contact BioModels.net Team">contact us</a>.
</p>

<h3 id="SUPPORTED_FORMATS">What are the model encoding formats supported?</h3>
<p>
    Currently BioModels Database only provides full support for a subset of the modelling formats landscape. This is mainly due to the fact that we strongly believe in interoperability, which requires distributing to the community models in semantically rich, standard formats. The full list of supported modelling formats is available on the <a href="submit" title="Submit a model to BioModels Database">submission page</a>, and basically covers SBML (all Levels and Versions) and CellML (1.0 and 1.1).
</p>
<p>
    However, we are aware that those formats do not cover all modelling techniques used today. As we are committed to encourage models sharing and reuse, we will strive to find a solution to host models originally encoded in MATLAB or from other generic modelling platforms. So, if you cannot convert your model(s) to one of the fully supported formats, please do contact us (at biomodels-cura AT ebi.ac.uk, which is a non public mailing list dedicated to our curators).
</p>

<h3 id="MODEL_UPDATE">How to submit a revised version of a model?</h3>
<dl>
    <dt>
        <strong>The model is not yet published in BioModels Database</strong>
    </dt>
    <dd>
        <div>You can us a revised version at: <b>biomodels-cura</b> AT <b>ebi.ac.uk</b> together with the model submission identifier and a comment explaining which modifications were made. This mailing list is not public and the only recipients are the curators of BioModels Database, so you can safely send us models not yet published this way.</div>
    </dd>
    <dt>
        <strong>The model has already been published in BioModels Databse</strong>
    </dt>
    <dd>
        <div>If you modified a model originally coming from BioModels Database, you can submitted it back. However, you need to be aware that BioModels Database currently only hosts models which have been described in a scientific publication. This means that if you made significant changes to the model, you should consider writing a paper about it. Please refer to the <a href="#MODEL_SUBMISSION" title="How to submit a model?">submission procedure</a> for more information. If you only made minor changes to the model, please follow the procedure described above (for not yet published models).</div>
    </dd>
</dl>

<h3 id="CELLML_CONV">Why does my SBML model contain no species or reactions after a conversion from CellML?</h3>
<p>
    The issue is that there is no annotation in the <a href="http://www.cellml.org" title="CellML">CellML</a> file that could help the converter to determine if a certain variable is a SBML species, parameter or compartment. And the same applies to any CellML equation. The current version of the converter is not trying to do any complicated guesses and therefore converts every CellML variable into a SBML parameter and all the equations into rules. We are currently trying to define some annotations that would allow to create better SBML files from CellML and vice-versa, the ultimate goal would be to be able to do a round-trip conversion, starting either from a SBML model or a CellML model, without loosing any information.
</p>

<h3 id="SUBMIT">Why do I receive error messages when trying to submit a model?</h3>
<p>
    Sometimes, trying to submit a model to BioModels Database with the <a href="submit-model.do">submission interface</a> results in an error message of the type:
</p>
<pre>
    Unable to successfully complete the submission of the model contained within the file:

    C:\Documents and Settings\user\models\My Model.xml
    due to the following errors:

    org.w3c.dom.ls.LSException: no protocol:
    /ebi/research/compneur/WWW/biomodels/models-main/uplo/C:\Documents and Settings\user\models\My Model.xml
</pre>
<p>
    This problem is due to a bug in certain versions of Microsoft Internet Explorer which do not correctly strip the file path before the filename. Commonly, this type of error may be seen in browsers where all image links are broken, and point to the original location, for example of the webdesigner's local disk. In these instances, we would advocate the use of a different web browser. There are several free open source alternative one that are available for all platforms, such as <a
    href="http://www.mozilla.org/en-US/" title="Firefox">Firefox</a> or <a href="http://www.seamonkey-project.org/" title="Seamonkey">Seamonkey</a>.
</p>


<!-- ################################################# -->
<!-- Questions dealing with the curation of the models -->
<!-- ################################################# -->

<h3 id="MODEL_CURATION">What checks are performed to ensure model correctness?</h3>
<p>
    Before being made publicly available through BioModels Database, models are thoroughly verified. This involves several types of checks, some automated and some manually performed by a team of curators. The following checks are performed:
</p>
<ul class="faq_list">
    <li>The syntax of the encoded file is verified. This implies a set of correct hierarchy of elements and attributes, a consistent use of identifiers, but also a deeper verification of the consistency of the model depending on its format (e.g. a parameter controlled by a rule cannot be declared constant in SBML).</li>
    <li>The correspondence between the encoded model and the model described in the published article is checked. This implies verification of the structure of the reaction network, the formalisms used, the values of the parameters, the amounts and the units.</li>
    <li>Finally the model is simulated to see if the published results can be reproduced. For this step, a different simulator is employed than the one used by the original authors, in order to ensure the reproducibility of the results.</li>
</ul>
<p>
    Those checks come from the <a href="#MIRIAM" title="MIRIAM guidelines">MIRIAM</a> set of guidelines.
</p>

<h3 id="MIRIAM">What is MIRIAM?</h3>
<p>
    MIRIAM is the <a href="http://co.mbine.org/standards/miriam" title="More information about MIRIAM">Minimum Information Required in the Annotation of Models</a> (<a href="http://identifiers.org/pubmed/16333295" title="Access to the 2005 publication in Nature Biotechnology about MIRIAM">publication</a> and <a href="http://www.ebi.ac.uk/biomodels/doc/MIRIAM_06Decr2005.pdf" title="PDF version of the press release">press Release</a>). Initiated by the <a href="http://biomodels.net/" title="BioModels.net initiative">BioModels.net</a> project, it is a set of guidelines defining how a model should be encoded and annotated in order to be successfully distributed, exchanged and ultimately reused.
</p>
<p>
    In particular MIRIAM requires that a model provides all the information necessary to instantiated in a simulation, such as the initial conditions. In addition, the reaction graph generated from this simulation must reproduce the results of the original publication, in which the model was first described. Moreover, the MIRIAM guidelines require that all model components contain sufficient controlled annotation such that <em>each</em> component can be unambiguously identified. The <a href="http://www.ebi.ac.uk/miriam/" title="MIRIAM Registry">MIRIAM Registry</a> and <a href="http://identifiers.org/" title="Identifiers.org">Identifiers.org</a> have been developed specifically to provide generation and resolution services for unique and perennial identifiers to be used in controlled annotations.
</p>
<p>
    All models stored in the curated branch of BioModels Database are <a href="#MIRIAM_COMPLIANCE" title="How is MIRIAM compliance ensured?">MIRIAM-compliant</a>.
</p>

<h3 id="MIRIAM_COMPLIANCE">How is MIRIAM compliance ensured?</h3>
<p>
    All the models in the curated branch are fully <a href="#MIRIAM" title="Go to the 'MIRIAM' section of this FAQ">MIRIAM</a> compliant. Each of the MIRIAM requirements is satisfied in the following way:
</p>
<dl>
    <dt>
        <strong>Models be encoded in a public standard format</strong>
    </dt>
    <dd>
        <div>All models in BioModels Database are converted into valid SBML.</div>
    </dd>
    <dt>
        <strong>Models must be clearly related to a single reference</strong>
    </dt>
    <dd>
        <div>Each model is derived from a scientific publication.</div>
    </dd>
    <dt>
        <strong>The model must correspond to the biological processes represented in the publication</strong>
    </dt>
    <dd>
        <div>Models are manually checked to confirm accurate biological representation in the model.</div>
    </dd>
    <dt>
        <strong>The simulation results generated from the model must reflect those in the reference publication</strong>
    </dt>
    <dd>
        <div>Models are manually checked to confirm accurate simulation results are generated from the model. The result of this step is available in the "Curation" tab.</div>
    </dd>
    <dt><strong>External data resources annotation</strong></dt>
    <dd>
        <div>Models elements are well annotated.</div>
    </dd>
</dl>

<h3 id="ANNOTATION">What is annotation, and what purpose does it serve in a model?</h3>
<p>
    Model elements can describe a plethora of different entities, such as genes, proteins or metabolites. The usage of free text or non-standard nomenclature to identify those elements is not reliable, as it can introduce ambiguity to the model components, such that subsequent users of a model would struggle to identify the precise entities involved.
</p>
<p>
    Annotation is useful to identify model elements. This ensures that users will be able to understand the models and increases the possibility of their reuse. Moreover, annotation is a key element for processes such as data comparison, data integration and data conversion. Finally, this also allows provision of accurate search engines (BioModels Database makes heavy use of annotation when users search for models of interest).
</p>
<p>
    Models in BioModels Database are provided with consistent annotation using unambiguous identifiers. Those identifiers are generated by the <a href="http://www.ebi.ac.uk/miriam/" title="MIRIAM Registry">MIRIAM Registry</a> and <a href="http://identifiers.org/" title="Identifiers.org">Identifiers.org</a> services. They can be used to reference records from external databases (such as Taxonomy, EMBL-Bank or UniProt), terms from ontologies (such as Gene Ontology, SBO or ChEBI), publications, etc.
</p>
<p>
    For more information about annotation, please refer our <a href="http://www.ebi.ac.uk/biomodels-main/annotation" title="BioModels Database Annotation Information">general introduction to the annotation of models</a>.
</p>

<h3 id="ANNOTATORS">Who annotates the models?</h3>
<p>
    Models can be submitted to BioModels Database already annotated. In this case, any existing annotations encoded using the annotation scheme developed for <a href="http://sbml.org/Documents" title="SBML specs">SBML</a> (introduced since Level 2 Version 2 and supported by numerous tools) will be extracted and recorded in BioModels Database.
</p>
<p>
    Our curation team check any existing annotation and add new ones as necessary.
</p>

<h3 id="ANNOT_IN_SBML">How are annotations stored in SBML?</h3>
<p>
    The annotation of each model component is stored in the corresponding SBML element using the a scheme initially designed by Nicolas Le Novère and Andrew Finney, and now part of <a href="http://sbml.org/Documents" title="SBML specs">SBML</a> (since Level 2 Version 2). It relies on the use of <a href="http://www.w3.org/RDF/" title="RDF">RDF</a>, <a href="http://www.dublincore.org/" title="Dublin Core">Dublin Core</a>, <a href="http://www.w3.org/TR/vcard-rdf/" title="vCard">vCard</a> and the <a href="http://biomodels.net/qualifiers/">BioModels.net qualifiers</a>.
</p>
<img src="http://www.ebi.ac.uk/biomodels/FAQ/img/MIRIAM_annotations.png" title="SBML scheme for annotation encoding" alt="Piece of SBML showing some annotation" class="screenshot" />
<p>
    Please refer to the <a href="http://sbml.org/Documents/Specifications" title="SBML Specifications">SBML specification</a> for more information about the annotation scheme.
</p>

<h3 id="FULL_ANNOTATION">When is a model completely annotated?</h3>
<p>
    Annotating each model component with the most relevant resource records takes great efforts, especially since the number of submitted models has grown rapidly. For example, the 25st release of BioModels Database (18th June 2013) contains <b>171,432</b> cross-references (links to external resources contained in the annotations). This number need to be compared with the total number of species (156,665) and relationships (174,986 this number including reactions, rate rules, events and assignment rules) involved in the existing 963 models. Therefore current annotations do not cover all model elements.
</p>
<p>
    Besides manpower limitations, this discrepancy is sometimes due to a lack of adequate or suitable resources for annotation, for instance for molecular entities which are created only for simulation purposes. Moreover, biological data resources are often slightly lagging behind newly generated knowledge, and it is possible that a particular resource does not offer the relevant information at the time the model is annotated. In the case of hierarchical controlled vocabularies, such as Gene Ontology or ChEBI, there is the option to use a term at a higher level of abstraction if the required precise term does not currently exist. Most often, one can always add some information, even if not optimal. Hence, model annotation needs to be, and indeed is, a continuous process.
</p>

<h3 id="CURATION_TOOLS">What are the tools used by the curators of BioModels Database?</h3>
<p>
    Our curators use a wide range of tools to perform their curation tasks: <a href="http://www.celldesigner.org/">CellDesigner</a>, <a href="http://www.copasi.org/">COPASI</a>, <a href="http://sbw.kgi.edu/software/jarnac.htm">Jarnac</a>, <a href="http://sbml.org/Software/MathSBML">MathSBML</a>, <a href="http://sbw.kgi.edu/Simulation2005/">RoadRunner</a>, <a href="http://www.tbi.univie.ac.at/~raim/odeSolver/">SBMLOdeSolver</a>, <a href="http://www.ebi.ac.uk/compneur-srv/SBMLeditor.html">SBMLeditor</a>, <a href="http://www.semanticsbml.org/">SemanticSBML</a>, <a href="http://jigcell.cs.vt.edu/">JigCell</a>, <a href="http://www.math.pitt.edu/%7Ebard/xpp/xpp.html">XPP-Aut</a>, ...
</p>
<p>
    Moreover, they also use tools such as <a href="http://www.gnuplot.info/" title="Gnuplot">Gnuplot</a> and various custom scripts.
</p>

<h3 id="NON_CURA_BRANCH">What is the non-curated branch of BioModels Database?</h3>
<p>
    The success and rapid growth of BioModels databases has led to a couple of issues, both of which are handled using the non-curated branch of BioModels Database. Firstly, BioModels Database faces an ever-growing, and increasingly complex, number of model submissions which are growing at a pace that exceeds our curation capacity. Secondly, many of the models submitted, while syntactically correct, cannot be curated for various reasons. Hence, the non-curated branch of BioModels Database is a public holding area for models that have yet to be curated, or cannot be curated for various reasons. The models held in the non-curated branch are of four types:
</p>
<ol class="faq_list">
    <li>Models that are perfectly fine, but have yet to be curated.</li>
    <li>Models that are not curatable, since they are not <a href="#MIRIAM" title="Go to the 'MIRIAM' section of this FAQ">MIRIAM</a> compliant.</li>
    <li>Models that are not currently curatable, since they are not kinetic models (e.g. Flux Balance Analysis models).</li>
    <li>Models for which SBML supports only part of the description, most of it being in annotation (e.g. spatial models).</li>
</ol>
<p>
    Note that all the models in the non-curated branch have nevertheless passed the XML check, SBML syntax check and SBML consistency checks.
</p>


<!-- ############################################################# -->
<!-- Questions dealing with the underlying software infrastructure -->
<!-- ############################################################# -->

<h3 id="BIOMDB_SOFTWARE">What are the tools used to develop and run the software infrastructure behind BioModels Database?</h3>
<p>
    The infrastructure is powered by <a href="http://tomcat.apache.org/" title="Tomcat">Apache Tomcat</a>, the web services use <a href="http://ws.apache.org/axis/" title="Axis">Axis</a>, the database runs <a href="http://dev.mysql.com/">MySQL</a>, the search is powered by <a href="http://lucene.apache.org/" title="Apache Lucene">Lucene</a>, ...
</p>
<p>
    For more information about the software infrastructure, please refer to:
</p>
<ul>
    <li><a href="http://sourceforge.net/projects/biomodels/" title="Project on SourceForge.net">BioModels project on SourceForge.net</a> (source code of the infrastructure, the library and the converters, as well as the bug trackers)</li>
    <li><a href="http://www.ebi.ac.uk/biomodels-main/develop" title="Installation guide">Developer's page</a> (guide explaining how to create your own custom installation of the infrastructure)</li>
</ul>
<p>
    BioModels Database makes use of many third-party free software such as: <a href="http://sbml.org/Software/libSBML" title="libSBML">libSBML</a>, <a href="http://jsbml.sourceforge.net/" title="JSBML">JSBML</a>, <a href="http://lucene.apache.org/">Lucene</a>, <a href="http://xml.apache.org/xalan-j/">Xalan</a>, <a href="http://xerces.apache.org/xerces2-j/">Xerces</a>, <a href="http://www.biopax.org/paxtools.php">Paxtools</a>, ...
</p>
<p>
    A new open community project has been launched: <a href="https://bitbucket.org/jummp/jummp/" title="JUst a Model Management Platform">Jummp</a>. It aims to provide the next generation model management platform and will ultimately be used as the software platform behind BioModels Database, as well as other projects and communities.
</p>

<h3 id="BIOMDB_SOFTWARE_REUSE">Can I install my own version of BioModels Database?</h3>
<p>
    The models can be <a href="#MODEL_REUSE" title="What are the conditions of use and distribution for unmodified models originating from BioModels Database?">freely reused</a> and the software running BioModels Database itself is an open source project distributed under the GNU General Public License. For more information, please refer to the <a href="#BIOMDB_SOFTWARE" title="What are the tools used to develop and run the software infrastructure behind BioModels Database?">software infrastructure</a> entry of this FAQ.
</p>
<p>
    All converters are also available under an open source license and can be downloaded from the <a href="https://sourceforge.net/projects/biomodels/files/" title="Download files from BioModels SF project">download section of the BioModels SourceForge repository</a>.
</p>
<p>
    In order to store your own models or just plug part(s) of the BioModels Database infrastructure into your own, you'll need to setup your own local instance of the repository. In order to help you through this procedure, we created the following page: <a href="http://www.ebi.ac.uk/biomodels-main/develop" title="How to install you own instance of the repository">development with BioModels Database source code</a>.
</p>


<!-- ####################### -->
<!-- Miscellaneous questions -->
<!-- ####################### -->

<h3 id="ID_SCHEME">What is the naming and identifier scheme used in BioModels Database?</h3>
<p>
    During the <a href="#MODEL_CURATION" title="Go to: curators">curation process</a>, the <em>curators</em> will give an appropriate name to each model. This follows the general scheme: <em>AuthorYear_Topic_Method</em>. For example, Edelstein1996_EPSP_AChEvent refers to the model <a href="http://www.ebi.ac.uk/biomodels-main/BIOMD0000000001" title="Go to model: BIOMD0000000001">BIOMD0000000001</a>.
</p>
<p>
    At the time of submission, a unique submission identifier assigned to the model. It is composed of the character sequence "MODEL" followed by ten digits extracted from the timestamp of submission. When a model is moved to the curated branch, a new model identifier is generated and assigned to it. This identifier is composed of the character sequence "BIOMD" followed by ten digits reflecting the model's position in the branch (for example "BIOMD0000000216" for the 216th model successfully curated). Both identifiers are unique and permanent, and will never be re-assigned to a different model, even if for some reason a particular model must be retracted from the database. Both identifiers can be used to retrieve the model and quoted in subsequent publications.
</p>

<h3 id="MODEL_AUTHORS">Who are the authors of a model?</h3>
<p>
    The <em>authors</em> of a model are the individuals who initially described the model in a peer-reviewed publication.
</p>

<h3 id="MODEL_SUBMITTER">Who is the submitter of a model?</h3>
<p>
    The <em>submitter</em> of a model is the person who actually submitted the model and therefore requested its addition in BioModels Database. Anybody can submit a model to BioModels Database. Please refer to <a href="#MODEL_SUBMISSION" title="Model submission procedure">the submission procedure</a>, if you want to know more.
</p>

<h3 id="MODEL_CREATORS">Who are the encoders of a model?</h3>
<p>
    The <em>encoders</em> of a model are the individuals who actually encoded the model in its present form, based on the published article. These individuals could be the authors of the publication, members of the curation team, or any other scientists who decided to encode the published article.
</p>

<h3 id="ERROR_REACTION">Why do I get an error message stating some reaction modifiers are not declared?</h3>
<p>
    Some models contain reactions where the kinetics are modified by compounds that are not themselves reactants. These compounds are not consumed or produced by the reaction, and are known as modifiers. This error occurs when some software tools do not explicitly list the modifiers contained in the model reactions. A consistency check has been implemented to identify missing modifier declarations, and is now applied to all models submitted to BioModels Database. This error should not happen any more.
</p>

<h3 id="SESSION_EXPIRED">Why do I see a "session has expired" message when browsing the resource?</h3>
<p>
    If you see a message stating "Your session has expired. Please reload the main model page." while browsing the database or trying to submit a comment or a bug report about a specific model, please first check that you are using the proper URL to access the resource (the string "compneur-srv" should not be present in the URL).
</p>
<p>
    For example, if you were using:
    <br />
    <a href="http://www.ebi.ac.uk/compneur-srv/biomodels-main/publ-model.do?mid=BIOMD0000000020" title="Obsolete URL to access BIOMD0000000020">http://www.ebi.ac.uk/compneur-srv/biomodels-main/publ-model.do?mid=BIOMD0000000020</a>
    <br />
    please instead use:
    <br />
    <a href="http://www.ebi.ac.uk/biomodels-main/publ-model.do?mid=BIOMD0000000020" title="URL to access BIOMD0000000020">http://www.ebi.ac.uk/biomodels-main/publ-model.do?mid=BIOMD0000000020</a> (same URL without the <i>compneur-srv</i> part).
    <br />
    or, even simpler:
    <br />
    <a href="http://www.ebi.ac.uk/biomodels-main/BIOMD0000000020" title="Proper URL to access BIOMD0000000020">http://www.ebi.ac.uk/biomodels-main/BIOMD0000000020</a> (same URL with only the model identifier).
</p>
<p>
    If that does not solve your problem, please <a href="#TEAM_CONTACT" title="How to contact the team behind BioModels Database?">contact us</a>, mentioning the URL you were using.
</p>

<br />

</body>
<content tag="faq">
    selected
</content>
<content tag="title">
    <g:message code="${titleCode}" default="Frequently Asked Questions" />
</content>
