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






<%@ page contentType="text/html; charset=UTF-8" %>
<head>
    <meta name="layout" content="${session['branding.style']}/main" />
    <script type="text/javascript">
        function toggleDisplayById(theElementId) {
            var obj = document.getElementById(theElementId)
            if (obj.className == "") {
                obj.className = "hidden";
            } else {
                obj.className = "";
            }
        }
    </script>
</head>
<body>
    <h2>How can I quote BioModels Database?</h2>

<p>
  Several papers about BioModels Database and related efforts have been published. Please see below for how to cite: <a href="#biom
odels" title="How to cite BioModels Database">BioModels Database</a> | <a href="#path2models" title="How to cite Path2Models">Path2
Models</a> | <a href="#associated_services" title="How to cite BioModels' associated services">associated services</a> | <a href="#
others" title="Other publications related to BioModels Database">others</a>.
</p>
<p>
  For a more complete list, please refer to <a href="http://scholar.google.co.uk/citations?user=sxPul0AAAAAJ" title="BioModels rela
ted publications on Google Scholar">our Google Scholar profile</a>.
</p>


<h3 id="biomodels">BioModels Database</h3>

<dl>
    <dt>
        A Lloret‐Villas, TM Varusai, N Juty, C Laibe, N Le Novère, H Hermjakob and V Chelliah
    </dt>
    <dd style="margin-left:0;">
        <div class="pubtitle">The Impact of Mathematical Modeling in Understanding the Mechanisms Underlying
        Neurodegeneration: Evolving Dimensions and Future Directions.</div>
        <div class="pubjournal"><em>CPT: Pharmacometrics &amp; Systems Pharmacology</em> 2017</div>
        [<a href="//ascpt.onlinelibrary.wiley.com/doi/abs/10.1002/psp4.12155"
            title="Publication on CPT: Pharmacometrics &amp; Systems Pharmacology (Open Access)">
        CPT: Pharmacometrics &amp; Systems Pharmacology</a>]
    [<a href="javascript:toggleDisplayById('bib_ALVillas2017');"
        title="Display/hide the BibTeX entry for this publication">
        <img src="//www.ebi.ac.uk/biomodels-static/icons/plus.gif" alt="+ icon"
             style="padding-right: 5px;" />BibTeX entry</a>]
<pre id="bib_ALVillas2017" class="hidden">
@ARTICLE{BioModels2017a,
    author  = {A Lloret‐Villas and TM Varusai and N Juty and C Laibe and N Le Novère and H Hermjakob and V Chelliah},
    title   = {The Impact of Mathematical Modeling in Understanding the Mechanisms Underlying
               Neurodegeneration: Evolving Dimensions and Future Directions},
    journal = {CPT: Pharmacometrics &amp; Systems Pharmacology},
    volume  = {6},
    number  = {2},
    pages   = {73-86},
    year    = {2017},
    doi     = {10.1002/psp4.12155},
    URL     = {https://ascpt.onlinelibrary.wiley.com/doi/abs/10.1002/psp4.12155}
}
</pre>
    </dd>
</dl>

<dl>
    <dt>
        Mihai Glont, Tung V. N. Nguyen, Martin Graesslin, Robert Hälke, Raza Ali,
        Jochen Schramm, Sarala M. Wimalaratne, Varun B. Kothamachu , Nicolas Rodriguez,
        Maciej J. Swat, Jurgen Eils, Roland Eils, Camille Laibe, Rahuman S. Malik-Sheriff,
        Vijayalakshmi Chelliah, Nicolas Le Novère and Henning Hermjakob
    </dt>
    <dd style="margin-left:0;">
        <div class="pubtitle">BioModels: expanding horizons to include more modelling approaches and formats.</div>
        <div class="pubjournal"><em>Nucl. Acids Res.</em> 2017</div>
        [<a href="//academic.oup.com/nar/advance-article/doi/10.1093/nar/gkx1023/4584626"
            title="Publication on Nucl. Acids Res. (Open Access)">Nucl. Acids Res.</a>]
    [<a href="javascript:toggleDisplayById('bib_Glont2017');"
        title="Display/hide the BibTeX entry for this publication">
        <img src="//www.ebi.ac.uk/biomodel-static/icons/plus.gif" alt="+ icon" style="padding-right: 5px;" />BibTeX entry</a>]
<pre id="bib_Glont2017" class="hidden">
@ARTICLE{BioModels2017b,
    author  = {Glont, Mihai and Nguyen, Tung V. N. and Graesslin, Martin and Hälke, Robert
            and Ali, Raza and Schramm, Jochen and Wimalaratne, Sarala M. and Kothamachu, Varun B.
            and Rodriguez, Nicolas and Swat, Maciej J. and Eils, Jurgen and Eils, Roland
            and Laibe, Camille and Malik-Sheriff, Rahuman S. and Chelliah, Vijayalakshmi
            and Le Novère, Nicolas and Hermjakob, Henning},
    title   = {BioModels: expanding horizons to include more modelling approaches and formats},
    journal = {Nucleic Acids Research},
    volume  = {},
    number  = {},
    pages   = {D1248–D1253},
    year    = {2017},
    doi     = {10.1093/nar/gkx1023},
    URL     = {https://dx.doi.org/10.1093/nar/gkx1023},
}
</pre>
    </dd>
</dl>

<dl>
    <dt>
        Vijayalakshmi Chelliah, Nick Juty, Ishan Ajmera, Raza Ali, Marine Dumousseau,
        Mihai Glont, Michael Hucka, Gaël Jalowicki, Sarah Keating, Vincent Knight-Schrijver,
        Audald Lloret-Villas, Kedar Nath Natarajan, Jean-Baptiste Pettit, Nicolas Rodriguez,
        Michael Schubert, Sarala M. Wimalaratne, Yangyang Zhao, Henning Hermjakob,
        Nicolas Le Novère and Camille Laibe
    </dt>
    <dd style="margin-left:0;">
        <div class="pubtitle">BioModels: ten-year anniversary.</div>
        <div class="pubjournal"><em>Nucl. Acids Res.</em> 2015</div>
        [<a href="//nar.oxfordjournals.org/content/early/2014/11/20/nar.gku1181"
            title="Publication on Nucl. Acids Res. (Open Access)">Nucl. Acids Res.</a>]
        [<a href="javascript:toggleDisplayById('bib_Chelliah2015');"
            title="Display/hide the BibTeX entry for this publication">
        <img src="//www.ebi.ac.uk/biomodel-static/icons/plus.gif" alt="+ icon" style="padding-right: 5px;" />BibTeX entry</a>]
<pre id="bib_Chelliah2015" class="hidden">
@ARTICLE{BioModels2015a,
  author  = {Chelliah, Vijayalakshmi and Juty, Nick and Ajmera, Ishan and Ali, Raza and Dumousseau, Marine and Glont, Mihai
             and Hucka, Michael and Jalowicki, Ga{\"e}l and Keating, Sarah and Knight-Schrijver, Vincent and Lloret-Villas, Audald
             and Nath Natarajan, Kedar and Pettit, Jean-Baptiste and Rodriguez, Nicolas and Schubert, Michael
             and Wimalaratne, Sarala M. and Zhao, Yangyang and Hermjakob, Henning and Le Nov{\`e}re, Nicolas and Laibe, Camille},
  title   = {{BioModels: ten-year anniversary.}},
  journal = {Nucl. Acids Res.},
  year    = {2015},
  doi     = {10.1093/nar/gku1181}}
</pre>
    </dd>
</dl>

<dl>
  <dt>
      Nick Juty, Raza Ali, Mihai Glont, Sarah Keating, Nicolas Rodriguez, Maciej J. Swat,
      Sarala M. Wimalaratne, Henning Hermjakob, Nicolas Le Novère, Camille Laibe and
      Vijayalakshmi Chelliah
  </dt>
  <dd style="margin-left:0;">
      <div class="pubtitle">BioModels: Content, Features, Functionality and Use.</div>
      <div class="pubjournal"><em>CPT: Pharmacometrics &amp; Systems Pharmacology</em> 2015</div>
      [<a href="//onlinelibrary.wiley.com/doi/10.1002/psp4.3/full"
          title="Publication on CPT:PSP (Open Access)">CPT: Pharmacometrics &amp; Systems Pharmacology</a>]
      [<a href="javascript:toggleDisplayById('bib_Juty2015');"
          title="Display/hide the BibTeX entry for this publication">
      <img src="//www.ebi.ac.uk/biomodel-static/icons/plus.gif"
           alt="+ icon" style="padding-right: 5px;" />BibTeX entry</a>]
<pre id="bib_Juty2015" class="hidden">
@ARTICLE{BioModels2015b,
  author  = {Juty, Nick and Ali, Raza and Glont, Mihai and Keating, Sarah and Rodriguez, Nicolas and Swat, Maciej J.
             and Wimalaratne, Sarala M. and  Hermjakob, Henning and Le Nov{\`e}re, Nicolas and Laibe, Camille and Chelliah, Vijayalakshmi},
  title   = {{BioModels: Content, Features, Functionality and Use.}},
  journal = {CPT: Pharmacometrics &amp; Systems Pharmacology},
  year    = {2015},
  doi     = {10.1002/psp4.3}
}
</pre>
  </dd>
</dl>

<dl>
  <dt>
      Chen Li, Marco Donizelli, Nicolas Rodriguez, Harish Dharuri, Lukas Endler,
      Vijayalakshmi Chelliah, Lu Li, Enuo He, Arnaud Henry, Melanie I. Stefan,
      Jacky L. Snoep, Michael Hucka, Nicolas Le Novère and Camille Laibe
  </dt>
  <dd style="margin-left:0;">
      <div class="pubtitle">BioModels Database: An enhanced, curated and annotated resource for published quantitative kinetic models.</div>
      <div class="pubjournal"><em>BMC Systems Biology</em> 2010, 4:92</div>
      [<a href="//europepmc.org/abstract/MED/20587024"
          title="Publication on Europe PMC">Europe PMC</a>]
      [<a href="//www.biomedcentral.com/1752-0509/4/92"
          title="Publication on BMC Systems Biology (Open Access)">BMC Sys Bio</a>]
      [<a href="javascript:toggleDisplayById('bib_Li2010b');"
          title="Display/hide the BibTeX entry for this publication">
          <img src="//www.ebi.ac.uk/biomodel-static/icons/plus.gif"
               alt="+ icon" style="padding-right: 5px;" />BibTeX entry</a>]
<pre id="bib_Li2010b" class="hidden">
@ARTICLE{BioModels2010,
  author  = {Li, Chen and Donizelli, Marco and Rodriguez, Nicolas and Dharuri, Harish and Endler, Lukas and Chelliah, Vijayalakshmi
             and Li, Lu and He, Enuo and Henry, Arnaud and Stefan, Melanie I. and Snoep, Jacky L. and Hucka, Michael
             and Le Nov{\`e}re, Nicolas and Laibe, Camille},
  title   = {{BioModels Database: An enhanced, curated and annotated resource for published quantitative kinetic models.}},
  journal = {BMC Systems Biology},
  year    = {2010},
  month   = {Jun},
  volume  = {4},
  pages   = {92},
  pmid    = {20587024}
}
</pre>
  </dd>
</dl>


<dl>
  <dt>
      Nicolas Le Novère, Benjamin Bornstein, Alexander Broicher, Mélanie Courtot,
      Marco Donizelli, Harish Dharuri, Lu Li, Herbert Sauro, Maria Schilstra,
      Bruce Shapiro, Jacky L. Snoep and Michael Hucka
  </dt>
  <dd style="margin-left:0;">
      <div class="pubtitle">BioModels Database: A Free, Centralized Database of Curated, Published, Quantitative Kinetic Models of Biochemical and Cellular Systems.</div>
      <div class="pubjournal"><em>Nucleic Acids Research</em> 2006, 34(Database issue):D689-91</div>
      [<a href="//europepmc.org/abstract/MED/16381960"
          title="Publication on Europe PMC">Europe PMC</a>]
      [<a href="//nar.oxfordjournals.org/cgi/content/full/34/suppl_1/D689"
          title="Publication on Nucleic Acids Research">Nucleic Acids Res</a>]
      [<a href="javascript:toggleDisplayById('bib_lenovere2006');"
          title="Display/hide the BibTeX entry for this publication">
          <img src="//www.ebi.ac.uk/biomodel-static/icons/plus.gif"
               alt="+ icon" style="padding-right: 5px;" />BibTeX entry</a>]
<pre id="bib_lenovere2006" class="hidden">
@ARTICLE{BioModels2006,
  author  = {Le Nov{\`e}re, Nicolas and Bornstein, Benjamin and Broicher, Alexander and Courtot, M{\'e}lanie
            and Donizelli, Marco and Dharuri, Harish and Li, Lu and Sauro, Herbert and Schilstra, Maria
            and Shapiro, Bruce and Snoep, Jacky L. and Hucka, Michael},
  title   = {{BioModels Database}: a free, centralized database of curated, published, quantitative kinetic models of biochemical and cellular systems},
  journal = {Nucleic Acids Research},
  year    = {2006},
  month   = {Jan},
  volume  = {34},
  pages   = {D689--D691},
  number  = {Database issue},
  pmid    = {16381960}
}
</pre>
  </dd>
</dl>


<h3 id="path2models">Path2Models</h3>

<dl>
  <dt>
      Finja Büchel, Nicolas Rodriguez, Neil Swainston, Clemens Wrzodek, Tobias Czauderna,
      Roland Keller, Florian Mittag, Michael Schubert, Mihai Glont, Martin Golebiewski,
      Martijn van Iersel, Sarah Keating, Matthias Rall, Michael Wybrow, Henning Hermjakob,
      Michael Hucka, Douglas B Kell, Wolfgang Müller, Pedro Mendes, Andreas Zell,
      Claudine Chaouiya, Julio Saez-Rodriguez, Falk Schreiber, Laibe, Camille,
      Andreas Dräger and Nicolas Le Novère
  </dt>
  <dd style="margin-left:0;">
      <div class="pubtitle">Path2Models: large-scale generation of computational models from biochemical pathway maps.</div>
      <div class="pubjournal"><em>BMC Systems Biology</em> 2013, 7:116</div>
      [<a href="//europepmc.org/abstract/MED/24180668"
          title="Publication on Europe PMC">Europe PMC</a>]
      [<a href="//www.biomedcentral.com/1752-0509/7/116"
          title="Publication on BMC Systems Biology (Open Access)">BMC Sys Bio</a>]
      [<a href="javascript:toggleDisplayById('bib_Buchel2013');"
          title="Display/hide the BibTeX entry for this publication">
          <img src="//www.ebi.ac.uk/biomodel-static/icons/plus.gif"
               alt="+ icon" style="padding-right: 5px;" />BibTeX entry</a>]
<pre id="bib_Buchel2013" class="hidden">
@ARTICLE{BioModels2010,
  author  = {B{\"u}chel, Finja and Rodriguez, Nicolas and Swainston, Neil and Wrzodek, Clemens and Czauderna, Tobias and Keller, Roland
            and Mittag, Florian and Schubert, Michael and Glont, Mihai and Golebiewski, Martin and van Iersel, Martijn and Keating, Sarah
            and Rall, Matthias and Wybrow, Michael and Hermjakob, Henning and Hucka, Michael and Kell, Douglas B and M{\"u}ller, Wolfgang
            and Mendes, Pedro and Zell, Andreas and Chaouiya, Claudine and Saez-Rodriguez, Julio and Schreiber, Falk and Laibe, Camille
            and Dr{\"a}ger, Andreas and Le Nov{\`e}re, Nicolas},
  title   = {{Path2Models: large-scale generation of computational models from biochemical pathway maps.}},
  journal = {BMC Systems Biology},
  year    = {2013},
  month   = {Nov},
  volume  = {7},
  pages   = {116},
  pmid    = {24180668}
}
</pre>
  </dd>
</dl>

<h3 id="associated_services">Associated services</h3>
<dl>
  <dt>
      Sarala M Wimalaratne, Pierre Grenon, Henning Hermjakob, Nicolas Le Novère and Camille Laibe
  </dt>
  <dd style="margin-left:0;">
      <div class="pubtitle">BioModels linked dataset.</div>
      <div class="pubjournal"><em>BMC Systems Biology</em> 2014, 8(1):91</div>
      [<a href="//europepmc.org/abstract/MED/25182954"
          title="Publication on Europe PMC">Europe PMC</a>]
      [<a href="//www.biomedcentral.com/1752-0509/8/91"
          title="Publication on BMC Systems Biology (Open Access)">BMC Sys Bio</a>]
      [<a href="javascript:toggleDisplayById('bib_Wimalaratne2014');"
          title="Display/hide the BibTeX entry for this publication">
          <img src="//www.ebi.ac.uk/biomodel-static/icons/plus.gif"
               alt="+ icon" style="padding-right: 5px;" />BibTeX entry</a>]
<pre id="bib_Wimalaratne2014" class="hidden">
@ARTICLE{Wimalaratne2014,
	Title   = {BioModels linked dataset},
	Author  = {Wimalaratne, Sarala M and Grenon, Pierre and Hermjakob, Henning and Le Nov{\`e}re, Nicolas and Laibe, Camille},
	DOI     = {10.1186/s12918-014-0091-5},
        pmid    = {25182954},
	Number  = {1},
	Volume  = {8},
	Month   = {August},
	Year    = {2014},
	Journal = {BMC systems biology},
	ISSN    = {1752-0509},
	Pages   = {91}
}
</pre>
  </dd>
</dl>

<dl>
  <dt>
      Chen Li, Mélanie Courtot, Nicolas Le Novère and Camille Laibe
  </dt>
  <dd style="margin-left:0;">
      <div class="pubtitle">BioModels.net Web Services, a free and integrated toolkit for computational modelling software.</div>
      <div class="pubjournal"><em>Briefings in Bioinformatics</em> 2010, 11:270-277</div>
      [<a href="//europepmc.org/abstract/MED/19939940"
          title="Publication on Europe PMC">Europe PMC</a>]
      [<a href="//bib.oxfordjournals.org/content/11/3/270.abstract"
          title="Publication on Briefings in Bioinformatics (Open Access)">Brief Bioinform</a>]
      [<a href="javascript:toggleDisplayById('bib_Li2010a');"
          title="Display/hide the BibTeX entry for this publication">
          <img src="//www.ebi.ac.uk/biomodel-static/icons/plus.gif"
               alt="+ icon" style="padding-right: 5px;" />BibTeX entry</a>]
<pre id="bib_Li2010a" class="hidden">
@ARTICLE{BioModelsWebServices2010,
  author  = {Li, Chen and Courtot, M{\'e}lanie and Le Nov{\`e}re, Nicolas and Laibe, Camille},
  title   = {BioModels.net Web Services, a free and integrated toolkit for computational modelling software.},
  journal = {Brief Bioinform},
  year    = {2010},
  volume  = {11},
  pages   = {270--277},
  number  = {3},
  month   = {May},
  pmid    = {19939940}
}
</pre>
  </dd>
</dl>

<h3 id="others">Other publications</h3>
<dl>
  <dt>
      Vijayalakshmi Chelliah, Camille Laibe, Nicolas Le Novère
  </dt>
  <dd style="margin-left:0;">
      <div class="pubtitle">BioModels Database: A Repository of Mathematical Models of Biological Processes.</div>
      <div class="pubjournal"><em>Methods in Molecular Biology</em> 2013, 1021:189-199</div>
      [<a href="//europepmc.org/abstract/MED/23715986"
          title="Publication on Europe PMC">Europe PMC</a>]
      [<a href="//link.springer.com/protocol/10.1007%2F978-1-62703-450-0_10"
          title="Publication on Springer (Open Access)">Springer</a>]
      [<a href="javascript:toggleDisplayById('bib_Chelliah2013');"
          title="Display/hide the BibTeX entry for this publication">
          <img src="//www.ebi.ac.uk/biomodel-static/icons/plus.gif"
               alt="+ icon" style="padding-right: 5px;" />BibTeX entry</a>]

<pre id="bib_Chelliah2013" class="hidden">
@ARTICLE{BioModels2013,
  author  = {Chelliah, Vijayalakshmi and Laibe, Camille and Le Nov{\`e}re, Nicolas},
  title   = {BioModels Database: A Repository of Mathematical Models of Biological Processes.},
  journal = {Methods Mol Biol},
  year    = {2013},
  volume  = {1021},
  pages   = {189--199},
  pmid    = {23715986},
}
</pre>
  </dd>
</dl>

</body>
<content tag="citation">
    selected
</content>
<content tag="title">
    <g:message code="${titleCode}" default="How do I cite BioModels Database?" />
</content>
