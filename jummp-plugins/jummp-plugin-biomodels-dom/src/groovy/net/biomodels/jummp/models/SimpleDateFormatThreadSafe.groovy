package net.biomodels.jummp.models

import java.text.AttributedCharacterIterator
import java.text.DateFormatSymbols
import java.text.FieldPosition
import java.text.NumberFormat
import java.text.ParseException
import java.text.ParsePosition
import java.text.SimpleDateFormat

class SimpleDateFormatThreadSafe extends SimpleDateFormat {

    private static final long serialVersionUID = 5448371898056188202L
    ThreadLocal<SimpleDateFormat> localSimpleDateFormat

    SimpleDateFormatThreadSafe() {
        super()
        localSimpleDateFormat = new ThreadLocal<SimpleDateFormat>() {
            protected SimpleDateFormat initialValue() {
                return new SimpleDateFormat()
            }
        }
    }

    SimpleDateFormatThreadSafe(final String pattern) {
        super(pattern)
        localSimpleDateFormat = new ThreadLocal<SimpleDateFormat>() {
            protected SimpleDateFormat initialValue() {
                return new SimpleDateFormat(pattern)
            }
        }
    }

    SimpleDateFormatThreadSafe(final String pattern, final DateFormatSymbols formatSymbols) {
        super(pattern, formatSymbols)
        localSimpleDateFormat = new ThreadLocal<SimpleDateFormat>() {
            protected SimpleDateFormat initialValue() {
                return new SimpleDateFormat(pattern, formatSymbols)
            }
        }
    }

    SimpleDateFormatThreadSafe(final String pattern, final Locale locale) {
        super(pattern, locale)
        localSimpleDateFormat = new ThreadLocal<SimpleDateFormat>() {
            protected SimpleDateFormat initialValue() {
                return new SimpleDateFormat(pattern, locale)
            }
        }
    }

    Object parseObject(String source) throws ParseException {
        return localSimpleDateFormat.get().parseObject(source)
    }

    String toString() {
        return localSimpleDateFormat.get().toString()
    }

    Date parse(String source) throws ParseException {
        return localSimpleDateFormat.get().parse(source)
    }

    Object parseObject(String source, ParsePosition pos) {
        return localSimpleDateFormat.get().parseObject(source, pos)
    }

    void setCalendar(Calendar newCalendar) {
        localSimpleDateFormat.get().setCalendar(newCalendar)
    }

    Calendar getCalendar() {
        return localSimpleDateFormat.get().getCalendar()
    }

    void setNumberFormat(NumberFormat newNumberFormat) {
        localSimpleDateFormat.get().setNumberFormat(newNumberFormat)
    }

    NumberFormat getNumberFormat() {
        return localSimpleDateFormat.get().getNumberFormat()
    }

    void setTimeZone(TimeZone zone) {
        localSimpleDateFormat.get().setTimeZone(zone)
    }

    TimeZone getTimeZone() {
        return localSimpleDateFormat.get().getTimeZone()
    }

    void setLenient(boolean lenient) {
        localSimpleDateFormat.get().setLenient(lenient)
    }

    boolean isLenient() {
        return localSimpleDateFormat.get().isLenient()
    }

    void set2DigitYearStart(Date startDate) {
        localSimpleDateFormat.get().set2DigitYearStart(startDate)
    }

    Date get2DigitYearStart() {
        return localSimpleDateFormat.get().get2DigitYearStart()
    }

    StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition pos) {
        return localSimpleDateFormat.get().format(date, toAppendTo, pos)
    }

    AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        return localSimpleDateFormat.get().formatToCharacterIterator(obj)
    }

    Date parse(String text, ParsePosition pos) {
        return localSimpleDateFormat.get().parse(text, pos)
    }

    String toPattern() {
        return localSimpleDateFormat.get().toPattern()
    }

    String toLocalizedPattern() {
        return localSimpleDateFormat.get().toLocalizedPattern()
    }

    void applyPattern(String pattern) {
        localSimpleDateFormat.get().applyPattern(pattern)
    }

    void applyLocalizedPattern(String pattern) {
        localSimpleDateFormat.get().applyLocalizedPattern(pattern)
    }

    DateFormatSymbols getDateFormatSymbols() {
        return localSimpleDateFormat.get().getDateFormatSymbols()
    }

    void setDateFormatSymbols(DateFormatSymbols newFormatSymbols) {
        localSimpleDateFormat.get().setDateFormatSymbols(newFormatSymbols)
    }

    Object clone() {
        return localSimpleDateFormat.get().clone()
    }

    int hashCode() {
        return localSimpleDateFormat.get().hashCode()
    }

    boolean equals(Object obj) {
        return localSimpleDateFormat.get().equals(obj)
    }

}
