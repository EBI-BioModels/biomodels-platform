package net.biomodels.jummp.qcinfo

import grails.transaction.Transactional

@Transactional
class QcInfoService {
    private final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()

    def createQcInfo(FlagLevel flagLevel, String comment) {
        QcInfo qcInfo = new QcInfo()
        qcInfo.flag = flagLevel
        qcInfo.comment = comment
        return qcInfo
    }
}
