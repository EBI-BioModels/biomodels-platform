package net.biomodels.jummp.plugins.format

import net.biomodels.jummp.core.model.FileFormatService
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.model.ModellingApproach
import org.apache.tika.detect.DefaultDetector
import org.apache.tika.metadata.Metadata
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class AbstractFormatDetectionService implements FileFormatService {
    final protected Logger logger = LoggerFactory.getLogger(this.getClass())
    final protected static Map<String, String> TARGET_MIME_TYPES = ["java": "text/x-java-source",
                             "c_cpp": "text/x-csrc",
                             "python": "text/x-python",
                             "R": "text/x-rsrc",
                             "mathematica": "application/mathematica"]
    @Override
    boolean validate(List<File> model, List<String> errors) {
        return false
    }

    @Override
    String extractName(List<File> model) {
        return null
    }

    @Override
    String extractDescription(List<File> model) {
        return null
    }

    @Override
    boolean updateName(RevisionTransportCommand revision, String name) {
        return false
    }

    @Override
    boolean updateDescription(RevisionTransportCommand revision, String description) {
        return false
    }

    @Override
    List<String> getAllAnnotationURNs(RevisionTransportCommand revision) {
        return null
    }

    @Override
    List<String> getPubMedAnnotation(RevisionTransportCommand revision) {
        return null
    }

    @Override
    String getFormatVersion(RevisionTransportCommand revision) {
        return null
    }

    @Override
    boolean doBeforeSavingAnnotations(File annoFile, RevisionTransportCommand newRevision) {
        return false
    }

    @Override
    ModellingApproach getModellingApproach(RevisionTransportCommand revision) {
        return null
    }

    protected boolean areTheseFilesInThisFormat(final String mimeType, final List<File> files) {
        def result = files.any { File f ->
            isWellKnownFile(mimeType, f)
        }
        return result
    }

    private boolean isWellKnownFile(final String mimeType, final File f) {
        DefaultDetector mimeDetector = new DefaultDetector()
        Metadata metadata = new Metadata()
        metadata.set(Metadata.RESOURCE_NAME_KEY, f.name)
        boolean result = f.withInputStream { InputStream stream ->
            try {
                String detectedMime = mimeDetector.detect(stream, metadata)?.toString()
                logger.debug("File $f has media type $detectedMime")
                return detectedMime == mimeType
            } catch (IOException e) {
                String n = f.name
                logger.error("Could not probe $n for MIME type detection.", e)
            }
        }
        result
    }
}
