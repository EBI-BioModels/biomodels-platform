package net.biomodels.jummp.deployment.biomodels.feeds

import com.rometools.rome.feed.synd.SyndFeedImpl

class CustomSyndFeedImpl extends SyndFeedImpl {
    private static final long serialVersionUID = 1L

    protected Date publishedDate
    protected String language
    protected String copyright

    @Override
    Date getPublishedDate() {
        return this.publishedDate
    }

    @Override
    void setPublishedDate(final Date publishedDate) {
        this.publishedDate = new Date(publishedDate.getTime())
    }

    @Override
    String getLanguage() {
        return this.language
    }

    @Override
    void setLanguage(final String language) {
        this.language = language
    }

    @Override
    String getCopyright() {
        return this.copyright
    }

    @Override
    void setCopyright(final String copyright) {
        this.copyright = copyright
    }
}
