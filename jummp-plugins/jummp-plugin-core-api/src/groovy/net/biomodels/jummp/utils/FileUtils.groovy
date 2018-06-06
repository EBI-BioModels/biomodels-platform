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
    static <T> T loadObjectFromFile(File file, Class<T> tClass) {
        T object = new FileInputStream(file).withObjectInputStream(tClass.classLoader) {
            is -> is.readObject()
        } as T
        return object
    }
}