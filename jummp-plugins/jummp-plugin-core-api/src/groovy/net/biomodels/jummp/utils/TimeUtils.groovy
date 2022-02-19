package net.biomodels.jummp.utils

class TimeUtils {

    private static final ONE_SECOND = 1000

    public static final ONE_YEAR = 365 * 24 * 60 * 60

    public static final TWO_YEAR = 2 * ONE_YEAR

    static Long getCurrentTimestamp() {
        return new Date().getTime() / ONE_SECOND
    }

    static Long getTimestamp(Date date) {
        return date.getTime() / ONE_SECOND
    }

}