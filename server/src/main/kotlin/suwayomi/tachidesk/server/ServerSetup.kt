package suwayomi.tachidesk.server

/*
 * Copyright (C) Contributors to the Suwayomi project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import android.app.Application
import android.content.Context
import eu.kanade.tachiyomi.App
import mu.KotlinLogging
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.dsl.binds
import org.koin.dsl.module
import suwayomi.server.BuildConfig
import xyz.nulldev.androidcompat.AndroidCompat
import xyz.nulldev.androidcompat.AndroidCompatInitializer
import xyz.nulldev.ts.config.ConfigKodeinModule

private val logger = KotlinLogging.logger {}

val androidCompat by lazy { AndroidCompat() }

fun applicationSetup() {
    logger.info("Running Inspector ${BuildConfig.VERSION} revision ${BuildConfig.REVISION}")

    // Load config API
    DI.global.addImport(ConfigKodeinModule().create())
    // Load Android compatibility dependencies
    AndroidCompatInitializer().init()
    val app = App()
    // start app
    androidCompat.startApp(app)
    startKoin {
        loadKoinModules(
            module {
                single { app } binds arrayOf(Context::class, Application::class)
            }
        )
    }
}
