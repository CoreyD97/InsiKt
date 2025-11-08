package com.coreyd97.insikt

import burp.api.montoya.MontoyaApi
import com.coreyd97.insikt.exports.LogExportService
import com.coreyd97.insikt.exports.LogExportServiceImpl
import com.coreyd97.insikt.exports.LogExporter
import com.coreyd97.insikt.exports.elastic.ElasticExporter
import com.coreyd97.insikt.filter.FilterExpression
import com.coreyd97.insikt.filter.FilterLibrary
import com.coreyd97.insikt.filter.FilterLibraryImpl
import com.coreyd97.insikt.filter.ParserService
import com.coreyd97.insikt.filter.ParserServiceImpl
import com.coreyd97.insikt.filter.TableColorService
import com.coreyd97.insikt.filter.TableFilterService
import com.coreyd97.insikt.filter.TableFilterServiceImpl
import com.coreyd97.insikt.filter.TagService
import com.coreyd97.insikt.grepper.GrepperService
import com.coreyd97.insikt.logging.logentry.LogEntryFactory
import com.coreyd97.insikt.logging.LogEntryMenuFactory
import com.coreyd97.insikt.view.logtable.LogTableColumnModel
import com.coreyd97.insikt.view.logtable.LogTableModel
import com.coreyd97.insikt.logging.EntryImportWorkerFactory
import com.coreyd97.insikt.logging.LogProcessor
import com.coreyd97.insikt.logging.LogProcessorImpl
import com.coreyd97.insikt.view.logtable.LogTable
import com.coreyd97.insikt.logview.repository.InMemoryLogRepository
import com.coreyd97.insikt.logview.repository.LogRepository
import com.coreyd97.insikt.view.PreferencesPanel
import com.coreyd97.insikt.reflection.ReflectionService
import com.coreyd97.insikt.util.NamedThreadFactory
import com.coreyd97.insikt.util.PausableThreadPoolExecutor
import com.coreyd97.insikt.view.LoggerMenu
import com.coreyd97.insikt.view.InsiktPanel
import com.coreyd97.insikt.view.InsiktPanelImpl
import com.coreyd97.insikt.view.logtable.LogView
import com.coreyd97.insikt.view.logtable.LogViewImpl
import com.coreyd97.insikt.view.shared.RequestViewer
import com.google.inject.AbstractModule
import com.google.inject.Provider
import com.google.inject.Provides
import com.google.inject.Scopes
import com.google.inject.Singleton
import com.google.inject.multibindings.Multibinder
import com.google.inject.name.Named
import java.awt.Window
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.TimeUnit

class ExecutorNames {
    companion object {
        const val ENTRY_PROCESS = "entryProcessExecutor"
        const val ENTRY_IMPORT = "entryImportExecutor"
        const val CLEANUP = "cleanupExecutor"
    }
}

class InsiktModule(val montoya: MontoyaApi) : AbstractModule() {

    override fun configure() {
        bind(MontoyaApi::class.java).toInstance(montoya)
        bind(FilterLibrary::class.java).to(FilterLibraryImpl::class.java)
        bind(TableFilterService::class.java).to(TableFilterServiceImpl::class.java)
        bind(LogRepository::class.java).to(InMemoryLogRepository::class.java).`in`(Scopes.SINGLETON)
        bind(LogProcessor::class.java).to(LogProcessorImpl::class.java).`in`(Scopes.SINGLETON)
        bind(ParserService::class.java).to(ParserServiceImpl::class.java)
        bind(LogView::class.java).to(LogViewImpl::class.java)
        bind(LoggingController::class.java)
        bind(LogExportService::class.java).to(LogExportServiceImpl::class.java)
        val exportBinder = Multibinder.newSetBinder(binder(), LogExporter::class.java)
//        exportBinder.addBinding().to(ElasticExporter::class.java)
        bind(ReflectionService::class.java)
        bind(TableColorService::class.java)
        bind(TagService::class.java)
        bind(GrepperService::class.java)
        bind(LoggerContextMenuFactory::class.java)
        bind(LogEntryMenuFactory::class.java)
        bind(LogTableModel::class.java)
        bind(LogTableColumnModel::class.java)
        bind(LogEntryFactory::class.java)
        bind(EntryImportWorkerFactory::class.java)
        bind(RequestViewer::class.java)
        bind(LoggerMenu::class.java).`in`(Scopes.SINGLETON)
        bind(InsiktPanel::class.java).to(InsiktPanelImpl::class.java)
        bind(PreferencesPanel::class.java).`in`(Scopes.SINGLETON)


        // Enable static (companion object) injection for FilterExpression
        requestStaticInjection(FilterExpression::class.java)
        requestStaticInjection(BurpAppender::class.java)
    }

    @Provides
    @Singleton
    @Named("extension")
    fun extensionWindow(montoya: MontoyaApi, panelProvider: Provider<InsiktPanel>): Window {
        val suiteFrame = montoya.userInterface().swingUtils().suiteFrame()
        
        val panel = panelProvider.get().getComponent()
        return montoya.userInterface().swingUtils().windowForComponent(panel) ?: suiteFrame
    }

    @Provides
    @Singleton
    @Named("LogViewSelected")
    fun logViewSelectedRequestViewer(provider: Provider<RequestViewer>): RequestViewer {
        return provider.get()
    }

    @Provides
    @Singleton
    fun logTable(montoyaApi: MontoyaApi,
                 filterLibrary: FilterLibrary,
                 filterService: TableFilterService,
                 logEntryMenuFactory: LogEntryMenuFactory,
                 @Named("LogViewSelected") requestViewer: RequestViewer,
                 dataModel: LogTableModel,
                 columnModel: LogTableColumnModel): LogTable =
        LogTable(montoyaApi, filterLibrary, filterService, requestViewer, logEntryMenuFactory, dataModel, columnModel)

    @Provides @Singleton @Named(ExecutorNames.ENTRY_PROCESS)
    fun provideEntryProcessExecutor(): PausableThreadPoolExecutor =
        PausableThreadPoolExecutor(
            0, Int.MAX_VALUE,
            30L, TimeUnit.SECONDS,
            SynchronousQueue(),
            NamedThreadFactory("LPP-LogManager")
        )

    @Provides @Singleton @Named(ExecutorNames.ENTRY_IMPORT)
    fun provideEntryImportExecutor(): PausableThreadPoolExecutor =
        PausableThreadPoolExecutor(
            0, 10,
            60L, TimeUnit.MILLISECONDS,
            LinkedBlockingQueue(),
            NamedThreadFactory("LPP-Import")
        )

    @Provides
    @Singleton
    @Named(ExecutorNames.CLEANUP)
    fun provideCleanupExecutor(): ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor(NamedThreadFactory("LPP-LogManager-Cleanup"))

}