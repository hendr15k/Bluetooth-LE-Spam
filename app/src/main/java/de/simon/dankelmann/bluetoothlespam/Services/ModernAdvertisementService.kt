package de.simon.dankelmann.bluetoothlespam.Services

import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.util.Log
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementError
import de.simon.dankelmann.bluetoothlespam.Enums.TxPowerLevel
import de.simon.dankelmann.bluetoothlespam.Helpers.BluetoothHelpers.Companion.bluetoothAdapter
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IAdvertisementServiceCallback
import de.simon.dankelmann.bluetoothlespam.Interfaces.Services.IAdvertisementService
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.PermissionCheck.PermissionCheck

class ModernAdvertisementService(
    private val context: Context,
): IAdvertisementService{

    // private
    private val _logTag = "AdvertisementService"
    private var _advertiser: BluetoothLeAdvertiser? = null
    private var _advertisementServiceCallbacks:MutableList<IAdvertisementServiceCallback> = mutableListOf()
    private var _currentAdvertisementSet: AdvertisementSet? = null
    private var _txPowerLevel:TxPowerLevel = TxPowerLevel.TX_POWER_HIGH

    fun prepareAdvertisementSet(advertisementSet: AdvertisementSet):AdvertisementSet{
        advertisementSet.advertiseSettings.txPowerLevel = _txPowerLevel
        advertisementSet.advertisingSetParameters.txPowerLevel = _txPowerLevel
        advertisementSet.advertisingSetCallback = getAdvertisingSetCallback(advertisementSet)
        return advertisementSet
    }

    private fun dispatchStart(advertisementSet: AdvertisementSet?) {
        _advertisementServiceCallbacks.toList().forEach {
            it.onAdvertisementSetStart(advertisementSet)
        }
    }

    private fun dispatchSucceeded(advertisementSet: AdvertisementSet?) {
        _advertisementServiceCallbacks.toList().forEach {
            it.onAdvertisementSetSucceeded(advertisementSet)
        }
    }

    private fun dispatchFailed(advertisementSet: AdvertisementSet?, error: AdvertisementError) {
        _advertisementServiceCallbacks.toList().forEach {
            it.onAdvertisementSetFailed(advertisementSet, error)
        }
    }

    private fun dispatchStop(advertisementSet: AdvertisementSet?) {
        _advertisementServiceCallbacks.toList().forEach {
            it.onAdvertisementSetStop(advertisementSet)
        }
    }



    override fun startAdvertisement(advertisementSet: AdvertisementSet) {
        if (!advertisementSet.validate()) {
            Log.w(_logTag, "Advertisement Set could not be validated - data too large")
            dispatchFailed(advertisementSet, AdvertisementError.ADVERTISE_FAILED_DATA_TOO_LARGE)
            return
        }
        if (!PermissionCheck.hasAdvertisePermission(context)) {
            Log.d(_logTag, "Missing permission to execute advertisement")
            dispatchFailed(advertisementSet, AdvertisementError.ADVERTISE_FAILED_UNKNOWN)
            return
        }

        val preparedAdvertisementSet = prepareAdvertisementSet(advertisementSet)
        _currentAdvertisementSet = preparedAdvertisementSet

        try {
            val advertiser = context.bluetoothAdapter()?.bluetoothLeAdvertiser
            if (advertiser == null) {
                Log.d(_logTag, "Advertiser is null")
                _currentAdvertisementSet = null
                dispatchFailed(advertisementSet, AdvertisementError.ADVERTISE_FAILED_FEATURE_UNSUPPORTED)
                return
            }

            _advertiser = advertiser
            if(preparedAdvertisementSet.scanResponse != null){
                advertiser.startAdvertisingSet(
                    preparedAdvertisementSet.advertisingSetParameters.build(),
                    preparedAdvertisementSet.advertiseData.build(),
                    preparedAdvertisementSet.scanResponse!!.build(),
                    null, null,
                    preparedAdvertisementSet.advertisingSetCallback
                )
            } else {
                advertiser.startAdvertisingSet(
                    preparedAdvertisementSet.advertisingSetParameters.build(),
                    preparedAdvertisementSet.advertiseData.build(),
                    null, null, null,
                    preparedAdvertisementSet.advertisingSetCallback
                )
            }
            Log.d(_logTag, "Started Modern Advertisement")
        } catch (error: SecurityException) {
            Log.e(_logTag, "Unable to start advertisement", error)
            _currentAdvertisementSet = null
            dispatchFailed(advertisementSet, AdvertisementError.ADVERTISE_FAILED_UNKNOWN)
        } catch (error: IllegalStateException) {
            Log.e(_logTag, "Unable to start advertisement", error)
            _currentAdvertisementSet = null
            dispatchFailed(advertisementSet, AdvertisementError.ADVERTISE_FAILED_INTERNAL_ERROR)
        }
    }

    override fun stopAdvertisement() {
        val currentAdvertisementSet = _currentAdvertisementSet
        if (currentAdvertisementSet == null) {
            Log.d(_logTag, "Current Modern Advertising Set is null")
            return
        }

        try {
            if (PermissionCheck.hasAdvertisePermission(context) && _advertiser != null) {
                _advertiser!!.stopAdvertisingSet(currentAdvertisementSet.advertisingSetCallback)
            } else {
                Log.d(_logTag, "Unable to access advertiser while stopping")
                _currentAdvertisementSet = null
                dispatchStop(currentAdvertisementSet)
            }
        } catch (error: SecurityException) {
            Log.e(_logTag, "Unable to stop advertisement", error)
            _currentAdvertisementSet = null
            dispatchStop(currentAdvertisementSet)
        } catch (error: IllegalStateException) {
            Log.e(_logTag, "Unable to stop advertisement", error)
            _currentAdvertisementSet = null
            dispatchStop(currentAdvertisementSet)
        }
    }

    override fun setTxPowerLevel(txPowerLevel: TxPowerLevel) {
        _txPowerLevel = txPowerLevel
    }

    override fun getTxPowerLevel(): TxPowerLevel{
        return _txPowerLevel
    }

    override fun addAdvertisementServiceCallback(callback: IAdvertisementServiceCallback){
        if(!_advertisementServiceCallbacks.contains(callback)){
            _advertisementServiceCallbacks.add(callback)
        }
    }
    override fun removeAdvertisementServiceCallback(callback: IAdvertisementServiceCallback){
        if(_advertisementServiceCallbacks.contains(callback)){
            _advertisementServiceCallbacks.remove(callback)
        }
    }

    override fun isLegacyService(): Boolean {
        return false
    }

    private fun getAdvertisingSetCallback(advertisementSet: AdvertisementSet): AdvertisingSetCallback {
        return object : AdvertisingSetCallback() {
            override fun onAdvertisingSetStarted(advertisingSet: AdvertisingSet?, txPower: Int, status: Int) {
                if(status == AdvertisingSetCallback.ADVERTISE_SUCCESS){
                    Log.d(_logTag, "Advertising set started with txPower=$txPower")
                    dispatchStart(advertisementSet)
                    dispatchSucceeded(advertisementSet)
                } else {
                    // Failed to even start advertising
                    val advertisementError = when (status) {
                        AdvertisingSetCallback.ADVERTISE_FAILED_ALREADY_STARTED -> AdvertisementError.ADVERTISE_FAILED_ALREADY_STARTED
                        AdvertisingSetCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> AdvertisementError.ADVERTISE_FAILED_FEATURE_UNSUPPORTED
                        AdvertisingSetCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> AdvertisementError.ADVERTISE_FAILED_INTERNAL_ERROR
                        AdvertisingSetCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> AdvertisementError.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS
                        AdvertisingSetCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> AdvertisementError.ADVERTISE_FAILED_DATA_TOO_LARGE
                        else -> AdvertisementError.ADVERTISE_FAILED_UNKNOWN
                    }
                    Log.e(_logTag, "Failed to start advertising set: $advertisementError")
                    dispatchFailed(advertisementSet, advertisementError)
                    if (_currentAdvertisementSet === advertisementSet) {
                        _currentAdvertisementSet = null
                    }
                }
            }

            override fun onAdvertisingDataSet(advertisingSet: AdvertisingSet, status: Int) {
                if (status == AdvertisingSetCallback.ADVERTISE_SUCCESS) {
                    Log.d(_logTag, "Advertising data set confirmed")
                } else {
                    Log.e(_logTag, "Advertising data set failed with status $status")
                    dispatchFailed(advertisementSet, AdvertisementError.ADVERTISE_FAILED_DATA_TOO_LARGE)
                }
            }

            override fun onScanResponseDataSet(advertisingSet: AdvertisingSet, status: Int) {
                if (status == AdvertisingSetCallback.ADVERTISE_SUCCESS) {
                    Log.d(_logTag, "Scan response data set confirmed")
                }
                // No separate callback needed here — this just confirms the scan response
                // was accepted. The advertisement is already considered active.
            }

            override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet) {
                Log.d(_logTag, "Advertising set stopped")
                if (_currentAdvertisementSet === advertisementSet) {
                    _currentAdvertisementSet = null
                }
                dispatchStop(advertisementSet)
            }
        }
    }

}
