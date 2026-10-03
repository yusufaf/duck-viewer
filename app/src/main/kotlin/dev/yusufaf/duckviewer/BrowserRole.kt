package dev.yusufaf.duckviewer

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

class BrowserRole(context: Context) {
    private val roleManager: RoleManager? = context.getSystemService(RoleManager::class.java)

    /** False when the OEM hides the browser role; only the settings route is left then. */
    val isAvailable: Boolean get() = roleManager?.isRoleAvailable(RoleManager.ROLE_BROWSER) == true

    val isHeld: Boolean get() = roleManager?.isRoleHeld(RoleManager.ROLE_BROWSER) == true

    fun requestIntent(): Intent? = roleManager?.createRequestRoleIntent(RoleManager.ROLE_BROWSER)

    fun settingsIntent(): Intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
}
