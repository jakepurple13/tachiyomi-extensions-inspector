package suwayomi.tachidesk.manga.impl.util

/*
 * Copyright (C) Contributors to the Suwayomi project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.os.Bundle
import com.googlecode.d2j.dex.Dex2jar
import com.googlecode.d2j.reader.MultiDexFileReader
import com.googlecode.dex2jar.tools.BaksmaliBaseDexExceptionHandler
import mu.KotlinLogging
import net.dongliu.apk.parser.ApkFile
import net.dongliu.apk.parser.ApkParsers
import org.w3c.dom.Element
import org.w3c.dom.Node
import xyz.nulldev.androidcompat.pm.InstalledPackage.Companion.toList
import xyz.nulldev.androidcompat.pm.toPackageInfo
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.kotlinFunction

object PackageTools {
    private val logger = KotlinLogging.logger {}

    const val EXTENSION_FEATURE = "programmersbox.otaku.extension"
    const val METADATA_SOURCE_CLASS = "programmersbox.otaku.class"
    const val LIB_VERSION_MIN = 1.3
    const val LIB_VERSION_MAX = 1.4

    /**
     * Convert dex to jar, a wrapper for the dex2jar library
     */
    fun dex2jar(dexFile: File, jarFile: File) {
        // adopted from com.googlecode.dex2jar.tools.Dex2jarCmd.doCommandLine
        // source at: https://github.com/DexPatcher/dex2jar/tree/v2.1-20190905-lanchon/dex-tools/src/main/java/com/googlecode/dex2jar/tools/Dex2jarCmd.java

        val jarFilePath = jarFile.toPath()
        val reader = MultiDexFileReader.open(Files.readAllBytes(dexFile.toPath()))
        val handler = BaksmaliBaseDexExceptionHandler()
        Dex2jar
            .from(reader)
            .withExceptionHandler(handler)
            .reUseReg(false)
            .topoLogicalSort()
            .skipDebug(true)
            .optimizeSynchronized(false)
            .printIR(false)
            .noCode(false)
            .skipExceptions(false)
            .to(jarFilePath)
        if (handler.hasException()) {
            val errorFile: Path = jarFilePath.parent.resolve("${dexFile.nameWithoutExtension}-error.txt")
            logger.error(
                """
                Detail Error Information in File $errorFile
                Please report this file to one of following link if possible (any one).
                https://sourceforge.net/p/dex2jar/tickets/
                https://bitbucket.org/pxb1988/dex2jar/issues
                https://github.com/pxb1988/dex2jar/issues
                dex2jar@googlegroups.com
                """.trimIndent()
            )
            handler.dump(errorFile, emptyArray<String>())
        }
    }

    /** A modified version of `xyz.nulldev.androidcompat.pm.InstalledPackage.info` */
    fun getPackageInfo(apkFilePath: String): PackageInfo {
        val apk = File(apkFilePath)
        return ApkParsers.getMetaInfo(apk).toPackageInfo(apk).apply {
            val parsed = ApkFile(apk)
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc = parsed.manifestXml.byteInputStream().use {
                dBuilder.parse(it)
            }

            logger.trace(parsed.manifestXml)

            applicationInfo.metaData = Bundle().apply {
                val appTag = doc.getElementsByTagName("application").item(0)

                appTag?.childNodes?.toList()
                    .orEmpty()
                    .asSequence()
                    .filter {
                        it.nodeType == Node.ELEMENT_NODE
                    }.map {
                        it as Element
                    }.filter {
                        it.tagName == "meta-data"
                    }.forEach {
                        putString(
                            it.attributes.getNamedItem("android:name").nodeValue,
                            it.attributes.getNamedItem("android:value").nodeValue
                        )
                    }
            }

            signatures = (
                parsed.apkSingers.flatMap { it.certificateMetas }
                /*+ parsed.apkV2Singers.flatMap { it.certificateMetas }*/
                ) // Blocked by: https://github.com/hsiafan/apk-parser/issues/72
                .map { Signature(it.data) }.toTypedArray()
        }
    }

    /**
     * loads the extension main class called $className from the jar located at $jarPath
     * It may return an instance of HttpSource or SourceFactory depending on the extension.
     */
    fun loadExtensionSources(jarPath: String, className: String): Any {
        val classLoader = URLClassLoader(arrayOf<URL>(URL("file:$jarPath")))
        val classToLoad = Class.forName(className, false, classLoader)
        println(classToLoad)
        println(className)
        println(classToLoad.getDeclaredConstructor().toGenericString())
        val c = classToLoad.kotlin
        //val s = c.constructors.single()
        //val s = c.primaryConstructor//getDeclaredConstructor().kotlinFunction
        //s?.isAccessible = true
        //println(s)
        return c.objectInstance!!//s!!.call()//classToLoad.getDeclaredConstructor().newInstance()
    }
}
