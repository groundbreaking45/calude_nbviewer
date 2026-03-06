package com.nbviewer.presentation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.nbviewer.R
import com.nbviewer.databinding.FragmentHomeBinding

/**
 * Home screen — the app's default start destination.
 *
 * RESPONSIBILITIES:
 *   1. Present the "Open File" entry point.
 *   2. Launch the system file picker via SAF.
 *   3. Validate the returned URI (null check only — content validation is the parser's job).
 *   4. Navigate to [NotebookViewerFragment] with the URI string as a nav argument.
 *
 * WHAT IT DOES NOT DO:
 *   - Does not read the file.
 *   - Does not parse anything.
 *   - Does not hold a ViewModel (no state to survive here — picker result is one-shot).
 *
 * SAF DESIGN (ADR-005):
 *   [ActivityResultContracts.OpenDocument] is used instead of OpenDocumentTree or
 *   GetContent. OpenDocument:
 *     - Does not require READ_EXTERNAL_STORAGE on API 33+.
 *     - Works with cloud storage providers (Drive, Dropbox) automatically.
 *     - Returns a content:// URI that the ContentResolver can open.
 *
 *   MIME TYPE: We pass "*/*" to show all files. .ipynb has no registered MIME type
 *   so restricting to a specific type would hide valid files on most devices.
 *   The parser validates content regardless of how the file was selected.
 *
 * LIFECYCLE SAFETY:
 *   [registerForActivityResult] must be called before [onAttach] / in field initialization,
 *   not inside click handlers. Android enforces this — calling it after onStart throws
 *   IllegalStateException. Our declaration as a property satisfies this requirement.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // -------------------------------------------------------------------------
    // SAF Launcher — must be registered as a property, not inside a callback
    // -------------------------------------------------------------------------

    /**
     * System file picker launcher.
     *
     * Mime type "*/*" shows all files. The user can navigate to any .ipynb file
     * regardless of how the OS has categorized its MIME type.
     *
     * The lambda runs on the main thread after the picker returns.
     * A null result means the user cancelled — show no error.
     * A non-null Uri is forwarded to the viewer immediately.
     */
    private val openFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        when {
            uri == null -> {
                // User pressed back / cancelled — silent, no error
            }
            else -> {
                navigateToViewer(uri.toString())
            }
        }
    }

    // -------------------------------------------------------------------------
    // Fragment lifecycle
    // -------------------------------------------------------------------------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonOpenFile.setOnClickListener { launchFilePicker() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Null the binding to prevent memory leaks across Fragment view lifecycle
        _binding = null
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun launchFilePicker() {
        try {
            // Pass "*/*" — see MIME TYPE note above
            openFileLauncher.launch(arrayOf("*/*"))
        } catch (e: Exception) {
            // Very rare: no file manager app installed on device
            Toast.makeText(
                requireContext(),
                getString(R.string.error_no_file_manager),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Navigate to the viewer, passing the URI string as a Bundle argument.
     *
     * We use a raw Bundle rather than Safe Args in M1 to avoid the Safe Args
     * Gradle plugin dependency. Safe Args can be adopted at M4 when the nav
     * graph is finalized.
     *
     * The argument key "sourceUriString" is the contract between this fragment
     * and [NotebookViewerFragment]. Documented here and in the receiver.
     */
    private fun navigateToViewer(uriString: String) {
        val args = Bundle().apply {
            putString(ARG_SOURCE_URI, uriString)
        }
        findNavController().navigate(R.id.action_home_to_viewer, args)
    }

    companion object {
        /**
         * Navigation argument key for the source URI string.
         * Shared contract between HomeFragment, MainActivity (intent path),
         * and NotebookViewerFragment.
         */
        const val ARG_SOURCE_URI = "sourceUriString"
    }
}
