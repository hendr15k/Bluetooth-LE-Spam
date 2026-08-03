package de.simon.dankelmann.bluetoothlespam.PermissionCheck

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import de.simon.dankelmann.bluetoothlespam.Constants.Constants

class PermissionCheck() {
    companion object {

        private val _logTag = "PermissionCheck"

        /**
         * Gets a list of permissions that are relevant for the SDK level we are running on.
         */
        fun getAllRelevantPermissions(): List<String> {
            val allPermissions = mutableListOf<String>()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                allPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                allPermissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
                allPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
                allPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                allPermissions.add(Manifest.permission.BLUETOOTH)
                allPermissions.add(Manifest.permission.BLUETOOTH_ADMIN)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    allPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
                } else {
                    allPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            }

            return allPermissions
        }

        fun requestMissingPermissions(activity: Activity): Boolean {
            val missingPermissions = getAllRelevantPermissions()
                .filterNot { checkPermission(it, activity) }

            if (missingPermissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    activity,
                    missingPermissions.toTypedArray(),
                    Constants.REQUEST_CODE_MULTIPLE_PERMISSIONS
                )
            }

            return missingPermissions.isEmpty()
        }

        fun checkPermission(permission: String, context: Context): Boolean {
            val result = ContextCompat.checkSelfPermission(context, permission)
            return result == PackageManager.PERMISSION_GRANTED
        }

        fun hasAdvertisePermission(context: Context): Boolean {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Manifest.permission.BLUETOOTH_ADVERTISE
            } else {
                Manifest.permission.BLUETOOTH_ADMIN
            }
            return checkPermission(permission, context)
        }

        fun hasConnectPermission(context: Context): Boolean {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Manifest.permission.BLUETOOTH_CONNECT
            } else {
                Manifest.permission.BLUETOOTH
            }
            return checkPermission(permission, context)
        }

        fun hasScanPermission(context: Context): Boolean {
            val bluetoothPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Manifest.permission.BLUETOOTH_SCAN
            } else {
                Manifest.permission.BLUETOOTH_ADMIN
            }
            if (!checkPermission(bluetoothPermission, context)) {
                return false
            }

            return when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> true
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                    checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, context)
                else -> checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, context) ||
                    checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, context)
            }
        }
    }
}
