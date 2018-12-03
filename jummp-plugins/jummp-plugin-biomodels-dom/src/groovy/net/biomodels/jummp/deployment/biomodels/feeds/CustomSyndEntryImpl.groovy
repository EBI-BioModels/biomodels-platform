package net.biomodels.jummp.deployment.biomodels.feeds

import com.rometools.rome.feed.synd.SyndEntryImpl

class CustomSyndEntryImpl extends SyndEntryImpl {
    private static final long serialVersionUID = 1L

    protected Date publishedDate

    @Override
    Date getPublishedDate() {
        return this.publishedDate
    }

    @Override
    void setPublishedDate(final Date publishedDate) {
        this.publishedDate = new Date(publishedDate.getTime())
    }
}
