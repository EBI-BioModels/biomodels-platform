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
        .level3 {
            color: #0e90d2 !important;
        }
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

<div id="what-is-biomodels-section" class="row faq-section-box">
    <div class="columns small-12 medium-2 large-2">
        <ol start="1" type="I">
            <li class="faq_title">What is BioModels and how does it differ from other resources?
                <ol class="faq_subheading">
                    <li><a href="#what-is-biomodels">What is BioModels?</a></li>
                    <li><a href="#differ-mod">How does BioModels differ from other databases of models?</a></li>
                    <li><a href="#biomodels-licence">Licence</a></li>
                </ol>
            </li>
        </ol>
    </div>
    <div class="columns small-12 medium-10 large-10">
        <h3 id="what-is-biomodels">What is BioModels?</h3>
        <p>BioModels is a repository of mathematical models representing biological systems.&nbsp It currently hosts
        a range of models describing processes like signalling, protein-drug interaction interactions, metabolic
        pathways, epidemic models and many more. The models that BioModels hosts are&nbsp;usually described in
        peer-reviewed scientific literature and in some cases, they are generated automatically from pathway
        resources (Path2Models). Many models are manually <a title="More about model curation (in this FAQ)"
                                                             href="#model-curation">curated</a> and <a
            title="More about annotation (in this FAQ)" href="#annotation">semantically enriched with
            cross-references</a> to external data resources (such as publications, databases of compounds and
        pathways, ontologies, etc.). BioModels allows the scientific community to store, search and retrieve
        mathematical models of their interest. In addition to offering support for models in different formats and
        enabling their conversion between different representational formats, the database offers&nbsp;<a
            title="Programmatic access via web services"
            href="https://bitbucket.org/biomodels/jummp-biomodels/wiki/Web%20Services">programmatic access</a> to
        developers via web services.
        </p>

        <p>Users can browse and search the content of the repository, and download models in
            <a title="SBML" href="http://www.sbml.org/">SBML</a> format, as well as various other formats, such as XPP,
        VCML, SciLab, Octave, BioPAX, PNG, SVG, ... A human readable summary of each model is also available in PDF.
        </p>

        <p>More information can be found in the associated <a title="BioModels publications"
                                                              href="citation">publications</a>.</p>

        <h3 id="differ-mod">How does BioModels differ from other databases of models?</h3>
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

        <h3 id="biomodels-licence">Licence</h3>
        <p>All models are provided under the terms of the <a title="Creative Commons CC0"
                                                             href="http://creativecommons.org/publicdomain/zero/1.0/">Creative
            Commons CC0 Public Domain Dedication</a>, cf. our <a
            title="BioModels: terms of use"
            href="termsofuse">terms of use</a>. This means that the models are available
        freely for use, modification and distribution, to all users.
        </p>
    </div>
</div>

<div class="row">
    &nbsp;
</div>

<div id="citation-contact-info" class="row faq-section-box">
    <div class="columns small-12 medium-2 large-2">
        <ol start="2" type="I" class="topic">
            <li class="faq_title">Citations and contact information
                <ol class="faq_subheading" style="list-style-type: decimal;">
                    <li><a href="#quote-biomodels">How to cite BioModels?</a></li>
                    <li><a href="#quote-model">How to cite a model present in BioModels?</a></li>
                    <li><a href="#team-contact">How to contact the team behind BioModels?</a></li>
                    <li><a
                        href="#original-author">I wish to contact the original author(s) of a model, but the listed email does not work. What should I do?</a>
                    </li>
                </ol>
            </li>
        </ol>
    </div>
    <div class="columns small-12 medium-10 large-10">
        <h3 id="quote-biomodels">How to cite BioModels?</h3>
        <p>For your work relating to BioModels generally, please cite this paper.<br/>
        Malik-Sheriff <em>et al.</em> BioModels — 15 years of sharing computational models in life science.
        Nucl. Acids Res. 2020, 48, D1, D407–415. </p>
        <p>For example:<br/>
            <span>This model was deposited in BioModels [1] and assigned the identifier MODEL2007280002.<br/>
                [1] Malik-Sheriff <em>et al.</em> BioModels — 15 years of sharing computational models in life science.
            Nucl. Acids Res. 2020, 48, D1, D407–415.</span>
        </p>
        <p>Please, have a look at the <a title="How to quote BioModels?" href="citation">citation information</a>
            page to get details of the publications relevant to different work you are citing.
        </p>

        <h3 id="quote-model">How to cite a model present in BioModels?</h3>
        <p>The best way to cite a model present in BioModels is to state the reference publication associated
        with the model. You can also mention the model's identifier (of the form "BIOMD" or "MODEL" followed by 10
        digits). Using the <a href="http://identifiers.org/" target="_blank">identifers.org</a> service to make a
        link to your model. For example: http://identifiers.org/biomodels.db/MODEL2007280002
        </p>

        <h3 id="team-contact">How to contact the team behind BioModels?</h3>

        <p>The easiest way to contact the team developing and maintaining the software infrastructure and the content of
        BioModels is to use the following email address:
            <strong>biomodels-net-support</strong> AT <strong>lists.sf.net</strong>.
        </p>

        <p>You can also refer to the information provided on the <a title="contact us page" href="contact">contact us</a> page.
        </p>

        <h3 id="original-author">I wish to contact the original author(s) of a model, but the listed email does not
        work.
        What should I do?</h3>

        <p>This will happen occasionally as people move on to new roles or to new positions in different institutions.
        There is no easy solution for this, so we would suggest in the first instance to write to the BioModels
        curation team, which can have them look into it on your behalf. Please provide as much information as you have
        available, and avenues you may have explored already. The curation team can be contacted via:
            <strong>biomodels-cura</strong> AT <strong>ebi.ac.uk</strong>.
        </p>
    </div>
</div>

<div class="row">
    &nbsp;
</div>

<div id="model-correctness-reuse" class="row faq-section-box">
    <div class="columns small-12 medium-2 large-2">
        <ol start="3" type="I">
            <li class="faq_title">Questions dealing with model correctness, reuse and distribution
                <ol class="faq_subheading">
                    <li><a href="#model-reliability">How reliable are the models hosted in BioModels?</a></li>
                    <li><a href="#report-error">What should I do if I find an error in a model?</a></li>
                    <li><a
                        href="#model-reuse">What are the conditions of use and distribution for unmodified models originating from BioModels?</a>
                    </li>
                    <li><a
                        href="#model-modify">What are the conditions of use and distribution for modified models that originated from BioModels?</a>
                    </li>
                    <li><a href="#model-conversion">Can I convert a model from BioModels into another SBML version?</a>
                    </li>
                </ol>
            </li>
        </ol>
    </div>
    <div class="columns small-12 medium-10 large-10">
        <h3 id="model-reliability">How reliable are the models hosted in BioModels?</h3>
        <p>Before being publicly available in the BioModels curated section, a model passes through a stringent
        curation pipeline. This
        ensures its syntactic correctness, semantic soundness, and its correspondence with its reference publication, both in
        terms of model structure and simulation results. Consequently the structure of a model would not normally change,
        while its annotation is expected to improve constantly over time. BioModels cannot make any statement on the
        scientific correctness of the model.</p>

        <h3 id="report-error">What should I do if I find an error in a model?</h3>
        <p>The models present in BioModels have already been extensively checked and corrected. However, it remains
        possible that some errors may have crept through our rigorous curation pipeline. If you discover any errors with a
        specific model, or have any potential concerns, please do <a
            title="How to contact the team behind BioModels?"
            href="#team-contact">contact us</a>. Comments and bugs for specific models may be submitted directly from the menu
        bar at the top of the web page describing each model, where you will find a "Submit Model Comment/Bug" link.
        </p>

        <h3 id="model-reuse">
            What are the conditions of use and distribution for unmodified models originating from
        BioModels Database?</h3>
        <p>You can use and freely distribute the models present in BioModels in their current form. Please refer to
        the <a href="termsofuse">legal terms of use</a> for more details.</p>

        <h3 id="model-modify">What are the conditions of use and distribution for modified models that originated from
        BioModels?</h3>
        <p>You can modify and freely distribute a modified version a model that is present in BioModels in whole
        or part. A modified model is defined as one where an existing model is extended/reduced/merged with other
        models. In this case, there is a change in the biological description and components.</p>

        <p>The modified model <em>must</em> be renamed, and a new submission to BioModels should be made. This new
        model will then undergo the curation process again as a separate model. When curating the modified model,
        a curator will also add a reference to the parent model on BioModels from which it was derived.
        </p>

        <p>Please refer to the <a href="termsofuse">legal terms of use</a> for more details.</p>

        <h3 id="model-conversion">Can I convert a model from BioModels into another SBML version?</h3>
        <p>Conversion of SBML model files between different levels/versions is necessary for users who use tools that do
        not support the versions/levels of the original SBML file. The current version of BioModels does not provide an
        automatic conversion between different SBML levels/versions. You can do this offline using tools like COPASI.</p>

        <p>In a future release of BioModels, we aim to provide automatic SBML convertors which will allow you to
        download the model file in different SBML versions.</p>
    </div>
</div>

<div class="row">
    &nbsp;
</div>

<div id="provided-features" class="row faq-section-box">
    <div class="columns small-12 medium-2 large-2">
        <ol start="4" type="I">
            <li class="faq_title">Questions dealing with the provided features of BioModels
                <ol class="faq_subheading">
                    <li><a href="#browse-models">How to browse and search BioModels?</a></li>
                    <li><a href="#tab-info">What information can be found using the model tabs?</a></li>
                    <li><a href="#download-model">How to download a model from BioModels?</a></li>
                    <li><a href="#sbgn">Does BioModels provide sbgn maps?</a></li>
                    <li><a href="#matlab">Does&nbsp;BioModels export models under the matlab format?</a></li>
                </ol>
            </li>
        </ol>
    </div>
    <div class="columns small-12 medium-10 large-10">
        <h3 id="browse-models">How to browse and search BioModels?</h3>

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

        <h3 id="tab-info">What information can be found using the model tabs?</h3>

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

        <h3 id="download-model">How to download a model from BioModels?</h3>

        <p>There are several ways to download the models:</p>
        <ul class="faq_list">
            <li>Each model can be downloaded from its own description page, via a "Download Model" link on&nbsp;the
            side bar menu which is active only when examining a model's description page and on all its tabs.</li>
            <li>SBML version of the models can be programmatically obtained using the <a
                href="https://www.ebi.ac.uk/biomodels/docs/">RESTful API</a>.</li>
        </ul>

        <h3 id="sbgn">Does BioModels provide sbgn maps?</h3>

        <p>BioModels provides graphical representations of the models in various formats (PNG and SVG). These files
        are available from the "Files" tab on a model's description page.</p>

        <p>Due to workforce limitations, and the volume of work that generating SBGN exports necessitate, the layout of most
        of these maps is not currently fully compliant with the specification of <a
            title="SBGN Specifications of Process_Diagram"
            href="http://www.sbgn.org/Documents/Specifications#Process_Diagram">SBGN Process Diagrams</a> Level 1 Version 1.
        However, the BioModels.net team has been involved in SBGN development since it's inception, and is fully committed to
        support it as much, and as quickly, as possible.
        </p>

        <h3 id="matlab">Does&nbsp;BioModels export models under matlab format?</h3>

        <p>BioModels developers are doing their best to provide a variety of export formats. BioModels is a
        public resource, and is totally committed to support open standard formats. Therefore, our first priority is the
        support of free (non-proprietary) software. In addition, BioModels is entirely funded by taxpayers' money,
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

<div id="model-submission" class="row faq-section-box">
    <div class="columns small-12 medium-2 large-2">
        <ol start="5" type="I">
            <li class="faq_title">Questions dealing with model submission
                <ol class="faq_subheading">
                    <li><a href="#submit-model">What do I need to submit a new model?</a>
                        <ol type="a">
                            <li><a href="#register-new-account" class="level3">How to register for a new user account?</a></li>
                            <li><a href="#how-to-submit-a-new-model" class="level3">How to submit a new model?</a></li>
                        </ol>
                    </li>
                    <li>
                        <a href="#update-existing-model">Can I update the details of an existing model?</a>
                        <ol type="a">
                            <li><a href="#how-to-update-model" class="level3">How to update model files or revised version of a
                            model, model name and description?</a></li>
                            <li><a href="#update-publication" class="level3">How to update the publication
                            details associated with a model?</a></li>
                            <li><a href="#submit-no-publication" class="level3">What will happen if I submit the model without
                            Publication details?</a></li>
                            <li><a href="#submit-before-paper" class="level3">Can I submit a model before it is
                            described in a published paper?</a>
                            <li><a href="#changes-stored-revisions" class="level3">Are the model changes maintained as revisions?</a></li>
                        </ol>
                    </li>
                    </li>

                    <li><a
                        href="#access-after-submission">Will a model be publicly accessible immediately after its submission?</a>
                        <ol type="a">
                            <li><a href="#cannot-find-my-model" class="level3">Why cannot I find my model in BioModels?</a></li>
                            <li><a href="#make-model-public" class="level3">What need to be done to make model public?</a></li>
                            <li><a href="#what-will-happen-model-nonpublic" class="level3">What will happen if don’t make model
                            public?</a></li>
                        </ol>
                    </li>
                    <li><a href="#reviewer-access">How can reviewers access unpublished models?</a></li>
                    <li><a href="#supported-formats">What are the supported model encoding formats?</a></li>
                    <li><a href="#how-to-submit-revised-version">How to submit a revised version of a model?</a></li>
                    <li><a
                        href="#cellml-conversion">Why does my SBML model contain no species or reactions after a conversion from CellML?</a>
                    </li>
                    <li><a href="#submission-error">What do I do if I receive error messages when trying to submit a model?</a></li>
                </ol>
            </li>
        </ol>
    </div>
    <div class="columns small-12 medium-10 large-10">
        <h3 id="submit-model">What do I need to submit a new model?</h3>
        <p>First, users need to register with BioModel to get user credentials
        (account and password). Using
        credentials, one can login for their account and can submit model to
        BioModels. For more guidance, please refer:
            <a href="${manualUrl}/getting-started.html#slide4"
               target="_blank">Model Submission guidelines (Slide 4)</a>.</p>

        <h4 id="register-new-account" class="level3">How to register for a new user account?</h4>
        <p>For registering with BioModels, please visit the <a href="${serverUrl}">BioModels
        website</a> and click on <a href="${serverUrl}/registration">Register</a> link from the right side the main
        menu. Follow the on-screen instruction for the next steps. For more guidance, please refer:
            <a href="${manualUrl}/getting-started.html#slide5"
               target="_blank">Model
            Submission guidelines (Slide 5-8)</a></p>

        <h4 id="how-to-submit-a-new-model" class="level3">How to submit a new model?</h4>

        <p>Anybody can submit a model to BioModels by creating an account on BioModels. Even if you are not the
        actual author or encoder of a model you can submit it for inclusion in BioModels. If you do so, you will be
        recorded as the <em>submitter</em>, not as an author or an encoder. However, we do encourage you to contact the
        original authors to be sure they are happy with the submission.
        </p>

        <p>
            To submit a new model, user has to login to BioModels
            using their user credentials. Post login, click on “Submit”
            tab and proceed with model submission. For details,
            please refer:  <a href="${manualUrl}/getting-started.html#slide9"
                              target="_blank">Model Submission guidelines
            (Slide 9-16)</a>.
        </p>

        <p>If a model is encoded in
            <a title="Systems Biology Markup Language (SBML)"
               href="http://sbml.org/">SBML</a> or <a
            title="CellML language" href="http://www.cellml.org/">CellML</a>, the submission is entirely processed,
        via the <a
            title="Model submission page"
            href="submit">submission page</a>. If the model is encoded using a different format, please read the section
        of this FAQ about the <a
            title="Could BioModels accept models encoded using my own software-specific format?"
            href="#supported-formats">supported formats</a>.</p>

        <p>Once the model has been submitted, the submitter will receive an email notification with a unique, stable and
        perennial <em>submission identifier</em>. This identifier is composed of the string "MODEL" followed by 10 digits,
        for example <strong>MODEL1003250000</strong>. This identifier can be used to reference the model in
        publications, and once the model is publicly available on BioModels, users can directly access the model
        using this identifier.
        </p>

        <p>BioModels currently only publishes models which have been described in a peer reviewed scientific
        publication. Therefore, authors are encouraged to submit models before publication of the associated paper
        (and will receive an identifier that they can used in the publication), but the models will only be publicly
        available on BioModels once the paper has been published.</p>

        <p>In order to access a model, knowing it submission identifier, you can use the URL: <strong>https://www.ebi.ac
        .uk/biomodels/</strong> followed
        by the identifier. For example: <a title="Access to the model: MODEL1002160000"
                                           href="https://www.ebi.ac.uk/biomodels/MODEL1002160000"
                                           target="_blank">https://www.ebi.ac.uk/biomodels/MODEL1002160000</a>.
        </p>

        <h3 id="update-existing-model">Can I update the details of an existing model?</h3>
        <p>Models submitted to BioModels remain personally private to the submitter until
        they want to publish them from their side. User can update the model
        files, model name, publication details, etc at any point in time before
        making the model public. All the changes are revision controlled.</p>
        <h4 id="how-to-update-model" class="level3">How to update model files or revised version of a
        model, model name and description?</h4>
        <p>
            User need to login using their user credentials and go to
            “My Account” &rightarrow; “My Models”. This will display the list of
            models submitted by user. Click on the model for which
            you want to update the files. On the left of side of the
            page, you will find option to “Update”, “Publish”, “Convert”
            etc. Click on “Update” and follow the on-screen
            instructions. For details, please refer: <a href="${manualUrl}/getting-started.html#slide20"
                                                        target="_blank">Model
            Submission guidelines (Slide 20-28)</a>
        </p>
        <h4 id="update-publication" class="level3">How to update the publication
        details associated with a model?</h4>
        <p>
            <strong>Method 1:</strong> This method is very similar to updating Model
            files. User need to login using their user credentials and
        go to <strong>My Account</strong>, choose <strong>My Models</strong>. This will display the
            list of models submitted by user. Click on the model for
            which you want to update the publication. On the left of
            side of the page, you will find option to <strong>Update</strong>,
            <strong>Publish</strong> <strong>Convert</strong>, etc. Click on <strong>Update</strong> and continue
            clicking "Next" to go to the Publication page. Follow the onscreen
            instructions further. For details, please refer:
            <a href="${manualUrl}/getting-started.html#slide20"
               target="_blank">Model Submission guidelines (Slide 20-28)</a>
        </p>
        <p>
            <strong>Method 2:</strong> This method is direct method for updating
            publication details. User need to login using their user
        credentials and go to <strong>My Account</strong> &rightarrow; <strong>My Models</strong>. This
            will display the list of models submitted by user. Click on
            the model for which you want to update the publication. In
            the model overview tab, you will find the all the basic
            information of the model along with publication details.
            You will find a small tab called <strong>Edit</strong> next to publication.
            Click on Edit and you will be directed to a different page
            for publication editing option. Enter the details of new
            publication (ID), click <strong>Update</strong> and “Save” post crosschecking
            the details. For details, please refer: <a href="${manualUrl}/getting-started.html#slide31"
                                                       target="_blank">
            Model Submission guidelines (Slide 31-33)</a>
        </p>

        <h4 id="submit-no-publication" class="level3">What will happen if I submit the model without publication
        details?</h4>
        <p>
            You and your model will not get into any trouble although your submission cannot provide any reviewed
            publication. Your model is only accessible to you and some authorised accounts like collaborators,
            reviewers. To ensure submissions to BioModels qualified and meet conventional standards, models on the publication requests without providing a
            peer reviewed publication will be published with caution and be flagged with an exclamation to notify users.
            The citation as well as usability of a model with the publication details are
            significantly higher than those without associating with any publication
            details.
        </p>

        <h4 id="submit-before-paper" class="level3">Can I submit a model before it is described in a published paper?</h4>
        <p>Yes. Models can be submitted prior to the publication of the associated paper(s). It is actually strongly
        advised to do so: at the submission time, each model is assigned a unique and perennial identifier which allows
        users to access and retrieve it. This identifier can be used by authors as a reference in their publications.</p>

        <p>If you submitted a model before the publication of the associated paper,
        please <a title="Contact BioModels.net Team"
                  href="contact">keep us informed</a> of the status of your publication. That way, we can make the model
        available from BioModels as quickly as possible.
        </p>

        <p>When submitting your paper, we encourage you to&nbsp;include the submission identifier in your&nbsp;publication&nbsp;and cite
        BioModels by referring to the guidelines on <a
            href="#quote-biomodels">how to cite BioModels</a>.</p>

        <h4 id="changes-stored-revisions" class="level3">Are the model changes maintained as revisions?</h4>
        <p>All the updating or changes done are maintained as
        revisions.</p>

        <h3 id="access-after-submission">Will a model be publicly accessible immediately after its submission?</h3>
        <p>No. Models are not directly visible and retrievable by the public as soon as they are submitted to BioModels
        Database. From submission to their public release, all models undergo various automated and manually performed
        curation and annotation steps to ensure a consistent level of quality and accuracy.</p>
        <p><strong>Notes</strong>: It takes 24 hours at the very latest for your model to be indexed and appeared in
        the public data in BioModels.</p>
        <p>Moreover, all models are only made publicly available from BioModels after the publication of the
        corresponding papers. Please refer to the section of this FAQ about the <a
            title="Will a model be publicly accessible immediately after its submission?"
            href="#access-after-submission">access to a model after its submission</a> for more information about when a
        model will be publicly available from BioModels.
        </p>

        <h4 id="cannot-find-my-model" class="level3">Why cannot I find my model in BioModels?</h4>
        <p>Probably, you submitted the model but forgot to submit it for the publication. To know more about publishing
        a model, please consult <a href="#make-model-public">making a model public</a>.</p>
        <p>After you request to publish your model, it may take upto 48 hours for a model to be available in the
        search. If you still cannot find it, please <a href="contact">contact us</a>.</p>

        <h4 id="make-model-public" class="level3">What need to be done to make model public?</h4>
        <p>First, make sure your model associated with peer-reviewed publication details. If not done yet, please go
        update it as instructed in the section <a href="#update-publication">how to update model publication</a>.</p>

        <p>User need to login using their user credentials and go to
        “My Account” &rightarrow; “My Models”. This will display the list of
        models submitted by user. Click on the model for which
        you want to Publish. On the left of side of the page, you
        will find option to “Update”, “Publish”, “Convert” etc. Click
        on “Publish” to request to make model public. Our
        database curators will take a quick look and publish the
        model if all is well. If not, curator will contact the
        model submitter. For details, please refer: BioModels
            model Submission guidelines (<a href="${manualUrl}/getting-started.html#slide17"
                                            target="_blank">Slide 17-19 (New model)</a>
        or <a href="${manualUrl}/getting-started.html#slide28"
              target="_blank">28-30 (existing model)</a>).</p>

        <h4 id="what-will-happen-model-nonpublic" class="level3">What will happen if don’t the make model public?</h4>
        <p>The model will remain inaccessible to anyone except those with an authorised account (author,
        collaborators, reviewers). If a peer reviewed publication references the model identifier, BioModels will
        release the model as soon as technically possible, even without notification from the author. However, we
        appreciate if authors send us an update once they know the publication date of the associated publication.</p>

        <h3 id="reviewer-access">How can reviewers access unpublished models?</h3>
        <p>BioModels can provide access to unpublished models to reviewers. This is not automatic, so the model
        submitter needs to request it (for example emailing us at
            <strong>biomodels-cura</strong> AT <strong>ebi.ac.uk</strong>). In this case, we will provide the submitter
        with a URL which can be used to download the model encoded in SBML. If security is a concern, we can supply a
        protected access, where reviewers will be asked for a password to access the model.
        </p>
        <p>In future updates to BioModels, we plan to automate this process. If you have more specific needs,
        please do not hesitate to <a
            title="Contact BioModels.net Team" href="contact">contact us</a>.</p>

        <h3 id="supported-formats">What are the supported model encoding formats?</h3>
        <p>BioModels is a strong advocate for interoperability and reproducibility, and the dissemination of models in
           semantically-rich community standards, along with associated data and metadata is a token of our
        commitment to these endeavours. We endorse submissions encoded in SBML, PharmML as well as COMBINE Archive.
        For a full list of supported formats, please consult the <a
            href="./model/create" target="_blank">submission guidelines</a>.</p>

        <p>If you wish to deposit a model encoded in a different format, please check  if it can be converted to one of
            the preferred formats (e.g. by consulting <a
            href="http://sbml.org/SBML_Software_Guide/SBML_Software_Matrix" target="_blank">SBML Software Matrix</a>).
        For instance, ODE models encoded in MatLab may be translated using <a
            href="http://sbml.org/Software/MOCCASIN" target="_blank">MOCCASIN</a>.</p>

        <p>In case of questions or issues, please email <strong>biomodels-cura AT ebi.ac.uk</strong> -
        a private mailing list used by our curators.

        <h3 id="how-to-submit-revised-version">How to submit a revised version of a model?</h3>
        <dl>
            <dt><strong>If the model is not yet published in BioModels</strong></dt>
            <dd>
                <div>You can send us the&nbsp;revised version at: <strong>biomodels-cura</strong> AT <strong>ebi.ac.uk</strong> together with the model submission identifier and a comment explaining which modifications were made. This mailing list is not public and the only recipients are the curators of BioModels, so you can safely send us models not yet published this way.
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

        <h3 id="cellml-conversion">Why does my SBML model contain no species or reactions after a conversion from CellML?</h3>

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

        <h3 id="submission-error">What do I do if I receive error messages when trying to submit a model?</h3>
        <p>Any validation issues detected during the submission process will be reported as warnings, making it possible to
        proceed with the submission in spite of the encoding problems. It may be perfectly possible to submit a model that
        is incorrectly encoded in SBML and then share it with someone that can help fix the SBML representation.</p>
    </div>
</div>

<div class="row">
    &nbsp;
</div>

<div id="model-curation-section" class="row faq-section-box">
    <div class="columns small-12 medium-2 large-2">
        <ol start="6" type="I">
            <li class="faq_title">Questions dealing with the curation of the models
                <ol class="faq_subheading">
                    <li><a href="#model-curation">What checks are performed to ensure model correctness?</a></li>
                    <li><a href="#miriam">What is MIRIAM?</a></li>
                    <li><a href="#miriam-compliance">How is MIRIAM compliance ensured?</a></li>
                    <li><a href="#annotation">What is annotation, and what purpose does it serve in a model?</a></li>
                    <li><a href="#annotators">Who annotates the models?</a></li>
                    <li><a href="#annotations-stored-in-sbml">How are annotations stored in SBML?</a></li>
                    <li><a href="#full-annotation">When is a model completely annotated?</a></li>
                    <li><a href="#curation-tools">What are the tools used by the curators of BioModels?</a></li>
                    <li><a href="#non-cura-branch">What is the non-curated branch of BioModels?</a></li>
                </ol>
            </li>
        </ol>
    </div>
    <div class="columns small-12 medium-10 large-10">
        <h3 id="model-curation">What checks are performed to ensure model correctness?</h3>

        <p>Before being made publicly available through BioModels, models are thoroughly verified. This involves
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

        <p>These checks come from the <a title="MIRIAM guidelines" href="#miriam">MIRIAM</a> set of guidelines.</p>

        <h3 id="miriam">What is MIRIAM?</h3>
        <p>MIRIAM is the <a title="More information about MIRIAM"
                            href="http://co.mbine.org/standards/miriam">Minimum Information Required in the Annotation of
            Models</a> (<a
            title="Access to the 2005 publication in Nature Biotechnology about MIRIAM"
            href="http://identifiers.org/pubmed/16333295">publication</a>). Initiated by the
            <a title="BioModels.net initiative"
               href="http://biomodels.net/">BioModels.net</a> project, it is a set of guidelines defining how a model should
        be encoded and annotated in order to be successfully distributed, exchanged and ultimately reused.
        </p>

        <p>In particular MIRIAM requires that a model provides all the information necessary to be instantiated in a
        simulation,
        such as the initial conditions. In addition, the reaction graph generated from this simulation must reproduce the
        results of the original publication, in which the model was first described.
        Moreover, the MIRIAM guidelines require that all model components contain sufficient controlled annotation such
        that <em>each</em> component can be unambiguously identified. The <a
            title="MIRIAM Registry" href="https://www.ebi.ac.uk/miriam/">MIRIAM Registry</a> has been merged into
            <a title="Identifiers.org"
               href="http://identifiers.org/">Identifiers.org</a> which has been developed specifically to provide
        generation
        and resolution services for unique and perennial identifiers to be used in controlled annotations.
        </p>

        <p>All models stored in the curated branch of BioModels are <a title="How is MIRIAM compliance ensured?"
                                                                                href="#miriam-compliance">MIRIAM-compliant</a>.
        </p>

        <h3 id="miriam-compliance">How is MIRIAM compliance ensured?</h3>

        <p>All the models in the curated branch are fully <a title="Go to the 'MIRIAM' section of this FAQ"
                                                             href="#miriam">MIRIAM</a> compliant. Each of the MIRIAM
        requirements is satisfied in the following way:
        </p>
        <dl>
            <dt><strong>Models be encoded in a public standard format</strong></dt>
            <dd>
                <div>All models in BioModels are converted into valid SBML.</div>
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

        <h3 id="annotation">What is annotation, and what purpose does it serve in a model?</h3>
        <p>Once we ensure a model's correctness, we annotate the model with biological information. Model elements can
        describe a plethora of different entities, such as genes, proteins or metabolites. The usage of free text or
        non-standard nomenclature to identify those elements is not reliable, as it can introduce ambiguity to the model
        components, such that subsequent users of a model would struggle to identify the precise entities involved.</p>

        <p>Annotation is a process that&nbsp;identifies model elements by linking them to existing data resources. This ensures
        that users will be able to understand the models and increases the possibility of their reuse. Moreover, annotation
        is a key element for processes such as data comparison, data integration and data conversion. Finally, it&nbsp;also allows
        the provision of models accurately through search engines (BioModels makes heavy use of annotation when
        users search for models of interest).</p>

        <p>Models in BioModels are provided with consistent annotation using unambiguous identifiers. Those
        identifiers are generated by the <a title="Identifiers.org"
               href="http://identifiers.org/">Identifiers.org</a> services. They can be used to reference records from
        external databases (such as Taxonomy, EMBL-Bank or UniProt), terms from ontologies
        (such as Gene Ontology, SBO or ChEBI), publications, etc.
        </p>

        <p>For more information about annotation, please refer our
            <a title="BioModels Annotation Information"
               href="curation#general-introduction-annotation">general introduction to the annotation of models</a>.
        </p>

        <h3 id="annotators">Who annotates the models?</h3>

        <p>Models can be submitted to BioModels already annotated. In this case, any existing annotations encoded
        using the annotation scheme developed for <a
            title="SBML specs"
            href="http://sbml.org/Documents">SBML</a> (introduced since Level 2 Version 2 and supported by numerous tools)
        will be extracted and recorded in BioModels.
        </p>

        <p>Our curation team currently checks existing annotations and adds new ones as necessary. We do encourage authors
        submitting the models to annotate their models as much as possible by following the <a
            href="https://www.ebi.ac.uk/biomodels/annotation">guidelines that the curators use during annotation</a>.
        This will help us&nbsp;accelerate the model&nbsp;curation process.
        </p>

        <h3 id="annotations-stored-in-sbml">How are annotations stored in SBML?</h3>

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

        <h3 id="full-annotation">When is a model completely annotated?</h3>
        <p>Annotating each model component with the most relevant resource records takes great efforts, especially since
        the number of submitted models has grown rapidly. For example, the 25th release of BioModels (18th June 2013)
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

        <p>
            <img src="${resource(contextPath: "${grailsApplication.config.grails.serverURL}", dir: 'images/biomodels',
                file: 'annotation.png')}" />
        </p>
        <h3 id="curation-tools">What are the tools used by the curators of BioModels?</h3>
        <p>Our curators have been making use of a wide range of tools to perform their curation tasks:
            <a href="http://www.celldesigner.org/">CellDesigner</a>,
            <a href="http://www.copasi.org/">COPASI</a>,
            <a href="http://sbml.org/Software/MathSBML">MathSBML</a>,
            <a href="http://sbw.kgi.edu/Simulation2005/">RoadRunner</a>,
            <a href="http://www.tbi.univie.ac.at/~raim/odeSolver/">SBMLOdeSolver</a>,
            <a href="https://www.ebi.ac.uk/compneur-srv/SBMLeditor.html">SBMLeditor</a>,
            <a href="http://www.semanticsbml.org/">SemanticSBML</a>,
            <a href="http://jigcell.cs.vt.edu/">JigCell</a>, <a href="https://vcell.org">VCell</a>,
            <a href="http://www.math.pitt.edu/%7Ebard/xpp/xpp.html">XPP-Aut</a>,...</p>
        <p>At present, our curators are mostly using COPASI, VCell and Matlab.</p>
        <p>Moreover, they also use tools such as <a title="Gnuplot"
                                                    href="http://www.gnuplot.info/">Gnuplot</a> and various custom scripts.</p>

        <h3 id="non-cura-branch">What is the non-curated branch of BioModels?</h3>

        <p>The success and rapid growth of BioModels has led to a couple of issues, both of which are handled using
        the non-curated branch of BioModels. Firstly, BioModels faces an ever-growing, and increasingly
        complex, number of model submissions which are growing at a pace that exceeds our curation capacity. Secondly, many
        of the models submitted, while syntactically correct, cannot be curated for various reasons. Hence, the non-curated
        branch of BioModels is a public holding area for models that have yet to be curated, or cannot be curated
        for various reasons. The models held in the non-curated branch are of four types:</p>
        <ol class="faq_list">
            <li>Models that are perfectly fine, but have yet to be curated.</li>
            <li>Models that are not curatable, since they are not <a title="Go to the 'MIRIAM' section of this FAQ"
                                                                     href="#miriam">MIRIAM</a> compliant.</li>
            <li>Models that are not currently curatable, since they are not kinetic models (e.g. logical models and Flux Balance Analysis models).</li>
            <li>Models for which SBML supports only part of the description, most of it being in annotation (e.g. spatial models).</li>
        </ol>
    </div>
</div>

<div class="row">
    &nbsp;
</div>

<div id="software-infrastructure" class="row faq-section-box">
    <div class="columns small-12 medium-2 large-2">
        <ol start="7" type="I">
            <li class="faq_title">Questions dealing with the underlying software infrastructure
                <ol class="faq_subheading">
                    <li><a
                        href="#biomd-software">What are the tools used to develop and run the software infrastructure behind BioModels?</a>
                    </li>
                    <li><a href="#biomd-software-reuse">Can I install my own version of BioModels?</a></li>
                </ol>
            </li>
        </ol>
    </div>
    <div class="columns small-12 medium-10 large-10">
        <h3 id="biomd-software">What are the tools used to develop and run the software infrastructure behind BioModels?</h3>
        <p>The current system is based on <a
            href="https://bitbucket.org/jummp/jummp/">Jummp,</a> a flexible, open-source model management platform that can be
        tailored to the needs of other projects or communities. Submissions are hosted using a combination of version control
        systems such as <a
            href="https://git-scm.com/">Git</a>, a relational database such as <a
            href="https://www.mysql.com/">MySQL</a>&nbsp;or <a
            href="https://www.postgresql.org/">PostgreSQL</a>, <a href="https://redis.io">Redis</a>,
        and a search engine like <a href="http://lucene.apache.org/">Lucene</a>.</p>

        <p>Support for standard formats is provided by dedicated software tools, such as &nbsp;<a
            href="http://sbml.org/Software/JSBML">JSBML</a>, <a
            href="http://www.pharmml.org/tools/libpharmml">libPharmML</a>, <a
            href="https://github.com/mglont/CombineArchive">libCOMBINEArchive</a>.</p>

        <h3 id="biomd-software-reuse">Can I install my own version of BioModels?</h3>
        <p>The models can be <a
            title="What are the conditions of use and distribution for unmodified models originating from BioModels?"
            href="#model-reuse">freely reused</a> and the software running BioModels itself is an open source project
        distributed under the GNU General Public License. For more information, please refer to the <a
            title="What are the tools used to develop and run the software infrastructure behind BioModels?"
            href="#biomd-software">software infrastructure</a> entry of this FAQ.</p>

        <p>All converters are also available under an open source license and can be downloaded from the <a
            title="Download files from BioModels SF project"
            href="https://sourceforge.net/projects/biomodels/files/">download section of the BioModels SourceForge
            repository</a>.
        </p>

        <p>In order to store your own models or just plug part(s) of the BioModels infrastructure into your own,
        you'll need to setup your own local instance of the repository. In order to help you through this procedure, we
        created the following page: <a
            title="How to install you own instance of the repository"
            href="https://bitbucket.org/jummp/jummp/wiki/install">development with BioModels source code</a>.</p>
    </div>
</div>

<div class="row">
    &nbsp;
</div>

<div id="misc-questions" class="row faq-section-box">
    <div class="columns small-12 medium-2 large-2">
        <ol start="8" type="I">
            <li class="faq_title">Miscellaneous questions
                <ol class="faq_subheading">
                    <li><a href="#id-scheme">What is the naming and identifier scheme used in BioModels?</a></li>
                    <li><a href="#model-authors">Who are the authors of a model?</a></li>
                    <li><a href="#model-submitter">Who is the submitter of a model?</a></li>
                    <li><a href="#model-creators">Who are the encoders of a model?</a></li>
                    <li><a
                        href="#error-reaction">Why do I get an error message stating some reaction modifiers are not
                        declared?</a>
                    </li>
                </ol>
            </li>
        </ol>
    </div>
    <div class="columns small-12 medium-10 large-10">
        <h3 id="id-scheme">What is the naming and identifier scheme used in BioModels?</h3>

        <p>At the time of submission, a unique submission identifier assigned to the model. It is composed of the character
        sequence "MODEL" followed by ten digits extracted from the timestamp of submission. When a model is moved to the
        curated branch, a new model identifier is generated and assigned to it. This identifier is composed of the character
        sequence "BIOMD" followed by ten digits reflecting the model's position in the branch (for example "BIOMD0000000216"
        for the 216th model successfully curated).</p>

        <p>During the <a title="Go to: curators"
                         href="#model-curation">curation process</a>, the <em>curators</em> will also give an appropriate
        name to each model. This follows the general scheme: <em>AuthorYear - Topic Method</em>.
        For example, Edelstein1996 - EPSP ACh Event refers to the model <a
            title="Go to model: BIOMD0000000001" href="https://www.ebi.ac.uk/biomodels/BIOMD0000000001">BIOMD0000000001</a>.</p>

        <p>Both identifiers are unique and permanent, and will never be re-assigned to a different model, even if for some
        reason a particular model must be retracted from the database. Both identifiers can be used to retrieve the model
        and quoted in subsequent publications.</p>

        <h3 id="model-authors">Who are the authors of a model?</h3>

        <p>The <em>authors</em> of a model are the individuals who initially described the model in a peer-reviewed publication.
        </p>

        <h3 id="model-submitter">Who is the submitter of a model?</h3>

        <p>The <em>submitter</em> of a model is the person who actually submitted the model and therefore requested its
        addition in BioModels. Anybody can submit a model to BioModels. Please refer to <a
            title="Model submission procedure" href="#model-submission">the submission procedure</a>, if you want to know
        more.
        </p>

        <h3 id="model-creators">Who are the encoders of a model?</h3>

        <p>The <em>encoders</em> of a model are the individuals who actually encoded the model in its present form, based on
        the published article. These individuals could be the authors of the publication, members of the curation team, or
        any other scientists who decided to encode the published article.
        </p>

        <h3 id="error-reaction">Why do I get an error message stating some reaction modifiers are not declared?</h3>

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
