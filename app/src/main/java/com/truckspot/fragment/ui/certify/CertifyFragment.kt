package com.truckspot.fragment.ui.certify

import CertifyAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.truckspot.R
import com.truckspot.databinding.FragmentCertifyBinding
import com.truckspot.di.NetworkModule
import com.truckspot.models.CertifyModelItem
import com.truckspot.repository.CertifyRepository
import com.truckspot.utils.NetworkResult
import com.truckspot.utils.PrefRepository
import com.truckspot.viewmodel.CertifyViewModel
import com.truckspot.viewmodel.certifyViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

class CertifyFragment : Fragment() {
    private var _binding: FragmentCertifyBinding? = null
    private val binding
        get() = _binding ?: run {
            val inflater = LayoutInflater.from(context)
            FragmentCertifyBinding.inflate(inflater).also { _binding = it }
        }

    private lateinit var certifyViewModel: CertifyViewModel
    private lateinit var prefRepository: PrefRepository
    private var certifyAdapter: CertifyAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCertifyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize PrefRepository
        prefRepository = PrefRepository(requireContext())

        // Set up retry button click listener
        binding.retryButton.setOnClickListener {
            certifyViewModel.loadCertifyData(requireContext())
        }

        // Initialize ViewModel
        setupViewModel()

        // Observe data changes
        observeCertifyData()
        observeUpdateCertifyData()
        
        // Setup SwipeRefreshLayout for pull-to-refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            certifyViewModel.loadCertifyData(requireContext())
        }
        binding.swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light
        )

        // Load initial data
        certifyViewModel.loadCertifyData(requireContext())
    }

    private fun setupViewModel() {
        val networkModule = NetworkModule()
        val certifyRepository = CertifyRepository(networkModule.provideGeIdeaAPI(networkModule.provideRetrofit(networkModule.provideOkHttpClient(requireContext()))))
        val factory = certifyViewModelFactory(certifyRepository)
        certifyViewModel = ViewModelProvider(this, factory)[CertifyViewModel::class.java]
    }

    private fun observeCertifyData() {
        certifyViewModel.certifyData.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    showLoading(true)
                    showError(false)
                }
                is NetworkResult.Success -> {
                    showLoading(false)
                    showError(false)
                    binding.swipeRefreshLayout.isRefreshing = false
                    result.data?.let { dates ->
                        // Sort dates in descending order before rendering
                        val sortedDates = sortDatesDescending(dates)
                        renderDates(sortedDates)
                    }
                }
                is NetworkResult.Error -> {
                    showLoading(false)
                    showError(true, result.message ?: "Unknown error occurred")
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            }
        }
    }

    private fun observeUpdateCertifyData() {
        certifyViewModel.updateCertifyData.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    binding.updateProgressBar.visibility = View.VISIBLE
                }
                is NetworkResult.Success -> {
                    binding.updateProgressBar.visibility = View.GONE
                    // Clear all updating states
                    certifyAdapter?.let { adapter ->
                        adapter.items.forEach { item ->
                            adapter.setItemUpdating(item.date, false)
                        }
                    }
                    Toast.makeText(requireContext(), "Certification status updated successfully", Toast.LENGTH_SHORT).show()
                    // Refresh the data
                    certifyViewModel.loadCertifyData(requireContext())
                }
                is NetworkResult.Error -> {
                    binding.updateProgressBar.visibility = View.GONE
                    // Clear all updating states on error
                    certifyAdapter?.let { adapter ->
                        // Clear all updating states since we don't know which one failed
                        adapter.items.forEach { item ->
                            adapter.setItemUpdating(item.date, false)
                        }
                    }
                    Toast.makeText(requireContext(), "Failed to update: ${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.certifyListView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showError(show: Boolean, message: String = "") {
        binding.errorLayout.visibility = if (show) View.VISIBLE else View.GONE
        if (show && message.isNotEmpty()) {
            binding.errorTextView.text = message
        }
        binding.certifyListView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun renderDates(dates: List<CertifyModelItem>) {
        certifyAdapter = CertifyAdapter(requireContext(), dates) { date ->
            updateCertified(date)
        }
        binding.certifyListView.adapter = certifyAdapter
    }

    private fun updateCertified(date: String?) {
        date?.let { safeDate ->
            // Set the specific item as updating
            certifyAdapter?.setItemUpdating(safeDate, true)

            val updateCertifiedRequest = CertifyModelItem(
                date = safeDate,
                status = true,
                driverId = prefRepository.getDriverId()
            )
            certifyViewModel.updateCertified(updateCertifiedRequest, requireContext())
        }
    }

    /**
     * Sorts the list of dates in descending order (newest first)
     */
    private fun sortDatesDescending(dates: List<CertifyModelItem>): List<CertifyModelItem> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        return dates.sortedWith { item1, item2 ->
            try {
                // Parse dates for proper comparison
                val date1 = item1.date.let { dateFormat.parse(it) }
                val date2 = item2.date.let { dateFormat.parse(it) }

                // Sort in descending order (newest first)
                when {
                    date1 == null && date2 == null -> 0
                    date1 == null -> 1
                    date2 == null -> -1
                    else -> date2.compareTo(date1)
                }
            } catch (e: Exception) {
                when {
                    item1.date == null && item2.date == null -> 0
                    item1.date == null -> 1
                    item2.date == null -> -1
                    else -> item2.date!!.compareTo(item1.date!!)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}