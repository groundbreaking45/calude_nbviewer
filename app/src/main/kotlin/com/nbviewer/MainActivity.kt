package com.nbviewer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.nbviewer.databinding.ActivityMainBinding

/**
 * Single-activity host for NbViewer.
 *
 * RESPONSIBILITIES:
 *   1. Host the Navigation Component NavHostFragment.
 *   2. Intercept ACTION_VIEW intents (from "Open with" or file manager).
 *   3. Forward incoming URIs to the ViewModel via navigation arguments.
 *
 * WHAT IT DOES NOT DO:
 *   - No business logic.
 *   - No file reading.
 *   - No direct ViewModel interaction (delegates via NavArgs).
 *
 * INTENT HANDLING STRATEGY:
 *   External intents (ACTION_VIEW for .ipynb files) arrive in [onCreate] or
 *   [onNewIntent] (if singleTop). We extract the URI and pass it as a navigation
 *   argument to NotebookViewerFragment, bypassing HomeFragment entirely.
 *
 *   This means the app can be launched two ways:
 *     A) Via launcher icon → HomeFragment (file picker flow)
 *     B) Via "Open with" → directly to ViewerFragment with URI pre-loaded
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Handle intent that launched this Activity (e.g., "Open with" from file manager)
        if (savedInstanceState == null) {
            handleIntent(intent)
        }
    }

    /**
     * Called when Activity is already running (singleTop) and receives a new intent.
     * Example: user opens a second .ipynb file while NbViewer is already visible.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /**
     * Inspects the intent. If it's an ACTION_VIEW with a data URI, navigate
     * directly to the viewer with the URI encoded as a Safe Args string argument.
     *
     * If the intent is not ACTION_VIEW (standard launcher start), do nothing —
     * the nav graph default start destination (HomeFragment) handles the flow.
     */
    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            val uriString = intent.data?.toString()
            if (!uriString.isNullOrBlank()) {
                val args = Bundle().apply {
                    putString("sourceUriString", uriString)
                }
                navController.navigate(R.id.notebookViewerFragment, args)
            }
        }
    }
}
