<%--
 Copyright (C) 2010-2025 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
 with Jummp; if not, see <https://www.gnu.org/licenses/agpl-3.0.html>.
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="${session['branding.style']}/main" />
    <title><g:message code="${titleCode}" default="Annotation Guidelines"/> | BioModels</title>
</head>

<body>
<p class="back-to-curation-docs">
    &laquo; <a href="${createLink(controller: 'jummp', action: 'curatorZone')}">Curation documentation</a>
</p>

<h1>BioModels Database Annotation Guidelines</h1>


<p>
  This document contains a set of guidelines for the annotation of models, that is, to link model components with terms from controlled vocabularies and other data resources.
</p>
<p>
  This document has been designed with our curators in mind, please refer to our <a href="${createLink(controller: 'jummp', action: 'annotationInfo')}" title="Annotation Information">annotation information</a> page for an introduction about model annotation.
</p>


<h2>Selecting Adequate Accession Numbers</h2>

<p>
  When annotating a component with an external resource reference, the first step is to identify what is exactly this reference. It should be a perennial tag. For instance an "entry name" of <a href="http://www.uniprot.org/" title="UniProt">UniProt</a>, such as CALM_HUMAN, <strong>is not</strong> perennial. It is modified on a regular basis to better reflect the classification of the protein. The "Accession", on the contrary, such as <a href="http://www.uniprot.org/uniprot/P62158">P62158</a>, is perennial. Even if some accession numbers are later on downgraded from primary to secondary (for instance when database entries are merged), one can always retrieve the correct UniProt entry based on those accession numbers.
</p>
<p>
BioModels Database follows the <a href="http://biomodels.net/miriam/" title="MIRIAM guidelines">MIRIAM guidelines</a> for annotation and curation, and employs MIRIAM URIs to encode cross references to external resources. The different data collections currently supported by the <a href="http://www.ebi.ac.uk/miriam/" title="MIRIAM Registry">MIRIAM Registry</a> can be accessed under <a href="http://www.ebi.ac.uk/miriam/main/collections/"  title="MIRIAM data collections">http://www.ebi.ac.uk/miriam/main/collections/</a>. Some of the entries give usage examples detailing which SBML elements are most likely to be annotated by this specific data type. The CHEBI <a href="http://www.ebi.ac.uk/miriam/main/usage/MIR:00000002">usage example</a> shows that parameter and species elements can potentially be annotated with CHEBI IDs. BioModels Database, as well as some other tools such as Semantic SBML and SBML editor, make use of this information for preselecting data collections in their annotation interface. If you find an example usage of a data type misleading or missing, you can suggest modifications on its usage page.
</p>
<p>
 It can be hard to find a term of the adequate level of specificity for annotation. In general one should always select the closest and most specific relevant piece of data still general enough to encompass all aspects of the annotated element covered by the data type. In the case of hierarchical knowledge, e.g. controlled vocabularies or classifications, one should carefully choose the level of detail. Sometimes, the finest level is acceptable. For instance, in order to annotate the activation of cdc2 kinase by cyclins in amphibians, one should use the <a href="http://www.geneontology.org/" title="GO">Gene Ontology</a> term <a href="http://www.ebi.ac.uk/QuickGO/GTerm?id=GO:0045737" title="GO:0045737">GO:0045737 "positive regulation of cyclin dependent protein kinase activity"</a>, rather than the more general parent term <a href="http://www.ebi.ac.uk/QuickGO/GTerm?id=GO:0000079" title="GO:0000079">GO:0000079 "regulation of cyclin dependent protein kinase activity"</a>. Indeed, the latter is also the parent of <a href="http://www.ebi.ac.uk/QuickGO/GTerm?id=GO:0045736" title="GO:0045736">GO:0045736 "negative regulation of cyclin dependent protein kinase activity"</a>, which is not adequate to describe the reaction under annotation. On the contrary, and considering a completely different type of knowledge, the model of mitotic oscillator presented in Goldbeter (1991) [<a href="/BIOMD0000000003" title="Model BIOMD0000000003">BIOMD0000000003</a> and <a href="/BIOMD0000000004" title="Model BIOMD0000000004">BIOMD0000000004</a>] describes a generic mechanism of amphibian cell cycle. Therefore, the taxonomy classification <a href="http://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=8292" title="Taxonomy: 8292">8292 "<em>Amphibia</em>"</a> should be used, rather than the more precises <a href="http://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=8355" title="Taxonomy: 8355">8355 "<em>Xenopus l&aelig;vis</em>"</a> or <a href="http://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=8401" title="Taxonomy: 8401">8401 "<em>Rana esculenta</em>"</a>.
</p>
<p>
  A generic annotation is always better than nothing! For instance annotating the dissociation of MAPKKK with MAPKK using the <a href="http://www.geneontology.org" title="GO">Gene Ontology</a> term <a href="http://www.ebi.ac.uk/QuickGO/GTerm?id=GO:0043241" title="GO:0043241">GO:0043241 "protein complex disassembly"</a> carries a significant amount of information when it comes to characterise the reaction. It is definitively better that no annotation at all. Another  example is the annotation of a particular messenger mRNA <a href="http://www.genome.jp/kegg/" title="KEGG">KEGG</a> <a href="http://www.genome.jp/dbget-bin/www_bget?cpd:C00046" title="C00046">C00046 "Ribonucleic acid"</a>.
</p>


<h2>Qualification of Annotation</h2>

<p>
  The qualification of an annotation is important to grasp the relation between a model component and its annotation. The relationships are rarely one-to-one, and the information content of an annotation is greatly increased if one knows what it represents rather than to know that it is vaguely "related to".
</p>
<p>
  The qualifier of an annotation should reflect the relationships between the biological objects represented by the model element and the annotation:
</p>

<div style="text-align: center; padding-top:5px;">
  <img src="https://www.biomodels.org/static-assets/images/qualifiers.png" alt="relation between model and data" />
</div>

<p>
  The definition of all the qualifiers used by BioModels Database can be found on the <a href="http://biomodels.net/qualifiers/" title="BioModels.net qualifiers">BioModels.net website</a>.
</p>
<p>
  The simplest qualifications are the annotation of, or by, an abstracted entity. E.g. a species representing a "cyclin" <em>has version</em> "cig1", "cdc13" etc. Conversely a reaction representing "phosphorylation of cdk2" <em>is version of</em> "phosphorylation of protein". Versions of species are <em>physical modifications</em>, such as conformational states, covalent modifications, etc. The same species in different compartments are not alternative versions.
</p>
<p>
  Finding the correct qualifications can be tricky when the lack of directly relevant annotation forces the use of non-directly related information. To exemplify the problem, let's consider organisms:
</p>

<ul>
  <li>A model of "xenopus", annotation by "amphibian" data: <em>isVersionOf</em></li>
  <li>A model of "amphibian", annotation by "xenopus" data: <em>hasVersion</em></li>
  <li>A model of "xenopus", annotation by "frog" data: <em>isHomologTo</em></li>
  <li>A model of "amphibian", annotation by "human" data: <b>?</b> <span style="font-style:italic">Since "human" is a species, and "amphibian" a group of species, it could be considered as a <em>hasVersion</em> relationship, even if "human" is not a version of "amphibian"</span></li>
</ul>

<p>
  Several sets of annotations can be created for a model component. The sets are homogeneous and different qualifications are stored in different sets. Several sets can exist with the same qualification, and represent alternative, sometimes overlapping, annotations. For instance, if a model reaction represent the combination of three successive biochemical reactions, one can have two sets of <em>has part</em> annotations, one with three EC codes, and one with three KEGG reaction identifiers. In general only the qualifiers <em>hasPart</em> and <em>hasVersion</em> should contain more than one reference in a given set. The concepts represented by the different references in one set must not overlap and should be of the same data type, if possible. In some cases, for example a complex of the protein calmodulin with Ca<sup>2+</sup>, it has to be a mixture of references to the UniProt entry of calmodulin and the CHEBI entry for Ca<sup>2+</sup>.
</p>
<p>
  There is only one level of explicit qualification. In addition, an implicit <em>hasVersion</em> is embedded in the sets. If there are two sets of <em>hasPart</em> annotations, both sets are alternative complexes made-up of their parts.  When the exact description of the relation between a model component and its annotation would require combination of several qualifiers, a precedence has to be established:
</p>

<ul>
  <li><em>hasPart</em> has precedence over <em>hasVersion</em></li>
  <li><em>isPartOf</em> has precedence over <em>isVersionOf</em></li>
  <li><em>hasPart</em> has precedence over <em>isHomologTo</em></li>
</ul>

<p>
  For example, a protein complex of "amphibian" annotated with proteins of "xenopus" should have one <em>hasPart</em>, rather than several <em>hasVersion</em> sets (Note that one <em>hasVersion</em> set with all the annotations would mean that they are alternative versions).
</p>

<h2>Annotation with SBO terms</h2>

<p>
SBML models from level 2 version 2 onwards give the option of annotating elements directly with terms from the <a href="http://www.ebi.ac.uk/sbo/" title="SBO">Systems Biology Ontology (SBO)</a> using the <em>sboTerm</em> attribute. These annotations allow to put another layer of semantics on a model and are for example essential for creating graphical representations such as <a href="http://www.sbgn.org/" title="SBGN">Systems Biology Graphical Notation (SBGN)</a> diagrams, or converting SBML to other model description formats, <a href="http://www.biopax.org/" title="BioPAX">such as BioPAX</a>.
</p>
<p>
For BioModels Database at the following elements should be annotated with SBO terms:
</p>
<table border="1" cellspacing="0" cellpadding="4" width="80%" align="center">
  <tr align="center">
    <td>element</td>
    <td>child of SBO term</td>
  </tr>
  <tr align="center">
    <td>compartment</td>
    <td><a href="http://www.ebi.ac.uk/sbo/main/browse.jsp?sboId=SBO:0000290">SBO:0000290</a> - physical compartment</td>
  </tr>
  <tr align="center">
    <td>species</td>
    <td><a href="http://www.ebi.ac.uk/sbo/main/browse.jsp?sboId=SBO:0000240">SBO:0000240</a> - material entity</td>
  </tr>
  <tr align="center">
    <td>reaction</td>
    <td><a href="http://www.ebi.ac.uk/sbo/main/browse.jsp?sboId=SBO:0000375">SBO:0000375</a> - process</td>
  </tr>
   <tr align="center">
    <td>reactant</td>
    <td><a href="http://www.ebi.ac.uk/sbo/main/browse.jsp?sboId=SBO:0000010">SBO:0000010</a> - reactant</td>
  </tr>
  <tr align="center">
    <td>product</td>
    <td><a href="http://www.ebi.ac.uk/sbo/main/browse.jsp?sboId=SBO:0000011">SBO:0000011</a> - product</td>
  </tr>
<tr align="center">
    <td>modifier</td>
    <td><a href="http://www.ebi.ac.uk/sbo/main/browse.jsp?sboId=SBO:0000019">SBO:0000019</a> - modifier</td>
  </tr>
<tr align="center">
    <td>kineticLaw</td>
    <td><a href="http://www.ebi.ac.uk/sbo/main/browse.jsp?sboId=SBO:0000001">SBO:0000001</a> - rate law</td>
  </tr>
<tr align="center">
    <td>parameter</td>
    <td><a href="http://www.ebi.ac.uk/sbo/main/browse.jsp?sboId=SBO:0000002">SBO:0000002</a> - quantitative systems description parameter</td>
  </tr>
</table>

<p>
Annotating reactants, products, modifiers, rate laws and parameters can be quite a time consuming task, although there exist tools to help with that, for example semanticSBML. Furthermore, Michael Schubert wrote a python script that detects many rate laws automagically and annotates all the above.<br/>
Additionally to the above, the model element's sboTerm can be used to indicate the <a href="http://www.ebi.ac.uk/sbo/main/browse.jsp?sboId=SBO:0000004">mathematical framework</a> under which the model should be interpreted.
</p>
<h2>Annotation of the <tt>model</tt> element</h2>

<p>
  The encoders are all the traceable persons who created or modified the structure of the encoded model. All encoders should be quoted adequately. In particular the initial creators should be tracked if they are not specified explicitly, for instance in the <tt>notes</tt> elements. If the model has been taken from another data resource, and no curator identity is available, one must quote the creator(s) of the resource.
</p>
<p>
  BioModels Database curators should be quoted as well.
</p>


<h2>Annotation of <tt>species</tt></h2>

<p>
  One should avoid to annotate a species with homologs as much as possible. Sometimes a protein is not described in UniProt, but it
could be derived from Ensembl. In such a situation, it is better to annotate with Ensembl than to use an homolog present in UniProt.
</p>
<p>
  Often a model is pretty generic and defines only classes of molecules. The use of controlled vocabularies and hierarchical classification such as InterPro, can help to chose the right level of abstraction (careful with InterPro to use the family branches, not the domain or catalytic site ones). Sometimes, one can nevertheless annotate a component with a particular instance, based on the biochemistry implied in the model. For instance, Hoefnagel et al (2002) [<a href="/BIOMD0000000017" title="Model BIOMD0000000017">BIOMD0000000017</a>] only defines the species "lactate", created from pyruvate. It seems therefore reasonable to annotate it with the <a href="http://www.ebi.ac.uk/chebi/" title="ChEBI">ChEBI</a> term <a href="http://www.ebi.ac.uk/chebi/searchId.do?chebiId=CHEBI:24996" title="CHEBI:24996">CHEBI:24996 "lactate"</a>, rather than any specific isomer. However, one can notice that the authors mention only the Lactate deshydrogenase, and not the D-lactate deshydrogenase. One can thus also annotate the species with <a href="http://www.genome.jp/kegg/" title="KEGG">KEGG</a> <a href="http://www.genome.jp/dbget-bin/www_bget?cpd:C00186" title="C00186">C00186 "(S)-Lactate"</a>.
</p>
<p>
  Many external resources do not offer different levels of knowledge. For instance, UniProt database lists proteins, not protein types. When one wants to annotate a generic type of protein, one needs to list all the suitable proteins (or let's say a significant subset, such as all the paralogs in one species). For instance, the model of mitotic oscillator presented in Goldbeter (1991) [<a href="/BIOMD0000000003">BIOMD0000000003</a> and <a href="/BIOMD0000000004" title="Model BIOMD0000000004">BIOMD0000000004</a>] describes a species "cdc2k", that could be annotated with <a href="http://www.uniprot.org/uniprot/P35567" title="UniProt: P35567">P35567 (CDC21_XENLA)</a> and <a href="http://www.uniprot.org/uniprot/P24033" title="UniProt: P24033">P24033 (CDC22_XENLA)</a>, the two forms of cdc2 in <em>Xenopus l&aelig;vis</em>.
</p>
<p>
  One should be extremely careful not to always completely equate the name of a species with a specific biochemical entity. For instance, the creator of the model described in Curtot <em>et al</em> (1998) [<a href="/BIOMD0000000015" title="Model BIOMD0000000015">BIOMD0000000015</a>] used the term "ATP" to call the species X<sub>4</sub> described in the paper. This species actually represents the sum Adenosine+AMP+ADP+ATP. As a consequence, not only the species "ATP" has to be annotated 4 times, but the reactions involving the species "ATP" are actually sets of 4 different reactions (some actually never happening in Nature, but that's another story).
</p>
<p>
  One has to be very careful with the hits returned from search engines. They can be very misleading. A bad annotation unfortunately spread widely through wrong associations. For instance on February 2005, a search of the database <a href="http://www.bind.ca/" title="BIND">BIND</a> with "acetylcholine" returned the complex made-up of p25, CDK5 and PCTAIRE-motif protein kinase 1. As far as the author - who spent 12 years working on acetylcholine receptors - knows, there is no relationship between this complex and any aspects of acetylcholine physiology.
</p>
<p>
  One should only use <a href="http://www.geneontology.org/" title="GO">Gene Ontology</a> terms coming from the <em>Cellular Component</em> vocabulary to annotate species. One should never use terms coming from the <em>Molecular Function</em> or <em>Biological Process</em>, even if they fit with the function of the species. Those vocabulary should be used to annotate reactions, rules and events instead.
</p>


<h2>Annotation of Reactions</h2>

<p>
  Although one should annotate a reaction with an <a href="http://www.chem.qmul.ac.uk/iubmb/enzyme/" title="EC code">EC code</a>, a <a href="http://www.genome.jp/kegg/reaction/" title="KEGG reaction">KEGG reaction</a>, an <a href="http://www.ebi.ac.uk/intact/" title="IntAct">IntAct</a> or BIND identifier, sometimes this is just not possible. If the reaction is an abstract summary of a linear pathway, one should annotate it with all the relevant codes, as one would annotate a multimolecular complex with the identifiers relevant for all its components. For instance the reaction "den" of Curtot <em>et al</em> (1998) [<a href="/BIOMD0000000015" title="Model BIOMD0000000015">BIOMD0000000015</a>] correspond to the KEGG reactions <a href="http://www.genome.jp/dbget-bin/www_bget?rn:R01072" title="Kegg reaction: ">R01072</a>, <a href="http://www.genome.jp/dbget-bin/www_bget?rn:R01127" title="Kegg reaction: R01127">R01127</a>, <a href="http://www.genome.jp/dbget-bin/www_bget?rn:R04144" title="Kegg reaction: R04144">R04144</a>, <a href="http://www.genome.jp/dbget-bin/www_bget?rn:R04208" title="Kegg reaction: R04208">R04208</a>, <a href="http://www.genome.jp/dbget-bin/www_bget?rn:R04209" title="Kegg reaction: R04209">R04209</a>, <a href="http://www.genome.jp/dbget-bin/www_bget?rn:R004325" title="Kegg reaction: R04325">R04325</a>, <a href="http://www.genome.jp/dbget-bin/www_bget?rn:R04463" title="Kegg reaction: R04463">R04463</a>, <a href="http://www.genome.jp/dbget-bin/www_bget?rn:R04559" title="Kegg reaction: R04559">R04559</a>, <a href="http://www.genome.jp/dbget-bin/www_bget?rn:R04560" title="Kegg reaction: R04560">R04560</a>, <a href="http://www.genome.jp/dbget-bin/www_bget?rn:R04591" title="Kegg reaction: R04591">R04591</a>! If the reaction is a generic one, sometimes a Gene Ontology term is sufficient, such as <a href="http://www.ebi.ac.uk/ego/DisplayGoTerm?selected=GO:0006308" title="GO:0006308">GO:0006308 "DA catabolism"</a> for the reaction "dnag" of of Curtot <em>et al</em> (1998) [<a href="/BIOMD0000000015" title="Model BIOMD0000000015">BIOMD0000000015</a>]. As usual, anything is better than nothing, providing that the information is not misleading.
</p>
<p>
  It happens that several reactions are possible, which link a given set of substrates to a given set of products. Often they differ by the differential use of small molecules. A careful reading of the paper can help picking the right annotation. For instance in of Curtot <em>et al</em> (1998) [<a href="/BIOMD0000000015" title="BIOMD0000000015">BIOMD0000000015</a>], a reaction links GMP and XMP, with ATP as a modifier. Two reactions correspond in KEGG, <a href="http://www.genome.jp/dbget-bin/www_bget?rn:R01230" title="Kegg reaction: R01230">R01230</a> and <a href="http://www.genome.jp/dbget-bin/www_bget?rn:R01231" title="Kegg recation: R01231">R01231</a>, the former producing ammonia, while the latter producing L-glutamate. However, in appendix A of the paper, one can read that the reaction "gmps" correspond to XML+ATP+glutamine=>GMP+AMP+Pi. Bingo.
</p>
<p>
  Sometimes, a look at the publications listed in KEGG is sufficient to help the decision. For instance, S-adenosylmethioninamine is transformed into 5'-methylthioadenosine by two reactions, using the <a href="http://www.ebi.ac.uk/intenz/query?cmd=SearchEC&amp;ec=2.5.1.22" title="IntEnz Enzyme Nomenclature: EC 2.5.1.22">spermine synthase (EC 2.5.1.22)</a> and the <a href="http://www.ebi.ac.uk/intenz/query?cmd=SearchEC&amp;ec=2.5.1.23" title="IntEnz Enzyme Nomenclature: EC 2.5.1.23">sym-norspermidine synthase (EC 2.5.1.23)</a>. However, a look at the references shows that the first reaction has been studied in rat and bovine, while the second has been studied in the simpler eukaryot Euglena. If you want to annotate a human pathway, as in Curtot <em>et al</em> (1998) [<a href="/BIOMD0000000015" title="Model BIOMD0000000015">BIOMD0000000015</a>], the former is a reasonable choice.
</p>

<br />

</body>
<content tag="curator-zone">
    selected
</content>
</html>
