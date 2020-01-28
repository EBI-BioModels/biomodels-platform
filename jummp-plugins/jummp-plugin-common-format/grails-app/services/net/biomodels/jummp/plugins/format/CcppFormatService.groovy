package net.biomodels.jummp.plugins.format

class CcppFormatService extends AbstractFormatDetectionService {
    @Override
    boolean areFilesThisFormat(List<File> files) {
        areTheseFilesInThisFormat(TARGET_MIME_TYPES["c_cpp"] as String, files)
    }
}
