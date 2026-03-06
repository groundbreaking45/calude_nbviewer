package com.nbviewer.presentation.viewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nbviewer.App
import com.nbviewer.databinding.FragmentViewerBinding
import com.nbviewer.domain.model.NotebookSource
import com.nbviewer.presentation.home.HomeFragment.Companion.ARG_SOURCE_URI
import kotlinx.coroutines.launch

/**
 * Displays the contents of a parsed Jupyter Notebook.
 *
 * RESPONSIBILITIES:
 *   1. Extract the URI string argument from navigation bundle.
 *   2. Pass it to [NotebookViewerViewModel] as a [NotebookSource.UriSource].
 *   3. Collect [NotebookUiState] and drive the UI.
 *   4. Host the RecyclerView with [NotebookAdapter] (wired in M4).
 *
 * STATE HANDLING:
 *   [NotebookUiState.Idle]    → show nothing (shouldn't normally be visible post-navigation)
 *   [NotebookUiState.Loading] → show progress bar, hide content
 *   [NotebookUiState.Success] → hide progress, show RecyclerView with cells
 *   [NotebookUiState.Error]   → hide progress, show error message with retry option
 *
 * LIFECYCLE-SAFE STATE COLLECTION:
 *   We use [repeatOnLifecycle(Lifecycle.State.STARTED)] to collect StateFlow.
 *   This pattern automatically:
 *     - Stops collecting when the Fragment goes to background (saves CPU).
 *     - Resumes collecting when it returns to foreground.
 *     - Does NOT re-trigger the ViewModel load on resume (ViewModel guards Loading).
 *   This is the Android-recommended pattern as of API 26+ / Lifecycle 2.6+.
 *
 * ROTATION SAFETY:
 *   ViewModel survives rotation. The fragment re-collects uiState.value immediately
 *   on resume — if Success, the adapter repopulates from the cached state.
 *   No file re-read occurs on rotation.
 *
 * M1 NOTE: The RecyclerView adapter is a stub (set to null). The full adapter
 * wiring occurs in M4. In M1, Success state is verified by logging cell count.
 */
class NotebookViewerFragment : Fragment() {

    private var _binding: FragmentViewerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotebookViewerViewModel by viewModels {
        val useCase = (requireActivity().application as App).container.loadNotebookUseCase
        NotebookViewerViewModel.factory(useCase)
    }

    private lateinit var adapter: NotebookAdapter

    // -------------------------------------------------------------------------
    // Fragment lifecycle
    // -------------------------------------------------------------------------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeUiState()
        triggerLoad()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Prevent RecyclerView leaking the adapter's reference to views
        binding.recyclerViewCells.adapter = null
        _binding = null
    }

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    private fun setupRecyclerView() {
        adapter = NotebookAdapter()
        binding.recyclerViewCells.adapter = adapter
        binding.recyclerViewCells.setHasFixedSize(true)
    }

    /**
     * Reads the URI argument and initiates the load.
     *
     * Called once per view creation. The ViewModel's [load] guard prevents
     * re-triggering if the load is already in progress or complete.
     *
     * ARGUMENT SOURCES:
     *   - From HomeFragment (SAF picker): Bundle arg "sourceUriString"
     *   - From MainActivity (ACTION_VIEW intent): same Bundle arg key
     */
    private fun triggerLoad() {
        val uriString = arguments?.getString(ARG_SOURCE_URI)
        if (uriString != null) {
            viewModel.load(NotebookSource.UriSource(uriString))
        } else {
            // No URI provided — defensive fallback. Should not occur in normal flow.
            showError("No file was specified. Please go back and select a file.")
        }
    }

    // -------------------------------------------------------------------------
    // State observation
    // -------------------------------------------------------------------------

    /**
     * Collects [NotebookUiState] safely using repeatOnLifecycle(STARTED).
     *
     * This coroutine is tied to the Fragment's viewLifecycleOwner and automatically
     * cancels when the view is destroyed — no manual cleanup required.
     */
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: NotebookUiState) {
        when (state) {
            is NotebookUiState.Idle    -> showIdle()
            is NotebookUiState.Loading -> showLoading()
            is NotebookUiState.Success -> showSuccess(state)
            is NotebookUiState.Error   -> showError(state.message)
        }
    }

    // -------------------------------------------------------------------------
    // UI state drivers
    // -------------------------------------------------------------------------

    private fun showIdle() {
        binding.progressBar.visibility = View.GONE
        binding.recyclerViewCells.visibility = View.GONE
        binding.textViewError.visibility = View.GONE
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerViewCells.visibility = View.GONE
        binding.textViewError.visibility = View.GONE
    }

    private fun showSuccess(state: NotebookUiState.Success) {
        binding.progressBar.visibility = View.GONE
        binding.textViewError.visibility = View.GONE
        binding.recyclerViewCells.visibility = View.VISIBLE
        adapter.submitList(state.cells)
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.recyclerViewCells.visibility = View.GONE
        binding.textViewError.visibility = View.VISIBLE
        binding.textViewError.text = message
    }
}
