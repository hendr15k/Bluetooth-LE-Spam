package de.simon.dankelmann.bluetoothlespam.Services

import android.Manifest
import android.bluetooth.BluetoothAdapter
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
    private var _bluetoothAdapter: BluetoothAdapter? = null
    private var _advertiser: BluetoothLeAdvertiser? = null
    private var _advertisementServiceCallbacks:MutableList<IAdvertisementServiceCallback> = mutableListOf()
    private var _currentAdvertisementSet: AdvertisementSet? = null
    private var _txPowerLevel:TxPowerLevel = TxPowerLevel.TX_POWER_HIGH

    init {
        _bluetoothAdapter = context.bluetoothAdapter()
        if(_bluetoothAdapter != null){
            _advertiser = _bluetoothAdapter!!.bluetoothLeAdvertiser
        }
    }

    fun prepareAdvertisementSet(advertisementSet: AdvertisementSet):AdvertisementSet{
        advertisementSet.advertiseSettings.txPowerLevel = _txPowerLevel
        advertisementSet.advertisingSetParameters.txPowerLevel = _txPowerLevel
        advertisementSet.advertisingSetCallback = getAdvertisingSetCallback()
        return advertisementSet
    }

    private fun dispatchStart(advertisementSet: AdvertisementSet?) {
        _advertisementServiceCallbacks.forEach {
            it.onAdvertisementSetStart(advertisementSet)
        }
    }

    private fun dispatchSucceeded(advertisementSet: AdvertisementSet?) {
        _advertisementServiceCallbacks.forEach {
            it.onAdvertisementSetSucceeded(advertisementSet)
        }
    }

    private fun dispatchFailed(advertisementSet: AdvertisementSet?, error: AdvertisementError) {
        _advertisementServiceCallbacks.forEach {
            it.onAdvertisementSetFailed(advertisementSet, error)
        }
    }

    private fun dispatchStop(advertisementSet: AdvertisementSet?) {
        _advertisementServiceCallbacks.forEach {
            it.onAdvertisementSetStop(advertisementSet)
        }
    }



    override fun startAdvertisement(advertisementSet: AdvertisementSet) {
        if(_advertiser != null){
            if(advertisementSet.validate()){
                if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_ADVERTISE, context)){
                    val preparedAdvertisementSet = prepareAdvertisementSet(advertisementSet)
                    if(preparedAdvertisementSet.scanResponse != null){
                        _advertiser!!.startAdvertisingSet(
                            preparedAdvertisementSet.advertisingSetParameters.build(),
                            preparedAdvertisementSet.advertiseData.build(),
                            preparedAdvertisementSet.scanResponse!!.build(),
                            null, null,
                            preparedAdvertisementSet.advertisingSetCallback
                        )
                    } else {
                        _advertiser!!.startAdvertisingSet(
                            preparedAdvertisementSet.advertisingSetParameters.build(),
                            preparedAdvertisementSet.advertiseData.build(),
                            null, null, null,
                            preparedAdvertisementSet.advertisingSetCallback
                        )
                    }
                    _currentAdvertisementSet = preparedAdvertisementSet
                    // Do NOT call onAdvertisementSetStart here — the BLE stack has not
                    // confirmed anything yet. The callback fires from onAdvertisingSetStarted.
                    Log.d(_logTag, "Started Modern Advertisement")
                } else {
                    Log.d(_logTag, "Missing permission to execute advertisement")
                    dispatchFailed(advertisementSet, AdvertisementError.ADVERTISE_FAILED_FEATURE_UNSUPPORTED)
                }
            } else {
                Log.d(_logTag, "Advertisement Set could not be validated")
                dispatchFailed(advertisementSet, AdvertisementError.ADVERTISE_FAILED_DATA_TOO_LARGE)
            }
        } else {
            Log.d(_logTag, "Advertiser is null")
            dispatchFailed(advertisementSet, AdvertisementError.ADVERTISE_FAILED_FEATURE_UNSUPPORTED)
        }
    }

    override fun stopAdvertisement() {
        if(_advertiser != null){
            if (_currentAdvertisementSet != null) {
                if (PermissionCheck.checkPermission(
                        Manifest.permission.BLUETOOTH_ADVERTISE, context
                    )
                ) {
                    _advertiser!!.stopAdvertisingSet(_currentAdvertisementSet!!.advertisingSetCallback)
                    _currentAdvertisementSet = null
                } else {
                    Log.d(_logTag, "Missing permission to stop advertisement")
                }
            } else {
                Log.d(_logTag, "Current Modern Advertising Set is null")
            }
        } else {
            Log.d(_logTag, "Advertiser is null")
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

    private fun getAdvertisingSetCallback(): AdvertisingSetCallback {
        return object : AdvertisingSetCallback() {
            override fun onAdvertisingSetStarted(advertisingSet: AdvertisingSet?, txPower: Int, status: Int) {
                if(status == AdvertisingSetCallback.ADVERTISE_SUCCESS){
                    // Advertising started successfully — notify that the set has started.
                    // This is the correct place for onAdvertisementSetStart (not in startAdvertisement()).
                    Log.d(_logTag, "Advertising set started with txPower=$txPower")
                    dispatchStart(_currentAdvertisementSet)
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
                    dispatchFailed(_currentAdvertisementSet, advertisementError)
                }
            }

            override fun onAdvertisingDataSet(advertisingSet: AdvertisingSet, status: Int) {
                if (status == AdvertisingSetCallback.ADVERTISE_SUCCESS) {
                    // Advertising data confirmed by the controller — the advertisement is truly
                    // active now. This is the right moment to mark it as succeeded.
                    Log.d(_logTag, "Advertising data set confirmed")
                    dispatchSucceeded(_currentAdvertisementSet)
                } else {
                    Log.e(_logTag, "Advertising data set failed with status $status")
                    dispatchFailed(_currentAdvertisementSet, AdvertisementError.ADVERTISE_FAILED_DATA_TOO_LARGE)
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
                dispatchStop(_currentAdvertisementSet)
            }
        }
    }

}