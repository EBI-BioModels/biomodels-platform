<%--
  Shared HTML shell for transactional BioModels emails.

  Renders the orange accent bar + deep-navy UF/LSM header + white body + grey footer
  used by every HTML email the application sends. Callers supply only the inner body.

  Model:
    bodyContent   - raw HTML for the white body panel (required)
    rootUrl       - absolute site URL used in footer links (required)
    footerNote    - raw HTML for the contextual first line of the footer
                    (optional; defaults to the generic "automatically generated" note)
    showMaintainer - append the "BioModels is maintained by ... University of Florida Health"
                     block after the footer note (optional; default false)
    year          - copyright year for the maintainer block (optional; defaults to current year)

  Brand colours are defined here and nowhere else:
    #ED6B21 accent  |  #072C55 header  |  #D3DEEB header sub-heading  |  #0F5CB1 links
--%>
<g:set var="siteUrl" value="${rootUrl ?: 'https://www.biomodels.org'}"/>
<g:set var="copyrightYear" value="${year ?: new Date().format('yyyy')}"/>
<div style="background-color:#f4f4f4;margin:0;padding:32px 0;font-family:Arial,Helvetica,sans-serif;color:#333333;">
  <div style="max-width:620px;margin:0 auto;background-color:#ffffff;border-radius:4px;overflow:hidden;box-shadow:0 2px 6px rgba(0,0,0,0.10);">
    <div style="background-color:#ED6B21;height:5px;"></div>
    <div style="background-color:#072C55;padding:24px 32px 20px;">
      <div style="font-size:22px;font-weight:bold;color:#ffffff;letter-spacing:0.5px;">BioModels</div>
      <div style="font-size:12px;color:#D3DEEB;margin-top:4px;letter-spacing:0.3px;">Laboratory for Systems Medicine &bull; University of Florida</div>
    </div>
    <div style="padding:32px;font-size:15px;line-height:1.7;color:#333333;">
      ${raw(bodyContent ?: '')}
    </div>
    <hr style="border:none;border-top:1px solid #e8e8e8;margin:0;"/>
    <div style="background-color:#f8f8f8;padding:20px 32px;font-size:12px;color:#777777;line-height:1.6;">
      <g:if test="${footerNote}">${raw(footerNote)}</g:if>
      <g:else>This is an automatically generated email from <a href="${siteUrl}" style="color:#0F5CB1;">BioModels</a> &mdash; replies are not monitored.</g:else>
      <g:if test="${showMaintainer}"><br/><br/>
      BioModels is maintained by the Laboratory for Systems Medicine,
      Department of Medicine, Division of Pulmonary &ndash; Systems Medicine,
      <a href="https://systemsmedicine.pulmonary.medicine.ufl.edu/biomodels/" style="color:#0F5CB1;">University of Florida</a>.<br/>
      &copy; ${copyrightYear} University of Florida Health</g:if>
    </div>
  </div>
</div>
