package net.biomodels.jummp.plugins.format

class JavaFormatService extends AbstractFormatDetectionService {
    @Override
    boolean areFilesThisFormat(List<File> files) {
        areTheseFilesInThisFormat(TARGET_MIME_TYPES["java"] as String, files)
    }
}
