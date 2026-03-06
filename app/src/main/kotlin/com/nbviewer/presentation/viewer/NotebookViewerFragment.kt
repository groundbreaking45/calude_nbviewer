package com.nbviewer.presentation.viewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.nbviewer.App
import com.nbviewer.databinding.FragmentViewerBinding
import com.nbviewer.domain.model.NotebookSource
import kotlinx.coroutines.launch

class NotebookViewerFragment : Fragment() {

    private var _binding: FragmentViewerBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: NotebookAdapter

    private val viewModel: NotebookViewerViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val useCase = (requireActivity().application as App)
                    .container.loadNotebookUseCase
                return NotebookViewerViewModel(useCase) as T
            }
        }
    }

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
        observeState()

        val uriString = arguments?.getString("sourceUriString")
        if (uriString != null) {
            viewModel.load(NotebookSource.UriSource(uriString))
        } else {
            showError("No file was provided.")
        }
    }

    private fun setupRecyclerView() {
        adapter = NotebookAdapter()
        binding.recyclerViewCells.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewCells.adapter = adapter
        binding.recyclerViewCells.setHasFixedSize(false)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is NotebookUiState.Idle    -> showIdle()
                        is NotebookUiState.Loading -> showLoading()
                        is NotebookUiState.Success -> showSuccess(state)
                        is NotebookUiState.Error   -> showError(state.message)
                    }
                }
            }
        }
    }

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}