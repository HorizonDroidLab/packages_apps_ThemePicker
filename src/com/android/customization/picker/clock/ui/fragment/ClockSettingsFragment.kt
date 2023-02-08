/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.customization.picker.clock.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.lifecycle.lifecycleScope
import com.android.customization.module.ThemePickerInjector
import com.android.customization.picker.clock.ui.binder.ClockSettingsBinder
import com.android.customization.picker.clock.ui.viewmodel.ClockSettingsViewModel
import com.android.wallpaper.R
import com.android.wallpaper.model.WallpaperColorsViewModel
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.AppbarFragment
import com.android.wallpaper.picker.customization.ui.binder.ScreenPreviewBinder
import com.android.wallpaper.picker.customization.ui.viewmodel.ScreenPreviewViewModel
import com.android.wallpaper.util.PreviewUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class ClockSettingsFragment : AppbarFragment() {
    companion object {
        const val DESTINATION_ID = "clock_settings"

        @JvmStatic
        fun newInstance(): ClockSettingsFragment {
            return ClockSettingsFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view =
            inflater.inflate(
                R.layout.fragment_clock_settings,
                container,
                false,
            )
        setUpToolbar(view)

        val context = requireContext()
        val activity = requireActivity()
        val injector = InjectorProvider.getInjector() as ThemePickerInjector

        val lockScreenView: CardView = view.requireViewById(R.id.lock_preview)
        val colorViewModel = ViewModelProvider(activity)[WallpaperColorsViewModel::class.java]
        val displayUtils = injector.getDisplayUtils(context)
        ScreenPreviewBinder.bind(
                activity = activity,
                previewView = lockScreenView,
                viewModel =
                    ScreenPreviewViewModel(
                        previewUtils =
                            PreviewUtils(
                                context = context,
                                authority =
                                    resources.getString(
                                        R.string.lock_screen_preview_provider_authority,
                                    ),
                            ),
                        wallpaperInfoProvider = {
                            suspendCancellableCoroutine { continuation ->
                                injector
                                    .getCurrentWallpaperInfoFactory(context)
                                    .createCurrentWallpaperInfos(
                                        { homeWallpaper, lockWallpaper, _ ->
                                            continuation.resume(
                                                homeWallpaper ?: lockWallpaper,
                                                null,
                                            )
                                        },
                                        /* forceRefresh= */ true,
                                    )
                            }
                        },
                        onWallpaperColorChanged = { colors ->
                            colorViewModel.setLockWallpaperColors(colors)
                        },
                    ),
                lifecycleOwner = this,
                offsetToStart = displayUtils.isOnWallpaperDisplay(activity),
            )
            .show()

        lifecycleScope.launch {
            val clockRegistry =
                withContext(Dispatchers.IO) { injector.getClockRegistryProvider(context).get() }
            ClockSettingsBinder.bind(
                view,
                ClockSettingsViewModel(
                    context,
                    injector.getClockPickerInteractor(context, clockRegistry)
                ),
                this@ClockSettingsFragment,
            )
        }

        return view
    }

    override fun getDefaultTitle(): CharSequence {
        return requireContext().getString(R.string.clock_settings_title)
    }
}