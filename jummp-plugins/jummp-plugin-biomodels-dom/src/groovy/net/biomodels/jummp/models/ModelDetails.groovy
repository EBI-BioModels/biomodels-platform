package net.biomodels.jummp.models

import com.fasterxml.jackson.annotation.JsonIgnore
import net.biomodels.jummp.model.Model

import java.lang.reflect.Modifier

class ModelDetails implements Serializable {
    @JsonIgnore
    Model model
    String name
    Date updateDate

    ModelDetails(Model model, String name, Date updateDate) {
        this.model = model
        this.name = name
        this.updateDate = updateDate
    }

    Map<String, Object> asMap() {
        this.class.declaredFields.findAll { !it.synthetic }.collectEntries {
            [ (it.name):this."$it.name" ]
        }
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
