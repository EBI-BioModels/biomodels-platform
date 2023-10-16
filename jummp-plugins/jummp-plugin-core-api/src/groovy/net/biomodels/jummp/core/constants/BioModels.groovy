package net.biomodels.jummp.core.constants

final class BioModels {
    // the minimal number of cross references displayed or hidden at a time
    static final int BM_MIN_NB_CR = 5
    static final String BM_DEV_ROOT_URL = "https://wwwdev.ebi.ac.uk/biomodels"
    static final String BM_ROOT_URL = "https://www.ebi.ac.uk/biomodels"
    static final String BM_PROD_SEARCH_URL_PREFIX = "$BM_ROOT_URL/search"
    static final String EBI_PROD_WS_REST_BM_URL = "https://www.ebi.ac.uk/ebisearch/ws/rest/biomodels"
    static final String EBI_BMPROD_PUBLIC_FTP = "https://ftp.ebi.ac.uk/pub/databases/biomodels"
    static final String EBI_BMDEV_PUBLIC_FTP = "https://ftp.ebi.ac.uk/pub/databases/biomodels/dev"

    // the maximum file size or the total size allowed to upload and download by streaming directly via web interface
    static long MAX_FILE_SIZE = 500 * 1024 * 1024
}
