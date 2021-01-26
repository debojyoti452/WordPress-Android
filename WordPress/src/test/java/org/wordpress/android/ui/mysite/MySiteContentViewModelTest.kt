package org.wordpress.android.ui.mysite

import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAIN_CREDIT_PROMPT_SHOWN
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAIN_CREDIT_REDEMPTION_SUCCESS
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAIN_CREDIT_REDEMPTION_TAPPED
import org.wordpress.android.fluxc.model.JetpackCapability
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.UPDATE_SITE_TITLE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.UPLOAD_SITE_ICON
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase
import org.wordpress.android.ui.mysite.ListItemAction.ACTIVITY_LOG
import org.wordpress.android.ui.mysite.ListItemAction.ADMIN
import org.wordpress.android.ui.mysite.ListItemAction.COMMENTS
import org.wordpress.android.ui.mysite.ListItemAction.MEDIA
import org.wordpress.android.ui.mysite.ListItemAction.PAGES
import org.wordpress.android.ui.mysite.ListItemAction.PLAN
import org.wordpress.android.ui.mysite.ListItemAction.PLUGINS
import org.wordpress.android.ui.mysite.ListItemAction.POSTS
import org.wordpress.android.ui.mysite.ListItemAction.SCAN
import org.wordpress.android.ui.mysite.ListItemAction.SHARING
import org.wordpress.android.ui.mysite.ListItemAction.SITE_SETTINGS
import org.wordpress.android.ui.mysite.ListItemAction.STATS
import org.wordpress.android.ui.mysite.ListItemAction.THEMES
import org.wordpress.android.ui.mysite.ListItemAction.VIEW_SITE
import org.wordpress.android.ui.mysite.MySiteContentViewModel.TextInputDialogModel
import org.wordpress.android.ui.mysite.MySiteContentViewModelTest.SiteInfoBlockAction.ICON_CLICK
import org.wordpress.android.ui.mysite.MySiteContentViewModelTest.SiteInfoBlockAction.SWITCH_SITE_CLICK
import org.wordpress.android.ui.mysite.MySiteContentViewModelTest.SiteInfoBlockAction.TITLE_CLICK
import org.wordpress.android.ui.mysite.MySiteContentViewModelTest.SiteInfoBlockAction.URL_CLICK
import org.wordpress.android.ui.mysite.MySiteItem.DomainRegistrationBlock
import org.wordpress.android.ui.mysite.MySiteItem.ListItem
import org.wordpress.android.ui.mysite.MySiteItem.QuickActionsBlock
import org.wordpress.android.ui.mysite.MySiteItem.SiteInfoBlock
import org.wordpress.android.ui.mysite.MySiteItem.SiteInfoBlock.IconState
import org.wordpress.android.ui.mysite.QuickStartRepository.QuickStartModel
import org.wordpress.android.ui.mysite.SiteDialogModel.AddSiteIconDialogModel
import org.wordpress.android.ui.mysite.SiteDialogModel.ChangeSiteIconDialogModel
import org.wordpress.android.ui.mysite.SiteNavigationAction.AddNewSite
import org.wordpress.android.ui.mysite.SiteNavigationAction.ConnectJetpackForStats
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenActivityLog
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenAdmin
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenComments
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenDomainRegistration
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenMeScreen
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenMedia
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenPages
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenPlan
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenPlugins
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenPosts
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenScan
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenSharing
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenSite
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenSitePicker
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenSiteSettings
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenStats
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenThemes
import org.wordpress.android.ui.mysite.SiteNavigationAction.StartWPComLoginForJetpackStats
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.FluxCUtilsWrapper
import org.wordpress.android.util.MediaUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.WPMediaUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.BackupScreenFeatureConfig
import org.wordpress.android.util.config.ScanScreenFeatureConfig
import org.wordpress.android.viewmodel.ContextProvider

@RunWith(MockitoJUnitRunner::class)
class MySiteContentViewModelTest : BaseUnitTest() {
    @Mock lateinit var siteInfoBlockBuilder: SiteInfoBlockBuilder
    @Mock lateinit var siteItemsBuilder: SiteItemsBuilder
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    @Mock lateinit var accountStore: AccountStore
    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock lateinit var wpMediaUtilsWrapper: WPMediaUtilsWrapper
    @Mock lateinit var mediaUtilsWrapper: MediaUtilsWrapper
    @Mock lateinit var fluxCUtilsWrapper: FluxCUtilsWrapper
    @Mock lateinit var contextProvider: ContextProvider
    @Mock lateinit var siteIconUploadHandler: SiteIconUploadHandler
    @Mock lateinit var siteStoriesHandler: SiteStoriesHandler
    @Mock lateinit var domainRegistrationHandler: DomainRegistrationHandler
    @Mock lateinit var backupScreenFeatureConfig: BackupScreenFeatureConfig
    @Mock lateinit var jetpackCapabilitiesUseCase: JetpackCapabilitiesUseCase
    @Mock lateinit var scanScreenFeatureConfig: ScanScreenFeatureConfig
    @Mock lateinit var quickStartRepository: QuickStartRepository
    @Mock lateinit var quickStartItemBuilder: QuickStartItemBuilder
    @Mock lateinit var siteStore: SiteStore
    private lateinit var viewModel: MySiteContentViewModel
    private lateinit var uiModels: MutableList<List<MySiteItem>>
    private lateinit var snackbars: MutableList<SnackbarMessageHolder>
    private lateinit var textInputDialogModels: MutableList<TextInputDialogModel>
    private lateinit var dialogModels: MutableList<SiteDialogModel>
    private lateinit var navigationActions: MutableList<SiteNavigationAction>
    private val siteId = 1
    private val updatedSiteId = 2
    private val siteUrl = "http://site.com"
    private val siteIcon = "http://site.com/icon.jpg"
    private val siteName = "Site"
    private val emailAddress = "test@email.com"
    private lateinit var site: SiteModel
    private val onShowSiteIconProgressBar = MutableLiveData<Boolean>()
    private val isDomainCreditAvailable = MutableLiveData<Boolean>()
    private val quickStartModel = MutableLiveData<QuickStartModel>()

    @InternalCoroutinesApi
    @Before
    fun setUp() = test {
        onShowSiteIconProgressBar.value = null
        isDomainCreditAvailable.value = null
        whenever(selectedSiteRepository.showSiteIconProgressBar).thenReturn(onShowSiteIconProgressBar)
        whenever(domainRegistrationHandler.isDomainCreditAvailable).thenReturn(isDomainCreditAvailable)
        whenever(quickStartRepository.quickStartModel).thenReturn(quickStartModel)
        whenever(jetpackCapabilitiesUseCase.getOrFetchJetpackCapabilities(anyLong())).thenReturn(listOf())
        site = SiteModel()
        site.id = siteId
        site.url = siteUrl
        site.name = siteName
        site.iconUrl = siteIcon
        whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(site)
        initSiteInfoBuilder()
        initSiteItems()
        viewModel = MySiteContentViewModel(
                networkUtilsWrapper,
                TEST_DISPATCHER,
                TEST_DISPATCHER,
                analyticsTrackerWrapper,
                siteInfoBlockBuilder,
                siteItemsBuilder,
                accountStore,
                selectedSiteRepository,
                siteStore,
                wpMediaUtilsWrapper,
                mediaUtilsWrapper,
                fluxCUtilsWrapper,
                contextProvider,
                siteIconUploadHandler,
                siteStoriesHandler,
                domainRegistrationHandler,
                backupScreenFeatureConfig,
                jetpackCapabilitiesUseCase,
                scanScreenFeatureConfig,
                quickStartRepository,
                quickStartItemBuilder
        )
        viewModel.start(siteId)
        uiModels = mutableListOf()
        snackbars = mutableListOf()
        textInputDialogModels = mutableListOf()
        dialogModels = mutableListOf()
        navigationActions = mutableListOf()
        viewModel.uiModel.observeForever {
            uiModels.add(it)
        }
        viewModel.onSnackbarMessage.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                snackbars.add(it)
            }
        }
        viewModel.onTextInputDialogShown.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                textInputDialogModels.add(it)
            }
        }
        viewModel.onBasicDialogShown.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                dialogModels.add(it)
            }
        }
        viewModel.onNavigation.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                navigationActions.add(it)
            }
        }
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
    }

    @Test
    fun `model is contains header of selected site`() {
        assertThat(uiModels).hasSize(1)
        assertThat(uiModels.last()).isNotEmpty()

        assertThat(getLastItems().first()).isInstanceOf(SiteInfoBlock::class.java)
    }

    @Test
    fun `site block title click shows snackbar message when network not available`() {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        invokeSiteInfoBlockAction(TITLE_CLICK)

        assertThat(textInputDialogModels).isEmpty()
        assertThat(snackbars).containsOnly(
                SnackbarMessageHolder(UiStringRes(R.string.error_network_connection))
        )
    }

    @Test
    fun `site block title click shows snackbar message when hasCapabilityManageOptions is false`() {
        site.hasCapabilityManageOptions = false
        site.origin = SiteModel.ORIGIN_WPCOM_REST

        invokeSiteInfoBlockAction(TITLE_CLICK)

        assertThat(textInputDialogModels).isEmpty()
        assertThat(snackbars).containsOnly(
                SnackbarMessageHolder(
                        UiStringRes(R.string.my_site_title_changer_dialog_not_allowed_hint)
                )
        )
    }

    @Test
    fun `site block title click shows snackbar message when origin not ORIGIN_WPCOM_REST`() {
        site.hasCapabilityManageOptions = true
        site.origin = SiteModel.ORIGIN_XMLRPC

        invokeSiteInfoBlockAction(TITLE_CLICK)

        assertThat(textInputDialogModels).isEmpty()
        assertThat(snackbars).containsOnly(
                SnackbarMessageHolder(UiStringRes(R.string.my_site_title_changer_dialog_not_allowed_hint))
        )
    }

    @Test
    fun `site block title click shows input dialog when editing allowed`() {
        site.hasCapabilityManageOptions = true
        site.origin = SiteModel.ORIGIN_WPCOM_REST
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)

        invokeSiteInfoBlockAction(TITLE_CLICK)

        assertThat(snackbars).isEmpty()
        assertThat(textInputDialogModels.last()).isEqualTo(
                TextInputDialogModel(
                        callbackId = MySiteContentViewModel.SITE_NAME_CHANGE_CALLBACK_ID,
                        title = R.string.my_site_title_changer_dialog_title,
                        initialText = siteName,
                        hint = R.string.my_site_title_changer_dialog_hint,
                        isMultiline = false,
                        isInputEnabled = true
                )
        )
    }

    @Test
    fun `site block icon click shows change icon dialog when site has icon`() {
        site.hasCapabilityManageOptions = true
        site.hasCapabilityUploadFiles = true
        site.iconUrl = siteIcon

        invokeSiteInfoBlockAction(ICON_CLICK)

        assertThat(dialogModels.last()).isEqualTo(ChangeSiteIconDialogModel)
    }

    @Test
    fun `site block icon click shows add icon dialog when site doesn't have icon`() {
        site.hasCapabilityManageOptions = true
        site.hasCapabilityUploadFiles = true
        site.iconUrl = null

        invokeSiteInfoBlockAction(ICON_CLICK)

        assertThat(dialogModels.last()).isEqualTo(AddSiteIconDialogModel)
    }

    @Test
    fun `site block icon click shows snackbar when upload files not allowed and site doesn't have Jetpack`() {
        site.hasCapabilityManageOptions = true
        site.hasCapabilityUploadFiles = false
        site.setIsWPCom(false)

        invokeSiteInfoBlockAction(ICON_CLICK)

        assertThat(dialogModels).isEmpty()
        assertThat(snackbars).containsOnly(
                SnackbarMessageHolder(UiStringRes(R.string.my_site_icon_dialog_change_requires_jetpack_message))
        )
    }

    @Test
    fun `site block icon click shows snackbar when upload files not allowed and site has icon`() {
        site.hasCapabilityManageOptions = true
        site.hasCapabilityUploadFiles = false
        site.setIsWPCom(true)
        site.iconUrl = siteIcon

        invokeSiteInfoBlockAction(ICON_CLICK)

        assertThat(dialogModels).isEmpty()
        assertThat(snackbars).containsOnly(
                SnackbarMessageHolder(UiStringRes(R.string.my_site_icon_dialog_change_requires_permission_message))
        )
    }

    @Test
    fun `site block icon click shows snackbar when upload files not allowed and site does not have icon`() {
        site.hasCapabilityManageOptions = true
        site.hasCapabilityUploadFiles = false
        site.setIsWPCom(true)
        site.iconUrl = null

        invokeSiteInfoBlockAction(ICON_CLICK)

        assertThat(dialogModels).isEmpty()
        assertThat(snackbars).containsOnly(
                SnackbarMessageHolder(UiStringRes(R.string.my_site_icon_dialog_add_requires_permission_message))
        )
    }

    @Test
    fun `on site name chosen updates title if network available `() {
        val title = "updated site name"
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)

        viewModel.onSiteNameChosen(title)

        verify(selectedSiteRepository).updateTitle(title)
    }

    @Test
    fun `on site name chosen shows snackbar if network not available `() {
        val title = "updated site name"
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        viewModel.onSiteNameChosen(title)

        verify(selectedSiteRepository, never()).updateTitle(any())
        assertThat(snackbars).containsOnly(SnackbarMessageHolder(UiStringRes(R.string.error_update_site_title_network)))
    }

    @Test
    fun `site block url click opens site`() {
        invokeSiteInfoBlockAction(URL_CLICK)

        assertThat(navigationActions).containsOnly(OpenSite(site))
    }

    @Test
    fun `site block switch click opens site picker`() {
        invokeSiteInfoBlockAction(SWITCH_SITE_CLICK)

        assertThat(navigationActions).containsOnly(OpenSitePicker(site))
    }

    @Test
    fun `passes active UPDATE_SITE_TITLE into site info block builder`() {
        whenever(
                siteInfoBlockBuilder.buildSiteInfoBlock(
                        site = eq(site),
                        showSiteIconProgressBar = any(),
                        titleClick = any(),
                        iconClick = any(),
                        urlClick = any(),
                        switchSiteClick = any(),
                        showUpdateSiteTitleFocusPoint = eq(true),
                        showUploadSiteIconFocusPoint = eq(false)
                )
        ).thenReturn(
                SiteInfoBlock(
                        title = siteName,
                        url = siteUrl,
                        iconState = IconState.Visible(siteIcon),
                        showTitleFocusPoint = true,
                        showIconFocusPoint = false,
                        onTitleClick = null,
                        onIconClick = mock(),
                        onUrlClick = mock(),
                        onSwitchSiteClick = mock()
                )
        )

        quickStartModel.value = QuickStartModel(UPDATE_SITE_TITLE, listOf())

        assertThat(findSiteInfoBlock()!!.showTitleFocusPoint).isTrue()
    }

    @Test
    fun `passes active UPLOAD_SITE_ICON into site info block builder`() {
        whenever(
                siteInfoBlockBuilder.buildSiteInfoBlock(
                        site = eq(site),
                        showSiteIconProgressBar = any(),
                        titleClick = any(),
                        iconClick = any(),
                        urlClick = any(),
                        switchSiteClick = any(),
                        showUpdateSiteTitleFocusPoint = eq(false),
                        showUploadSiteIconFocusPoint = eq(true)
                )
        ).thenReturn(
                SiteInfoBlock(
                        title = siteName,
                        url = siteUrl,
                        iconState = IconState.Visible(siteIcon),
                        showTitleFocusPoint = false,
                        showIconFocusPoint = true,
                        onTitleClick = null,
                        onIconClick = mock(),
                        onUrlClick = mock(),
                        onSwitchSiteClick = mock()
                )
        )

        quickStartModel.value = QuickStartModel(UPLOAD_SITE_ICON, listOf())

        assertThat(findSiteInfoBlock()!!.showIconFocusPoint).isTrue()
    }

    @Test
    fun `avatar press opens me screen`() {
        viewModel.onAvatarPressed()

        assertThat(navigationActions).containsOnly(OpenMeScreen)
    }

    @Test
    fun `quick actions does not show pages button when site doesn't have the required capability`() {
        site.hasCapabilityEditPages = false

        val quickActionsBlock = findQuickActionsBlock()

        assertThat(quickActionsBlock).isNotNull
        assertThat(quickActionsBlock?.showPages).isFalse
    }

    @Test
    fun `quick action stats click opens stats screen when user is logged in and site is WPCOM`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        site.setIsWPCom(true)

        findQuickActionsBlock()?.onStatsClick?.click()

        assertThat(navigationActions).containsOnly(OpenStats(site))
    }

    @Test
    fun `quick action stats click opens stats screen when user is logged in and site is Jetpack`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        site.setIsJetpackInstalled(true)
        site.setIsJetpackConnected(true)

        findQuickActionsBlock()?.onStatsClick?.click()

        assertThat(navigationActions).containsOnly(OpenStats(site))
    }

    @Test
    fun `quick action stats click opens connect jetpack screen when user is logged in and site is self-hosted`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        site.setIsJetpackInstalled(false)
        site.setIsJetpackConnected(false)

        findQuickActionsBlock()?.onStatsClick?.click()

        assertThat(navigationActions).containsOnly(ConnectJetpackForStats(site))
    }

    @Test
    fun `quick action stats click starts login when user is not logged in and site is Jetpack`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        site.setIsJetpackInstalled(true)
        site.setIsJetpackConnected(true)

        findQuickActionsBlock()?.onStatsClick?.click()

        assertThat(navigationActions).containsOnly(StartWPComLoginForJetpackStats)
    }

    @Test
    fun `quick action stats click opens connect jetpack screen when user is not logged in and site is self-hosted`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        site.setIsJetpackInstalled(false)
        site.setIsJetpackConnected(false)

        findQuickActionsBlock()?.onStatsClick?.click()

        assertThat(navigationActions).containsOnly(ConnectJetpackForStats(site))
    }

    @Test
    fun `quick action pages click opens pages screen`() {
        findQuickActionsBlock()?.onPagesClick?.click()

        assertThat(navigationActions).containsOnly(OpenPages(site))
    }

    @Test
    fun `quick action posts click opens posts screen`() {
        findQuickActionsBlock()?.onPostsClick?.click()

        assertThat(navigationActions).containsOnly(OpenPosts(site))
    }

    @Test
    fun `quick action media click opens media screen`() {
        findQuickActionsBlock()?.onMediaClick?.click()

        assertThat(navigationActions).containsOnly(OpenMedia(site))
    }

    @Test
    fun `handling successful login result opens stats screen`() {
        viewModel.handleSuccessfulLoginResult()

        assertThat(navigationActions).containsOnly(OpenStats(site))
    }

    @Test
    fun `activity item click emits OpenActivity navigation event`() {
        invokeItemClickAction(ACTIVITY_LOG)

        assertThat(navigationActions).containsExactly(OpenActivityLog(site))
    }

    @Test
    fun `scan item click emits OpenScan navigation event`() {
        invokeItemClickAction(SCAN)

        assertThat(navigationActions).containsExactly(OpenScan(site))
    }

    @Test
    fun `plan item click emits OpenPlan navigation event`() {
        invokeItemClickAction(PLAN)

        assertThat(navigationActions).containsExactly(OpenPlan(site))
    }

    @Test
    fun `posts item click emits OpenPosts navigation event`() {
        invokeItemClickAction(POSTS)

        assertThat(navigationActions).containsExactly(OpenPosts(site))
    }

    @Test
    fun `pages item click emits OpenPages navigation event`() {
        invokeItemClickAction(PAGES)

        assertThat(navigationActions).containsExactly(OpenPages(site))
    }

    @Test
    fun `admin item click emits OpenAdmin navigation event`() {
        invokeItemClickAction(ADMIN)

        assertThat(navigationActions).containsExactly(OpenAdmin(site))
    }

    @Test
    fun `sharing item click emits OpenSharing navigation event`() {
        invokeItemClickAction(SHARING)

        assertThat(navigationActions).containsExactly(OpenSharing(site))
    }

    @Test
    fun `site settings item click emits OpenSiteSettings navigation event`() {
        invokeItemClickAction(SITE_SETTINGS)

        assertThat(navigationActions).containsExactly(OpenSiteSettings(site))
    }

    @Test
    fun `themes item click emits OpenThemes navigation event`() {
        invokeItemClickAction(THEMES)

        assertThat(navigationActions).containsExactly(OpenThemes(site))
    }

    @Test
    fun `plugins item click emits OpenPlugins navigation event`() {
        invokeItemClickAction(PLUGINS)

        assertThat(navigationActions).containsExactly(OpenPlugins(site))
    }

    @Test
    fun `media item click emits OpenMedia navigation event`() {
        invokeItemClickAction(MEDIA)

        assertThat(navigationActions).containsExactly(OpenMedia(site))
    }

    @Test
    fun `comments item click emits OpenMedia navigation event`() {
        invokeItemClickAction(COMMENTS)

        assertThat(navigationActions).containsExactly(OpenComments(site))
    }

    @Test
    fun `view site item click emits OpenSite navigation event`() {
        invokeItemClickAction(VIEW_SITE)

        assertThat(navigationActions).containsExactly(OpenSite(site))
    }

    @Test
    fun `stats item click emits OpenStats navigation event if site is WPCom and has access token`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        site.setIsWPCom(true)

        invokeItemClickAction(STATS)

        assertThat(navigationActions).containsExactly(OpenStats(site))
    }

    @Test
    fun `stats item click emits OpenStats navigation event if site is Jetpack and has access token`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        site.setIsJetpackConnected(true)
        site.setIsJetpackInstalled(true)

        invokeItemClickAction(STATS)

        assertThat(navigationActions).containsExactly(OpenStats(site))
    }

    @Test
    fun `stats item click emits StartWPComLoginForJetpackStats if site is Jetpack and doesn't have access token`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        site.setIsJetpackConnected(true)

        invokeItemClickAction(STATS)

        assertThat(navigationActions).containsExactly(StartWPComLoginForJetpackStats)
    }

    @Test
    fun `stats item click emits ConnectJetpackForStats if neither Jetpack, nor WPCom and no access token`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        site.setIsJetpackConnected(false)
        site.setIsWPCom(false)

        invokeItemClickAction(STATS)

        assertThat(navigationActions).containsExactly(ConnectJetpackForStats(site))
    }

    @Test
    fun `domain registration item click opens domain registration`() {
        isDomainCreditAvailable.postValue(true)

        findDomainRegistrationBlock()?.onClick?.click()

        verify(analyticsTrackerWrapper).track(DOMAIN_CREDIT_REDEMPTION_TAPPED, site)

        assertThat(navigationActions).containsOnly(OpenDomainRegistration(site))
    }

    @Test
    fun `correct event is tracked when domain registration item is shown`() {
        isDomainCreditAvailable.postValue(true)

        verify(analyticsTrackerWrapper).track(DOMAIN_CREDIT_PROMPT_SHOWN)
    }

    @Test
    fun `snackbar is shown and event is tracked when handling successful domain registration result without email`() {
        viewModel.handleSuccessfulDomainRegistrationResult(null)

        verify(analyticsTrackerWrapper).track(DOMAIN_CREDIT_REDEMPTION_SUCCESS)

        val message = UiStringRes(R.string.my_site_verify_your_email_without_email)

        assertThat(snackbars).containsOnly(SnackbarMessageHolder(message))
    }

    @Test
    fun `snackbar is shown and event is tracked when handling successful domain registration result with email`() {
        viewModel.handleSuccessfulDomainRegistrationResult(emailAddress)

        verify(analyticsTrackerWrapper).track(DOMAIN_CREDIT_REDEMPTION_SUCCESS)

        val message = UiStringResWithParams(R.string.my_site_verify_your_email, listOf(UiStringText(emailAddress)))

        assertThat(snackbars).containsOnly(SnackbarMessageHolder(message))
    }

    @Test
    fun `site items builder invoked with the selected site's backup screen availability`() {
        whenever(backupScreenFeatureConfig.isEnabled()).thenReturn(true)

        reset(siteItemsBuilder)

        onShowSiteIconProgressBar.value = true

        verify(siteItemsBuilder).buildSiteItems(
                site = eq(site),
                onClick = any(),
                isBackupAvailable = eq(true),
                isScanAvailable = any()
        )
    }

    @Test
    fun `scan menu item is visible, when jetpack capabilities contain JETPACK item`() = test {
        whenever(scanScreenFeatureConfig.isEnabled()).thenReturn(true)
        whenever(jetpackCapabilitiesUseCase.getOrFetchJetpackCapabilities(anyLong())).thenReturn(
                listOf(JetpackCapability.SCAN)
        )

        val updatedSite = updateSite()

        verify(siteItemsBuilder).buildSiteItems(
                site = eq(updatedSite),
                onClick = any(),
                isBackupAvailable = any(),
                isScanAvailable = eq(true)
        )
    }

    private fun updateSite(): SiteModel {
        val updatedSite = SiteModel()
        updatedSite.id = updatedSiteId
        whenever(siteStore.getSiteByLocalId(updatedSiteId)).thenReturn(updatedSite)

        viewModel.start(updatedSiteId)

        return updatedSite
    }

    @Test
    fun `add new site press is handled correctly`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        viewModel.onAddSitePressed()

        assertThat(navigationActions).containsOnly(AddNewSite(true))
    }

    private fun findQuickActionsBlock() = getLastItems().find { it is QuickActionsBlock } as QuickActionsBlock?

    private fun findDomainRegistrationBlock() =
            getLastItems().find { it is DomainRegistrationBlock } as DomainRegistrationBlock?

    private fun findSiteInfoBlock() =
            getLastItems().find { it is SiteInfoBlock } as SiteInfoBlock?

    private fun getLastItems() = uiModels.last()

    private fun invokeSiteInfoBlockAction(action: SiteInfoBlockAction) {
        val siteInfoBlock = findSiteInfoBlock()!!
        when (action) {
            TITLE_CLICK -> siteInfoBlock.onTitleClick!!
            ICON_CLICK -> siteInfoBlock.onIconClick
            URL_CLICK -> siteInfoBlock.onUrlClick
            SWITCH_SITE_CLICK -> siteInfoBlock.onSwitchSiteClick
        }.click()
    }

    private fun initSiteInfoBuilder() {
        doAnswer {
            SiteInfoBlock(
                    title = siteName,
                    url = siteUrl,
                    iconState = IconState.Visible(siteIcon),
                    showTitleFocusPoint = false,
                    showIconFocusPoint = false,
                    onTitleClick = buildSiteInfoBlockInteraction(it.getArgument(2)),
                    onIconClick = buildSiteInfoBlockInteraction(it.getArgument(3)),
                    onUrlClick = buildSiteInfoBlockInteraction(it.getArgument(4)),
                    onSwitchSiteClick = buildSiteInfoBlockInteraction(it.getArgument(5))
            )
        }.whenever(siteInfoBlockBuilder).buildSiteInfoBlock(
                site = any(),
                showSiteIconProgressBar = any(),
                titleClick = any(),
                iconClick = any(),
                urlClick = any(),
                switchSiteClick = any(),
                showUpdateSiteTitleFocusPoint = any(),
                showUploadSiteIconFocusPoint = any()
        )
    }

    private fun buildSiteInfoBlockInteraction(function: ((SiteModel) -> Unit)) =
            ListItemInteraction.create { function.invoke(site) }

    private fun invokeItemClickAction(action: ListItemAction) {
        uiModels.last().filterIsInstance<ListItem>()[ListItemAction.values().indexOf(action)].onClick.click()
    }

    private fun initSiteItems() {
        doAnswer {
            val action = it.getArgument(1) as (ListItemAction) -> Unit
            ListItemAction.values().map { listItemAction ->
                ListItem(
                        R.drawable.ic_dropdown_primary_30_24dp,
                        UiStringText("empty"),
                        null,
                        null,
                        ListItemInteraction.create { action.invoke(listItemAction) })
            }
        }.whenever(siteItemsBuilder).buildSiteItems(any(), any(), any(), any())
    }

    private enum class SiteInfoBlockAction {
        TITLE_CLICK, ICON_CLICK, URL_CLICK, SWITCH_SITE_CLICK
    }
}
