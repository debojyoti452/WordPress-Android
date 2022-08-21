package org.wordpress.android.sharedlogin.resolver

import android.content.ContentResolver
import android.content.Context
import android.database.MatrixCursor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.UpdateTokenPayload
import org.wordpress.android.provider.query.QueryResult
import org.wordpress.android.resolver.ContentResolverWrapper
import org.wordpress.android.sharedlogin.JetpackSharedLoginFlag
import org.wordpress.android.sharedlogin.WordPressPublicData
import org.wordpress.android.sharedlogin.provider.SharedLoginProvider
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.AccountActionBuilderWrapper
import org.wordpress.android.viewmodel.ContextProvider

class SharedLoginResolverTest {
    private val jetpackSharedLoginFlag: JetpackSharedLoginFlag = mock()
    private val contextProvider: ContextProvider = mock()
    private val wordPressPublicData: WordPressPublicData = mock()
    private val dispatcher: Dispatcher = mock()
    private val queryResult: QueryResult = mock()
    private val accountStore: AccountStore = mock()
    private val contentResolverWrapper: ContentResolverWrapper = mock()
    private val accountActionBuilderWrapper: AccountActionBuilderWrapper = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val classToTest = SharedLoginResolver(
            jetpackSharedLoginFlag,
            contextProvider,
            wordPressPublicData,
            dispatcher,
            queryResult,
            accountStore,
            contentResolverWrapper,
            accountActionBuilderWrapper,
            appPrefsWrapper
    )
    private val loggedInToken = "valid"
    private val notLoggedInToken = ""
    private val wordPressCurrentPackageId = "packageId"
    private val uriValue = "content://$wordPressCurrentPackageId.${SharedLoginProvider::class.simpleName}"
    private val context: Context = mock()
    private val contentResolver: ContentResolver = mock()
    private val updateTokenAction: Action<UpdateTokenPayload> = mock()
    private val mockCursor: MatrixCursor = mock()

    @Before
    fun setup() {
        whenever(contextProvider.getContext()).thenReturn(context)
        whenever(context.contentResolver).thenReturn(contentResolver)
        whenever(wordPressPublicData.currentPackageId()).thenReturn(wordPressCurrentPackageId)
        whenever(mockCursor.getString(0)).thenReturn(notLoggedInToken)
        whenever(accountActionBuilderWrapper.newUpdateAccessTokenAction(loggedInToken)).thenReturn(updateTokenAction)
        whenever(contentResolverWrapper.queryUri(contentResolver, uriValue)).thenReturn(mockCursor)
    }

    @Test
    fun `Should NOT query ContentResolver if feature flag is DISABLED`() {
        whenever(appPrefsWrapper.getIsFirstTrySharedLoginJetpack()).thenReturn(true)
        whenever(accountStore.accessToken).thenReturn(notLoggedInToken)
        whenever(jetpackSharedLoginFlag.isEnabled()).thenReturn(false)
        classToTest.tryJetpackLogin()
        verify(contentResolverWrapper, never()).queryUri(contentResolver, uriValue)
    }

    @Test
    fun `Should NOT query ContentResolver if IS already logged in`() {
        whenever(appPrefsWrapper.getIsFirstTrySharedLoginJetpack()).thenReturn(true)
        whenever(accountStore.accessToken).thenReturn(loggedInToken)
        whenever(jetpackSharedLoginFlag.isEnabled()).thenReturn(true)
        classToTest.tryJetpackLogin()
        verify(contentResolverWrapper, never()).queryUri(contentResolver, uriValue)
    }

    @Test
    fun `Should NOT query ContentResolver if IS NOT the first try`() {
        whenever(appPrefsWrapper.getIsFirstTrySharedLoginJetpack()).thenReturn(false)
        whenever(accountStore.accessToken).thenReturn(notLoggedInToken)
        whenever(jetpackSharedLoginFlag.isEnabled()).thenReturn(true)
        classToTest.tryJetpackLogin()
        verify(contentResolverWrapper, never()).queryUri(contentResolver, uriValue)
    }

    @Test
    fun `Should query ContentResolver if NOT already logged in, feature flag is ENABLED and IS first try`() {
        featureEnabled()
        classToTest.tryJetpackLogin()
        verify(contentResolverWrapper).queryUri(contentResolver, uriValue)
    }

    @Test
    fun `Should dispatch UpdateTokenPayload if access token is NOT empty`() {
        featureEnabled()
        whenever(queryResult.getValue<String>(mockCursor)).thenReturn(loggedInToken)
        classToTest.tryJetpackLogin()
        verify(dispatcher).dispatch(updateTokenAction)
    }

    @Test
    fun `Should NOT dispatch UpdateTokenPayload if access token IS empty`() {
        featureEnabled()
        whenever(queryResult.getValue<String>(mockCursor)).thenReturn(notLoggedInToken)
        classToTest.tryJetpackLogin()
        verify(dispatcher, times(0)).dispatch(updateTokenAction)
    }

    private fun featureEnabled() {
        whenever(appPrefsWrapper.getIsFirstTrySharedLoginJetpack()).thenReturn(true)
        whenever(accountStore.accessToken).thenReturn(notLoggedInToken)
        whenever(jetpackSharedLoginFlag.isEnabled()).thenReturn(true)
    }
}
