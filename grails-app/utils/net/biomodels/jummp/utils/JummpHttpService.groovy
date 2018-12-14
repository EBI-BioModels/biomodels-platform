package net.biomodels.jummp.utils

class JummpHttpService {
    static String getStatus(String url) throws IOException {
        String result = ""
        int code = getStatusCode(url)
        try {
            URL siteURL = new URL(url)
            HttpURLConnection connection = (HttpURLConnection) siteURL.openConnection()
            connection.setRequestMethod("GET")
            connection.setConnectTimeout(3000)
            connection.connect()

            code = connection.getResponseCode()
            if (code == 200) {
                result = "-> Green <-\t" + "Code: " + code

            } else {
                result = "-> Yellow <-\t" + "Code: " + code
            }
        } catch (Exception e) {
            result = "-> Red <-\t" + "Wrong domain - Exception: " + e.getMessage()

        }
        System.out.println(url + "\t\tStatus:" + result)
        result
    }

    static int getStatusCode(String url) throws IOException {
        int code = 200
        try {
            URL siteURL = new URL(url)
            HttpURLConnection connection = (HttpURLConnection) siteURL.openConnection()
            connection.setRequestMethod("GET")
            connection.setConnectTimeout(3000)
            connection.connect()
            code = connection.getResponseCode()
        } catch (Exception e) {
            code = 404
        }
        return code
    }
}
