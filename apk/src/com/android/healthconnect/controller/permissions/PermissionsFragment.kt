package com.android.healthconnect.controller.permissions

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.SwitchPreference
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.categories.fromHealthPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermissionStrings
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.permissions.requestpermissions.RequestPermissionHeaderPreference
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.children
import com.android.settingslib.widget.MainSwitchPreference
import com.android.settingslib.widget.OnMainSwitchChangeListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Fragment for displaying permission switches. */
@AndroidEntryPoint(PreferenceFragmentCompat::class)
class PermissionsFragment : Hilt_PermissionsFragment() {

    companion object {
        private const val ALLOW_ALL_PREFERENCE = "allow_all_preference"
        private const val READ_CATEGORY = "read_permission_category"
        private const val WRITE_CATEGORY = "write_permission_category"
        private const val HEADER = "request_permissions_header"
    }

    private val viewModel: RequestPermissionViewModel by activityViewModels()
    @Inject lateinit var healthPermissionReader: HealthPermissionReader

    private val header: RequestPermissionHeaderPreference? by lazy {
        preferenceScreen.findPreference(HEADER)
    }

    private val allowAllPreference: MainSwitchPreference? by lazy {
        preferenceScreen.findPreference(ALLOW_ALL_PREFERENCE)
    }

    private val mReadPermissionCategory: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(READ_CATEGORY)
    }

    private val mWritePermissionCategory: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(WRITE_CATEGORY)
    }

    private val onSwitchChangeListener: OnMainSwitchChangeListener =
        OnMainSwitchChangeListener { _, grant ->
            mReadPermissionCategory?.children?.forEach { preference ->
                (preference as SwitchPreference).isChecked = grant
            }
            mWritePermissionCategory?.children?.forEach { preference ->
                (preference as SwitchPreference).isChecked = grant
            }
            viewModel.updatePermissions(grant)
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.permissions_screen, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.appMetadata.observe(viewLifecycleOwner) { app ->
            header?.bind(app.appName) {
                val startRationaleIntent =
                    healthPermissionReader.getApplicationRationaleIntent(app.packageName)
                startActivity(startRationaleIntent)
            }
            mReadPermissionCategory?.title = getString(R.string.read_permission_category, app.appName)
            mWritePermissionCategory?.title = getString(R.string.write_permission_category, app.appName)
        }
        viewModel.permissionsList.observe(viewLifecycleOwner) { permissions ->
            updateDataList(permissions)
            setupAllowAll()
        }
    }

    private fun setupAllowAll() {
        viewModel.allPermissionsGranted.observe(viewLifecycleOwner) { allPermissionsGranted ->
            // does not trigger removing/enabling all permissions
            allowAllPreference?.removeOnSwitchChangeListener(onSwitchChangeListener)
            allowAllPreference?.isChecked = allPermissionsGranted
            allowAllPreference?.addOnSwitchChangeListener(onSwitchChangeListener)
        }
        allowAllPreference?.addOnSwitchChangeListener(onSwitchChangeListener)
    }

    private fun updateDataList(permissionsList: List<HealthPermission>) {
        mReadPermissionCategory?.removeAll()
        mWritePermissionCategory?.removeAll()

        permissionsList.forEach { permission ->
            val value = viewModel.isPermissionGranted(permission)
            if (PermissionsAccessType.READ == permission.permissionsAccessType) {
                mReadPermissionCategory?.addPreference(getPermissionPreference(value, permission))
            } else if (PermissionsAccessType.WRITE == permission.permissionsAccessType) {
                mWritePermissionCategory?.addPreference(getPermissionPreference(value, permission))
            }
        }

        if (mReadPermissionCategory?.preferenceCount == 0) {
            mReadPermissionCategory?.isVisible = false
        }
        if (mWritePermissionCategory?.preferenceCount == 0) {
            mWritePermissionCategory?.isVisible = false
        }
    }

    private fun getPermissionPreference(
        defaultValue: Boolean,
        permission: HealthPermission
    ): Preference {
        return SwitchPreference(requireContext()).also {
            val healthCategory = fromHealthPermissionType(permission.healthPermissionType)
            it.setIcon(healthCategory.icon)
            it.setDefaultValue(defaultValue)
            it.setTitle(
                HealthPermissionStrings.fromPermissionType(permission.healthPermissionType)
                    .uppercaseLabel)
            it.setOnPreferenceChangeListener { _, newValue ->
                viewModel.updatePermission(permission, newValue as Boolean)
                true
            }
        }
    }
}
