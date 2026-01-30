package com.zim.jackettprowler

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.zim.jackettprowler.databinding.ActivityMainBinding
import com.zim.jackettprowler.services.DaemonController
import com.zim.jackettprowler.services.NetworkQualityMonitor
import com.zim.jackettprowler.services.ProviderAnalytics
import com.zim.jackettprowler.services.SearchResultCache
import com.zim.jackettprowler.video.VideoResultAdapter
import com.zim.jackettprowler.video.VideoSearchService
import com.zim.jackettprowler.video.VideoResult
import com.zim.jackettprowler.video.VideoDownloadService
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val client = OkHttpClient()

    private var adapter: TorrentAdapter? = null
    private var videoAdapter: VideoResultAdapter? = null
    private var lastQuery: String? = null
    private var lastSource: Source? = null
    private var isVideoMode: Boolean = false

    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)
    
    // Track active search job to allow cancellation
    private var searchJob: Job? = null

    // Hard-coded from your setup
    private val JACKETT_BASE_URL = "http://192.168.1.175:9117"
    private val JACKETT_API_KEY = "sfbizvj42r5h41a2aojb2t29zouqgd3s"

    private val PROWLARR_BASE_URL = "http://192.168.1.175:9696"
    private val PROWLARR_API_KEY = "11e5676f4c3444479cea3671a6c0c55b"
    
    // Torznab services
    private lateinit var jackettService: TorznabService
    private lateinit var prowlarrService: TorznabService
    
    // Download history manager
    private lateinit var historyManager: DownloadHistoryManager
    
    // Torrent aggregator
    private lateinit var aggregator: TorrentAggregator
    
    // Tracker manager for enhancing magnet links
    private lateinit var trackerManager: TrackerManager
    
    // Video search service
    private lateinit var videoSearchService: VideoSearchService
    
    // Video download service
    private lateinit var videoDownloadService: VideoDownloadService
    
    // Background services
    private lateinit var searchCache: SearchResultCache
    private lateinit var networkMonitor: NetworkQualityMonitor
    private lateinit var providerAnalytics: ProviderAnalytics

    enum class Source {
        JACKETT,
        PROWLARR,
        ALL_SOURCES  // New option for aggregated search
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Load API credentials from SharedPreferences (can be edited in Settings)
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        
        // Initialize with hard-coded defaults if not set
        val jackettUrl = prefs.getString("jackett_url", JACKETT_BASE_URL) ?: JACKETT_BASE_URL
        val jackettKey = prefs.getString("jackett_api_key", JACKETT_API_KEY) ?: JACKETT_API_KEY
        val prowlarrUrl = prefs.getString("prowlarr_url", PROWLARR_BASE_URL) ?: PROWLARR_BASE_URL
        val prowlarrKey = prefs.getString("prowlarr_api_key", PROWLARR_API_KEY) ?: PROWLARR_API_KEY
        
        // Save defaults if first run
        if (!prefs.contains("jackett_url")) {
            prefs.edit()
                .putString("jackett_url", JACKETT_BASE_URL)
                .putString("jackett_api_key", JACKETT_API_KEY)
                .putString("prowlarr_url", PROWLARR_BASE_URL)
                .putString("prowlarr_api_key", PROWLARR_API_KEY)
                .apply()
        }
        
        // Initialize Torznab services with current settings
        jackettService = TorznabService(jackettUrl, jackettKey)
        prowlarrService = TorznabService(prowlarrUrl, prowlarrKey)
        
        // Initialize download history manager
        historyManager = DownloadHistoryManager(this)
        
        // Initialize torrent aggregator
        aggregator = TorrentAggregator(this)
        
        // Initialize tracker manager
        trackerManager = TrackerManager(this)
        
        // Initialize video search service
        videoSearchService = VideoSearchService(this)
        
        // Initialize video download service
        videoDownloadService = VideoDownloadService(this)
        
        // Initialize background services
        searchCache = SearchResultCache(this)
        networkMonitor = NetworkQualityMonitor(this)
        providerAnalytics = ProviderAnalytics(this)
        
        // Start network monitoring
        networkMonitor.startMonitoring()
        
        // Initialize background daemon (for health checks, etc.)
        DaemonController.initialize(this)

        setupRecyclerView()
        setupListeners()
        checkConnections()
    }

    override fun onResume() {
        super.onResume()
        // Reload Torznab services in case settings changed
        reloadServices()
    }

    private fun reloadServices() {
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val jackettUrl = prefs.getString("jackett_url", JACKETT_BASE_URL) ?: JACKETT_BASE_URL
        val jackettKey = prefs.getString("jackett_api_key", JACKETT_API_KEY) ?: JACKETT_API_KEY
        val prowlarrUrl = prefs.getString("prowlarr_url", PROWLARR_BASE_URL) ?: PROWLARR_BASE_URL
        val prowlarrKey = prefs.getString("prowlarr_api_key", PROWLARR_API_KEY) ?: PROWLARR_API_KEY
        
        jackettService = TorznabService(jackettUrl, jackettKey)
        prowlarrService = TorznabService(prowlarrUrl, prowlarrKey)
    }

    private fun setupRecyclerView() {
        adapter = TorrentAdapter(emptyList()) { result ->
            showTorrentClientChooser(result)
        }
        binding.recyclerViewResults.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewResults.adapter = adapter
        
        // Setup video results recycler with download callback
        videoAdapter = VideoResultAdapter(
            onItemClick = { result -> openVideoResult(result) },
            onDownloadClick = { result -> showVideoDownloadOptions(result) }
        )
        binding.recyclerViewVideoResults.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewVideoResults.adapter = videoAdapter
    }

    private fun setupListeners() {
        binding.buttonSearch.setOnClickListener {
            val query = binding.editTextQuery.text.toString().trim()
            if (query.isNotEmpty()) {
                if (isVideoMode) {
                    performVideoSearch(query)
                } else {
                    performSearch(query, getSelectedSource())
                }
            }
        }
        
        // Mode toggle listeners
        binding.radioTorrents.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                isVideoMode = false
                switchToTorrentMode()
            }
        }
        
        binding.radioVideos.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                isVideoMode = true
                switchToVideoMode()
            }
        }

        binding.buttonRefresh.setOnClickListener {
            binding.textStatus.text = "Rechecking connections..."
            adapter?.updateData(emptyList())

            uiScope.launch(Dispatchers.IO) {
                val status = getConnectionStatus()
                launch(Dispatchers.Main) {
                    binding.textStatus.text = status
                }

                val query = lastQuery
                val source = lastSource
                if (!query.isNullOrEmpty() && source != null) {
                    try {
                        val service = if (source == Source.JACKETT) jackettService else prowlarrService
                        val results = service.search(query, TorznabService.SearchType.SEARCH, limit = 100)
                        launch(Dispatchers.Main) {
                            adapter?.updateData(results.sortedByDescending { it.seeders })
                            binding.textStatus.text =
                                "$status | Refreshed: ${results.size} result(s)."
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        launch(Dispatchers.Main) {
                            binding.textStatus.text =
                                "$status | Refresh search error: ${e.message}"
                        }
                    }
                }
            }
        }

        binding.editTextQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.editTextQuery.text.toString().trim()
                if (query.isNotEmpty()) {
                    if (isVideoMode) {
                        performVideoSearch(query)
                    } else {
                        performSearch(query, getSelectedSource())
                    }
                }
                true
            } else {
                false
            }
        }

        binding.buttonSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun switchToTorrentMode() {
        binding.recyclerViewResults.visibility = View.VISIBLE
        binding.recyclerViewVideoResults.visibility = View.GONE
        binding.spinnerSource.visibility = View.VISIBLE
        binding.textStatus.text = "🧲 Torrent mode - Search torrents"
    }
    
    private fun switchToVideoMode() {
        binding.recyclerViewResults.visibility = View.GONE
        binding.recyclerViewVideoResults.visibility = View.VISIBLE
        binding.spinnerSource.visibility = View.GONE  // Source selector not used for videos
        
        val videoSites = videoSearchService.getEnabledSites()
        if (videoSites.isEmpty()) {
            binding.textStatus.text = "🎬 Video mode - No video sites configured. Go to Settings to add some!"
        } else {
            binding.textStatus.text = "🎬 Video mode - ${videoSites.size} sites enabled"
        }
    }
    
    private fun performVideoSearch(query: String) {
        val sites = videoSearchService.getEnabledSites()
        if (sites.isEmpty()) {
            binding.textStatus.text = "No video sites configured! Go to Settings → Clearnet Video Sites"
            return
        }
        
        // Cancel any previous search
        searchJob?.cancel()
        
        binding.textStatus.text = "🎬 Searching ${sites.size} video sites for \"$query\"..."
        videoAdapter?.updateData(emptyList())
        
        searchJob = uiScope.launch(Dispatchers.IO) {
            try {
                val result = videoSearchService.searchAll(query, 100)
                
                launch(Dispatchers.Main) {
                    videoAdapter?.updateData(result.results, grouped = true)
                    binding.textStatus.text = result.getStatusSummary()
                    
                    // Show detailed status on long press
                    binding.textStatus.setOnLongClickListener {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("Video Search Details")
                            .setMessage(result.getDetailedStatus())
                            .setPositiveButton("OK", null)
                            .show()
                        true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                launch(Dispatchers.Main) {
                    binding.textStatus.text = "Video search error: ${e.message}"
                }
            }
        }
    }
    
    private fun openVideoResult(result: VideoResult) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.videoUrl))
            startActivity(intent)
            binding.textStatus.text = "🎬 Opening: ${result.title.take(50)}..."
        } catch (e: Exception) {
            binding.textStatus.text = "Error opening video: ${e.message}"
        }
    }
    
    private fun showVideoDownloadOptions(result: VideoResult) {
        binding.textStatus.text = "⏳ Extracting video streams..."
        
        uiScope.launch(Dispatchers.IO) {
            val extraction = videoDownloadService.extractStreams(result)
            
            launch(Dispatchers.Main) {
                if (extraction.success && extraction.streams.isNotEmpty()) {
                    showStreamSelectionDialog(result, extraction.streams)
                } else {
                    // Extraction failed - offer to open in browser or external app
                    showExternalDownloadOptions(result, extraction.error)
                }
            }
        }
    }
    
    private fun showStreamSelectionDialog(result: VideoResult, streams: List<VideoDownloadService.VideoStream>) {
        val options = streams.map { stream ->
            val qualityInfo = if (stream.quality != "auto") stream.quality else "Auto"
            val formatInfo = stream.format.uppercase()
            val audioTag = if (stream.isAudioOnly) " (Audio)" else ""
            "$qualityInfo - $formatInfo$audioTag"
        }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("📥 Download: ${result.title.take(40)}...")
            .setItems(options) { _, which ->
                val selectedStream = streams[which]
                startVideoDownload(selectedStream, result.title)
            }
            .setNeutralButton("Open in Browser") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.videoUrl))
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showExternalDownloadOptions(result: VideoResult, error: String) {
        AlertDialog.Builder(this)
            .setTitle("Download Options")
            .setMessage("Could not extract video streams directly.\n\nReason: $error\n\nYou can try alternative methods:")
            .setPositiveButton("Open in Browser") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.videoUrl))
                startActivity(intent)
            }
            .setNeutralButton("Copy URL") { _, _ ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Video URL", result.videoUrl)
                clipboard.setPrimaryClip(clip)
                binding.textStatus.text = "📋 URL copied to clipboard"
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    @Suppress("DEPRECATION")
    private fun startVideoDownload(stream: VideoDownloadService.VideoStream, title: String) {
        // Check for storage permission on older Android versions
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1001)
                binding.textStatus.text = "⚠️ Storage permission required"
                return
            }
        }
        
        binding.textStatus.text = "📥 Starting download: ${title.take(40)}..."
        
        // Create progress dialog
        val progressDialog = android.app.ProgressDialog(this).apply {
            setTitle("Downloading Video")
            setMessage(title.take(50))
            setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL)
            max = 100
            setCancelable(false)
            show()
        }
        
        uiScope.launch(Dispatchers.IO) {
            val result = videoDownloadService.downloadVideo(stream, title) { progress ->
                launch(Dispatchers.Main) {
                    progressDialog.progress = progress
                }
            }
            
            launch(Dispatchers.Main) {
                progressDialog.dismiss()
                
                result.fold(
                    onSuccess = { file ->
                        binding.textStatus.text = "✅ Downloaded: ${file.name}"
                        
                        // Offer to open the file
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("Download Complete")
                            .setMessage("Video saved to:\n${file.absolutePath}")
                            .setPositiveButton("Open") { _, _ ->
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW)
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        this@MainActivity,
                                        "${packageName}.provider",
                                        file
                                    )
                                    intent.setDataAndType(uri, "video/*")
                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    startActivity(intent)
                                } catch (e: Exception) {
                                    binding.textStatus.text = "Could not open video: ${e.message}"
                                }
                            }
                            .setNegativeButton("OK", null)
                            .show()
                    },
                    onFailure = { error ->
                        binding.textStatus.text = "❌ Download failed: ${error.message}"
                        
                        // Offer alternative options
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("Download Failed")
                            .setMessage("Error: ${error.message}\n\nTry opening in browser instead?")
                            .setPositiveButton("Open in Browser") { _, _ ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(stream.url))
                                startActivity(intent)
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                )
            }
        }
    }

    private fun getSelectedSource(): Source {
        val pos = binding.spinnerSource.selectedItemPosition
        return when (pos) {
            0 -> Source.ALL_SOURCES  // All sources (default)
            1 -> Source.JACKETT      // Jackett only
            2 -> Source.PROWLARR     // Prowlarr only
            else -> Source.ALL_SOURCES
        }
    }

    private fun performSearch(query: String, source: Source) {
        lastQuery = query
        lastSource = source

        // For ALL_SOURCES, use the aggregator
        if (source == Source.ALL_SOURCES) {
            performAggregatedSearch(query)
            return
        }

        // Check if this source's indexers are enabled
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val sourceKey = if (source == Source.JACKETT) "jackett" else "prowlarr"
        val isSourceEnabled = prefs.getBoolean("indexer_${sourceKey}-all_enabled", true)
        
        if (!isSourceEnabled) {
            binding.textStatus.text = "$source indexers are disabled. Enable them in Settings > Manage Indexers"
            return
        }

        val useSmartSearch = binding.toggleDescriptiveSearch.isChecked
        val downloadableOnly = binding.toggleDownloadableOnly.isChecked
        val searchType = if (useSmartSearch) "Smart Search" else "Standard"
        val dlFilter = if (downloadableOnly) " (DL)" else ""
        
        // Cancel any previous search
        searchJob?.cancel()
        
        binding.textStatus.text = "Searching $source ($searchType$dlFilter) for \"$query\"..."
        adapter?.updateData(emptyList())

        searchJob = uiScope.launch(Dispatchers.IO) {
            try {
                val allResults = mutableSetOf<TorrentResult>()
                val service = if (source == Source.JACKETT) jackettService else prowlarrService
                
                // Primary search using TorznabService
                allResults.addAll(service.search(query, TorznabService.SearchType.SEARCH, limit = 100))
                
                // If smart search is enabled, also search for keywords
                if (useSmartSearch) {
                    val keywords = extractKeywords(query)
                    for (keyword in keywords) {
                        if (keyword.length > 2) { // Only search meaningful keywords
                            try {
                                allResults.addAll(service.search(keyword, TorznabService.SearchType.SEARCH, limit = 50))
                            } catch (_: Exception) {
                                // Ignore errors from individual keyword searches
                            }
                        }
                    }
                }

                saveSearchQuery(query)
                var results = allResults.toList().sortedByDescending { it.seeders }
                
                // Filter for downloadable results only if enabled
                if (downloadableOnly) {
                    val originalCount = results.size
                    results = filterDownloadableResults(results)
                    val filteredCount = originalCount - results.size
                    
                    launch(Dispatchers.Main) {
                        adapter?.updateData(results)
                        binding.textStatus.text =
                            "Search OK on $source ($searchType) | ${results.size} result(s) | $filteredCount filtered"
                    }
                } else {
                    launch(Dispatchers.Main) {
                        adapter?.updateData(results)
                        binding.textStatus.text =
                            "Search OK on $source ($searchType) | ${results.size} result(s)."
                    }
                }
            } catch (e: CancellationException) {
                // Search was cancelled, this is expected - don't show error
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                launch(Dispatchers.Main) {
                    binding.textStatus.text = "Search error on $source: ${e.message}"
                }
            }
        }
    }
    
    /**
     * Perform aggregated search across all sources
     */
    private fun performAggregatedSearch(query: String) {
        val downloadableOnly = binding.toggleDownloadableOnly.isChecked
        val filterText = if (downloadableOnly) " (DL only)" else ""
        
        // Cancel any previous search
        searchJob?.cancel()
        
        binding.textStatus.text = "Searching all sources$filterText for \"$query\"..."
        adapter?.updateData(emptyList())

        searchJob = uiScope.launch(Dispatchers.IO) {
            try {
                val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
                val includeOnion = prefs.getBoolean("enable_onion_sites", false)
                
                val aggregatedResults = aggregator.searchAll(
                    query = query,
                    jackettService = jackettService,
                    prowlarrService = prowlarrService,
                    limit = 100,
                    includeCustomSites = true,
                    includeOnionSites = includeOnion,
                    includeBuiltInProviders = true,  // EXPLICITLY enable built-in providers
                    includeImportedIndexers = true   // EXPLICITLY enable imported indexers
                )

                saveSearchQuery(query)
                
                // Filter for downloadable results only if enabled
                val filteredResults = if (downloadableOnly) {
                    filterDownloadableResults(aggregatedResults.results)
                } else {
                    aggregatedResults.results
                }

                launch(Dispatchers.Main) {
                    adapter?.updateData(filteredResults)
                    val dlInfo = if (downloadableOnly) " | ${aggregatedResults.results.size - filteredResults.size} filtered" else ""
                    binding.textStatus.text = aggregatedResults.getStatusSummary() + dlInfo
                    
                    // Show detailed status on long press
                    binding.textStatus.setOnLongClickListener {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("Source Details")
                            .setMessage(aggregatedResults.getDetailedStatus())
                            .setPositiveButton("OK", null)
                            .show()
                        true
                    }
                }
            } catch (e: CancellationException) {
                // Search was cancelled, this is expected - don't show error
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                launch(Dispatchers.Main) {
                    binding.textStatus.text = "Aggregated search error: ${e.message}"
                }
            }
        }
    }
    
    /**
     * Filter results to only show alive torrents (not dead - must have seeders or peers)
     */
    private fun filterDownloadableResults(results: List<TorrentResult>): List<TorrentResult> {
        return results.filter { result ->
            isDownloadable(result)
        }
    }
    
    /**
     * Check if a torrent is alive (has seeders or peers)
     * Returns true if seeders > 0 or peers > 0
     */
    private fun isDownloadable(result: TorrentResult): Boolean {
        // A torrent is "downloadable" if it has at least one seeder or peer
        return result.seeders > 0 || result.peers > 0
    }

    private fun extractKeywords(query: String): List<String> {
        // Remove common words and split into keywords
        val stopWords = setOf("the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by", "from", "when", "gets", "is", "are", "was", "were")
        return query.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it !in stopWords }
            .distinct()
    }

    private fun checkConnections() {
        binding.textStatus.text = "Checking Jackett & Prowlarr..."
        uiScope.launch(Dispatchers.IO) {
            val status = getConnectionStatus()
            launch(Dispatchers.Main) {
                binding.textStatus.text = status
            }
        }
    }

    private fun getConnectionStatus(): String {
        val jackettOk = try {
            jackettService.testConnection()
        } catch (_: Exception) {
            false
        }

        val prowlarrOk = try {
            prowlarrService.testConnection()
        } catch (_: Exception) {
            false
        }

        return when {
            jackettOk && prowlarrOk -> "Connected: Jackett & Prowlarr"
            jackettOk && !prowlarrOk -> "Connected: Jackett only (Prowlarr unreachable)"
            !jackettOk && prowlarrOk -> "Connected: Prowlarr only (Jackett unreachable)"
            else -> "No connection to Jackett or Prowlarr"
        }
    }

    private fun showTorrentClientChooser(result: TorrentResult) {
        // Save to history
        historyManager.addDownload(result)

        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val qbEnabled = prefs.getBoolean(getString(R.string.pref_qb_enabled), false)
        
        val torrentClients = mutableListOf<TorrentClientOption>()
        
        // Add qBittorrent if configured
        if (qbEnabled) {
            val baseUrl = prefs.getString(getString(R.string.pref_qb_base_url), "") ?: ""
            val username = prefs.getString(getString(R.string.pref_qb_username), "") ?: ""
            val password = prefs.getString(getString(R.string.pref_qb_password), "") ?: ""
            
            if (baseUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()) {
                torrentClients.add(TorrentClientOption("qBittorrent (Configured)", "qbittorrent", baseUrl, username, password))
            }
        }
        
        // Detect installed torrent clients
        val installedClients = detectInstalledTorrentClients()
        torrentClients.addAll(installedClients)
        
        if (torrentClients.isEmpty()) {
            // No clients found, show install options
            showNoClientsDialog(result)
            return
        }
        
        if (torrentClients.size == 1) {
            // Only one client, use it directly
            downloadWithClient(result, torrentClients[0])
            return
        }
        
        // Multiple clients, show chooser
        val clientNames = torrentClients.map { it.displayName }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Download with:")
            .setItems(clientNames) { _, which ->
                downloadWithClient(result, torrentClients[which])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun detectInstalledTorrentClients(): List<TorrentClientOption> {
        val clients = mutableListOf<TorrentClientOption>()
        val knownClients = listOf(
            "org.proninyaroslav.libretorrent" to "LibreTorrent",  // LibreTorrent first (preferred)
            "com.utorrent.client" to "µTorrent",
            "com.bittorrent.client" to "BitTorrent",
            "org.transdroid.full" to "Transdroid",
            "com.deluge.android" to "Deluge",
            "com.frostwire.android" to "FrostWire",
            "com.flxrs.danmaku.flinger" to "Flud"
        )
        
        val pm = packageManager
        for ((packageName, displayName) in knownClients) {
            try {
                pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
                clients.add(TorrentClientOption(displayName, packageName, null, null, null))
            } catch (_: PackageManager.NameNotFoundException) {
                // Client not installed
            }
        }
        
        return clients
    }

    private fun showNoClientsDialog(result: TorrentResult) {
        val recommendations = listOf(
            "LibreTorrent - Free, open source (RECOMMENDED)",
            "µTorrent - Popular choice",
            "Flud - Simple and effective"
        )
        
        AlertDialog.Builder(this)
            .setTitle("No Torrent Clients Found")
            .setMessage("You need to install a torrent client app to download torrents.\n\nRecommended clients:\n${recommendations.joinToString("\n")}")
            .setPositiveButton("Install LibreTorrent") { _, _ ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=org.proninyaroslav.libretorrent"))
                    startActivity(intent)
                } catch (_: Exception) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=org.proninyaroslav.libretorrent"))
                    startActivity(intent)
                }
            }
            .setNeutralButton("Copy Link") { _, _ ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Torrent Link", result.link)
                clipboard.setPrimaryClip(clip)
                binding.textStatus.text = "Link copied to clipboard"
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun downloadWithClient(result: TorrentResult, client: TorrentClientOption) {
        // Enhance magnet link with trackers before downloading
        val downloadLink = if (result.isMagnetLink()) {
            val originalLink = result.getDownloadLink()
            // Clean and properly format the magnet link for LibreTorrent compatibility
            val cleanedLink = cleanMagnetLink(originalLink)
            val enhancedLink = trackerManager.enhanceMagnetLink(cleanedLink)
            
            // Show tracker stats in status
            val stats = trackerManager.getTrackerStats(enhancedLink)
            if (stats.total > 0) {
                binding.textStatus.text = "📡 Enhanced with ${stats.total} trackers (${stats.udp} UDP, ${stats.http} HTTP)"
            }
            
            enhancedLink
        } else {
            result.link
        }
        
        when (client.type) {
            "qbittorrent" -> {
                uiScope.launch(Dispatchers.IO) {
                    try {
                        val qb = QbittorrentClient(client.baseUrl!!, client.username!!, client.password!!)
                        qb.addTorrentFromUrl(downloadLink)
                        launch(Dispatchers.Main) {
                            binding.textStatus.text = "✓ Downloading in qBittorrent"
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        launch(Dispatchers.Main) {
                            binding.textStatus.text = "qB error: ${e.message}"
                        }
                    }
                }
            }
            else -> {
                // Use Android intent for other clients
                try {
                    // LibreTorrent requires proper URI encoding
                    val safeLink = if (downloadLink.startsWith("magnet:")) {
                        // Ensure the magnet link is properly encoded
                        ensureMagnetLinkEncoding(downloadLink)
                    } else {
                        downloadLink
                    }
                    
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeLink))
                    intent.setPackage(client.type)
                    startActivity(intent)
                    binding.textStatus.text = "✓ Opening in ${client.displayName}"
                } catch (e: Exception) {
                    // Fallback to generic intent
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadLink))
                        startActivity(intent)
                        binding.textStatus.text = "✓ Opening in default app"
                    } catch (e2: Exception) {
                        binding.textStatus.text = "Error: No app can handle this link"
                    }
                }
            }
        }
    }
    
    /**
     * Clean magnet link by removing invalid characters and ensuring proper format
     */
    private fun cleanMagnetLink(magnetLink: String): String {
        if (!magnetLink.startsWith("magnet:")) return magnetLink
        
        // Remove any leading/trailing whitespace
        var cleaned = magnetLink.trim()
        
        // Ensure magnet:? prefix is correct
        if (cleaned.startsWith("magnet:") && !cleaned.startsWith("magnet:?")) {
            cleaned = cleaned.replace("magnet:", "magnet:?")
        }
        
        // Fix double question marks
        cleaned = cleaned.replace("magnet:??", "magnet:?")
        
        // Ensure xt parameter exists (required for LibreTorrent)
        if (!cleaned.contains("xt=")) {
            // Try to extract info hash from other parts of the link
            val hashPattern = Regex("[a-fA-F0-9]{40}")
            val match = hashPattern.find(cleaned)
            if (match != null) {
                val hash = match.value.lowercase()
                if (!cleaned.contains("urn:btih:")) {
                    cleaned = "magnet:?xt=urn:btih:$hash${cleaned.substringAfter("magnet:?").let { if (it.isNotEmpty()) "&$it" else "" }}"
                }
            }
        }
        
        return cleaned
    }
    
    /**
     * Ensure magnet link has proper URI encoding for LibreTorrent
     */
    private fun ensureMagnetLinkEncoding(magnetLink: String): String {
        // LibreTorrent is picky about encoding - ensure special chars are properly encoded
        // But we don't want to double-encode
        var result = magnetLink
        
        // Don't modify if link looks already properly encoded
        if (magnetLink.contains("%") && !magnetLink.contains(" ")) {
            return magnetLink
        }
        
        // Encode spaces
        result = result.replace(" ", "%20")
        
        // Encode brackets that might cause issues
        result = result.replace("[", "%5B")
        result = result.replace("]", "%5D")
        
        return result
    }

    data class TorrentClientOption(
        val displayName: String,
        val type: String,
        val baseUrl: String?,
        val username: String?,
        val password: String?
    )

    private fun openTorrentLink(result: TorrentResult) {
        historyManager.addDownload(result)

        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val enabled = prefs.getBoolean(getString(R.string.pref_qb_enabled), false)

        if (enabled) {
            val baseUrl = prefs.getString(getString(R.string.pref_qb_base_url), "") ?: ""
            val username = prefs.getString(getString(R.string.pref_qb_username), "") ?: ""
            val password = prefs.getString(getString(R.string.pref_qb_password), "") ?: ""

            if (baseUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()) {
                uiScope.launch(Dispatchers.IO) {
                    try {
                        val qb = QbittorrentClient(baseUrl, username, password)
                        qb.addTorrentFromUrl(result.link)
                        launch(Dispatchers.Main) {
                            binding.textStatus.text = "Sent to qBittorrent."
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        launch(Dispatchers.Main) {
                            binding.textStatus.text =
                                "qB error: ${e.message}. Falling back to local app..."
                            openTorrentLinkExternal(result.link)
                        }
                    }
                }
                return
            }
        }

        openTorrentLinkExternal(result.link)
    }

    private fun openTorrentLinkExternal(link: String) {
        // Enhance magnet link with trackers before opening
        val downloadLink = if (link.startsWith("magnet:")) {
            val enhancedLink = trackerManager.enhanceMagnetLink(link)
            val stats = trackerManager.getTrackerStats(enhancedLink)
            if (stats.total > 0) {
                binding.textStatus.text = "📡 Enhanced with ${stats.total} trackers"
            }
            enhancedLink
        } else {
            link
        }
        
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadLink))
            startActivity(intent)
        } catch (e: Exception) {
            binding.textStatus.text = "No app to handle this link."
        }
    }

    private fun saveSearchQuery(query: String) {
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val existing = prefs.getStringSet("saved_searches", mutableSetOf())?.toMutableSet()
            ?: mutableSetOf()
        if (!existing.contains(query)) {
            existing.add(query)
            prefs.edit().putStringSet("saved_searches", existing).apply()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        networkMonitor.destroy()
    }
}