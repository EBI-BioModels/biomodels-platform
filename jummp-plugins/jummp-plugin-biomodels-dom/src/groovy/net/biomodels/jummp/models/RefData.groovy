package net.biomodels.jummp.models

/**
 * Because Java language don't allow to pass the reference of primitive data type through the parameter
 * This will wrap your primitive data and by this, the value inside will change after call function
 * @param <T>
 */
class RefData<T> {
    T data;

    RefData(T data) {
        this.data = data
    }

    T getData() {
        return data
    }

    void setData(T data) {
        this.data = data
    }
}