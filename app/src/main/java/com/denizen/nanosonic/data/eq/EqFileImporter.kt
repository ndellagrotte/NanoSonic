package com.denizen.nanosonic.data.eq

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.denizen.nanosonic.core.model.ParametricEq
import com.denizen.nanosonic.data.autoeq.ParametricEqParser
import com.denizen.nanosonic.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Reads a user-picked ParametricEQ.txt file. */
class EqFileImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    data class Imported(val name: String, val eq: ParametricEq)

    class InvalidEqFileException(message: String) : Exception(message)

    /** @throws InvalidEqFileException when the file isn't a usable parametric EQ. */
    suspend fun read(uri: Uri): Imported = withContext(io) {
        val resolver = context.contentResolver
        val fileName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: "custom_eq.txt"

        val text = resolver.openInputStream(uri)?.use { input ->
            val bytes = input.readNBytes(MAX_BYTES + 1)
            if (bytes.size > MAX_BYTES) throw InvalidEqFileException("File is too large to be an EQ profile")
            bytes.decodeToString()
        } ?: throw InvalidEqFileException("Could not open the file")

        val eq = ParametricEqParser.parse(text)
        ParametricEqParser.validate(eq)?.let { throw InvalidEqFileException(it) }
        Imported(fileName.removeSuffix(".txt"), eq)
    }

    private companion object {
        const val MAX_BYTES = 64 * 1024
    }
}
