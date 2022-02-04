package net.biomodels.jummp.utils

import java.util.concurrent.ThreadLocalRandom

class MathUtils {
    static final int rand(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    static final String generatePassword(String alphabet, int n) {
        new Random().with {
            (1..n).collect { alphabet[ nextInt( alphabet.length() ) ] }.join()
        }
    }
}
