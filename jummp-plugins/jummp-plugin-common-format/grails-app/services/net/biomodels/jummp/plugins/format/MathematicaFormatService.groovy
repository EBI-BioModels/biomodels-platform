package net.biomodels.jummp.plugins.format

class MathematicaFormatService extends AbstractFormatDetectionService {
    @Override
    boolean areFilesThisFormat(List<File> files) {
        areTheseFilesInThisFormat(TARGET_MIME_TYPES["mathematica"] as String, files)
    }
}
