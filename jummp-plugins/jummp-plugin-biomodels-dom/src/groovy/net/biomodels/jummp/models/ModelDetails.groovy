package net.biomodels.jummp.models

import net.biomodels.jummp.model.Model

class ModelDetails implements Serializable {
    Model model
    String name
    Date updateDate

    ModelDetails(Model model, String name, Date updateDate) {
        this.model = model
        this.name = name
        this.updateDate = updateDate
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        ModelDetails that = (ModelDetails) o

        if (model != that.model) return false
        if (name != that.name) return false
        if (updateDate != that.updateDate) return false

        return true
    }

    int hashCode() {
        int result
        result = (model != null ? model.hashCode() : 0)
        result = 31 * result + (name != null ? name.hashCode() : 0)
        result = 31 * result + (updateDate != null ? updateDate.hashCode() : 0)
        return result
    }
}