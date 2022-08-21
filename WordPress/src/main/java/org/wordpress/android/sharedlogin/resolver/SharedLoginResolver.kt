package org.wordpress.android.sharedlogin.resolver

import android.content.Intent
import android.database.Cursor
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.provider.query.QueryResult
import org.wordpress.android.resolver.ContentResolverWrapper
import org.wordpress.android.sharedlogin.JetpackSharedLoginFlag
import org.wordpress.android.sharedlogin.WordPressPublicData
import org.wordpress.android.sharedlogin.provider.SharedLoginProvider
import org.wordpress.android.ui.main.WPMainActivity
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.AccountActionBuilderWrapper
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

class SharedLoginResolver @Inject constructor(
    private val jetpackSharedLoginFlag: JetpackSharedLoginFlag,
    private val contextProvider: ContextProvider,
    private val wordPressPublicData: WordPressPublicData,
    private val dispatcher: Dispatcher,
    private val queryResult: QueryResult,
    private val accountStore: AccountStore,
    private val contentResolverWrapper: ContentResolverWrapper,
    private val accountActionBuilderWrapper: AccountActionBuilderWrapper,
    private val appPrefsWrapper: AppPrefsWrapper
) {
    fun tryJetpackLogin() {
        val isAlreadyLoggedIn = accountStore.accessToken.isNotEmpty()
        val isFirstTry = appPrefsWrapper.getIsFirstTrySharedLoginJetpack()
        if (isAlreadyLoggedIn || !jetpackSharedLoginFlag.isEnabled() || !isFirstTry) {
            return
        }
        appPrefsWrapper.saveIsFirstTrySharedLoginJetpack(false)
        val accessTokenResultCursor = getAccessTokenResultCursor()
        if (accessTokenResultCursor != null) {
            val accessToken = queryResult.getValue<String>(accessTokenResultCursor) ?: ""
            if (accessToken.isNotEmpty()) {
                dispatchUpdateAccessToken(accessToken)
                reloadMainScreen()
            }
        }
    }

    private fun getAccessTokenResultCursor(): Cursor? {
        val wordpressAccessTokenUriValue =
                "content://${wordPressPublicData.currentPackageId()}.${SharedLoginProvider::class.simpleName}"
        return contentResolverWrapper.queryUri(
                contextProvider.getContext().contentResolver,
                wordpressAccessTokenUriValue
        )
    }

    private fun dispatchUpdateAccessToken(accessToken: String) {
        dispatcher.dispatch(
                accountActionBuilderWrapper.newUpdateAccessTokenAction(accessToken)
        )
    }

    private fun reloadMainScreen() {
        contextProvider.getContext().run {
            val mainActivityIntent = Intent(this, WPMainActivity::class.java)
            mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(mainActivityIntent)
        }
    }
}
