package org.wordpress.android.ui.bloggingprompts.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.firstOrNull
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.DismissDialog
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenEditor
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenRemindersIntro
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenSitePicker
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingDialogFragment.DialogType
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingDialogFragment.DialogType.INFORMATION
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingDialogFragment.DialogType.ONBOARDING
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.viewmodel.ScopedViewModel
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

class BloggingPromptsOnboardingViewModel @Inject constructor(
    private val siteStore: SiteStore,
    private val uiStateMapper: BloggingPromptsOnboardingUiStateMapper,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val bloggingPromptsStore: BloggingPromptsStore,
    private val analyticsTracker: BloggingPromptsOnboardingAnalyticsTracker,
    @Named(BG_THREAD) val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(bgDispatcher) {
    private val _uiState = MutableLiveData<BloggingPromptsOnboardingUiState>()
    val uiState: LiveData<BloggingPromptsOnboardingUiState> = _uiState

    private val _action = MutableLiveData<BloggingPromptsOnboardingAction>()
    val action: LiveData<BloggingPromptsOnboardingAction> = _action

    private lateinit var dialogType: DialogType
    private var hasTrackedScreenShown = false
    private var site: SiteModel? = null

    fun start(type: DialogType) {
        if (!hasTrackedScreenShown) {
            hasTrackedScreenShown = true
            analyticsTracker.trackScreenShown()
        }
        dialogType = type

        site = selectedSiteRepository.getSelectedSite()

        _uiState.value = uiStateMapper.mapReady(dialogType, ::onPrimaryButtonClick, ::onSecondaryButtonClick)
    }

    private fun onPrimaryButtonClick() = launch {
        val action = when (dialogType) {
            ONBOARDING -> {
                analyticsTracker.trackTryItNowClicked()
                val bloggingPrompt = bloggingPromptsStore.getPromptForDate(site!!, Date()).firstOrNull()?.model
                OpenEditor(bloggingPrompt?.id ?: -1)
            }
            INFORMATION -> {
                analyticsTracker.trackGotItClicked()
                DismissDialog
            }
        }
        _action.postValue(action)
    }

    private fun onSecondaryButtonClick() {
        analyticsTracker.trackRemindMeClicked()
        if (siteStore.sitesCount > 1) {
            _action.value = OpenSitePicker(selectedSiteRepository.getSelectedSite())
        } else {
            siteStore.sites.firstOrNull()?.let {
                _action.value = OpenRemindersIntro(it.id)
            }
        }
    }

    fun onSiteSelected(selectedSiteLocalId: Int) {
        siteStore.getSiteByLocalId(selectedSiteLocalId)?.let { selectedSiteModel ->
            selectedSiteRepository.updateSite(selectedSiteModel)
        }
        _action.value = OpenRemindersIntro(selectedSiteLocalId)
    }
}
