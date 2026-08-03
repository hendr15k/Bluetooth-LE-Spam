package de.simon.dankelmann.bluetoothlespam.Handlers

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.ContinuityActionModalAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.ContinuityIos17CrashAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.ContinuityNewAirtagPopUpAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.ContinuityNewDevicePopUpAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.ContinuityNotYourDevicePopUpAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementError
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementQueueMode
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetType
import de.simon.dankelmann.bluetoothlespam.Helpers.QueueHandlerHelpers
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IAdvertisementServiceCallback
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IAdvertisementSetQueueHandlerCallback
import de.simon.dankelmann.bluetoothlespam.Interfaces.Services.IAdvertisementService
import de.simon.dankelmann.bluetoothlespam.Models.AdvertiseData
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetCollection
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetList
import de.simon.dankelmann.bluetoothlespam.Services.AdvertisementForegroundService
import de.simon.dankelmann.bluetoothlespam.R
import kotlin.random.Random

/**
 * Handler that takes an advertisement set, and iterates over the set according to a given AdvertisementQueueMode.
 *
 * The job of this handler is to select the next set, and provide it to the IAdvertisementService.
 *
 * The UI code should drive the advertising via this handler (via start, stop, set advertisement set, set queue mode).
 * This handler takes care of starting and stopping services as appropriate.
 */
class AdvertisementSetQueueHandler(
    context: Context,
    adService: IAdvertisementService,
) : IAdvertisementServiceCallback {

    private var _logTag = "AdvertisementSetQueueHandler"
    private val _context = context.applicationContext
    private val _handler = Handler(Looper.getMainLooper())
    private var _pendingAdvance: Runnable? = null

    private var _advertisementService: IAdvertisementService = adService
    private var _pendingAdvertisementService: IAdvertisementService? = null

    private var _advertisementQueueMode: AdvertisementQueueMode = AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_LINEAR
    private var _advertisementSetCollection: AdvertisementSetCollection =
        AdvertisementSetCollection()
    private var _intervalMillis: Long = QueueHandlerHelpers.getInterval(context)

    // Callbacks to listen to events of the underlying advertisement service
    private var _advertisementServiceCallbacks:MutableList<IAdvertisementServiceCallback> = mutableListOf()
    // Callbacks to listen to queue events
    private var _advertisementQueueHandlerCallbacks:MutableList<IAdvertisementSetQueueHandlerCallback> = mutableListOf()

    private var _active = false
    // Tracks how many consecutive advertisement sets have been skipped because
    // their payload exceeded the 31-byte legacy limit. When this reaches the
    // number of checked sets, every checked set is unadvertisable and we must
    // stop instead of looping (and overflowing the stack).
    private var _consecutiveValidationSkips = 0
    private var _currentAdvertisementSet: AdvertisementSet? = null
    private var _currentAdvertisementSetListIndex = 0
    private var _currentAdvertisementSetIndex = 0
    private var _selectedAdvertisementPending = false
    private var _pendingAdvertisementSetListIndex = 0
    private var _pendingAdvertisementSetIndex = 0
    private var _awaitingModernStop = false
    private var _stoppingAdvertisementSet: AdvertisementSet? = null
    private var _activationPending = false
    private var _advertisementInFlight = false

    init {
        _advertisementService.addAdvertisementServiceCallback(this)
    }

    fun isActive(): Boolean {
        return _active
    }

    fun setAdvertisementQueueMode(advertisementQueueMode: AdvertisementQueueMode){
        _advertisementQueueMode = advertisementQueueMode
    }

    fun getAdvertisementQueueMode():AdvertisementQueueMode{
        return _advertisementQueueMode
    }

    fun setInterval(milliseconds: Long) {
        if (milliseconds > 0) {
            _intervalMillis = milliseconds
        }
    }

    fun setAdvertisementService(advertisementService: IAdvertisementService) {
        if (
            _advertisementService === advertisementService &&
            _pendingAdvertisementService == null
        ) {
            return
        }

        cancelPendingAdvance()
        _pendingAdvertisementService = advertisementService

        if (_active || _advertisementInFlight) {
            deactivate(_context)
        }
        if (!_advertisementInFlight && !_awaitingModernStop) {
            applyPendingAdvertisementService()
        }
    }


    fun setSelectedAdvertisementSet(advertisementSetListIndex: Int, advertisementSetIndex: Int){
        val advertisementSet = _advertisementSetCollection.advertisementSetLists
            .getOrNull(advertisementSetListIndex)
            ?.advertisementSets
            ?.getOrNull(advertisementSetIndex)
        if (advertisementSet != null) {
            _pendingAdvertisementSetListIndex = advertisementSetListIndex
            _pendingAdvertisementSetIndex = advertisementSetIndex
            _selectedAdvertisementPending = true
        }
    }

    fun setAdvertisementSetCollection(advertisementSetCollection: AdvertisementSetCollection){
        if (_active) {
            deactivate(_context)
        }
        if(_advertisementSetCollection != advertisementSetCollection){
            _advertisementSetCollection = advertisementSetCollection
        }

        // Reset indices
        _currentAdvertisementSet= null
        _currentAdvertisementSetListIndex = 0
        _currentAdvertisementSetIndex = 0
        _selectedAdvertisementPending = false
        _pendingAdvertisementSetListIndex = 0
        _pendingAdvertisementSetIndex = 0
    }

    fun getAdvertisementSetCollection(): AdvertisementSetCollection{
        return _advertisementSetCollection
    }

    // Add / Remove AdvertisementSetCollections
    fun clearAdvertisementSetCollection(){
        _advertisementSetCollection.advertisementSetLists.clear()
    }
    fun addAdvertisementSetList(advertisementSetList: AdvertisementSetList){
        if(!_advertisementSetCollection.advertisementSetLists.contains(advertisementSetList)){
            _advertisementSetCollection.advertisementSetLists.add(advertisementSetList)
        }
    }

    fun removeAdvertisementSetList(advertisementSetList: AdvertisementSetList){
        if(_advertisementSetCollection.advertisementSetLists.contains(advertisementSetList)){
            _advertisementSetCollection.advertisementSetLists.remove(advertisementSetList)
        }
    }

    // Add / Remove Callbacks
    fun addAdvertisementServiceCallback(callback: IAdvertisementServiceCallback){
        if(!_advertisementServiceCallbacks.contains(callback)){
            _advertisementServiceCallbacks.add(callback)
        }
    }
    fun removeAdvertisementServiceCallback(callback: IAdvertisementServiceCallback){
        if(_advertisementServiceCallbacks.contains(callback)){
            _advertisementServiceCallbacks.remove(callback)
        }
    }

    fun addAdvertisementQueueHandlerCallback(callback: IAdvertisementSetQueueHandlerCallback){
        if(!_advertisementQueueHandlerCallbacks.contains(callback)){
            _advertisementQueueHandlerCallbacks.add(callback)
        }
    }
    fun removeAdvertisementQueueHandlerCallback(callback: IAdvertisementSetQueueHandlerCallback){
        if(_advertisementQueueHandlerCallbacks.contains(callback)){
            _advertisementQueueHandlerCallbacks.remove(callback)
        }
    }

    fun hasCheckedItems(): Boolean {
        // Check if any advertisement set is checked
        for (list in _advertisementSetCollection.advertisementSetLists) {
            for (set in list.advertisementSets) {
                if (set.isChecked) {
                    return true
                }
            }
        }
        return false
    }

    fun toggle(context: Context) {
        if (_active) {
            deactivate(context)
        } else {
            activate(context)
        }
    }

    fun activate(context: Context) {
        if (_active) {
            return
        }
        if (_awaitingModernStop || _pendingAdvertisementService != null) {
            _activationPending = true
            return
        }

        // Cannot activate anything if nothing is selected
        if (!hasCheckedItems()) {
            Toast.makeText(context, R.string.toast_no_items_selected, Toast.LENGTH_SHORT).show()
            return
        }

        _active = true
        _activationPending = false
        _consecutiveValidationSkips = 0
        cancelPendingAdvance()
        AdvertisementForegroundService.startService(context)
        _advertisementQueueHandlerCallbacks.forEach { it ->
            try {
                it.onQueueHandlerActivated()
            } catch (e: Exception) {
                Log.e(_logTag, "Failed to call onQueueHandlerActivated: ${e.message}")
            }
        }
        advertiseNextAdvertisementSet()
    }

    fun deactivate(context: Context, stopService: Boolean = false) {
        val wasActive = _active
        _active = false
        _activationPending = false
        cancelPendingAdvance()

        markModernAdvertisementAsStopping()
        _advertisementService.stopAdvertisement()

        if (stopService) {
            Log.d(_logTag, "Stopping Foreground Service")
            AdvertisementForegroundService.stopService(context)
        }

        if (wasActive) {
            _advertisementQueueHandlerCallbacks.forEach { it ->
                try {
                    it.onQueueHandlerDeactivated()
                } catch (e: Exception) {
                    Log.e(_logTag, "Failed to call onQueueHandlerDeactivated: ${e.message}")
                }
            }
        }
    }

    private fun advertiseNextAdvertisementSet() {
         if (!_active) {
             return
         }

         selectNextAdvertisementSet()

         val nextSet = _currentAdvertisementSet
         if (nextSet == null) {
             Log.e(_logTag, "Current Advertisement Set is null.")
             deactivate(_context, true)
             return
         }

         // Only advertise if the set is checked
         if (nextSet.isChecked) {
             val preparedSet = prepareAdvertisementSet(nextSet)
             // Validate data size before attempting to advertise
             if (preparedSet.validate()) {
                 _consecutiveValidationSkips = 0
                 _advertisementInFlight = true
                 _advertisementService.startAdvertisement(preparedSet)
             } else {
                 _consecutiveValidationSkips += 1
                 val checkedSetCount = _advertisementSetCollection.advertisementSetLists
                     .sumOf { list -> list.advertisementSets.count { it.isChecked } }
                 if (checkedSetCount == 0 || _consecutiveValidationSkips >= checkedSetCount) {
                     Log.w(_logTag, "All checked advertisement sets failed validation; deactivating queue.")
                     _consecutiveValidationSkips = 0
                     deactivate(_context, true)
                     return
                 }
                 Log.w(_logTag, "Skipping advertisement set '${preparedSet.title}' - data exceeds ${AdvertiseData.MAX_LEGACY_ADVERTISING_DATA_SIZE} byte limit")
                 advertiseNextAdvertisementSet()
             }
         } else {
             // A manually selected unchecked item should not block the queue.
             Log.d(_logTag, "Skipping unchecked advertisement set: ${nextSet.title}")
             advertiseNextAdvertisementSet()
         }
    }

    private fun prepareAdvertisementSet(advertisementSet: AdvertisementSet): AdvertisementSet {
        return when (advertisementSet.type) {
            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NEW_DEVICE -> ContinuityNewDevicePopUpAdvertisementSetGenerator.prepareAdvertisementSet(advertisementSet)
            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NEW_AIRTAG -> ContinuityNewAirtagPopUpAdvertisementSetGenerator.prepareAdvertisementSet(advertisementSet)
            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NOT_YOUR_DEVICE -> ContinuityNotYourDevicePopUpAdvertisementSetGenerator.prepareAdvertisementSet(advertisementSet)
            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_ACTION_MODALS -> ContinuityActionModalAdvertisementSetGenerator.prepareAdvertisementSet(advertisementSet)
            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_IOS_17_CRASH -> ContinuityIos17CrashAdvertisementSetGenerator.prepareAdvertisementSet(advertisementSet)
            else -> advertisementSet
        }
    }

    /**
     * Select the AdvertisementSet that should be advertised next.
     *
     * Precondition: at least one set is checked by the user.
     * The case of nothing being checked should be handled earlier.
     * If nothing is checked, this function will do nothing.
     */
    private fun selectNextAdvertisementSet() {
        // Explicit returns are used for clarity

        if (_selectedAdvertisementPending) {
            _selectedAdvertisementPending = false
            val selectedAdvertisementSet = _advertisementSetCollection.advertisementSetLists
                .getOrNull(_pendingAdvertisementSetListIndex)
                ?.advertisementSets
                ?.getOrNull(_pendingAdvertisementSetIndex)
            if (selectedAdvertisementSet != null) {
                _currentAdvertisementSetListIndex = _pendingAdvertisementSetListIndex
                _currentAdvertisementSetIndex = _pendingAdvertisementSetIndex
                _currentAdvertisementSet = selectedAdvertisementSet
                return
            }
        }

        when (_advertisementQueueMode) {
            AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_LINEAR -> {
                // If no AdvertisementSet is currently selected, make sure to start at the beginning
                val hasCurrentAdvertisementSet = _currentAdvertisementSet != null
                if (!hasCurrentAdvertisementSet) {
                    _currentAdvertisementSetListIndex = 0
                    _currentAdvertisementSetIndex = 0
                }

                val selectedList = _advertisementSetCollection.advertisementSetLists
                    .getOrNull(_currentAdvertisementSetListIndex)
                if (selectedList == null) {
                    _currentAdvertisementSet = null
                    return
                }
                Log.d(
                    _logTag,
                    "List: ${selectedList.title}, SETS: ${selectedList.advertisementSets.count()}, CurrentIndex: $_currentAdvertisementSetIndex"
                )

                // Find the next checked item in the current list
                val firstSetIndex = if (hasCurrentAdvertisementSet) {
                    _currentAdvertisementSetIndex + 1
                } else {
                    0
                }
                for (i in firstSetIndex until selectedList.advertisementSets.size) {
                    if (selectedList.advertisementSets[i].isChecked) {
                        // _currentAdvertisementSetListIndex is unchanged
                        _currentAdvertisementSetIndex = i
                        _currentAdvertisementSet = selectedList.advertisementSets[i]
                        return
                    }
                }

                // If we didn't find a checked item in the current list, move to the next list
                // Find the next list with checked items
                val startListIndex = _currentAdvertisementSetListIndex
                val numberOfLists = _advertisementSetCollection.advertisementSetLists.size

                // Loop through lists starting from the next one
                for (listOffset in 1..numberOfLists) {
                    val listIndex = (startListIndex + listOffset) % numberOfLists
                    val list = _advertisementSetCollection.advertisementSetLists[listIndex]

                    // Find the first checked item in this list
                    val firstCheckedIndex = list.advertisementSets.indexOfFirst { it.isChecked }
                    if (firstCheckedIndex >= 0) {
                        _currentAdvertisementSetListIndex = listIndex
                        _currentAdvertisementSetIndex = firstCheckedIndex
                        _currentAdvertisementSet = list.advertisementSets[firstCheckedIndex]
                        return
                    }
                }

                // No checked set found in any list — deactivating will stop the loop safely.
                _currentAdvertisementSet = null
                Log.w(_logTag, "No checked advertisement sets found; deactivating queue.")
                return
            }

            AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_RANDOM -> {
                // Create a list of all checked advertisement sets across all lists
                // TODO: Cache this, don't recompute it all the time?
                val checkedSets = mutableListOf<Triple<Int, Int, AdvertisementSet>>()

                _advertisementSetCollection.advertisementSetLists.forEachIndexed { listIndex, list ->
                    list.advertisementSets.forEachIndexed { setIndex, set ->
                        if (set.isChecked) {
                            checkedSets.add(Triple(listIndex, setIndex, set))
                        }
                    }
                }

                // If we have checked items, randomly select one of them
                if (checkedSets.isNotEmpty()) {
                    val randomIndex = Random.nextInt(checkedSets.size)
                    val selected = checkedSets[randomIndex]
                    _currentAdvertisementSetListIndex = selected.first
                    _currentAdvertisementSetIndex = selected.second
                    _currentAdvertisementSet = selected.third
                } else {
                    // If no checked items, do nothing
                }
            }
        }
    }

    private fun onAdvertisementSucceeded() {
        if (!_active) {
            return
        }

        markModernAdvertisementAsStopping()
        _advertisementService.stopAdvertisement()

        if (_advertisementService.isLegacyService()) {
            advertiseNextAdvertisementSet()
        } else {
            // Wait for the Stop Advertising Callback
        }
    }

    private fun onAdvertisementFailed() {
        Log.d(_logTag, "Advertisement failed, trying again")
        if (_active) {
            advertiseNextAdvertisementSet()
        }
    }

    private fun runLocalCallback(success:Boolean){
        if (!_active) {
            return
        }

        cancelPendingAdvance()
        val advance = Runnable {
            _pendingAdvance = null
            if (_active) {
                if(success){
                    onAdvertisementSucceeded()
                } else {
                    onAdvertisementFailed()
                }
            }
        }
        _pendingAdvance = advance
        _handler.postDelayed(advance, _intervalMillis)
    }

    private fun cancelPendingAdvance() {
        _pendingAdvance?.let(_handler::removeCallbacks)
        _pendingAdvance = null
    }

    private fun markModernAdvertisementAsStopping() {
        if (
            !_advertisementService.isLegacyService() &&
            _advertisementInFlight &&
            _currentAdvertisementSet != null
        ) {
            _awaitingModernStop = true
            _stoppingAdvertisementSet = _currentAdvertisementSet
        }
    }

    private fun applyPendingAdvertisementService(): Boolean {
        val advertisementService = _pendingAdvertisementService ?: return false
        _pendingAdvertisementService = null
        _advertisementService.removeAdvertisementServiceCallback(this)
        _advertisementService = advertisementService
        _advertisementService.addAdvertisementServiceCallback(this)
        _awaitingModernStop = false
        _stoppingAdvertisementSet = null
        _advertisementInFlight = false
        return true
    }

    private fun resumeAfterTerminalCallback() {
        applyPendingAdvertisementService()
        when {
            _active -> advertiseNextAdvertisementSet()
            _activationPending -> {
                _activationPending = false
                activate(_context)
            }
        }
    }

    // Callback Implementation, just pass to own Listeners
    override fun onAdvertisementSetStart(advertisementSet: AdvertisementSet?) {
        if (advertisementSet === _currentAdvertisementSet) {
            _advertisementInFlight = true
        }
        _advertisementServiceCallbacks.map {
            try {
                it.onAdvertisementSetStart(advertisementSet)
            } catch (e:Exception){
                Log.e(_logTag, "Error in: onAdvertisementSetStart ${e.message}")
            }
        }
    }

    override fun onAdvertisementSetStop(advertisementSet: AdvertisementSet?) {
        if (advertisementSet === _currentAdvertisementSet) {
            _advertisementInFlight = false
        }
        _advertisementServiceCallbacks.map {
            try {
                it.onAdvertisementSetStop(advertisementSet)
            } catch (e:Exception){
                Log.e(_logTag, "Error in: onAdvertisementSetStop ${e.message}")
            }
        }

        if (_advertisementService.isLegacyService()) {
            return
        }
        if (_awaitingModernStop && advertisementSet === _stoppingAdvertisementSet) {
            _awaitingModernStop = false
            _stoppingAdvertisementSet = null
            _advertisementInFlight = false
            resumeAfterTerminalCallback()
        }
    }

    override fun onAdvertisementSetSucceeded(advertisementSet: AdvertisementSet?) {
        if (advertisementSet === _currentAdvertisementSet) {
            runLocalCallback(true)
        }
        _advertisementServiceCallbacks.map {
            try {
                it.onAdvertisementSetSucceeded(advertisementSet)
            } catch (e:Exception){
                Log.e(_logTag, "Error in: onAdvertisementSetSucceeded ${e.message}")
            }
        }
    }

    override fun onAdvertisementSetFailed(advertisementSet: AdvertisementSet?, advertisementError: AdvertisementError) {
        if (advertisementSet === _currentAdvertisementSet) {
            _advertisementInFlight = false
            if (_awaitingModernStop && advertisementSet === _stoppingAdvertisementSet) {
                _awaitingModernStop = false
                _stoppingAdvertisementSet = null
                resumeAfterTerminalCallback()
            } else if (_pendingAdvertisementService != null) {
                resumeAfterTerminalCallback()
            } else {
                runLocalCallback(false)
            }
        }
        _advertisementServiceCallbacks.map {
            try {
                it.onAdvertisementSetFailed(advertisementSet, advertisementError)
            } catch (e:Exception){
                Log.e(_logTag, "Error in: onAdvertisementSetFailed ${e.message}")
            }
        }
    }
}
