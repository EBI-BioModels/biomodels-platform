package net.biomodels.jummp.utils

class DisplayFormat {
    static String format(double bytes, int digits) {
        String[] dictionary = ["bytes", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"]
        int index = 0
        for (index = 0; index < dictionary.length; index++) {
            if (bytes < 1024) {
                break
            }
            bytes = bytes / 1024
        }
        return String.format("%." + digits + "f", bytes) + " " + dictionary[index]
    }
}
