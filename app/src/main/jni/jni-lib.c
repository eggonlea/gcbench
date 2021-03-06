//
// Created by lilioss on 8/1/16.
//

#include <jni.h>
#include <stdlib.h>
#include <string.h>

jlong Java_com_lilioss_gcbench_GCBench_nativeMalloc(
        JNIEnv* env,
        jobject obj, /* this */
        jint size) {
    void *ptr = malloc(size);
    if (ptr != NULL) {
        memset(ptr, 0, size);
    }
    return (long)ptr;
}

void Java_com_lilioss_gcbench_GCBench_nativeFree(
        JNIEnv* env,
        jobject obj, /* this */
        jlong ptr) {
    free((void *)ptr);
}