package net.biomodels.jummp.utils

import java.util.concurrent.ThreadLocalRandom

class MathUtils {
    static final int rand(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}