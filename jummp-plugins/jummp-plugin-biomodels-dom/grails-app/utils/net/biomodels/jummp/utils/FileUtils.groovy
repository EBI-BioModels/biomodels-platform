package net.biomodels.jummp.utils

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
    static Serializable loadObjectFromFile(File file) {
        return new FileInputStream(file).withObjectInputStream(getClass().classLoader) {
            is -> is.readObject()
        } as Serializable
    }
}
