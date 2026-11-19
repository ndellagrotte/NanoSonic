package com.denizen.nanosonic.data.autoeq

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.InputStream
import javax.inject.Inject

/** Read-only access to bundled files, abstracted so the index can be tested against a plain directory. */
interface AssetSource {
    /** Child names of [path], sorted. Empty when [path] is a file or missing. */
    fun list(path: String): List<String>
    fun open(path: String): InputStream
}

class AndroidAssetSource @Inject constructor(@ApplicationContext context: Context) : AssetSource {
    private val assets = context.assets
    override fun list(path: String): List<String> = assets.list(path).orEmpty().sorted()
    override fun open(path: String): InputStream = assets.open(path)
}
