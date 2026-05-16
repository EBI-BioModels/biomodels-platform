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

    /**
     * Finds the difference between two Date objects in different time unit
     * @param beginDate A Date object indicating the beginning date
     * @param endDate A Date object indicating the ending date
     * @param unit  A String constant of ["MS", "S", "M", "H", "D"] which are short for
     * ["MilliSecond", "Second", "Minute", "Hour", "Day"]
     * @return A double value telling the difference in the selected unit time.
     */
    static double diffTwoDates(final Date beginDate, final Date endDate, final String unit = "H") {
        long diffInMills = endDate.getTime() - beginDate.getTime()
        double retVal
        switch (unit) {
            case "MS":
                retVal = diffInMills
                break
            case "S":
                retVal = diffInMills / 1000
                break
            case "M":
                retVal = diffInMills / (1000*60)
                break
            case "H":
                retVal  = diffInMills / (1000*60*60)
                break
            case "D":
                retVal = diffInMills / (1000*60*60*24)
                break
            default:
                retVal = diffInMills / (1000*60*60)
                break
        }
        retVal
    }

}