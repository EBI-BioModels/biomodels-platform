<%--
 Copyright (C) 2010-2020 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
    <meta name="layout" content="${session['branding.style']}/main"/>
    <title>${titlePage}</title>
    <style type="text/css">
        .row {

        }
        .faq_title {
            font-weight: bold;
            color: #aa2222;
        }

        .faq_subheading li {
            font-weight: normal;
        }
    </style>
</head>

<body>
<div class="row">
    <div class="columns small-12 medium-12 large-12">
        <h2>Frequently Asked Questions</h2>
    </div>
</div>

<div class="row faq-section-box">
    <div class="columns small-12 medium-2 large-2">
        <ol start="1" type="I">
            <li class="faq_title">What is BioModels and how does it differ from other resources?
                <ol class="faq_subheading">
                    <li><a href="#WHAT_IS_BIOMDB">What is BioModels Database?</a></li>
                    <li><a href="#DIFFER_MOD">How does BioModels Database differ from other databases of models?</a></li>
                </ol>
            </li>
        </ol>
    </div>
    <div class="columns small-12 medium-10 large-10">
        <h3 id="WHAT_IS_BIOMDB">What is BioModels?</h3>

        <p>BioModels Database is a repository of mathematical models representing biological systems.&nbsp It currently hosts
        a range of models describing processes like signalling; protein-drug interaction interactions; metabolic pathways;
        epidemic models and many more. The&nbsp;models that BioModels hosts are&nbsp;usually described in peer-reviewed
        scientific literature and in some cases, they are generated automatically from pathway resources (Path2Models). These
        models are manually <a title="More about model curation (in this FAQ)" href="#MODEL_CURATION">curated</a> and <a
            title="More about annotation (in this FAQ)" href="#ANNOTATION">semantically enriched with
            cross-references</a>&nbsp to external data resources (such as publications, databases of compounds and pathways, ontologies, etc.). BiModels allows the scientific community to store, search and retrieve mathematical models of their interest. In addition to offering support for supporting models in different formats and enabling their conversion between&nbsp;different representational formats, the database offers&nbsp;<a
            title="Programmatic access via web services"
            href="https://bitbucket.org/biomodels/jummp-biomodels/wiki/Web%20Services">programmatic access</a>&nbsp;to developers via web services.
        </p>

        <p>All models are provided under the terms of the <a title="Creative Commons CC0"
                                                             href="http://creativecommons.org/publicdomain/zero/1.0/">Creative
            Commons CC0 Public Domain Dedication</a>, cf. our <a
            title="BioModels Database: terms of use"
            href="https://www.ebi.ac.uk/biomodels/termsofuse">terms of use</a>. This means that the models are available
        freely for use, modification and distribution, to all users.
        </p>

        <p>Users can browse and search the content of the repository, and download models in
            <a title="SBML" href="http://www.sbml.org/">SBML</a> format, as well as various other formats, such as XPP,
        VCML, SciLab, Octave, BioPAX, PNG, SVG, ... A human readable summary of each model is also available in PDF.
        </p>

        <p>More information can be found in the associated <a title="BioModels Database publications"
                                                              href="citation">publications</a>.</p>

        <h3 id="DIFFER_MOD">How does BioModels Database differ from other databases of models?</h3>

        <p>Unlike other pathway databases like&nbsp;<a title="Reactome"
                                                       href="https://www.reactome.org/">Reactome</a>, BioModels contains
        quantitative information representing the&nbsp;quantities (amount or concentration of species) or kinetics in a model.
        A formal model can merge several biochemical reactions into one, or conversely, it can contain reactions without
        counterparts in the corresponding biological context. Having a&nbsp;quantitative representative model allows a user to
        carry out&nbsp;<em>simulations</em>&nbsp;where&nbsp;a model produces quantitative results commensurate with the available
        experimental knowledge.
        </p>

        <p>In addition to this, BioModels has its own controlled annotation and vocabulary which allows users to search not
        only for particular models based on their internal components elements, and&nbsp;extensive additional annotation added to
        the model through&nbsp;manual curation. These annotations increase&nbsp;access and visibility,&nbsp;both for the model and relevant
        resources linked to the model on&nbsp;BioModels, thereby facilitating the understanding of the concepts upon which the
        model is founded.</p>
    </div>
</div>

<div class="row">
    &nbsp;
</div>

<div class="row faq-section-box">
    <div class="columns small-12 medium-2 large-2">
        <ol start="2" type="I" class="topic">
            <li class="faq_title">Citations and contact information
                <ol class="faq_subheading" style="list-style-type: decimal;">
                    <li><a href="#QUOTE_BIOMDB">How to cite BioModels Database?</a></li>
                    <li><a href="#QUOTE_MODEL">How to cite a model present in BioModels Database?</a></li>
                    <li><a href="#TEAM_CONTACT">How to contact the team behind BioModels Database?</a></li>
                    <li><a
                        href="#ORIGINAL_AUTHOR">I wish to contact the original author(s) of a model, but the listed email does not work. What should I do?</a>
                    </li>
                </ol>
            </li>
        </ol>
    </div>
    <div class="columns small-12 medium-10 large-10">
        <h3 id="quote-biomodels">How to cite BioModels Database?</h3>

        <p>Please, have a look at the <a title="How to quote BioModels Database?" href="citation">citation information</a> page.
        </p>

        <h3 id="quote-model">How to cite a model present in BioModels Database?</h3>

        <p>The best way to cite a model present in BioModels Database is to state the reference publication associated
        with the model. You can also mention the model's identifier (of the form "BIOMD" or "MODEL" followed by 10 digits).</p>

        <h3 id="TEAM_CONTACT">How to contact the team behind BioModels Database?</h3>

        <p>The easiest way to contact the team developing and maintaining the software infrastructure and the content of
        BioModels Database is to use the following email address:
            <strong>biomodels-net-support</strong> AT <strong>lists.sf.net</strong>.
        </p>

        <p>You can also refer to the information provided on the <a title="contact us page" href="contact">contact us</a> page.
        </p>

        <h3 id="ORIGINAL_AUTHOR">I wish to contact the original author(s) of a model, but the listed email does not
        work.
        What should I do?</h3>

        <p>This will happen occasionally as people move on to new roles or to new positions in different institutions.
        There is no easy solution for this, so we would suggest in the first instance to write to the BioModels Database
        curation team, which can have them look into it on your behalf. Please provide as much information as you have
        available, and avenues you may have explored already. The curation team can be contacted via:
            <strong>biomodels-cura</strong> AT <strong>ebi.ac.uk</strong>.
        </p>
    </div>
</div>

<div class="row">
    &nbsp;
</div>

<div class="row faq-section-box">
    <div class="columns small-12 medium-2 large-2">
        <ol start="3" type="I">
            <li class="faq_title">Questions dealing with model correctness, reuse and distribution
                <ol class="faq_subheading">
                    <li><a href="#MODEL_RELIABILITY">How reliable are the models hosted in BioModels Database?</a></li>
                    <li><a href="#REPORT_ERROR">What should I do if I find an error in a model?</a></li>
                    <li><a
                        href="#MODEL_REUSE">What are the conditions of use and distribution for unmodified models originating from BioModels Database?</a>
                    </li>
                    <li><a
                        href="#MODEL_MODIFY">What are the conditions of use and distribution for modified models that originated from BioModels Database?</a>
                    </li>
                    <li><a href="#MODEL_CONVERSION">Can I convert a model from BioModels Database into another SBML version?</a>
                    </li>
                </ol>
            </li>
        </ol>
    </div>
    <div class="columns small-12 medium-10 large-10">
        <h3 id="MODEL_RELIABILITY">How reliable are the models hosted in BioModels Database?</h3>

        <p>Before being publicly available on BioModels Database, a model passes through a stringent curation pipeline. This
        ensures its syntactic correctness, semantic soundness, and its correspondence with its reference publication, both in
        terms of model structure and simulation results. Consequently the structure of a model would not normally change,
        while its annotation is expected to improve constantly over time.</p>

        <h3 id="REPORT_ERROR">What should I do if I find an error in a model?</h3>

        <p>The models present in BioModels Database have already been extensively checked and corrected. However, it remains
        possible that some errors may have crept through our rigorous curation pipeline. If you discover any errors with a
        specific model, or have any potential concerns, please do <a
            title="How to contact the team behind BioModels Database?"
            href="#TEAM_CONTACT">contact us</a>. Comments and bugs for specific models may be submitted directly from the menu
        bar at the top of the web page describing each model, where you will find a "Submit Model Comment/Bug" link.
        </p>

        <h3 id="MODEL_REUSE">What are the conditions of use and distribution for unmodified models originating from
        BioModels
        Database?</h3>

        <p>You can use and freely distribute the models present in BioModels Database in their current form. Please refer to the <a
            href="termsofuse">legal terms of use</a> for more details.</p>

        <h3 id="MODEL_MODIFY">What are the conditions of use and distribution for modified models that originated from
        BioModels Database?</h3>

        <p>You can modify and freely distribute a modified version a model that is present in BioModels Database,&nbsp;in whole
        or part. A modified model is defined as one where an&nbsp;&nbsp;existing model is extended/reduced/merged with other models.
        In this case, there is a change in the biological description and components.</p>

        <p>The modified model <em>must</em> be renamed, and a new submission to BioModels must&nbsp;be made. This new model will
        then undergo the curation process again as a separate model. When&nbsp;curating&nbsp;the modified model, a curator will also
        add a&nbsp;reference to&nbsp;the parent model on BioModels from which it&nbsp;was derived.
        </p>

        <p>Please refer to the <a href="termsofuse">legal terms of use</a> for more details.</p>

        <h3 id="MODEL_CONVERSION">Can I convert a model from BioModels Database into another SBML version?</h3>

        <p>Conversion of SBML model files between different levels/versions is necessary for&nbsp;&nbsp;users who use tools that do
        not support the versions/levels of the original SBML file. The current version of BioModels does not provide an
        automatic conversion between different SBML levels/versions. You can do this offline using tools like COPASI.</p>

        <p>In the next release of BioModels, we aim to provide automatic SBML convertors which will allow you to download
        the model file in different SBML versions.</p>
    </div>
</div>

<div class="row">
    &nbsp;
</div>

<div class="row faq-section-box">
    <div class="columns small-12 medium-2 large-2">
        <ol start="4" type="I">
            <li class="faq_title">Questions dealing with the provided features of BioModels Database
                <ol class="faq_subheading">
                    <li><a href="#BROWSE_BIOMDB">How to browse and search BioModels Database?</a></li>
                    <li><a href="#TAB_INFO">What information can be found using the model tabs?</a></li>
                    <li><a href="#DOWNLOAD_MODEL">How to download a model from BioModels Database?</a></li>
                    <li><a href="#SBGN">Does BioModels Database provide SBGN maps?</a></li>
                    <li><a href="#MATLAB">Does&nbsp;BioModels Database export models under the MATLAB format?</a></li>
                </ol>
            </li>
        </ol>
    </div>
    <div class="columns small-12 medium-10 large-10">
        <h3 id="BROWSE_BIOMDB">How to browse and search BioModels Database?</h3>

        <p>One of the&nbsp;main&nbsp;classification of models on BioModels is based on their curation status i.e. manually curated
        and&nbsp;non-curated models. By clicking on&nbsp;<a
            href="//www.ebi.ac.uk/biomodels/search?query=*%3A*">Browse</a>, you can choose <a
            href="//www.ebi.ac.uk/biomodels/search?query=*%3A*+and+curationstatus%3AManually+curated">curated</a>&nbsp;or <a
            href="//www.ebi.ac.uk/biomodels/search?query=*%3A*+and+curationstatus%3ANon-curated">non-curated</a>&nbsp;models,
        along with being able to apply several filters to identify your models of interest. This includes filtering on the
        basis of resources like CHEBI and Uniprot, disease associations, taxonomy and annotations like GO.
        </p>

        <p>A link to <a
            href="//www.ebi.ac.uk/biomodels/search?query=*%3A*">Browse</a>&nbsp;is available from the home page as well as the
        always visible top menu bar. Once you have identified a model of interest, you can click on the model identifier
        to reach the model description page.
        </p>

        <h3 id="TAB_INFO">What information can be found using the model tabs?</h3>

        <p>Here is a brief description of the information available through the tabs available on each curated model's
        description page:</p>
        <ol>
            <li><strong>Overview</strong>: This view has the abstract of the paper describing the model, citation and contributor details. It also has model level annotations, curation and validation status of the model and certification comments from the curator.
            </li>
            <li><strong>Files</strong>:&nbsp;This view has a list of all the available files associated with the model. This includes the most recent SBML model file, biopax file, SBGN representation of the model (.png file), a model report file (.pdf), .xpp file and a few other commonly used formats.&nbsp;
            </li>
            <li><strong>History</strong>:&nbsp;This view provides information about the original submitter, submission date and details regarding&nbsp;different versions of the model, in case changes were made to the original submitted model.&nbsp;
            </li>
            <li><strong>Curation</strong>: This view shows curation comments and the reproduced simulation figure from the paper, which was generated by the curator during the manual curation process.&nbsp;
            </li>
        </ol>

        <h3 id="DOWNLOAD_MODEL">How to download a model from BioModels Database?</h3>

        <p>There are several way to download the models:</p>
        <ul class="faq_list">
            <li>Each model can be downloaded from its own description page, via a "Download Model" link on&nbsp;the side bar menu which is active only when examining a model's description page and on all its tabs.</li>
            <li>All the models can be downloaded in one single archive. Several archives are available: two per release (one with only the SBML files, one with the SBML and all the export files) and one automatically generated every week (only contains the SBML files). The archives are available on the <a
                title="BioModels Database archives via FTP" href="ftp://ftp.ebi.ac.uk/pub/databases/biomodels/">FTP server</a>.
            </li>
            <li>SBML version of the models can be obtained using the <a
                href="https://bitbucket.org/biomodels/jummp-biomodels/wiki/Web%20Services">Web Services</a>.</li>
        </ul>

        <h3 id="SBGN">Does BioModels Database provide SBGN maps?</h3>

        <p>BioModels Database provides graphical representations of the models in various formats (PNG and SVG). These files
        are available from the "Files" tab on a model's description page.</p>

        <p>Due to workforce limitations, and the volume of work that generating SBGN exports necessitate, the layout of most
        of these maps is not currently fully compliant with the specification of <a
            title="SBGN Specifications of Process_Diagram"
            href="http://www.sbgn.org/Documents/Specifications#Process_Diagram">SBGN Process Diagrams</a> Level 1 Version 1.
        However, the BioModels.net team has been involved in SBGN development since it's inception, and is fully committed to
        support it as much, and as quickly, as possible.
        </p>

        <h3 id="MATLAB">Does&nbsp;BioModels Database export models under MATLAB format?</h3>

        <p>BioModels Database developers are doing their best to provide a variety of export formats. BioModels Database is a
        public resource, and is totally committed to support open standard formats. Therefore, our first priority is the
        support of free (non-proprietary) software. In addition, BioModels Database is entirely funded by taxpayers' money,
        and we feel it is inappropriate to develop software to interface with commercial tools as a priority.</p>

        <p>That said, we do provide exports for Octave, which uses a m-file similar to the one used by MATLAB, so you should
        be able to use it in this environment. Additionally, there exist several packages offering SBML support for MATLAB.
        See the <a
            title="SBML Toolbox" href="http://sbml.org/Software/SBMLToolbox">SBML Toolbox</a>, the <a
            title="Systems Biology Toolbox"
            href="http://www.fcc.chalmers.se/sys/products/systems-biology-toolbox-for-matlab">Systems Biology Toolbox</a>, and <a
            title="simbiology"
            href="http://www.mathworks.com/products/simbiology/">simbiology</a>, the toolbox developed by MathWorks.</p>
    </div>
</div>

<div class="row">
    &nbsp;
</div>

<div class="row faq-section-box">
    <div class="columns small-12 medium-2 large-2">
        <ol start="5" type="I">
            <li class="faq_title">Questions dealing with model submission
                <ol class="faq_subheading">
                    <li><a href="#MODEL_SUBMISSION">How to submit a model?</a></li>
                    <li><a href="#SUBMIT_BEFORE_PAPER">Can I submit a model before it is described in a published paper?</a>
                    </li>
                    <li><a
                        href="#ACCESS_AFTER_SUBMISSION">Will a model be publicly accessible immediately after its submission?</a>
                    </li>
                    <li><a href="#REVIEWER_ACCESS">Can reviewers access unpublished models?</a></li>
                    <li><a href="#SUPPORTED_FORMATS">What are the supported model encoding formats?</a></li>
                    <li><a href="#MODEL_UPDATE">How to submit a revised version of a model?</a></li>
                    <li><a
                        href="#CELLML_CONV">Why does my SBML model contain no species or reactions after a conversion from CellML?</a>
                    </li>
                    <li><a href="#SUBMIT">What do I do if I receive error messages when trying to submit a model?</a></li>
                </ol>
            </li>
        </ol>
    </div>
    <div class="columns small-12 medium-10 large-10">
        <h3 id="MODEL_SUBMISSION">How to submit a model?</h3>

        <p>Anybody can submit a model to BioModels Database by creating an account on BioModels. Even if you are not the
        actual author or encoder of a model you can submit it for inclusion in BioModels Database. If you do so, you will be
        recorded as the <em>submitter</em>, not as an author or an encoder. However, we do encourage you to contact the
        original authors to be sure they are happy with the submission.
        </p>

        <p>If a model is encoded in <a title="Systems Biology Markup Language (SBML)" href="http://sbml.org/">SBML</a> or <a
            title="CellML language" href="http://www.cellml.org/">CellML</a>, the submission is entirely processed, via the <a
            title="Model submission page"
            href="submit">submission page</a>. If the model is encoded using a different format, please read the section
        of this FAQ about the <a
            title="Could BioModels Database accept models encoded using my own software-specific format?"
            href="#SUPPORTED_FORMATS">supported formats</a>. If the model can be converted into SBML or CellML, please <a
            title="Contact BioModels.net Team" href="contact">contact us</a>.</p>

        <p>Once the model has been submitted, the submitter will receive an email notification with a unique, stable and
        perennial <em>submission identifier</em>. This identifier is composed of the string "MODEL" followed by 10 digits,
        for example <strong>MODEL1003250000</strong> (these digits are actually based on the submission date and time).
        This identifier can be used to reference the model in publications, and once the model is publicly available on
        BioModels Database, users can directly access the model using this identifier.
        </p>

        <p>BioModels Database currently only publishes models which have been described in a peer reviewed scientific
        publication. Therefore, authors are encouraged to submit models before publication of the associated paper
        (and will receive an identifier that they can used in the publication), but the models will only be publicly
        available on BioModels Database once the paper has been published.</p>

        <p>In order to access a model, knowing it submission identifier, you can use the URL: <strong>https://www.ebi.ac
        .uk/biomodels/</strong> followed by the identifier. For example: <a title="Access to the model: MODEL1002160000"
                                                                            href="https://www.ebi.ac.uk/biomodels/MODEL1002160000">https://www.ebi.ac.uk/biomodels/MODEL1002160000</a>.
        </p>

        <h3 id="SUBMIT_BEFORE_PAPER">Can I submit a model before it is described in a published paper?</h3>

        <p>Yes. Models can be submitted prior to the publication of the associated paper(s). It is actually strongly
        advised to do so: at the submission time, each model is assigned a unique and perennial identifier which allows
        users to access and retrieve it. This identifier can be used by authors as a reference in their publications.</p>

        <p>If you submitted a model before the publication of the associated paper,
        please <a title="Contact BioModels.net Team"
                  href="contact">keep us informed</a> of the status of your publication. That way, we can make the model
        available from BioModels Database as quickly as possible.
        </p>

        <p>When submitting your paper, we encourage you to&nbsp;include the submission identifier in your&nbsp;publication&nbsp;and cite
        BioModels by referring to the guidelines on <a
            href="#QUOTE_BIOMDB">how to cite BioModels Database</a>.</p>

        <h3 id="ACCESS_AFTER_SUBMISSION">Will a model be publicly accessible immediately after its submission?</h3>

        <p>No. Models are not directly visible and retrievable by the public as soon as they are submitted to BioModels
        Database. From submission to their public release, all models undergo various automated and manually performed
        curation and annotation steps to ensure a consistent level of quality and accuracy.</p>

        <p>Moreover, all models are only made publicly available from BioModels Database after the publication of the
        corresponding papers. Please refer to the section of this FAQ about the <a
            title="Will a model be publicly accessible immediately after its submission?"
            href="#ACCESS_AFTER_SUBMISSION">access to a model after its submission</a> for more information about when a
        model will be publicly available from BioModels Database.
        </p>

        <p>Finally, new models are usually made publicly available during "releases". There are between 2 to 4 releases
        of the database a year. Please <a
            title="Contact BioModels.net Team" href="contact">contact us</a> if you need your model to be available earlier.</p>

        <h3 id="REVIEWER_ACCESS">Can reviewers access unpublished models?</h3>

        <p>BioModels Database can provide access to unpublished models to reviewers. This is not automatic, so the model
        submitter needs to request it (for example emailing us at
            <strong>biomodels-cura</strong> AT <strong>ebi.ac.uk</strong>). In this case, we will provide the submitter
        with a URL which can be used to download the model encoded in SBML. If security is a concern, we can supply a
        protected access, where reviewers will be asked for a password to access the model.
        </p>

        <p>In future updates to BioModels, we plan to automate this process. If you have more specific needs,
        please do not hesitate to <a
            title="Contact BioModels.net Team" href="contact">contact us</a>.</p>

        <h3 id="SUPPORTED_FORMATS">What are the supported model encoding formats?</h3>

        <p>Currently BioModels Database only provides full support for a subset of the modelling formats landscape. This is
        mainly due to the fact that we strongly believe in interoperability, which requires distributing to the community
        models in semantically rich, standard formats. The full list of supported modelling formats is available on the <a
            title="Submit a model to BioModels Database"
            href="submit">submission page</a>, and basically covers SBML (all Levels and Versions) , CellML (1.0 and 1.1),
        PharmML, Matlab, Mathematica and COMBINE Archive.
        </p>

        <p>However, we are aware that those formats do not cover all modelling techniques used today. As we are committed to
        encourage models sharing and reuse, we will strive to find a solution to host models originally encoded in MATLAB or
        from other generic modelling platforms. So, if you cannot convert your model(s) to one of the fully supported formats,
        please do contact us (at biomodels-cura AT ebi.ac.uk, which is a non public mailing list dedicated to our curators).</p>

        <h3 id="MODEL_UPDATE">How to submit a revised version of a model?</h3>
        <dl>
            <dt><strong>If the model is not yet published in BioModels Database</strong></dt>
            <dd>
                <div>You can send us the&nbsp;revised version at: <strong>biomodels-cura</strong> AT <strong>ebi.ac.uk</strong> together with the model submission identifier and a comment explaining which modifications were made. This mailing list is not public and the only recipients are the curators of BioModels Database, so you can safely send us models not yet published this way.
                </div>

                <div>&nbsp;</div>
            </dd>
            <dt><strong>The model has already been published in BioModels Databse</strong></dt>
            <dd>
                <div>Any modification that would lead to a change in the biological component, results, etc. should be considered as new entry. We could accept revision to the model if the authors found an error in the publication, which they have reported/submitted the correction (errata) to the journal. For all other changes, the revised model is treated as a new entry, and curated before publishing.&nbsp;</div>

                <div>&nbsp;</div>

                <div>If you only made minor changes to the model, please follow the procedure described above (for not yet published models).</div>
            </dd>
        </dl>

        <h3 id="CELLML_CONV">Why does my SBML model contain no species or reactions after a conversion from CellML?</h3>

        <p>The issue is that there is no annotation in the <a title="CellML"
                                                              href="http://www.cellml.org">CellML</a> file that could help
        the converter to determine if a certain variable is a SBML species, parameter or compartment. And the same applies
        to any CellML equation.
        </p>

        <p>The current version of the converter is not trying to do any complicated guesses and therefore converts every
        CellML variable into a SBML parameter and all the equations into rules. We are currently trying to define some
        annotations that would allow to create better SBML files from CellML and vice-versa, the ultimate goal would be
        to be able to do a round-trip conversion, starting either from a SBML model or a CellML model, without loosing
        any information.</p>

        <h3 id="SUBMIT">What do I do if I receive error messages when trying to submit a model?</h3>

        <p>Any validation issues detected during the submission process will be reported as warnings, making it possible to
        proceed with the submission in spite of the encoding problems. It may be perfectly possible to submit a model that
        is incorrectly encoded in SBML and then share it with someone that can help fix the SBML representation.</p>
    </div>
</div>

<div class="row">
    &nbsp;
</div>

<div class="row faq-section-box">
    <div class="columns small-12 medium-2 large-2">
        <ol start="6" type="I">
            <li class="faq_title">Questions dealing with the curation of the models
                <ol class="faq_subheading">
                    <li><a href="#MODEL_CURATION">What checks are performed to ensure model correctness?</a></li>
                    <li><a href="#MIRIAM">What is MIRIAM?</a></li>
                    <li><a href="#MIRIAM_COMPLIANCE">How is MIRIAM compliance ensured?</a></li>
                    <li><a href="#ANNOTATION">What is annotation, and what purpose does it serve in a model?</a></li>
                    <li><a href="#ANNOTATORS">Who annotates the models?</a></li>
                    <li><a href="#ANNOT_IN_SBML">How are annotations stored in SBML?</a></li>
                    <li><a href="#FULL_ANNOTATION">When is a model completely annotated?</a></li>
                    <li><a href="#CURATION_TOOLS">What are the tools used by the curators of BioModels Database?</a></li>
                    <li><a href="#NON_CURA_BRANCH">What is the non-curated branch of BioModels Database?</a></li>
                </ol>
            </li>
        </ol>
    </div>
    <div class="columns small-12 medium-10 large-10">
        <h3 id="MODEL_CURATION">What checks are performed to ensure model correctness?</h3>

        <p>Before being made publicly available through BioModels Database, models are thoroughly verified. This involves
        several types of checks, some automated and some manually performed by a team of curators.
        The following checks are performed:</p>
        <ul class="faq_list">
            <li>The syntax of the encoded file is verified. This implies a set of correct hierarchy of elements and attributes,
            a consistent use of identifiers, but also a deeper verification of the consistency of the model depending on
            its format (e.g. a parameter controlled by a rule cannot be declared constant in SBML).</li>
            <li>The correspondence between the encoded model and the model described in the published article is checked.
            This implies verification of the structure of the reaction network, the formalisms used, the values of the
            parameters, the amounts and the units.</li>
            <li>Finally the model is simulated to see if the published results can be reproduced. For this step, a different
            simulator is employed than the one used by the original authors, in order to ensure the reproducibility of
            the results.</li>
        </ul>

        <p>These checks come from the <a title="MIRIAM guidelines" href="#MIRIAM">MIRIAM</a> set of guidelines.</p>

        <h3 id="MIRIAM">What is MIRIAM?</h3>

        <p>MIRIAM is the <a title="More information about MIRIAM"
                            href="http://co.mbine.org/standards/miriam">Minimum Information Required in the Annotation of
            Models</a> (<a
            title="Access to the 2005 publication in Nature Biotechnology about MIRIAM"
            href="http://identifiers.org/pubmed/16333295">publication</a> and
            <a title="PDF version of the press release"
               href="https://www.ebi.ac.uk/biomodels-static/doc/MIRIAM_06Decr2005.pdf">press Release</a>). Initiated by the
            <a title="BioModels.net initiative"
               href="http://biomodels.net/">BioModels.net</a> project, it is a set of guidelines defining how a model should
        be encoded and annotated in order to be successfully distributed, exchanged and ultimately reused.
        </p>

        <p>In particular MIRIAM requires that a model provides all the information necessary to instantiated in a simulation,
        such as the initial conditions. In addition, the reaction graph generated from this simulation must reproduce the
        results of the original publication, in which the model was first described.
        Moreover, the MIRIAM guidelines require that all model components contain sufficient controlled annotation such
        that <em>each</em> component can be unambiguously identified. The <a
            title="MIRIAM Registry" href="https://www.ebi.ac.uk/miriam/">MIRIAM Registry</a> and
            <a title="Identifiers.org"
               href="http://identifiers.org/">Identifiers.org</a> have been developed specifically to provide generation
        and resolution services for unique and perennial identifiers to be used in controlled annotations.
        </p>

        <p>All models stored in the curated branch of BioModels Database are <a title="How is MIRIAM compliance ensured?"
                                                                                href="#MIRIAM_COMPLIANCE">MIRIAM-compliant</a>.
        </p>

        <h3 id="MIRIAM_COMPLIANCE">How is MIRIAM compliance ensured?</h3>

        <p>All the models in the curated branch are fully <a title="Go to the 'MIRIAM' section of this FAQ"
                                                             href="#MIRIAM">MIRIAM</a> compliant. Each of the MIRIAM
        requirements is satisfied in the following way:
        </p>
        <dl>
            <dt><strong>Models be encoded in a public standard format</strong></dt>
            <dd>
                <div>All models in BioModels Database are converted into valid SBML.</div>
            </dd>
            <dt><strong>Models must be clearly related to a single reference</strong></dt>
            <dd>
                <div>Each model is derived from a scientific publication.</div>
            </dd>
            <dt><strong>The model must correspond to the biological processes represented in the publication</strong></dt>
            <dd>
                <div>Models are manually checked to confirm accurate biological representation in the model.</div>
            </dd>
            <dt><strong>The simulation results generated from the model must reflect those in the reference publication</strong>
            </dt>
            <dd>
                <div>Models are manually checked to confirm accurate simulation results are generated from the model.
                The result of this step is available in the "Curation" tab.</div>
            </dd>
            <dt><strong>External data resources annotation</strong></dt>
            <dd>
                <div>Models elements are well annotated.</div>
            </dd>
        </dl>

        <h3 id="ANNOTATION">What is annotation, and what purpose does it serve in a model?</h3>

        <p>Once we ensure a model's correctness, we annotate the model with biological information. Model elements can
        describe a plethora of different entities, such as genes, proteins or metabolites. The usage of free text or
        non-standard nomenclature to identify those elements is not reliable, as it can introduce ambiguity to the model
        components, such that subsequent users of a model would struggle to identify the precise entities involved.</p>

        <p>Annotation is a process that&nbsp;identifies model elements by linking them to existing data resources. This ensures
        that users will be able to understand the models and increases the possibility of their reuse. Moreover, annotation
        is a key element for processes such as data comparison, data integration and data conversion. Finally, it&nbsp;also allows
        the provision of models accurately through search engines (BioModels Database makes heavy use of annotation when
        users search for models of interest).</p>

        <p>Models in BioModels Database are provided with consistent annotation using unambiguous identifiers. Those
        identifiers are generated by the <a
            title="MIRIAM Registry" href="https://www.ebi.ac.uk/miriam/">MIRIAM Registry</a> and
            <a title="Identifiers.org"
               href="http://identifiers.org/">Identifiers.org</a> services. They can be used to reference records from
        external databases (such as Taxonomy, EMBL-Bank or UniProt), terms from ontologies
        (such as Gene Ontology, SBO or ChEBI), publications, etc.
        </p>

        <p>For more information about annotation, please refer our
            <a title="BioModels Database Annotation Information"
               href="https://www.ebi.ac.uk/biomodels/annotation">general introduction to the annotation of models</a>.
        </p>

        <h3 id="ANNOTATORS">Who annotates the models?</h3>

        <p>Models can be submitted to BioModels Database already annotated. In this case, any existing annotations encoded
        using the annotation scheme developed for <a
            title="SBML specs"
            href="http://sbml.org/Documents">SBML</a> (introduced since Level 2 Version 2 and supported by numerous tools)
        will be extracted and recorded in BioModels Database.
        </p>

        <p>Our curation team currently checks existing annotations and adds new ones as necessary. We do encourage authors
        submitting the models to annotate their models as much as possible by following the <a
            href="https://www.ebi.ac.uk/biomodels/annotation">guidelines that the curators use during annotation</a>.
        This will help us&nbsp;accelerate the model&nbsp;curation process.
        </p>

        <h3 id="ANNOT_IN_SBML">How are annotations stored in SBML?</h3>

        <p>The annotation of each model component is stored in the corresponding SBML element using the a scheme initially
        designed by Nicolas Le Nov&egrave;re and Andrew Finney, and now part of <a
            title="SBML specs" href="http://sbml.org/Documents">SBML</a> (since Level 2 Version 2). It relies on the use of <a
            title="RDF" href="http://www.w3.org/RDF/">RDF</a>, <a title="Dublin Core"
                                                                  href="http://www.dublincore.org/">Dublin Core</a>, <a
            title="vCard" href="http://www.w3.org/TR/vcard-rdf/">vCard</a> and the <a
            href="http://biomodels.net/qualifiers/">BioModels.net qualifiers</a>.</p>

        <p><img class="screenshot" title="SBML scheme for annotation encoding"
                src="//www.ebi.ac.uk/biomodels-static/FAQ/img/MIRIAM_annotations.png"
                alt="Piece of SBML showing some annotation"/></p>

        <p>Please refer to the
            <a title="SBML Specifications"
               href="http://sbml.org/Documents/Specifications">SBML specification</a> for more information
        about the annotation scheme.
        </p>

        <h3 id="FULL_ANNOTATION">When is a model completely annotated?</h3>

        <p>Annotating each model component with the most relevant resource records takes great efforts, especially since
        the number of submitted models has grown rapidly. For example, the 25th release of BioModels Database (18th June 2013)
        contains <strong>171,432</strong> cross-references (links to external resources contained in the annotations). This
        number need to be compared with the total number of species (156,665) and relationships (174,986 this number including
        reactions, rate rules, events and assignment rules) involved in the existing 963 models. Therefore current annotations
        do not cover all model elements.
        </p>

        <p>Besides manpower limitations, this discrepancy is sometimes due to a lack of adequate or suitable resources for
        annotation, for instance for molecular entities which are created only for simulation purposes. Moreover, biological
        data resources are often slightly lagging behind newly generated knowledge, and it is possible that a particular
        resource does not offer the relevant information at the time the model is annotated. In the case of hierarchical
        controlled vocabularies, such as Gene Ontology or ChEBI, there is the option to use a term at a higher level of
        abstraction if the required precise term does not currently exist. Most often, one can always add some information,
        even if not optimal. Hence, model annotation needs to be, and indeed is, a continuous process.</p>

        <h3 id="CURATION_TOOLS">What are the tools used by the curators of BioModels Database?</h3>

        <p>Our curators use a wide range of tools to perform their curation tasks: <a
            href="http://www.celldesigner.org/">CellDesigner</a>, <a href="http://www.copasi.org/">COPASI</a>, <a
            href="http://sbw.kgi.edu/software/jarnac.htm">Jarnac</a>, <a
            href="http://sbml.org/Software/MathSBML">MathSBML</a>, <a
            href="http://sbw.kgi.edu/Simulation2005/">RoadRunner</a>, <a
            href="http://www.tbi.univie.ac.at/~raim/odeSolver/">SBMLOdeSolver</a>, <a
            href="https://www.ebi.ac.uk/compneur-srv/SBMLeditor.html">SBMLeditor</a>, <a
            href="http://www.semanticsbml.org/">SemanticSBML</a>, <a href="http://jigcell.cs.vt.edu/">JigCell</a>, <a
            href="http://www.math.pitt.edu/%7Ebard/xpp/xpp.html">XPP-Aut</a>, ...</p>

        <p>Moreover, they also use tools such as <a title="Gnuplot"
                                                    href="http://www.gnuplot.info/">Gnuplot</a> and various custom scripts.</p>

        <h3 id="NON_CURA_BRANCH">What is the non-curated branch of BioModels Database?</h3>

        <p>The success and rapid growth of BioModels databases has led to a couple of issues, both of which are handled using
        the non-curated branch of BioModels Database. Firstly, BioModels Database faces an ever-growing, and increasingly
        complex, number of model submissions which are growing at a pace that exceeds our curation capacity. Secondly, many
        of the models submitted, while syntactically correct, cannot be curated for various reasons. Hence, the non-curated
        branch of BioModels Database is a public holding area for models that have yet to be curated, or cannot be curated
        for various reasons. The models held in the non-curated branch are of four types:</p>
        <ol class="faq_list">
            <li>Models that are perfectly fine, but have yet to be curated.</li>
            <li>Models that are not curatable, since they are not <a title="Go to the 'MIRIAM' section of this FAQ"
                                                                     href="#MIRIAM">MIRIAM</a> compliant.</li>
            <li>Models that are not currently curatable, since they are not kinetic models (e.g. logical models and Flux Balance Analysis models).</li>
            <li>Models for which SBML supports only part of the description, most of it being in annotation (e.g. spatial models).</li>
        </ol>

        <p>Note that all the models in the non-curated branch have nevertheless passed the XML check, SBML syntax check and
        SBML consistency checks.</p>
    </div>
</div>

<div class="row">
    &nbsp;
</div>

<div class="row faq-section-box">
    <div class="columns small-12 medium-2 large-2">
        <ol start="7" type="I">
            <li class="faq_title">Questions dealing with the underlying software infrastructure
                <ol class="faq_subheading">
                    <li><a
                        href="#BIOMDB_SOFTWARE">What are the tools used to develop and run the software infrastructure behind BioModels Database?</a>
                    </li>
                    <li><a href="#BIOMDB_SOFTWARE_REUSE">Can I install my own version of BioModels Database?</a></li>
                </ol>
            </li>
        </ol>
    </div>
    <div class="columns small-12 medium-10 large-10">
        <h3 id="BIOMDB_SOFTWARE">What are the tools used to develop and run the software infrastructure behind BioModels
        Database?</h3>

        <p>The beta system is based on <a
            href="https://bitbucket.org/jummp/jummp/">Jummp,</a> a flexible, open-source model management platform that can be
        tailored to the needs of other projects or communities. Submissions are hosted using a combination of version control
        systems such as <a
            href="https://git-scm.com/">Git</a>, a relational database such as <a
            href="https://www.mysql.com/">MySQL</a>&nbsp;or <a
            href="https://www.postgresql.org/">PostgreSQL</a>&nbsp;and a search engine like <a
            href="http://lucene.apache.org/">Lucene</a>.</p>

        <p>Support for standard formats is provided by dedicated software tools, such as &nbsp;<a
            href="http://sbml.org/Software/JSBML">JSBML</a>, <a
            href="http://www.pharmml.org/tools/libpharmml">libPharmML</a>, <a
            href="https://github.com/mglont/CombineArchive">libCOMBINEArchive</a>.</p>

        <h3 id="BIOMDB_SOFTWARE_REUSE">Can I install my own version of BioModels Database?</h3>

        <p>The models can be <a
            title="What are the conditions of use and distribution for unmodified models originating from BioModels Database?"
            href="#MODEL_REUSE">freely reused</a> and the software running BioModels Database itself is an open source project
        distributed under the GNU General Public License. For more information, please refer to the <a
            title="What are the tools used to develop and run the software infrastructure behind BioModels Database?"
            href="#BIOMDB_SOFTWARE">software infrastructure</a> entry of this FAQ.</p>

        <p>All converters are also available under an open source license and can be downloaded from the <a
            title="Download files from BioModels SF project"
            href="https://sourceforge.net/projects/biomodels/files/">download section of the BioModels SourceForge
            repository</a>.
        </p>

        <p>In order to store your own models or just plug part(s) of the BioModels Database infrastructure into your own,
        you'll need to setup your own local instance of the repository. In order to help you through this procedure, we
        created the following page: <a
            title="How to install you own instance of the repository"
            href="https://bitbucket.org/jummp/jummp/wiki/install">development with BioModels Database source code</a>.</p>
    </div>
</div>

<div class="row">
    &nbsp;
</div>

<div class="row faq-section-box">
    <div class="columns small-12 medium-2 large-2">
        <ol start="8" type="I">
            <li class="faq_title">Miscellaneous questions
                <ol class="faq_subheading">
                    <li><a href="#ID_SCHEME">What is the naming and identifier scheme used in BioModels Database?</a></li>
                    <li><a href="#MODEL_AUTHORS">Who are the authors of a model?</a></li>
                    <li><a href="#MODEL_SUBMITTER">Who is the submitter of a model?</a></li>
                    <li><a href="#MODEL_CREATORS">Who are the encoders of a model?</a></li>
                    <li><a
                        href="#ERROR_REACTION">Why do I get an error message stating some reaction modifiers are not declared?</a>
                    </li>
                </ol>
            </li>
        </ol>
    </div>
    <div class="columns small-12 medium-10 large-10">
        <h3 id="ID_SCHEME">What is the naming and identifier scheme used in BioModels Database?</h3>

        <p>At the time of submission, a unique submission identifier assigned to the model. It is composed of the character
        sequence "MODEL" followed by ten digits extracted from the timestamp of submission. When a model is moved to the
        curated branch, a new model identifier is generated and assigned to it. This identifier is composed of the character
        sequence "BIOMD" followed by ten digits reflecting the model's position in the branch (for example "BIOMD0000000216"
        for the 216th model successfully curated).</p>

        <p>During the <a title="Go to: curators"
                         href="#MODEL_CURATION">curation process</a>, the <em>curators</em> will also give an appropriate
        name to each model. This follows the general scheme: <em>AuthorYear - Topic Method</em>.
        For example, Edelstein1996 - EPSP ACh Event refers to the model <a
            title="Go to model: BIOMD0000000001" href="https://www.ebi.ac.uk/biomodels/BIOMD0000000001">BIOMD0000000001</a>.</p>

        <p>Both identifiers are unique and permanent, and will never be re-assigned to a different model, even if for some
        reason a particular model must be retracted from the database. Both identifiers can be used to retrieve the model
        and quoted in subsequent publications.</p>

        <h3 id="MODEL_AUTHORS">Who are the authors of a model?</h3>

        <p>The <em>authors</em> of a model are the individuals who initially described the model in a peer-reviewed publication.
        </p>

        <h3 id="MODEL_SUBMITTER">Who is the submitter of a model?</h3>

        <p>The <em>submitter</em> of a model is the person who actually submitted the model and therefore requested its
        addition in BioModels Database. Anybody can submit a model to BioModels Database. Please refer to <a
            title="Model submission procedure" href="#MODEL_SUBMISSION">the submission procedure</a>, if you want to know more.
        </p>

        <h3 id="MODEL_CREATORS">Who are the encoders of a model?</h3>

        <p>The <em>encoders</em> of a model are the individuals who actually encoded the model in its present form, based on
        the published article. These individuals could be the authors of the publication, members of the curation team, or
        any other scientists who decided to encode the published article.
        </p>

        <h3 id="ERROR_REACTION">Why do I get an error message stating some reaction modifiers are not declared?</h3>

        <p>Some models contain reactions where the kinetics are modified by compounds that are not themselves reactants. These
        compounds are not consumed or produced by the reaction, and are known as modifiers. This error occurs when some
        software tools do not explicitly list the modifiers contained in the model reactions. A consistency check has been
        implemented to identify missing modifier declarations, and is now applied to all models submitted to BioModels
        Database. This error should not happen any more.</p>
    </div>
</div>
<p>&nbsp;</p>
</body>
<content tag="faq">
    selected
</content>
<content tag="title">
    <g:message code="${titleCode}" default="Frequently Asked Questions"/>
</content>
