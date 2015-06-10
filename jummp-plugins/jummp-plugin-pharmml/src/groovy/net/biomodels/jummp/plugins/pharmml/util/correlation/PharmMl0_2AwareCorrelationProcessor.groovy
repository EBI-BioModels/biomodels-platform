/**
 * Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
 * Deutsches Krebsforschungszentrum (DKFZ)
 *
 * This file is part of Jummp.
 *
 * Jummp is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along
 * with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
 *
 * Additional permission under GNU Affero GPL version 3 section 7
 *
 * If you modify Jummp, or any covered work, by linking or combining it with
 * Apache Tika, Apache Commons, LibPharmml, Perf4j (or a modified version of these
 * libraries), containing parts covered by the terms of Apache License v2.0,
 * the licensors of this Program grant you additional permission to convey the
 * resulting work.
 * {Corresponding Source for a non-source form of such a combination shall
 * include the source code for the parts of Apache Tika, Apache Commons,
 * LibPharmml, Perf4j used as well as that of the covered work.}
 **/

package net.biomodels.jummp.plugins.pharmml.util.correlation

import eu.ddmore.libpharmml.dom.commontypes.ScalarRhs
import eu.ddmore.libpharmml.dom.modeldefn.Correlation
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/*
 * Class that provides convenience methods for processing Correlations for PharmML 0.2.
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
public class PharmMl0_2AwareCorrelationProcessor implements ICorrelationProcessor{
    /* the class logger */
    private static final Log log = LogFactory.getLog(this)
    private static final String IS_DEBUG_ENABLED = log.isDebugEnabled()

    public PharmMl0_2AwareCorrelationProcessor() {}

    /**
     * {@inheritDoc}
     */
    @Override
    List<CorrelationMatrix> convertToStringMatrix(List<Correlation> correlations,
                Map<String, List<String>> randomEffectsPerLevel) {

        def matricesByLevel = new HashMap<String, CorrelationMatrix>()
        correlations.each { c ->
            try {
                final String VAR = c.variabilityReference.symbRef?.symbIdRef ?:
                                c.variabilityReference.symbRef?.blkIdRef ?: "undefined"
                CorrelationMatrix cm
                ScalarRhs value
                if (matricesByLevel[VAR]) {
                    cm = matricesByLevel[VAR]
                } else {
                    String TYPE
                    value = c.covariance
                    if (!value) {
                        value = c.correlationCoefficient
                        TYPE = 'Correlation'
                    } else {
                        TYPE = 'Covariance'
                    }
                    cm = new CorrelationMatrix(TYPE, VAR)
                    matricesByLevel[VAR] = cm
                }
                final String R1 = c.randomVariable1.symbRef?.symbIdRef
                final String R2 = c.randomVariable2.symbRef?.symbIdRef
                final String KEY = "$VAR|$R1|$R2"
                final String KEY_REV = "$VAR|$R2|$R1"
                final String VALUE_STRING = value.symbRef.symbIdRef
                cm.put(KEY, VALUE_STRING)
                cm.put(KEY_REV, VALUE_STRING)
                cm.addRandomEffect(R1)
                cm.addRandomEffect(R2)
            } catch (Exception e) {
                log.error(e.message, e)
            }
        }
        matricesByLevel.entrySet().each {
            try {
                final String LVL = it.key
                final CorrelationMatrix cm = it.value
                final int MATRIX_SIZE = cm.size()
                final List EFFECTS = cm.randomEffects as List
                String[][] corrMatrix = new String[MATRIX_SIZE][MATRIX_SIZE]
                for (int i = 0; i < MATRIX_SIZE; i++) {
                    for (int j = 0; j < MATRIX_SIZE; j++) {
                        if (i == j) {
                            corrMatrix[i][j] = "1"
                        } else {
                            final String R1 = EFFECTS[i]
                            final String R2 = EFFECTS[j]
                            final String KEY = "$LVL|$R1|$R2"
                            final String KEY_REV = "$LVL|$R2|$R1"
                            final String RHO = cm.get(KEY)
                            final String RHO_REV = cm.get(KEY_REV)
                            if (RHO) {
                                corrMatrix[i][j] = RHO
                            } else if (RHO_REV) {
                                corrMatrix[i][j] = RHO_REV
                            }else {
                                corrMatrix[i][j] = "0"
                            }
                        }
                    }
                }
                cm.matrix = corrMatrix
            } catch(Exception e) {
                log.error e.message, e
                return []
            }
        }
        return matricesByLevel.values() as List
    }
}
