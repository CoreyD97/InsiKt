package com.coreyd97.insikt

import burp.api.montoya.BurpExtension
import burp.api.montoya.MontoyaApi
import burp.api.montoya.core.Registration
import com.coreyd97.insikt.exports.LogExportServiceImpl
import com.coreyd97.insikt.logging.LogProcessor
import com.coreyd97.insikt.view.logtable.LogTableModel
import com.coreyd97.insikt.view.PreferencesPanel
import com.coreyd97.insikt.util.APP_NAME
import com.coreyd97.insikt.util.VERSION
import com.coreyd97.insikt.view.LoggerMenu
import com.coreyd97.insikt.view.InsiktPanel
import com.coreyd97.insikt.view.colorizingdialog.ColorizingRuleDialogFactory
import com.coreyd97.montoyautilities.MontoyaUtilities
import com.google.inject.Guice
import org.apache.logging.log4j.LogManager
import java.awt.Window

class  Insikt : BurpExtension {
    val log = LogManager.getLogger(Insikt::class.java)
    val registrations = mutableListOf<Registration>()

    override fun initialize(montoya: MontoyaApi) {
        montoya.extension().setName(APP_NAME)

        MontoyaUtilities(montoya) //Initialize MontoyaUtilities
        val injector = Guice.createInjector(InsiktModule(montoya))
        log.info("${APP_NAME} ${VERSION} by Corey Arthur.")
        log.info("Feel free to reach out on BlueSky (@coreyarthur.com) with any questions.")

        injector.getInstance(LoggingController::class.java)

        val logProcessor = injector.getInstance(LogProcessor::class.java)
        montoya.http().registerHttpHandler(logProcessor).also { registrations.add(it) }
        montoya.proxy().registerResponseHandler(logProcessor).also { registrations.add(it) }
        montoya.proxy().registerRequestHandler(logProcessor).also { registrations.add(it) }

        val contextMenuFactory = injector.getInstance(LoggerContextMenuFactory::class.java)
        montoya.userInterface().registerContextMenuItemsProvider(contextMenuFactory)

        val loggerMenu = injector.getInstance(LoggerMenu::class.java)
        montoya.userInterface().menuBar().registerMenu(loggerMenu)

        val preferencesPanel = injector.getInstance(PreferencesPanel::class.java)
        montoya.userInterface().registerSettingsPanel(preferencesPanel)

        val mainPanel = injector.getInstance(InsiktPanel::class.java)
        montoya.userInterface().registerSuiteTab(APP_NAME, mainPanel.getComponent())

        val logTableModel = injector.getInstance(LogTableModel::class.java)
        montoya.extension().registerUnloadingHandler {
            registrations.forEach { runCatching { it.deregister() } }
            mainPanel?.unload()
            logProcessor.shutdown()
            logTableModel.dispose()
        }
    }
}
