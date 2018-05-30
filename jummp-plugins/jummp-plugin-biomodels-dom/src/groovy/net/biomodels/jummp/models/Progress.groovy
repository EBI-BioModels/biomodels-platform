package net.biomodels.jummp.models

class Progress implements Serializable {
    private float total
    private float current
    private Date createDate

    Progress(float total, float current) {
        this.total = total
        this.current = current
        createDate = new Date()
    }

    float getTotal() {
        return total
    }

    void setTotal(float total) {
        this.total = total
    }

    float getCurrent() {
        return current
    }

    void setCurrent(float current) {
        this.current = current
    }

    Date getCreateDate() {
        return createDate
    }

    void setCreateDate(Date createDate) {
        this.createDate = createDate
    }

    String getCreateDateStr() {
        return createDate.format("yyyy - MMM dd - HH:mm")
    }

    float getPercent() {
        return Math.round((current * 100 / total) * 100.0) / 100.0
    }

    void increaseProgress() {
        current++
    }
}
