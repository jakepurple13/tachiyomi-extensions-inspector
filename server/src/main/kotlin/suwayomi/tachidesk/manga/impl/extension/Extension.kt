package suwayomi.tachidesk.manga.impl.extension

/*
 * Copyright (C) Contributors to the Suwayomi project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

import com.programmersbox.models.ApiService
import com.programmersbox.models.ApiServicesCatalog
import com.programmersbox.models.ExternalApiServicesCatalog
import com.programmersbox.models.SourceInformation
import mu.KotlinLogging
import suwayomi.tachidesk.manga.impl.util.PackageTools.EXTENSION_FEATURE
import suwayomi.tachidesk.manga.impl.util.PackageTools.LIB_VERSION_MAX
import suwayomi.tachidesk.manga.impl.util.PackageTools.LIB_VERSION_MIN
import suwayomi.tachidesk.manga.impl.util.PackageTools.METADATA_SOURCE_CLASS
import suwayomi.tachidesk.manga.impl.util.PackageTools.dex2jar
import suwayomi.tachidesk.manga.impl.util.PackageTools.getPackageInfo
import suwayomi.tachidesk.manga.impl.util.PackageTools.loadExtensionSources
import java.io.File

object Extension {
    private val logger = KotlinLogging.logger {}

    suspend fun installAPK(tmpDir: File, fetcher: suspend () -> File): Pair<String, List<SourceInformation>> {
        val apkFile = fetcher()

        val jarFile = File(tmpDir, "${apkFile.nameWithoutExtension}.jar")

        val packageInfo = getPackageInfo(apkFile.absolutePath)

        if (!packageInfo.reqFeatures.orEmpty().any { it.name.contains(EXTENSION_FEATURE, true) }) {
            throw Exception("This apk is not an OtakuWorld extension")
        }

        // Validate lib version
        /*val libVersion = packageInfo.versionName.substringBeforeLast('.').toDouble()
        if (libVersion < LIB_VERSION_MIN || libVersion > LIB_VERSION_MAX) {
            throw Exception(
                "Lib version is $libVersion, while only versions " +
                    "$LIB_VERSION_MIN to $LIB_VERSION_MAX are allowed"
            )
        }*/

        val className = packageInfo.packageName + packageInfo.applicationInfo.metaData.getString(METADATA_SOURCE_CLASS)

        logger.trace("Main class for extension is $className")

        dex2jar(apkFile, jarFile)

        // collect sources from the extension
        return packageInfo.packageName to when (val instance = loadExtensionSources(jarFile.absolutePath, className)) {
            is ApiService -> listOf(
                SourceInformation(
                    apiService = instance,
                    name = instance.serviceName,
                    icon = null,
                    packageName = "",
                    null
                )
            )

            is ExternalApiServicesCatalog -> listOf(
                SourceInformation(
                    apiService = instance.createSources().random(),
                    name = instance.name,
                    icon = null,
                    packageName = "",
                    instance
                )
            )

            is ApiServicesCatalog -> listOf(
                SourceInformation(
                    apiService = instance.createSources().random(),
                    name = instance.name,
                    icon = null,
                    packageName = "",
                    instance
                )
            )

            else -> throw RuntimeException("Unknown source class type! ${instance.javaClass}")
        }
    }
}
