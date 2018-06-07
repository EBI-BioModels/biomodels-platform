package net.biomodels.jummp.exception.lsf

class LSFJobNotExistException extends RuntimeException {
    LSFJobNotExistException(String message) {
        super(message)
    }
}
