package net.biomodels.jummp.plugins.format

class PythonFormatService extends AbstractFormatDetectionService {
    @Override
    boolean areFilesThisFormat(List<File> files) {
        areTheseFilesInThisFormat(TARGET_MIME_TYPES["python"] as String, files)
    }
}
