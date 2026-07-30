//
// clang-format off
// Build as JNI helper for HiddenBluetoothCompat fallback.

#include <jni.h>
#include <string>

extern "C" {

static jclass GetBluetoothA2dpClass(JNIEnv *env) {
    return env->FindClass("android/bluetooth/BluetoothA2dp");
}

JNIEXPORT jobject JNICALL
Java_com_soren_airpodscompanion_HiddenBluetoothCompat_00024NativeHiddenApiBridge_getCodecConfigNative(
        JNIEnv *env, jclass, jobject a2dp) {
    jclass a2dpClass = GetBluetoothA2dpClass(env);
    if (!a2dpClass || !a2dp) return nullptr;

    jmethodID getCodecConfig = env->GetMethodID(a2dpClass, "getCodecConfig", "()Landroid/bluetooth/BluetoothCodecConfig;");
    if (!getCodecConfig) return nullptr;
    return env->CallObjectMethod(a2dp, getCodecConfig);
}

JNIEXPORT jboolean JNICALL
Java_com_soren_airpodscompanion_HiddenBluetoothCompat_00024NativeHiddenApiBridge_setCodecConfigPreferenceNative(
        JNIEnv *env, jclass, jobject a2dp, jobject codecConfig) {
    jclass a2dpClass = GetBluetoothA2dpClass(env);
    if (!a2dpClass || !a2dp || !codecConfig) return JNI_FALSE;

    jmethodID setCodecConfig = env->GetMethodID(
            a2dpClass,
            "setCodecConfigPreference",
            "(Landroid/bluetooth/BluetoothCodecConfig;)Z");
    if (!setCodecConfig) return JNI_FALSE;
    return env->CallBooleanMethod(a2dp, setCodecConfig, codecConfig);
}

JNIEXPORT jobject JNICALL
Java_com_soren_airpodscompanion_HiddenBluetoothCompat_00024NativeHiddenApiBridge_getCodecStatusNative(
        JNIEnv *env, jclass, jobject a2dp) {
    jclass a2dpClass = GetBluetoothA2dpClass(env);
    if (!a2dpClass || !a2dp) return nullptr;

    jmethodID getCodecStatus = env->GetMethodID(a2dpClass, "getCodecStatus",
                                                "()Landroid/bluetooth/BluetoothCodecStatus;");
    if (!getCodecStatus) return nullptr;
    return env->CallObjectMethod(a2dp, getCodecStatus);
}

JNIEXPORT jstring JNICALL
Java_com_soren_airpodscompanion_HiddenBluetoothCompat_00024NativeHiddenApiBridge_getParametersNative(
        JNIEnv *env, jclass, jobject audioManager, jstring keys) {
    jclass audioManagerClass = env->GetObjectClass(audioManager);
    if (!audioManagerClass || !audioManager || !keys) return nullptr;

    jmethodID getParameters = env->GetMethodID(
            audioManagerClass,
            "getParameters",
            "(Ljava/lang/String;)Ljava/lang/String;");
    if (!getParameters) return nullptr;
    return (jstring)env->CallObjectMethod(audioManager, getParameters, keys);
}

JNIEXPORT jboolean JNICALL
Java_com_soren_airpodscompanion_HiddenBluetoothCompat_00024NativeHiddenApiBridge_setParametersNative(
        JNIEnv *env, jclass, jobject audioManager, jstring keyValuePairs) {
    jclass audioManagerClass = env->GetObjectClass(audioManager);
    if (!audioManagerClass || !audioManager || !keyValuePairs) return JNI_FALSE;

    jmethodID setParameters = env->GetMethodID(
            audioManagerClass,
            "setParameters",
            "(Ljava/lang/String;)V");
    if (!setParameters) return JNI_FALSE;
    env->CallVoidMethod(audioManager, setParameters, keyValuePairs);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_soren_airpodscompanion_HiddenBluetoothCompat_00024NativeHiddenApiBridge_setBluetoothA2dpOnNative(
        JNIEnv *env, jclass, jobject audioManager, jboolean enabled) {
    jclass audioManagerClass = env->GetObjectClass(audioManager);
    if (!audioManagerClass || !audioManager) return JNI_FALSE;

    jmethodID setBluetoothA2dpOn = env->GetMethodID(
            audioManagerClass,
            "setBluetoothA2dpOn",
            "(Z)Z");
    if (!setBluetoothA2dpOn) return JNI_FALSE;
    return env->CallBooleanMethod(audioManager, setBluetoothA2dpOn, enabled);
}

JNIEXPORT jint JNICALL
Java_com_soren_airpodscompanion_HiddenBluetoothCompat_00024NativeHiddenApiBridge_getConnectionStateNative(
        JNIEnv *env, jclass, jobject adapter) {
    jclass adapterClass = env->GetObjectClass(adapter);
    if (!adapterClass || !adapter) return -1;

    jmethodID getConnectionState = env->GetMethodID(adapterClass, "getConnectionState", "()I");
    if (!getConnectionState) return -1;
    return env->CallIntMethod(adapter, getConnectionState);
}

JNIEXPORT jboolean JNICALL
Java_com_soren_airpodscompanion_HiddenBluetoothCompat_00024NativeHiddenApiBridge_disconnectNative(
        JNIEnv *env, jclass, jobject adapter, jobject device) {
    jclass adapterClass = env->GetObjectClass(adapter);
    if (!adapterClass || !adapter || !device) return JNI_FALSE;

    jmethodID disconnect = env->GetMethodID(
            adapterClass,
            "disconnect",
            "(Landroid/bluetooth/BluetoothDevice;)Z");
    if (!disconnect) return JNI_FALSE;
    return env->CallBooleanMethod(adapter, disconnect, device);
}

JNIEXPORT jint JNICALL
Java_com_soren_airpodscompanion_HiddenBluetoothCompat_00024NativeHiddenApiBridge_getLeConnectionStateNative(
        JNIEnv *env, jclass, jobject adapter, jobject device) {
    jclass adapterClass = env->GetObjectClass(adapter);
    if (!adapterClass || !adapter || !device) return -1;

    jmethodID getLeConnectionState = env->GetMethodID(
            adapterClass,
            "getLeConnectionState",
            "(Landroid/bluetooth/BluetoothDevice;)I");
    if (!getLeConnectionState) return -1;
    return env->CallIntMethod(adapter, getLeConnectionState, device);
}

}
