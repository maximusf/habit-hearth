package com.project.habithearth.data.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.project.habithearth.data.proto.UserProgressProto
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.InputStream
import java.io.OutputStream

/**
 * Teaches androidx.datastore how to read and write [UserProgressProto] in
 * protobuf wire format using kotlinx.serialization.
 *
 * The default value is [UserProgressProto.DEFAULT] (all zero/empty fields).
 * On a fresh install (or if the on-disk file is missing) DataStore hands this
 * back to callers, which the repositories will then map to UI defaults during
 * Phase 3 of the refactor.
 */
@OptIn(ExperimentalSerializationApi::class)
object UserProgressSerializer : Serializer<UserProgressProto> {

    override val defaultValue: UserProgressProto = UserProgressProto.DEFAULT

    override suspend fun readFrom(input: InputStream): UserProgressProto {
        return try {
            ProtoBuf.decodeFromByteArray(
                UserProgressProto.serializer(),
                input.readBytes(),
            )
        } catch (exception: SerializationException) {
            // Wrapped in CorruptionException so DataStore's corruption handler
            // (if registered) can recover by overwriting with defaultValue.
            // Without this wrapping, the raw SerializationException would
            // surface to every collector of the flow on read.
            throw CorruptionException("Cannot read UserProgressProto.", exception)
        }
    }

    override suspend fun writeTo(t: UserProgressProto, output: OutputStream) {
        output.write(
            ProtoBuf.encodeToByteArray(UserProgressProto.serializer(), t),
        )
    }
}
