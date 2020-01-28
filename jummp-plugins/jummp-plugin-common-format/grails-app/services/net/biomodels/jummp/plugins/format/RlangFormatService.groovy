package net.biomodels.jummp.plugins.format

class RlangFormatService extends AbstractFormatDetectionService {
    @Override
    boolean areFilesThisFormat(final List<File> files) {
        areTheseFilesInThisFormat(TARGET_MIME_TYPES["R"] as String, files)
    }
}
