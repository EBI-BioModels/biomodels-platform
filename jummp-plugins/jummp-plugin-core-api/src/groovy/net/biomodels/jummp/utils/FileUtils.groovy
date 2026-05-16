package net.biomodels.jummp.utils

import java.security.MessageDigest

class FileUtils {

    /**
     * Write object to file for future recover
     * @param name
     * @param object
     */
    static void writeObjectToFile(File file, Serializable object) {
        FileOutputStream cacheFile = new FileOutputStream(file)
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(cacheFile)
        objectOutputStream.writeObject(object)
        objectOutputStream.close()
        cacheFile.close()
    }

    /**
     * Load object from file
     * @param file
     * @return
     */
    static <T> T loadObjectFromFile(File file, Class<T> tClass) {
        T object = new FileInputStream(file).withObjectInputStream(tClass.classLoader) {
            is -> is.readObject()
        } as T
        return object
    }

    static String checksum(File file, String algorithm) {
        MessageDigest digest = MessageDigest.getInstance(algorithm)
        file.withInputStream() { is ->
            byte[] buffer = new byte[8192]
            int read
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read)
            }
        }

        digest.digest().encodeHex().toString()
    }
}
