#include "contacts.h"
#include "util.h"
#include "jni_utils.h"

extern "C"
JNIEXPORT jobject JNICALL
Java_network_noth_messenger_libsession_1util_Contacts_get(JNIEnv *env, jobject thiz,
                                                          jstring account_id) {
    // If an exception is thrown, return nullptr
    return jni_utils::run_catching_cxx_exception_or<jobject>(
            [=]() -> jobject {
                std::lock_guard lock{util::util_mutex_};
                auto contacts = ptrToContacts(env, thiz);
                auto account_id_chars = env->GetStringUTFChars(account_id, nullptr);
                auto contact = contacts->get(account_id_chars);
                env->ReleaseStringUTFChars(account_id, account_id_chars);
                if (!contact) return nullptr;
                jobject j_contact = serialize_contact(env, contact.value());
                return j_contact;
            },
            [](const char *) -> jobject { return nullptr; }
    );
}

extern "C"
JNIEXPORT jobject JNICALL
Java_network_noth_messenger_libsession_1util_Contacts_getOrConstruct(JNIEnv *env, jobject thiz,
                                                                     jstring account_id) {
    return jni_utils::run_catching_cxx_exception_or_throws<jobject>(env, [=] {
        std::lock_guard lock{util::util_mutex_};
        auto contacts = ptrToContacts(env, thiz);
        auto account_id_chars = env->GetStringUTFChars(account_id, nullptr);
        auto contact = contacts->get_or_construct(account_id_chars);
        env->ReleaseStringUTFChars(account_id, account_id_chars);
        return serialize_contact(env, contact);
    });
}

extern "C"
JNIEXPORT void JNICALL
Java_network_noth_messenger_libsession_1util_Contacts_set(JNIEnv *env, jobject thiz,
                                                          jobject contact) {
    jni_utils::run_catching_cxx_exception_or_throws<void>(env, [=] {
        std::lock_guard lock{util::util_mutex_};
        auto contacts = ptrToContacts(env, thiz);
        auto contact_info = deserialize_contact(env, contact, contacts);
        contacts->set(contact_info);
    });
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_network_noth_messenger_libsession_1util_Contacts_erase(JNIEnv *env, jobject thiz,
                                                            jstring account_id) {
    return jni_utils::run_catching_cxx_exception_or_throws<jboolean>(env, [=] {
        std::lock_guard lock{util::util_mutex_};
        auto contacts = ptrToContacts(env, thiz);
        auto account_id_chars = env->GetStringUTFChars(account_id, nullptr);

        bool result = contacts->erase(account_id_chars);
        env->ReleaseStringUTFChars(account_id, account_id_chars);
        return result;
    });
}

extern "C"
JNIEXPORT jobject JNICALL
Java_network_noth_messenger_libsession_1util_Contacts_all(JNIEnv *env, jobject thiz) {
    return jni_utils::run_catching_cxx_exception_or_throws<jobject>(env, [=] {
        std::lock_guard lock{util::util_mutex_};
        auto contacts = ptrToContacts(env, thiz);
        jclass stack = env->FindClass("java/util/Stack");
        jmethodID init = env->GetMethodID(stack, "<init>", "()V");
        jobject our_stack = env->NewObject(stack, init);
        jmethodID push = env->GetMethodID(stack, "push", "(Ljava/lang/Object;)Ljava/lang/Object;");
        for (const auto &contact: *contacts) {
            auto contact_obj = serialize_contact(env, contact);
            env->CallObjectMethod(our_stack, push, contact_obj);
        }
        return our_stack;
    });
}
