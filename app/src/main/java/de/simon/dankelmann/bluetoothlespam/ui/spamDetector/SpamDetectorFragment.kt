package de.simon.dankelmann.bluetoothlespam.ui.spamDetector

import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import de.simon.dankelmann.bluetoothlespam.Adapters.FlipperDeviceScanResultListViewAdapter
import de.simon.dankelmann.bluetoothlespam.Adapters.SpamPackageScanResultListViewAdapter
import de.simon.dankelmann.bluetoothlespam.BleSpamApplication
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IBluetoothLeScanCallback
import de.simon.dankelmann.bluetoothlespam.Interfaces.Services.IBluetoothLeScanService
import de.simon.dankelmann.bluetoothlespam.Models.FlipperDeviceScanResult
import de.simon.dankelmann.bluetoothlespam.Models.SpamPackageScanResult
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.Services.BluetoothLeScanForegroundService
import de.simon.dankelmann.bluetoothlespam.databinding.FragmentSpamDetectorBinding

class SpamDetectorFragment : IBluetoothLeScanCallback, Fragment() {

    private val _logTag = "SpamDetectorFragment"

    private var _viewModel: SpamDetectorViewModel? = null
    private val viewModel get() = _viewModel!!

    private var _binding: FragmentSpamDetectorBinding? = null
    private val binding get() = _binding!!

    private lateinit var _flipperDevicesRecyclerView: RecyclerView
    private lateinit var _flipperDevicesListViewAdapter: FlipperDeviceScanResultListViewAdapter

    private lateinit var _spamPackageRecyclerView: RecyclerView
    private lateinit var _spamPackageListViewAdapter: SpamPackageScanResultListViewAdapter

    override fun onResume() {
        super.onResume()

        val scanService = (requireContext().applicationContext as BleSpamApplication).scanService
        scanService.addBluetoothLeScanServiceCallback(this)

        syncWithScanServices(requireContext())
    }

    override fun onPause() {
        super.onPause()

        val scanService = (requireContext().applicationContext as BleSpamApplication).scanService
        scanService.removeBluetoothLeScanServiceCallback(this)
    }

    private fun syncWithScanServices(context: Context) {
        val scanService = (context.applicationContext as BleSpamApplication).scanService
        viewModel.isDetecting.postValue(scanService.isScanning())
        updateFlipperDevicesListView(context)
        updateSpamPackageListView(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewModel = ViewModelProvider(this)[SpamDetectorViewModel::class.java]
        _binding = FragmentSpamDetectorBinding.inflate(inflater, container, false)
        val root: View = binding.root

        _flipperDevicesRecyclerView = binding.spamDetectionFlipperDevicesList
        _spamPackageRecyclerView = binding.spamDetectionSpamPackageList

        setupUi(root.context)
        setupFlipperDevicesListView(root.context)
        setupSpamPackagesListView(root.context)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setupUi(context: Context) {
        // Views
        val toggleButton = binding.spamDetectorToggleButton
        val detectionAnimation = binding.spamDetectionAnimation

        // Listeners
        toggleButton.setOnClickListener {
            onToggleButtonClicked(context)
        }

        // Observers
        viewModel.isDetecting.observe(viewLifecycleOwner) { isDetecting ->
            if (isDetecting) {
                toggleButton.setImageDrawable(
                    ResourcesCompat.getDrawable(resources, R.drawable.pause, context.theme)
                )
                detectionAnimation.playAnimation()
            } else {
                toggleButton.setImageDrawable(
                    ResourcesCompat.getDrawable(resources, R.drawable.play_arrow, context.theme)
                )
                detectionAnimation.cancelAnimation()
                detectionAnimation.frame = 0
            }
        }
    }

    fun setupFlipperDevicesListView(context: Context) {
        val scanService = (context.applicationContext as BleSpamApplication).scanService
        _flipperDevicesListViewAdapter = FlipperDeviceScanResultListViewAdapter(
            scanService.getFlipperDevicesList()
        )
        _flipperDevicesRecyclerView.adapter = _flipperDevicesListViewAdapter
    }

    fun setupSpamPackagesListView(context: Context) {
        val scanService = (context.applicationContext as BleSpamApplication).scanService
        _spamPackageListViewAdapter = SpamPackageScanResultListViewAdapter(
            scanService.getSpamPackageScanResultList(), context
        )
        _spamPackageRecyclerView.adapter = _spamPackageListViewAdapter
    }

    fun updateFlipperDevicesListView(context: Context) {
        if (::_flipperDevicesListViewAdapter.isInitialized) {
            // The adapter shares the scan service's list reference, so it is already
            // up to date (including removals). Just trigger a rebind.
            _flipperDevicesListViewAdapter.notifyDataSetChanged()
        }
    }

    fun updateSpamPackageListView(context: Context) {
        if (::_spamPackageListViewAdapter.isInitialized) {
            _spamPackageListViewAdapter.notifyDataSetChanged()
        }
    }

    fun onToggleButtonClicked(context: Context) {
        if (viewModel.isDetecting.value == true) {
            BluetoothLeScanForegroundService.stopService(context)
            //AppContext.getBluetoothLeScanService().stopScanning()
            Log.d(_logTag, "Should Stop")
            viewModel.isDetecting.postValue(false)
        } else {
            val serviceStarted = BluetoothLeScanForegroundService.startService(context)
            //AppContext.getBluetoothLeScanService().startScanning()
            viewModel.isDetecting.postValue(serviceStarted)
        }
    }

    // BluetoothLeScanResult Callback Implementation
    override fun onScanStateChanged(isScanning: Boolean) {
        viewModel.isDetecting.postValue(isScanning)
    }

    override fun onScanResult(scanResult: ScanResult) {
        // Nothing to do yet
    }

    override fun onFlipperDeviceDetected(
        flipperDeviceScanResult: FlipperDeviceScanResult,
        alreadyKnown: Boolean
    ) {
        // Nothing to do yet
        //updateFlipperDevicesListView()
    }

    override fun onFlipperListUpdated() {
        // The fragment could be in the background, then the context is invalid
        context?.let {
            updateFlipperDevicesListView(it)
        }
    }

    override fun onSpamResultPackageDetected(
        spamPackageScanResult: SpamPackageScanResult,
        alreadyKnown: Boolean
    ) {
        // Nothing to do yet
        //updateSpamPackageListView()
    }

    override fun onSpamResultPackageListUpdated() {
        // The fragment could be in the background, then the context is invalid
        context?.let {
            updateSpamPackageListView(it)
        }
    }

}
