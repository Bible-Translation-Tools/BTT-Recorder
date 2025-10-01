package org.wycliffeassociates.translationrecorder.persistance

interface IPreferenceRepository {

    /**
     * Returns the value of a user preference
     * @param key
     * @return String
     */
    fun <T>getDefaultPref(key: String, type: Class<T>): T?

    /**
     * Returns the value of a user preference or the default value
     */
    fun <T>getDefaultPref(key: String, defaultValue: T, type: Class<T>): T

    /**
     * Sets the value of a default preference.
     * @param key
     * @param value if null the string will be removed
     */
    fun <T>setDefaultPref(key: String, value: T?, type: Class<T>)
}

inline fun <reified T> IPreferenceRepository.getDefaultPref(key: String) =
    getDefaultPref(key, T::class.java)

inline fun <reified T> IPreferenceRepository.getDefaultPref(key: String, defaultValue: T) =
    getDefaultPref(key, defaultValue, T::class.java)

inline fun <reified T> IPreferenceRepository.setDefaultPref(key: String, value: T?) =
    setDefaultPref(key, value, T::class.java)