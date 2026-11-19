package com.denizen.nanosonic.data.autoeq

import java.io.File
import java.io.InputStream

/** [AssetSource] over a directory whose layout mirrors `assets/` (the `autoeqDB` folder is [root]). */
class FileAssetSource(private val root: File) : AssetSource {
    val openedPaths = mutableListOf<String>()

    private fun resolve(path: String) = File(root, path.removePrefix("autoeqDB/"))

    override fun list(path: String): List<String> = resolve(path).list()?.sorted().orEmpty()

    override fun open(path: String): InputStream {
        openedPaths += path
        return resolve(path).inputStream()
    }

    companion object {
        fun fixture(): FileAssetSource {
            val url = requireNotNull(FileAssetSource::class.java.classLoader!!.getResource("autoeqDB-fixture")) { "fixture missing" }
            return FileAssetSource(File(url.toURI()))
        }
    }
}
