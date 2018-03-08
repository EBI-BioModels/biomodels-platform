package net.biomodels.jummp.utils

class TimeUtils {

    private static final ONE_SECOND = 1000;

    static Long getCurrentTimestamp() {
        return new Date().getTime() / ONE_SECOND;
    }

    static Long getTimestamp(Date date) {
        return date.getTime() / ONE_SECOND;
    }
}
