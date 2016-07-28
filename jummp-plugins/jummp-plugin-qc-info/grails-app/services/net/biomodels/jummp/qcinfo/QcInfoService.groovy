package net.biomodels.jummp.qcinfo

import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.Revision
import org.perf4j.aop.Profiled
import org.springframework.security.access.AccessDeniedException

class QcInfoService {
    private final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()
    /**
    * Dependency Injection of Spring Security Service
    */
    def springSecurityService
    /**
     * Dependency Injection of AclUtilService
     */
    def aclUtilService

    def certificationAuthorisationService

    public QcInfo createQcInfo(FlagLevel flagLevel, String comment) {
        QcInfo qcInfo = new QcInfo()
        qcInfo.flag = flagLevel
        qcInfo.comment = comment
        return qcInfo
    }

    @Profiled(tag="qcInfoService.addQcInfo")
    public boolean addQcInfo(Revision revision, QcInfo qcInfo) {
        if (!certificationAuthorisationService.isAllowed()) {
            throw new AccessDeniedException("You cannot certify this model.")
        }
        if (!revision) {
            throw new IllegalArgumentException("Cannot add QC information about a null revision")
        }
        if (revision.deleted) {
            throw new IllegalArgumentException("Revision may not be deleted")
        }

        Revision.withTransaction {status ->
            try {
                revision.qcInfo = qcInfo
                qcInfo.save()
                revision.save()
                return true
            } catch (Exception ex) {
                ex.printStackTrace()
                try {
                    status.setRollbackOnly()
                } catch (Exception ex2) {
                    ex2.printStackTrace()
                }
                return false
            }
        }
    }

    @Profiled(tag="qcInfoService.canCertify")
    public boolean canCertify(Model model) {
        if (!model) {
            throw new IllegalArgumentException("Model may not be null")
        }
        if (model.deleted) {
            return false
        }
        certificationAuthorisationService.isAllowed()
    }

    @Profiled(tag="qcInfoService.getQcInfo")
    public QcInfo getQcInfo(Revision revision) {
        return revision.qcInfo
    }
}
