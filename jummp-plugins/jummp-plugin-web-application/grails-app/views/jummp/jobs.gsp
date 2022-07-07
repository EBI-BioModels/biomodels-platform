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
    <title><g:message code="${titleCode}" default="Internship or Job Opportunities"/> | BioModels</title>
</head>
<body>

<h2>Internship opportunities</h2>

<h3 style="border-bottom: 1px solid grey">
    Curation internship: Literature curation of mechanistic models of Genome-Scale metabolic models</h3>

<p><a href="${serverURL}" title="BioModels Database">BioModels</a> is a
central repository of mathematical models of biological/biomedical processes. It is hosted
at EMBL-EBI and is one of the resources of the molecular systems cluster. The models
distributed through BioModels are extensively tested and encoded in standard formats
(for example, SBML (Systems Biology Markup Language)) and are free to use. In addition,
the models and their components are cross-referenced with external data resources and
ontologies, which facilitates search and retrieval, and maximises the benefits of the
growing number of already existing models.</p>
<div class="row">
    <div class="columns small-12 medium-4 large-4">
        <img src="${bmStaticAssetsURL}/images/curation/FROG/FROG_analysis_green_BG.svg"/>
    </div>
    <div class="columns small-12 medium-8 large-8">
        <p> Curating models of biological processes is an effective training in computational
        systems biology, where the curators gain an integrative knowledge on biological systems,
        modelling and bioinformatics. We have a number of internship opportunities available within
        the team, to carry out the curation of constraint-based models including Genome-Scale Metabolic models (GEMs).
        </p>
        <p>The internship is suitable for a student who is pursuing or completed master's or PhD degree and has experience
        in constraint-based modelling approaches including genome-scale metabolic model reconstruction, flux balance analysis
        or equivalent methods. The intern will apply the recently developed community standard FROG to assess the
        reproducibility of the constraint-based models and perform semantic enrichment to curate. Training will be given
        to understand the basics of FROG analysis and relevant tools. Potential possibilities exist for the intern to be
        a co-author of our community manuscript on FROG analysis.
        </p>
    </div>
</div>

<p>This is an in-person internship at the EMBL-EBI Campus based in Hinxton, Cambridge, UK. The provisional starting
date is September 2022, however, it can be adjusted. The position is for 6 months but can be extended up to one year.
A fixed monthly allowance is provided to help towards living costs. Support for the visa will be offered to the
selected candidate when required.
</p>
<p>For further enquiries or to make an application (with your CV and a cover letter),
please contact: Dr Rahuman Sheriff (sheriff AT ebi.ac.uk). The application is open until filled.</p>

<h3 style="border-bottom: 1px solid grey">
    EMBL-EBI - IITM Internship on Curation of Machine Learning Models in BioModels</h3>

<p>Machine learning (ML) models are widely used as tools in life science and medical research.
However, ML models are scattered across various resources including personal websites, git-hub, bitbucket,
and supplementary material, making it difficult to find, access, and reuse them.
We aim to extend the <a href="${serverURL}" target="_blank">BioModels</a> to support Findable, Accessible,
Interoperable, and Reusable (<a href="https://www.go-fair.org/fair-principles/" target="_blank">FAIR</a>)
dissemination of ML models in biomedical sciences.  BioModels is a world-leading repository of mechanistic models
of biological processes, hosted by EMBL-EBI. BioModels’s infrastructure was
recently enhanced to support version-controlled dissemination and curation of a
wide range of modelling frameworks and formats, providing capabilities to host
and disseminate ML models.</p>

<p>The internship is ideal for a student who is pursuing or completed a master's or PhD degree and aiming
to move towards the next step of their career. The applicant should have experience in machine learning
approaches including deep learning neural networks and other equivalent methods. During the internship,
the intern will rebuild ML models published in life science journals and submit them to the BioModels
repository. Potential possibilities exist for the intern to be a co-author on our high-impact manuscript.</p>

<p><strong>Prerequisite</strong>: very good programming skills in Python, R or equivalent languages, experience in building
machine learning models, and a good background in biological science. Experience in deep learning will be a plus.</p>

<p>The first 3 months of the internship are based at the <a href="https://rbcdsai.iitm.ac.in/" target="_blank">Robert
Bosch Centre for Data Science and Artificial Intelligence</a> &
<a href="https://www.iitm.ac.in/academics/departments/department-of-biotechnology"
   target="_blank">Dept. of Biotechnology</a>,
    <a href="https://www.iitm.ac.in/" target="_blank">Indian Institute of Technology Madras</a> at
    <a href="https://home.iitm.ac.in/kraman/lab/karthik/" target="_blank">Prof. Raman’s lab, Chennai, India</a>. The next 3 months will be based at the European Bioinformatics Institute (EMBL-EBI) Campus at Hinxton, Cambridge, UK. The position is for 6 months but can be extended up to one year. The provisional starting date is September 2022, however, it can be adjusted. A fixed monthly allowance is provided to help towards living costs. Support for a UK visa will be offered to the selected candidate.</p>

<p>For further enquiries or to make an application (with your CV and a cover letter),
please contact Prof. Karthik Raman (kraman AT iitm.ac.in) and or Dr Rahuman Sheriff (sheriff AT ebi.ac.uk).
The application is open until filled.</p>

<!--
<h3 style="border-bottom: 1px solid grey">Curation internship: Literature curation of mechanistic models of disease pathways</h3>

<p><a href="${serverURL}" title="BioModels Database">BioModels</a> is a
central repository of mathematical models of biological/biomedical processes. It is hosted
at EMBL-EBI and is one of the resources of the molecular systems cluster. The models
distributed through BioModels are extensively tested and encoded in standard formats
(for example, SBML (Systems Biology Markup Language)) and are free to use. In addition,
the models and their components are cross-referenced with external data resources and
ontologies, which facilitates search and retrieval, and maximises the benefits of the
growing number of already existing models.</p>
<p> Curating models of biological processes is an effective training in computational
systems biology, where the curators gain an integrative knowledge on biological systems,
modelling and bioinformatics. We have a number of internship opportunities available within
the team, to carry out targeted curation of mechanistic models of certain disease pathways.
The topics likely to be focused, but not limited to, are cancer, diabetes
[<a href="#ref1" title="Go to this reference">1</a>], arthritis (inflammation &amp; immunology),
plant systems, and neurodegeneration [<a href="#ref2" title="Go to this reference">2</a>].</p>
<p> The internships might be ideal for a master degree thesis or for candidates who are in between
their masters and PhD. An initial knowledge of biology, mathematics and computing is desirable.
The successful candidate will be a master student in bioinformatics or systems biology.
Training will be given to understand basics of modelling, and on modelling and simulation tools.
The position is ideally for 6 months, but can be extended up to one year. A fixed monthly
allowance is provided to help towards living costs.
</p>
<p>For further enquiries or to make an application (attach your CV and a cover letter),
please contact: Rahuman Sheriff (sheriff AT ebi.ac.uk).</p>

<ol>
    <li id="ref1">Ajmera I., Swat M., Laibe C., Le Novère N., Chelliah V.
        <a href="//identifiers.org/pubmed/23842097">
        The impact of mathematical modeling on the understanding of diabetes and related complications.</a>
        <em>CPT: Pharmacometrics &amp; Systems Pharmacology</em>. Jul 10;2:e54. 2013.</li>
    <li id="ref2">Lloret-Villas A., Varusai T.M., Juty N., Laibe C., Le Novèrei N., Hermjakob H., Chelliah V.
        <a href="//ascpt.onlinelibrary.wiley.com/doi/abs/10.1002/psp4.12155">
        The impact of mathematical modeling in understanding the mechanisms underlying
        neurodegeneration: evolving dimensions and future directions.</a>
        <em>CPT: Pharmacometrics &amp; Systems Pharmacology</em>. 2017</li>
</ol>
-->

%{--<h3 style="border-bottom: 1px solid grey">Software development internship:
Cluster Analysis of BioModels using Biomedical Ontologies</h3>
<p>Ontologies provide formal means of defining concepts and their interrelationships as a
knowledge graph routinely consisting of thousands or tens of thousands of terms arranged
hierarchically. Amid the exponential increase of data available in life sciences, ontologies
have become the cornerstone of data integration and search.</p>
<p><a href="http://www.ebi.ac.uk/biomodels" target="_blank">BioModels</a>
at the European Bioinformatics Institute is a repository of mathematical models
describing biological processes. It hosts over 8400 submissions derived from the literature
and over 142000 models that have been automatically generated from pathway resources. All
depositions are linked, using semantic annotations, to terms from established resources like
the NCBI Taxonomy database or biomedical ontologies such as Gene Ontology, ChEBI or Systems
Biology Ontology.</p>
<p>The hierarchical nature of these classifications can be leveraged to help users
progressively find the content of interest (e.g.
<a href="http://www.ebi.ac.uk/biomodels-main/gotree" target="_blank">
    http://www.ebi.ac.uk/biomodels-main/gotree</a>),
but a common usability challenge with such approaches is the need to prune intermediate
categories. Previous attempts to do so (<a href="" target="_blank">
http://www.ebi.ac.uk/biomodels-main/gochart</a>) have relied on manual input and are not
immediately reusable for other ontologies.</p>
<p>In this context, this project strives to
(1) develop automated methods for clustering models based on their semantic annotations
and an arbitrarily-chosen set of ontologies;
(2) create novel visualisation components which use the results of the cluster analysis
to provide user-friendly ways of browsing BioModels content.
<p>Required skills and experience:</p>
<ul>
    <li>solid understanding of Computer Science underpinnings, in particular,
        algorithms and data structures commonly-used in graph theory;</li>
    <li>familiarity with machine learning approaches and toolkits;</li>
    <li>good command of one or more mainstream programming languages (Java, C++, Python);</li>
    <li>willingness to develop new abilities as the project requires;</li>
    <li>self-motivated and capable of working both independently and as part of a team;</li>
    <li>proficient communication, interpersonal and English language skills;</li>
</ul>
<p>Desirable experience:</p>
<ul>
    <li>hands-on experience of working with Semantic Web technologies;</li>
    <li>familiarity with modern JavaScript language features and libraries</li>
</ul>
<p>This project is expected to last around six months and would be ideally suited for a
dissertation project or internship. For further enquiries or to make an application
(attach your CV and a cover letter), please contact: Mihai Glont (mglont AT ebi.ac.uk).--}%
</body>
<content tag="jobs">
    selected
</content>
<content tag="title">
    <g:message code="${titleCode}" default="Jobs" />
</content>
