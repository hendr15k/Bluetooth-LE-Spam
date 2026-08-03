package de.simon.dankelmann.bluetoothlespam.Helpers

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.preference.PreferenceManager
import de.simon.dankelmann.bluetoothlespam.Interfaces.Services.IAdvertisementService
import de.simon.dankelmann.bluetoothlespam.PermissionCheck.PermissionCheck
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.Services.LegacyAdvertisementService
import de.simon.dankelmann.bluetoothlespam.Services.ModernAdvertisementService

class BluetoothHelpers {

    companion object {

        fun Context.bluetoothManager(): BluetoothManager? =
            (this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)

        fun Context.bluetoothAdapter(): BluetoothAdapter? = this.bluetoothManager()?.adapter

        fun Context.isBluetooth5Supported(): Boolean {
            if (!PermissionCheck.hasConnectPermission(this)) {
                return false
            }
            val bluetoothAdapter = this.bluetoothAdapter() ?: return false
            return try {
                bluetoothAdapter.isLe2MPhySupported &&
                    bluetoothAdapter.isLeCodedPhySupported &&
                    bluetoothAdapter.isLeExtendedAdvertisingSupported &&
                    bluetoothAdapter.isLePeriodicAdvertisingSupported
            } catch (_: SecurityException) {
                false
            }
        }

        fun getAdvertisementService(context: Context): IAdvertisementService {
            var useLegacyAdvertisementService = true

            // Get from Settings, if present
            val preferences = PreferenceManager.getDefaultSharedPreferences(context).all
            val prefKey =
                context.resources.getString(R.string.preference_key_use_legacy_advertising)
            preferences.forEach {
                if (it.key == prefKey) {
                    useLegacyAdvertisementService = it.value as Boolean
                }
            }

            return when (useLegacyAdvertisementService) {
                true -> LegacyAdvertisementService(context)
                else -> {
                    ModernAdvertisementService(context)
                }
            }
        }
    }
}
