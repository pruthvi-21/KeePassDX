package com.kunzisoft.keepass.activities

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.SearchAutoComplete
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.MainCredentialDialogFragment
import com.kunzisoft.keepass.activities.fragments.GroupFragment
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.activities.legacy.DatabaseLockActivity
import com.kunzisoft.keepass.autofill.AutofillHelper
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.MainCredential
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.Type
import com.kunzisoft.keepass.database.search.SearchParameters
import com.kunzisoft.keepass.magikeyboard.MagikeyboardService
import com.kunzisoft.keepass.model.RegisterInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_ENTRY_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.NEW_NODES_KEY
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.getListNodesFromBundle
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.BACK_PREVIOUS_KEYBOARD_ACTION
import com.kunzisoft.keepass.utils.ColorUtils
import com.kunzisoft.keepass.utils.KeyboardUtil.showKeyboard
import com.kunzisoft.keepass.utils.getParcelableCompat
import com.kunzisoft.keepass.utils.readParcelableCompat
import com.kunzisoft.keepass.view.SearchFiltersView
import com.kunzisoft.keepass.view.ToolbarAction
import com.kunzisoft.keepass.view.WindowInsetPosition
import com.kunzisoft.keepass.view.applyWindowInsets
import com.kunzisoft.keepass.view.hideByFading
import com.kunzisoft.keepass.view.setTransparentNavigationBar
import com.kunzisoft.keepass.view.showActionErrorIfNeeded
import com.kunzisoft.keepass.view.updateLockPaddingLeft
import com.kunzisoft.keepass.viewmodels.GroupEditViewModel
import com.kunzisoft.keepass.viewmodels.GroupViewModel


class SearchActivity : DatabaseLockActivity(),
    GroupFragment.NodeClickListener,
    GroupFragment.NodesActionMenuListener,
    GroupFragment.GroupRefreshedListener,
    MainCredentialDialogFragment.AskMainCredentialDialogListener {

    // Views
    private var mainContainer: View? = null
    private var header: ViewGroup? = null
    private var footer: ViewGroup? = null
    private var coordinatorLayout: CoordinatorLayout? = null
    private var coordinatorError: CoordinatorLayout? = null
    private var lockView: View? = null
    private var toolbar: Toolbar? = null
    private var searchFiltersView: SearchFiltersView? = null
    private var toolbarAction: ToolbarAction? = null
    private var loadingView: ProgressBar? = null
    private var searchView: SearchView? = null

    private val mGroupViewModel: GroupViewModel by viewModels()
    private val mGroupEditViewModel: GroupEditViewModel by viewModels()

    private var mGroupFragment: GroupFragment? = null
    private var mRecyclingBinEnabled = false
    private var mRecyclingBinIsCurrentGroup = false

    private var actionNodeMode: ActionMode? = null

    // Manage group
    private var mSearchState: SearchState? = null
    private var mAutoSearch: Boolean = false // To mainly manage keyboard

    private var mMainGroup: Group? = null // Main group currently in memory
    private var mCurrentGroup: Group? = null // Group currently visible (search or main group)
    private var mOldGroupToUpdate: Group? = null

    private var mLockSearchListeners = false

    private val mOnSearchQueryTextListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean {
            onQueryTextChange(query)
            // Collapse the search filters
            searchFiltersView?.closeAdvancedFilters()
            // Close the keyboard
            WindowInsetsControllerCompat(window, window.decorView)
                .hide(WindowInsetsCompat.Type.ime())
            return true
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            if (newText != null && !mLockSearchListeners) {
                mSearchState?.let { searchState ->
                    searchState.searchParameters.searchQuery = newText
                    loadSearchGroup(searchState)
                }
            }
            return true
        }
    }
    private val mOnSearchFiltersChangeListener = object : ((SearchParameters) -> Unit) {
        override fun invoke(searchParameters: SearchParameters) {
            mSearchState?.let { searchState ->
                searchParameters.searchQuery = searchState.searchParameters.searchQuery
                searchState.searchParameters = searchParameters
                loadSearchGroup(searchState)
            }
        }
    }
    private val mOnSearchTextFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
        if (!mAutoSearch
            && hasFocus
            && PreferencesUtil.isKeyboardPreviousSearchEnable(this@SearchActivity)
        ) {
            // Change to the previous keyboard and show it
            sendBroadcast(Intent(BACK_PREVIOUS_KEYBOARD_ACTION))
            view.showKeyboard()
        }
    }

    private val mEntryActivityResultLauncher =
        EntryEditActivity.registerForEntryResult(this) { entryId ->
            entryId?.let {
                // Simply refresh the list when entry is updated
                loadGroup()
            } ?: Log.e(this.javaClass.name, "Entry cannot be retrieved in Activity Result")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Construct main view
        setContentView(R.layout.activity_search)

        // Initialize views
        header = findViewById(R.id.activity_group_header)
        footer = findViewById(R.id.activity_group_footer)
        coordinatorLayout = findViewById(R.id.group_coordinator)
        coordinatorError = findViewById(R.id.error_coordinator)
        toolbar = findViewById(R.id.toolbar)
        searchFiltersView = findViewById(R.id.search_filters)
        toolbarAction = findViewById(R.id.toolbar_action)
        lockView = findViewById(R.id.lock_button)
        loadingView = findViewById(R.id.loading)
        mainContainer = findViewById(R.id.activity_group_container_view)
        searchView = findViewById(R.id.search_view)
        searchView?.setOnQueryTextListener(mOnSearchQueryTextListener)


        toolbar?.title = " "
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        window.statusBarColor =
            ColorUtils.getColorFromAttr(this, R.attr.colorSurfaceVariant2, Color.BLACK)

        // To apply fit window with transparency
        setTransparentNavigationBar {
            header?.applyWindowInsets(WindowInsetPosition.TOP)
            coordinatorLayout?.applyWindowInsets(WindowInsetPosition.LEGIT_TOP)
            footer?.applyWindowInsets(WindowInsetPosition.BOTTOM)
        }

        lockView?.setOnClickListener {
            lockAndExit()
        }

        fixSearchViewHintColor()

        searchView?.requestFocus()
        searchView?.showKeyboard()

        mSearchState = SearchState(
            searchFiltersView?.searchParameters
                ?: PreferencesUtil.getDefaultSearchParameters(this), 0
        )

        searchFiltersView?.onParametersChangeListener = mOnSearchFiltersChangeListener
        searchFiltersView?.closeAdvancedFilters()

        mCurrentGroup = intent.extras?.getParcelableCompat(CURRENT_GROUP_KEY)
        mMainGroup = mCurrentGroup

        // Retrieve elements after an orientation change
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(OLD_GROUP_TO_UPDATE_KEY)) {
                mOldGroupToUpdate = savedInstanceState.getParcelableCompat(OLD_GROUP_TO_UPDATE_KEY)
                savedInstanceState.remove(OLD_GROUP_TO_UPDATE_KEY)
            }
        }

        // Initialize the fragment with the list
        mGroupFragment =
            supportFragmentManager.findFragmentByTag(GROUP_FRAGMENT_TAG) as GroupFragment?
        if (mGroupFragment == null)
            mGroupFragment = GroupFragment()

        // Attach fragment to content view
        supportFragmentManager.beginTransaction().replace(
            R.id.nodes_list_fragment_container,
            mGroupFragment!!,
            GROUP_FRAGMENT_TAG
        ).commit()

        // Observe current group (main or search group visible)
        mGroupViewModel.group.observe(this) {
            val currentGroup = it.group
            mCurrentGroup = currentGroup
            if (currentGroup.isVirtual) {
                mSearchState = SearchState(
                    it.searchParameters,
                    it.showFromPosition
                )
            }

            loadingView?.hideByFading()
        }

        mGroupViewModel.firstPositionVisible.observe(this) { firstPositionVisible ->
            mSearchState?.firstVisibleItem = firstPositionVisible
        }
    }

    @SuppressLint("PrivateResource", "RestrictedApi")
    private fun fixSearchViewHintColor() {
        val searchAutoComplete = searchView?.findViewById<SearchAutoComplete>(R.id.search_src_text)
        searchAutoComplete?.setHintTextColor(
            ResourcesCompat.getColor(
                resources,
                R.color.m3_textfield_label_color,
                theme
            )
        )
    }

    override fun viewToInvalidateTimeout(): View? {
        return coordinatorLayout
    }

    private fun loadMainGroup(groupState: GroupState) {
        mGroupViewModel.loadMainGroup(
            mDatabase,
            groupState.groupId,
            groupState.firstVisibleItem
        )
    }

    private fun loadSearchGroup(searchState: SearchState) {
        mGroupViewModel.loadSearchGroup(
            mDatabase,
            searchState.searchParameters,
            mMainGroup?.nodeId,
            searchState.firstVisibleItem
        )
    }

    private fun loadGroup() {
        val searchState = mSearchState
        when {
            searchState != null -> {
                finishNodeAction()
                loadSearchGroup(searchState)
            }

            else -> {
                loadMainGroup(GroupState(null, 0))
            }
        }
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        super.onDatabaseRetrieved(database)

        mGroupEditViewModel.setGroupNamesNotAllowed(database?.groupNamesNotAllowed)

        mRecyclingBinEnabled = !mDatabaseReadOnly
                && database?.isRecycleBinEnabled == true

        loadGroup()

        // Update view
        invalidateOptionsMenu()
    }

    override fun onDatabaseActionFinished(
        database: ContextualDatabase,
        actionTask: String,
        result: ActionRunnable.Result
    ) {
        super.onDatabaseActionFinished(database, actionTask, result)

        var entry: Entry? = null
        try {
            result.data?.getBundle(NEW_NODES_KEY)?.let { newNodesBundle ->
                entry = getListNodesFromBundle(database, newNodesBundle)[0] as Entry
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to retrieve entry action for selection", e)
        }

        when (actionTask) {
            ACTION_DATABASE_UPDATE_ENTRY_TASK -> {
                if (result.isSuccess) {
                    EntrySelectionHelper.doSpecialAction(intent,
                        {
                            // Standard not used after task
                        },
                        {
                            // Search not used
                        },
                        {
                            // Save not used
                        },
                        {
                            // Keyboard selection
                            entry?.let {
                                entrySelectedForKeyboardSelection(database, it)
                            }
                        },
                        { _, _ ->
                            // Autofill selection
                            entry?.let {
                                entrySelectedForAutofillSelection(database, it)
                            }
                        },
                        {
                            // Not use
                        }
                    )
                }
            }
        }

        coordinatorError?.showActionErrorIfNeeded(result)

        // Reload the group
        loadGroup()
        finishNodeAction()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mOldGroupToUpdate?.let {
            outState.putParcelable(OLD_GROUP_TO_UPDATE_KEY, it)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onGroupRefreshed() {
        val group = mCurrentGroup
        // Assign title
        if (group?.isVirtual == true) {
            searchFiltersView?.setNumbers(group.numberOfChildEntries)
            searchFiltersView?.setCurrentGroupText(mMainGroup?.title ?: "")
            searchFiltersView?.availableOther(mDatabase?.allowEntryCustomFields() ?: false)
            searchFiltersView?.availableTags(mDatabase?.allowTags() ?: false)
            searchFiltersView?.enableTags(mDatabase?.tagPool?.isNotEmpty() ?: false)
            searchFiltersView?.availableSearchableGroup(
                mDatabase?.allowCustomSearchableGroup() ?: false
            )
            searchFiltersView?.availableTemplates(mDatabase?.allowTemplatesGroup ?: false)
            searchFiltersView?.enableTemplates(mDatabase?.templatesGroup != null)
        } else {
            invalidateOptionsMenu()
        }
    }

    override fun onNodeClick(
        database: ContextualDatabase,
        node: Node
    ) {
        if (node.type == Type.ENTRY) {
            try {
                val entryVersioned = node as Entry
                EntrySelectionHelper.doSpecialAction(intent,
                    {
                        EntryActivity.launch(
                            this@SearchActivity,
                            database,
                            entryVersioned.nodeId,
                            mEntryActivityResultLauncher
                        )
                        // Do not reload group here
                    },
                    {
                        // Nothing here, a search is simply performed
                    },
                    { searchInfo ->
                        if (!database.isReadOnly) {
                            entrySelectedForSave(database, entryVersioned, searchInfo)
                            loadGroup()
                        } else
                            finish()
                    },
                    { searchInfo ->
                        if (!database.isReadOnly
                            && searchInfo != null
                            && PreferencesUtil.isKeyboardSaveSearchInfoEnable(this@SearchActivity)
                        ) {
                            updateEntryWithSearchInfo(database, entryVersioned, searchInfo)
                        }
                        entrySelectedForKeyboardSelection(database, entryVersioned)
                        loadGroup()
                    },
                    { searchInfo, _ ->
                        if (!database.isReadOnly
                            && searchInfo != null
                            && PreferencesUtil.isAutofillSaveSearchInfoEnable(this@SearchActivity)
                        ) {
                            updateEntryWithSearchInfo(database, entryVersioned, searchInfo)
                        }
                        entrySelectedForAutofillSelection(database, entryVersioned)
                        loadGroup()
                    },
                    { registerInfo ->
                        if (!database.isReadOnly) {
                            entrySelectedForRegistration(database, entryVersioned, registerInfo)
                            loadGroup()
                        } else
                            finish()
                    })
            } catch (e: ClassCastException) {
                Log.e(TAG, "Node can't be cast in Entry")
            }
        }
    }

    private fun entrySelectedForSave(
        database: ContextualDatabase,
        entry: Entry,
        searchInfo: SearchInfo
    ) {
        // Save to update the entry
        EntryEditActivity.launchToUpdateForSave(
            this@SearchActivity,
            database,
            entry.nodeId,
            searchInfo
        )
        onLaunchActivitySpecialMode()
    }

    private fun entrySelectedForKeyboardSelection(database: ContextualDatabase, entry: Entry) {
        // Populate Magikeyboard with entry
        MagikeyboardService.populateKeyboardAndMoveAppToBackground(
            this,
            entry.getEntryInfo(database)
        )
        onValidateSpecialMode()
    }

    private fun entrySelectedForAutofillSelection(database: ContextualDatabase, entry: Entry) {
        // Build response with the entry selected
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AutofillHelper.buildResponseAndSetResult(
                this,
                database,
                entry.getEntryInfo(database)
            )
        }
        onValidateSpecialMode()
    }

    private fun entrySelectedForRegistration(
        database: ContextualDatabase,
        entry: Entry,
        registerInfo: RegisterInfo?
    ) {
        // Registration to update the entry
        EntryEditActivity.launchToUpdateForRegistration(
            this@SearchActivity,
            database,
            entry.nodeId,
            registerInfo
        )
        onLaunchActivitySpecialMode()
    }

    private fun updateEntryWithSearchInfo(
        database: ContextualDatabase,
        entry: Entry,
        searchInfo: SearchInfo
    ) {
        val newEntry = Entry(entry)
        val entryInfo = newEntry.getEntryInfo(
            database,
            raw = true,
            removeTemplateConfiguration = false
        )
        val modification = entryInfo.saveSearchInfo(database, searchInfo)
        newEntry.setEntryInfo(database, entryInfo)
        if (modification) {
            updateEntry(entry, newEntry)
        }
    }

    private fun finishNodeAction() {
        actionNodeMode?.finish()
    }

    override fun onNodeSelected(
        database: ContextualDatabase,
        nodes: List<Node>
    ): Boolean {
        if (nodes.isNotEmpty()) {
            if (actionNodeMode == null || toolbarAction?.getSupportActionModeCallback() == null) {
                mGroupFragment?.actionNodesCallback(
                    database,
                    nodes,
                    this
                ) { _ ->
                    actionNodeMode = null
                }?.let {
                    actionNodeMode = toolbarAction?.startSupportActionMode(it)
                }
            } else {
                actionNodeMode?.invalidate()
            }
        } else {
            finishNodeAction()
        }
        return true
    }

    override fun onOpenMenuClick(
        database: ContextualDatabase,
        node: Node
    ): Boolean {
        finishNodeAction()
        onNodeClick(database, node)
        return true
    }

    override fun onEditMenuClick(
        database: ContextualDatabase,
        node: Node
    ): Boolean {
        finishNodeAction()
        when (node.type) {
            Type.ENTRY -> {
                EntryEditActivity.launchToUpdate(
                    this@SearchActivity,
                    database,
                    (node as Entry).nodeId,
                    mEntryActivityResultLauncher
                )
            }

            else -> {}
        }
        return true
    }

    override fun onCopyMenuClick(
        database: ContextualDatabase,
        nodes: List<Node>
    ): Boolean {
        return true
    }

    override fun onMoveMenuClick(
        database: ContextualDatabase,
        nodes: List<Node>
    ): Boolean {
        return true
    }

    override fun onPasteMenuClick(
        database: ContextualDatabase,
        pasteMode: GroupFragment.PasteMode?,
        nodes: List<Node>
    ): Boolean {
        return true
    }

    override fun onDeleteMenuClick(
        database: ContextualDatabase,
        nodes: List<Node>
    ): Boolean {
        deleteNodes(nodes)
        finishNodeAction()
        return true
    }

    override fun onAskMainCredentialDialogPositiveClick(
        databaseUri: Uri?,
        mainCredential: MainCredential
    ) {
        databaseUri?.let {
            mergeDatabaseFrom(it, mainCredential)
        }
    }

    override fun onAskMainCredentialDialogNegativeClick(
        databaseUri: Uri?,
        mainCredential: MainCredential
    ) {
    }

    override fun onResume() {
        super.onResume()
        searchView?.setOnQueryTextListener(mOnSearchQueryTextListener)

        // Show the lock button
        lockView?.visibility = if (PreferencesUtil.showLockDatabaseButton(this)) {
            View.VISIBLE
        } else {
            View.GONE
        }
        // Padding if lock button visible
        toolbarAction?.updateLockPaddingLeft()

        loadGroup()
    }

    override fun onPause() {
        super.onPause()

        finishNodeAction()
        searchView?.setOnQueryTextListener(null)
        searchFiltersView?.saveSearchParameters()
    }

    private fun addSearchQueryInSearchView(searchQuery: String) {
        searchView?.setOnQueryTextListener(null)
        if (mAutoSearch)
            searchView?.clearFocus()
        searchView?.setQuery(searchQuery, false)
        searchView?.setOnQueryTextListener(mOnSearchQueryTextListener)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        mLockSearchListeners = true
        searchView?.apply {
            setOnQueryTextFocusChangeListener(mOnSearchTextFocusChangeListener)
            val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager?
            (searchManager?.getSearchableInfo(
                ComponentName(this@SearchActivity, SearchActivity::class.java)
            ))?.let { searchableInfo ->
                setSearchableInfo(searchableInfo)
            }
            val searchState = mSearchState
            // already open
            if (searchState != null) {
                addSearchQueryInSearchView(searchState.searchParameters.searchQuery)
                searchFiltersView?.searchParameters = searchState.searchParameters
            }
        }
        mLockSearchListeners = false
        mAutoSearch = false

        super.onCreateOptionsMenu(menu)

        return true
    }

    override fun hideHomeButtonIfModeIsNotDefault(): Boolean {
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onDatabaseBackPressed()
                return true
            }

            R.id.menu_search -> {
                return true
            }

            R.id.menu_empty_recycle_bin -> {
                if (mRecyclingBinEnabled && mRecyclingBinIsCurrentGroup) {
                    mMainGroup?.getChildren()?.let { listChildren ->
                        // Automatically delete all elements
                        deleteNodes(listChildren, true)
                        finishNodeAction()
                    }
                }
                return true
            }

            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onCancelSpecialMode() {
        super.onCancelSpecialMode()
        loadGroup()
    }

    override fun onDatabaseBackPressed() {
        if (mGroupFragment?.nodeActionSelectionMode == true) {
            finishNodeAction()
        } else {
            super.onDatabaseBackPressed()
        }
    }

    data class SearchState(
        var searchParameters: SearchParameters,
        var firstVisibleItem: Int?
    ) : Parcelable {

        private constructor(parcel: Parcel) : this(
            parcel.readParcelableCompat<SearchParameters>() ?: SearchParameters(),
            parcel.readInt()
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeParcelable(searchParameters, flags)
            parcel.writeInt(firstVisibleItem ?: 0)
        }

        override fun describeContents() = 0

        companion object CREATOR : Parcelable.Creator<SearchState> {
            override fun createFromParcel(parcel: Parcel): SearchState {
                return SearchState(parcel)
            }

            override fun newArray(size: Int): Array<SearchState?> {
                return arrayOfNulls(size)
            }
        }
    }

    data class GroupState(
        var groupId: NodeId<*>?,
        var firstVisibleItem: Int?
    ) : Parcelable {

        private constructor(parcel: Parcel) : this(
            parcel.readParcelableCompat<NodeId<*>>(),
            parcel.readInt()
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeParcelable(groupId, flags)
            parcel.writeInt(firstVisibleItem ?: 0)
        }

        override fun describeContents() = 0

        companion object CREATOR : Parcelable.Creator<GroupState> {
            override fun createFromParcel(parcel: Parcel): GroupState {
                return GroupState(parcel)
            }

            override fun newArray(size: Int): Array<GroupState?> {
                return arrayOfNulls(size)
            }
        }
    }

    companion object {
        private val TAG = SearchActivity::class.java.name

        private const val GROUP_STATE_KEY = "GROUP_STATE_KEY"
        private const val GROUP_FRAGMENT_TAG = "GROUP_FRAGMENT_TAG"
        private const val OLD_GROUP_TO_UPDATE_KEY = "OLD_GROUP_TO_UPDATE_KEY"
        private const val CURRENT_GROUP_KEY = "CURRENT_GROUP_KEY"

        private fun buildIntent(
            context: Context,
            groupState: GroupState?,
            intentBuildLauncher: (Intent) -> Unit
        ) {
            val intent = Intent(context, SearchActivity::class.java)
            if (groupState != null) {
                intent.putExtra(GROUP_STATE_KEY, groupState)
            }
            intentBuildLauncher.invoke(intent)
        }

        private fun checkTimeAndBuildIntent(
            context: Context,
            groupState: GroupState?,
            intentBuildLauncher: (Intent) -> Unit
        ) {
            if (TimeoutHelper.checkTime(context)) {
                buildIntent(context, groupState, intentBuildLauncher)
            }
        }

        fun launch(
            context: Context,
            currentGroup: Group
        ) {
            checkTimeAndBuildIntent(context, null) { intent ->
                intent.putExtra(CURRENT_GROUP_KEY, currentGroup)
                context.startActivity(intent)
            }
        }
    }
}
