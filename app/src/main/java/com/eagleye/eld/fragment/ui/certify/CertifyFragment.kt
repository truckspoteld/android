package com.eagleye.eld.fragment.ui.certify

import CertifyAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.eagleye.eld.R
import com.eagleye.eld.databinding.FragmentCertifyBinding
import com.eagleye.eld.di.NetworkModule
import com.eagleye.eld.models.CertifyModelItem
import com.eagleye.eld.repository.CertifyRepository
import com.eagleye.eld.utils.NetworkResult
import com.eagleye.eld.utils.PrefRepository
import com.eagleye.eld.viewmodel.CertifyViewModel
import com.eagleye.eld.viewmodel.certifyViewModelFactory
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CertifyFragment : Fragment() {
    private var _binding: FragmentCertifyBinding? = null
    private val binding get() = _binding!!

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
            playClickAnimation(it)
            context?.let { ctx ->
                certifyViewModel.loadCertifyData(ctx)
            }
        }

        // Initialize ViewModel
        setupViewModel()

        // Observe data changes
        observeCertifyData()
        observeUpdateCertifyData()
        
        // Setup SwipeRefreshLayout for pull-to-refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            context?.let { ctx ->
                certifyViewModel.loadCertifyData(ctx)
            }
        }
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.nav_active_blue,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light
        )

        // Load initial data
        context?.let { ctx ->
            certifyViewModel.loadCertifyData(ctx)
        }
        
        // Final layout verification and start animations
        startEntranceAnimations()
    }

    private fun setupViewModel() {
        val networkModule = NetworkModule()
        val certifyRepository = CertifyRepository(networkModule.provideGeIdeaAPI(networkModule.provideRetrofit(networkModule.provideOkHttpClient(requireContext()))))
        val factory = certifyViewModelFactory(certifyRepository)
        certifyViewModel = ViewModelProvider(this, factory)[CertifyViewModel::class.java]
    }

    private fun observeCertifyData() {
        certifyViewModel.certifyData.observe(viewLifecycleOwner) { result ->
            if (_binding == null) return@observe
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
            if (_binding == null) return@observe
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
        if (_binding == null) return
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.cvListContainer.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showError(show: Boolean, message: String = "") {
        if (_binding == null) return
        binding.errorLayout.visibility = if (show) View.VISIBLE else View.GONE
        if (show && message.isNotEmpty()) {
            binding.errorTextView.text = message
        }
        binding.cvListContainer.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun renderDates(dates: List<CertifyModelItem>) {
        certifyAdapter = CertifyAdapter(requireContext(), dates) { date ->
            updateCertified(date)
        }
        binding.certifyListView.adapter = certifyAdapter
        binding.certifyListView.post {
            if (_binding != null) {
                setListViewHeightBasedOnChildren(binding.certifyListView)
            }
        }
    }

    private fun setListViewHeightBasedOnChildren(listView: android.widget.ListView) {
        val listAdapter = listView.adapter ?: return
        var totalHeight = 0
        val desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.width, View.MeasureSpec.UNSPECIFIED)
        for (i in 0 until listAdapter.count) {
            val listItem = listAdapter.getView(i, null, listView)
            listItem.measure(desiredWidth, View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            totalHeight += listItem.measuredHeight
        }
        val params = listView.layoutParams
        params.height = totalHeight + (listView.dividerHeight * (listAdapter.count - 1))
        listView.layoutParams = params
        listView.requestLayout()
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
            context?.let { ctx ->
                certifyViewModel.updateCertified(updateCertifiedRequest, ctx)
            }
        }
    }

    private fun startEntranceAnimations() {
        if (_binding == null) return
        
        val duration = 700L
        val stagger = 80L

        val views = listOf(binding.cvHeader, binding.cvListContainer)
        
        views.forEachIndexed { index, view ->
            view.visibility = View.INVISIBLE
            lifecycleScope.launch {
                delay(index * stagger)
                if (_binding != null) {
                    view.visibility = View.VISIBLE
                    val technique = if (index == 0) Techniques.FadeInDown else Techniques.FadeInUp
                    YoYo.with(technique).duration(duration).playOn(view)
                }
            }
        }
    }

    private fun playClickAnimation(view: View) {
        // High-end subtle scale feedback
        view.animate()
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(120)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(120)
                    .start()
            }
            .start()
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