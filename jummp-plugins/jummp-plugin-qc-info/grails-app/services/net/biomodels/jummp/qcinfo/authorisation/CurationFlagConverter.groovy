package net.biomodels.jummp.qcinfo.authorisation

import net.biomodels.jummp.qcinfo.FlagLevel

/**
 * Created by tnguyen on 25/07/16.
 */
class CurationFlagConverter {
    String encode(FlagLevel flag) {
        return flag.toString()
    }

    FlagLevel decode(String strFlag) {

    }
}
