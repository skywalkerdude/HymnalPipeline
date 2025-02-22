package com.hymnsmobile.pipeline.utils;

import com.google.protobuf.MessageLite;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ProtoUtils {

    public static byte[] hashProto(MessageLite message) throws NoSuchAlgorithmException {
        // Serialize the protobuf message to a byte array
        byte[] serializedMessage = message.toByteArray();

        // Create a MessageDigest instance for SHA-256
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        // Compute the hash
        return digest.digest(serializedMessage);
    }
}
