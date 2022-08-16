package org.wordpress.android.ui.mysite.jetpackbadge

import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.databinding.JetpackPoweredBottomSheetBinding
import org.wordpress.android.databinding.JetpackPoweredExpandedBottomSheetBinding
import org.wordpress.android.ui.ActivityLauncherWrapper
import org.wordpress.android.ui.ActivityLauncherWrapper.Companion.JETPACK_PACKAGE_NAME
import org.wordpress.android.ui.main.WPMainNavigationView.PageType
import org.wordpress.android.ui.main.WPMainNavigationView.PageType.MY_SITE
import org.wordpress.android.ui.main.WPMainNavigationView.PageType.NOTIFS
import org.wordpress.android.ui.main.WPMainNavigationView.PageType.READER
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredDialogAction.DismissDialog
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredDialogAction.OpenPlayStore
import org.wordpress.android.util.extensions.exhaustive
import javax.inject.Inject

@AndroidEntryPoint
class JetpackPoweredBottomSheetFragment : BottomSheetDialogFragment() {
    @Inject lateinit var activityLauncherWrapper: ActivityLauncherWrapper
    private val viewModel: JetpackPoweredDialogViewModel by viewModels()
    private var fullScreen: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fullScreen = arguments?.getBoolean(KEY_FULL_SCREEN, false) ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout = if (fullScreen) {
            R.layout.jetpack_powered_expanded_bottom_sheet
        } else {
            R.layout.jetpack_powered_bottom_sheet
        }

        return inflater.inflate(layout, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (fullScreen) {
            with(JetpackPoweredExpandedBottomSheetBinding.bind(view)) {
                when (arguments?.getSerializable(KEY_SITE_SCREEN) as? PageType ?: MY_SITE) {
                    MY_SITE -> {
                        title.text = getString(R.string.wp_jetpack_powered_stats_powered_by_jetpack)
                        caption.text = getString(R.string.wp_jetpack_powered_stats_powered_by_jetpack_caption)
                        secondaryButton.text = getString(R.string.wp_jetpack_continue_to_stats)
                    }
                    READER -> {
                        title.text = getString(R.string.wp_jetpack_powered_reader_powered_by_jetpack)
                        caption.text = getString(R.string.wp_jetpack_powered_reader_powered_by_jetpack_caption)
                        secondaryButton.text = getString(R.string.wp_jetpack_continue_to_reader)
                    }
                    NOTIFS -> {
                        title.text = getString(R.string.wp_jetpack_powered_notifications_powered_by_jetpack)
                        caption.text = getString(R.string.wp_jetpack_powered_notifications_powered_by_jetpack_caption)
                        secondaryButton.text = getString(R.string.wp_jetpack_continue_to_notifications)
                    }
                }
                primaryButton.setOnClickListener { viewModel.openJetpackAppDownloadLink() }
                secondaryButton.setOnClickListener { viewModel.dismissBottomSheet() }
            }
        } else {
            with(JetpackPoweredBottomSheetBinding.bind(view)) {
                primaryButton.setOnClickListener { viewModel.openJetpackAppDownloadLink() }
            }
        }

        (dialog as? BottomSheetDialog)?.apply {
            setOnShowListener {
                val bottomSheet: FrameLayout = dialog?.findViewById(
                        com.google.android.material.R.id.design_bottom_sheet
                ) ?: return@setOnShowListener
                val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
                if (fullScreen && bottomSheet.layoutParams != null) {
                    showFullScreenBottomSheet(bottomSheet)
                }
                expandBottomSheet(bottomSheetBehavior)
            }
        }

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.action.observe(viewLifecycleOwner) { action ->
            when (action) {
                is OpenPlayStore -> {
                    dismiss()
                    activity?.let {
                       activityLauncherWrapper.openPlayStoreLink(it, JETPACK_PACKAGE_NAME)
                    }
                }
                is DismissDialog -> {
                    dismiss()
                }
            }.exhaustive
        }
    }

    private fun showFullScreenBottomSheet(bottomSheet: FrameLayout) {
        val layoutParams = bottomSheet.layoutParams
        layoutParams.height = Resources.getSystem().displayMetrics.heightPixels
        bottomSheet.layoutParams = layoutParams
    }

    private fun expandBottomSheet(bottomSheetBehavior: BottomSheetBehavior<FrameLayout>) {
        bottomSheetBehavior.skipCollapsed = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    companion object {
        const val TAG = "JETPACK_POWERED_BOTTOM_SHEET_FRAGMENT"
        private const val KEY_FULL_SCREEN = "KEY_FULL_SCREEN"
        private const val KEY_SITE_SCREEN = "KEY_SITE_SCREEN"

        @JvmStatic
        fun newInstance(
            fullScreen: Boolean = false,
            pageType: PageType = MY_SITE
        ) = JetpackPoweredBottomSheetFragment().apply {
            arguments = Bundle().apply {
                putBoolean(KEY_FULL_SCREEN, fullScreen)
                putSerializable(KEY_SITE_SCREEN, pageType)
            }
        }
    }
}
