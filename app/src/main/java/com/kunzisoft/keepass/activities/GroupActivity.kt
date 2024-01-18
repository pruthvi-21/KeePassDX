package com.kunzisoft.keepass.activities

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
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
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.GroupDialogFragment
import com.kunzisoft.keepass.activities.dialogs.GroupEditDialogFragment
import com.kunzisoft.keepass.activities.dialogs.MainCredentialDialogFragment
import com.kunzisoft.keepass.activities.fragments.EntriesListFragment
import com.kunzisoft.keepass.activities.fragments.GroupFragment
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.activities.helpers.ExternalFileHelper
import com.kunzisoft.keepass.activities.helpers.SpecialMode
import com.kunzisoft.keepass.activities.legacy.DatabaseLockActivity
import com.kunzisoft.keepass.adapters.BreadcrumbAdapter
import com.kunzisoft.keepass.autofill.AutofillComponent
import com.kunzisoft.keepass.autofill.AutofillHelper
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.MainCredential
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.element.node.Type
import com.kunzisoft.keepass.database.helper.SearchHelper
import com.kunzisoft.keepass.magikeyboard.MagikeyboardService
import com.kunzisoft.keepass.model.GroupInfo
import com.kunzisoft.keepass.model.RegisterInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_ENTRY_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.NEW_NODES_KEY
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.getListNodesFromBundle
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.settings.SettingsActivity
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.ColorUtils
import com.kunzisoft.keepass.utils.getParcelableCompat
import com.kunzisoft.keepass.utils.getParcelableExtraCompat
import com.kunzisoft.keepass.utils.getParcelableList
import com.kunzisoft.keepass.utils.putParcelableList
import com.kunzisoft.keepass.utils.readParcelableCompat
import com.kunzisoft.keepass.view.AddNodeButtonView
import com.kunzisoft.keepass.view.DrawerLayout
import com.kunzisoft.keepass.view.NavigationDatabaseView
import com.kunzisoft.keepass.view.ToolbarAction
import com.kunzisoft.keepass.view.WindowInsetPosition
import com.kunzisoft.keepass.view.applyWindowInsets
import com.kunzisoft.keepass.view.hideByFading
import com.kunzisoft.keepass.view.setTransparentNavigationBar
import com.kunzisoft.keepass.view.showActionErrorIfNeeded
import com.kunzisoft.keepass.view.updateLockPaddingLeft
import com.kunzisoft.keepass.viewmodels.GroupEditViewModel
import com.kunzisoft.keepass.viewmodels.GroupViewModel


class GroupActivity : DatabaseLockActivity(),
    GroupFragment.NodeClickListener,
    GroupFragment.NodesActionMenuListener,
    GroupFragment.OnScrollListener,
    GroupFragment.GroupRefreshedListener,
    NavigationDatabaseView.OnNavItemClickedListener,
    MainCredentialDialogFragment.AskMainCredentialDialogListener {

    // Views
    private var mainContainer: View? = null
    private var header: ViewGroup? = null
    private var footer: ViewGroup? = null
    private var drawerLayout: DrawerLayout? = null
    private var databaseNavView: NavigationDatabaseView? = null
    private var coordinatorLayout: CoordinatorLayout? = null
    private var coordinatorError: CoordinatorLayout? = null
    private var lockView: View? = null
    private var toolbar: Toolbar? = null
    private var customSearchBarView: View? = null
    private var databaseModifiedView: ImageView? = null
    private var databaseColorView: ImageView? = null
    private var databaseNameView: TextView? = null
    private var toolbarAction: ToolbarAction? = null
    private var addNodeButtonView: AddNodeButtonView? = null
    private var breadcrumbListView: RecyclerView? = null
    private var loadingView: ProgressBar? = null

    private val mGroupViewModel: GroupViewModel by viewModels()
    private val mGroupEditViewModel: GroupEditViewModel by viewModels()

    private var mBreadcrumbAdapter: BreadcrumbAdapter? = null

    private var mGroupFragment = GroupFragment()
    private var mEntriesFragment = EntriesListFragment()
    private var mRecyclingBinEnabled = false
    private var mRecyclingBinIsCurrentGroup = false
    private var mRequestStartupSearch = true

    private var actionNodeMode: ActionMode? = null

    // Manage merge
    private var mExternalFileHelper: ExternalFileHelper? = null

    // Manage group
    private var mAutoSearch: Boolean = false // To mainly manage keyboard
    private var mMainGroupState: GroupState? = null // Group state, not a search
    private var mRootGroup: Group? = null // Root group in the tree
    private var mMainGroup: Group? = null // Main group currently in memory
    private var mCurrentGroup: Group? = null // Group currently visible (search or main group)
    private var mPreviousGroupsIds = mutableListOf<GroupState>()
    private var mOldGroupToUpdate: Group? = null

    private val mEntryActivityResultLauncher =
        EntryEditActivity.registerForEntryResult(this) { entryId ->
            entryId?.let {
                // Simply refresh the list when entry is updated
                loadGroup()
                mEntriesFragment.updateList(true)
            } ?: Log.e(this.javaClass.name, "Entry cannot be retrieved in Activity Result")
        }

    private var mIconSelectionActivityResultLauncher =
        IconPickerActivity.registerIconSelectionForResult(this) { icon ->
            // To create tree dialog for icon
            mGroupEditViewModel.selectIcon(icon)
        }

    private var mAutofillActivityResultLauncher: ActivityResultLauncher<Intent>? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            AutofillHelper.buildActivityResultLauncher(this)
        else null

    private var CLOSE_DRAWER = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Construct main view
        setContentView(layoutInflater.inflate(R.layout.activity_group, null))

        // Initialize views
        header = findViewById(R.id.activity_group_header)
        footer = findViewById(R.id.activity_group_footer)
        drawerLayout = findViewById(R.id.drawer_layout)
        databaseNavView = findViewById(R.id.database_nav_view)
        coordinatorLayout = findViewById(R.id.group_coordinator)
        coordinatorError = findViewById(R.id.error_coordinator)
        addNodeButtonView = findViewById(R.id.add_node_button)
        toolbar = findViewById(R.id.toolbar)
        customSearchBarView = findViewById(R.id.custom_search_bar_view)
        databaseModifiedView = findViewById(R.id.database_modified)
        databaseColorView = findViewById(R.id.database_color)
        databaseNameView = findViewById(R.id.database_name)
        breadcrumbListView = findViewById(R.id.breadcrumb_list)
        toolbarAction = findViewById(R.id.toolbar_action)
        lockView = findViewById(R.id.lock_button)
        loadingView = findViewById(R.id.loading)

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

        toolbar?.title = ""
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            0, 0
        )
        toggle.syncState()

        mainContainer = findViewById(R.id.activity_group_container_view)

        drawerLayout?.attachContainer(mainContainer)
        drawerLayout?.setNavigationView(databaseNavView)

        // Manage 'merge from" and "save to"
        mExternalFileHelper = ExternalFileHelper(this)
        mExternalFileHelper?.buildOpenDocument { uri ->
            launchDialogToAskMainCredential(uri)
        }
        mExternalFileHelper?.buildCreateDocument("application/x-keepass") { uri ->
            uri?.let {
                saveDatabaseTo(it)
            }
        }

        mBreadcrumbAdapter = BreadcrumbAdapter(this).apply {
            // Open group on breadcrumb click
            onItemClickListener = { node, _ ->
                // If last item & not a virtual root group
                val currentGroup = mMainGroup
                if (currentGroup != null && node == currentGroup
                    && (currentGroup != mDatabase?.rootGroup
                            || mDatabase?.rootGroupIsVirtual == false)
                ) {
                    finishNodeAction()
                    launchDialogToShowGroupInfo(currentGroup)
                } else {
                    if (mGroupFragment.nodeActionSelectionMode) {
                        finishNodeAction()
                    }
                    mDatabase?.let { database ->
                        onNodeClick(database, node)
                    }
                }
            }
            onLongItemClickListener = { node, position ->
                val currentGroup = mMainGroup
                if (currentGroup != null && node == currentGroup
                    && (currentGroup != mDatabase?.rootGroup
                            || mDatabase?.rootGroupIsVirtual == false)
                ) {
                    finishNodeAction()
                    launchDialogForGroupUpdate(currentGroup)
                } else {
                    onItemClickListener?.invoke(node, position)
                }
            }
        }
        breadcrumbListView?.apply {
            adapter = mBreadcrumbAdapter
        }

        // Retrieve group if defined at launch
        manageIntent(intent)

        // Retrieve elements after an orientation change
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(REQUEST_STARTUP_SEARCH_KEY)) {
                mRequestStartupSearch = savedInstanceState.getBoolean(REQUEST_STARTUP_SEARCH_KEY)
                savedInstanceState.remove(REQUEST_STARTUP_SEARCH_KEY)
            }
            if (savedInstanceState.containsKey(OLD_GROUP_TO_UPDATE_KEY)) {
                mOldGroupToUpdate = savedInstanceState.getParcelableCompat(OLD_GROUP_TO_UPDATE_KEY)
                savedInstanceState.remove(OLD_GROUP_TO_UPDATE_KEY)
            }
            CLOSE_DRAWER = savedInstanceState.getBoolean(CLOSE_DRAWER_ARG, false)
        }

        // Retrieve previous groups
        if (savedInstanceState != null && savedInstanceState.containsKey(PREVIOUS_GROUPS_IDS_KEY)) {
            try {
                mPreviousGroupsIds = savedInstanceState.getParcelableList(PREVIOUS_GROUPS_IDS_KEY)
                    ?: mutableListOf()
            } catch (e: Exception) {
                Log.e(TAG, "Unable to retrieve previous groups", e)
            }
            savedInstanceState.remove(PREVIOUS_GROUPS_IDS_KEY)
        }

        //initial selection
        selectNavigationItem(PreferencesUtil.getActiveHomeTab(this))

        // Observe main group
        mGroupViewModel.mainGroup.observe(this) {
            val mainGroup = it.group
            mMainGroup = mainGroup
            mRecyclingBinIsCurrentGroup = it.isRecycleBin
            // Save group state
            mMainGroupState = GroupState(mainGroup.nodeId, it.showFromPosition)
            // Update last access time.
            mainGroup.touch(modified = false, touchParents = false)
        }

        // Observe current group (main or search group visible)
        mGroupViewModel.group.observe(this) {
            val currentGroup = it.group
            mCurrentGroup = currentGroup
            // Main and search groups in activity are managed with another variables
            // to keep values during orientation

            loadingView?.hideByFading()
        }

        mGroupViewModel.firstPositionVisible.observe(this) { firstPositionVisible ->
            mMainGroupState?.firstVisibleItem = firstPositionVisible
        }

        mGroupEditViewModel.requestIconSelection.observe(this) { iconImage ->
            IconPickerActivity.launch(
                this@GroupActivity,
                iconImage,
                mIconSelectionActivityResultLauncher
            )
        }

        mGroupEditViewModel.requestDateTimeSelection.observe(this) { dateInstant ->
            if (dateInstant.type == DateInstant.Type.TIME) {
                // Launch the time picker
                MaterialTimePicker.Builder().build().apply {
                    addOnPositiveButtonClickListener {
                        mGroupEditViewModel.selectTime(this.hour, this.minute)
                    }
                    show(supportFragmentManager, "TimePickerFragment")
                }
            } else {
                // Launch the date picker
                MaterialDatePicker.Builder.datePicker().build().apply {
                    addOnPositiveButtonClickListener {
                        mGroupEditViewModel.selectDate(it)
                    }
                    show(supportFragmentManager, "DatePickerFragment")
                }
            }
        }

        mGroupEditViewModel.onGroupCreated.observe(this) { groupInfo ->
            if (groupInfo.title.isNotEmpty()) {
                mMainGroup?.let { parentGroup ->
                    createGroup(parentGroup, groupInfo)
                }
            }
        }

        mGroupEditViewModel.onGroupUpdated.observe(this) { groupInfo ->
            if (groupInfo.title.isNotEmpty()) {
                groupInfo.id?.let { groupId ->
                    mDatabase?.getGroupById(NodeIdUUID(groupId))?.let { oldGroupToUpdate ->
                        updateGroup(oldGroupToUpdate, groupInfo)
                    }
                }
            }
        }

        databaseNavView?.onNavItemClickedListener = this

        // Add listeners to the add buttons
        addNodeButtonView?.setAddGroupClickListener {
            val selectedGroup = if (databaseNavView?.navItemGroups?.isChecked == true) mMainGroup
            else mRootGroup
            selectedGroup?.let { currentGroup ->
                launchDialogForGroupCreation(currentGroup)
            }
        }
        addNodeButtonView?.setAddEntryClickListener {
            mDatabase?.let { database ->
                val selectedGroup =
                    if (databaseNavView?.navItemGroups?.isChecked == true) mMainGroup
                    else mRootGroup
                selectedGroup?.let { currentGroup ->
                    EntrySelectionHelper.doSpecialAction(intent,
                        {
                            EntryEditActivity.launchToCreate(
                                this@GroupActivity,
                                database,
                                currentGroup.nodeId,
                                mEntryActivityResultLauncher
                            )
                        },
                        {
                            // Search not used
                        },
                        { searchInfo ->
                            EntryEditActivity.launchToCreateForSave(
                                this@GroupActivity,
                                database,
                                currentGroup.nodeId,
                                searchInfo
                            )
                            onLaunchActivitySpecialMode()
                        },
                        { searchInfo ->
                            EntryEditActivity.launchForKeyboardSelectionResult(
                                this@GroupActivity,
                                database,
                                currentGroup.nodeId,
                                searchInfo
                            )
                            onLaunchActivitySpecialMode()
                        },
                        { searchInfo, autofillComponent ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                EntryEditActivity.launchForAutofillResult(
                                    this@GroupActivity,
                                    database,
                                    mAutofillActivityResultLauncher,
                                    autofillComponent,
                                    currentGroup.nodeId,
                                    searchInfo
                                )
                                onLaunchActivitySpecialMode()
                            } else {
                                onCancelSpecialMode()
                            }
                        },
                        { searchInfo ->
                            EntryEditActivity.launchToCreateForRegistration(
                                this@GroupActivity,
                                database,
                                currentGroup.nodeId,
                                searchInfo
                            )
                            onLaunchActivitySpecialMode()
                        }
                    )
                }
            }
        }
    }

    private fun showFragment(fragment: Fragment, tag: String) {
        val transaction = supportFragmentManager.beginTransaction()
        val existingFragment = supportFragmentManager.findFragmentByTag(tag)

        if (existingFragment == null) {
            transaction.add(R.id.fragment_container, fragment, tag)
        } else {
            transaction.show(existingFragment)
        }

        //hide other fragments
        for (frag in supportFragmentManager.fragments) {
            if (frag != null && frag != existingFragment) {
                transaction.hide(frag)
            }
        }

        transaction.commit()
    }

    override fun onNavItemClicked(itemId: Int) {
        when (itemId) {
            R.id.nav_item_groups -> selectNavigationItem(NavigationItem.GROUPS)
            R.id.nav_item_entries -> selectNavigationItem(NavigationItem.ENTRIES)

            R.id.nav_item_lock -> lockAndExit()
            R.id.nav_item_settings -> {
                // To avoid flickering when launch settings in a LockingActivity
                SettingsActivity.launch(this@GroupActivity, true)
                CLOSE_DRAWER = true
            }

            R.id.nav_item_about -> {
                startActivity(
                    Intent(this@GroupActivity, AboutActivity::class.java)
                )
                CLOSE_DRAWER = true
            }

            R.id.nav_item_save_database -> {
                saveDatabase()
                drawerLayout?.close()
            }

            R.id.nav_item_merge_database -> {
                mergeDatabase()
                drawerLayout?.close()
            }

            R.id.nav_item_reload_database -> {
                reloadDatabase()
                drawerLayout?.close()
            }

            R.id.nav_item_merge_from -> {
                mExternalFileHelper?.openDocument()
                CLOSE_DRAWER = true
            }

            R.id.nav_item_save_copy_to -> {
                mExternalFileHelper?.createDocument(
                    getString(R.string.database_file_name_default) +
                            getString(R.string.database_file_name_copy) +
                            mDatabase?.defaultFileExtension
                )
                CLOSE_DRAWER = true
            }
        }
    }

    private fun selectNavigationItem(item: NavigationItem) {
        when (item) {
            NavigationItem.GROUPS -> {
                databaseNavView?.navItemGroups?.isChecked = true
                databaseNavView?.navItemEntries?.isChecked = false
                showFragment(mGroupFragment, GROUP_FRAGMENT_TAG)

                drawerLayout?.close()

                finishNodeAction()
                breadcrumbListView?.isVisible = true
                addNodeButtonView?.enableAddGroup(true)
                PreferencesUtil.setActiveHomeTab(this, NavigationItem.GROUPS)
            }

            NavigationItem.ENTRIES -> {
                databaseNavView?.navItemGroups?.isChecked = false
                databaseNavView?.navItemEntries?.isChecked = true
                showFragment(mEntriesFragment, ENTRIES_FRAGMENT_TAG)

                mEntriesFragment.updateList()
                drawerLayout?.close()

                finishNodeAction()
                breadcrumbListView?.isVisible = false
                addNodeButtonView?.enableAddGroup(false)
                PreferencesUtil.setActiveHomeTab(this, NavigationItem.ENTRIES)
            }
        }
    }

    override fun viewToInvalidateTimeout(): View? {
        return drawerLayout
    }

    private fun loadMainGroup(groupState: GroupState) {
        mGroupViewModel.loadMainGroup(
            mDatabase,
            groupState.groupId,
            groupState.firstVisibleItem
        )
    }

    private fun loadGroup() {
        val currentGroupState = mMainGroupState
        when {
            currentGroupState != null -> {
                loadMainGroup(currentGroupState)
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

        mRootGroup = database?.rootGroup
        loadGroup()

        // Update view
        database?.let {
            mBreadcrumbAdapter?.iconDrawableFactory = it.iconDrawableFactory
        }
        refreshDatabaseViews()
        invalidateOptionsMenu()

        //invalidate navView menu
        databaseNavView?.let {
            if (mDatabaseReadOnly) {
                it.navItemSaveDb?.isVisible = false
                it.navItemMergeDb?.isVisible = false
            }
            if (!mMergeDataAllowed) {
                it.navItemMergeDb?.isVisible = false
            }
            if (mSpecialMode != SpecialMode.DEFAULT) {
                it.navItemMergeDb?.isVisible = false
                it.navItemReloadDb?.isVisible = false
            }
        }

        customSearchBarView?.setOnClickListener {
            mCurrentGroup?.let { group ->
                SearchActivity.launch(this, group)
            }
        }
    }

    private fun refreshDatabaseViews() {
        mDatabase?.let {
            val databaseName = it.name.ifEmpty { getString(R.string.database) }
            databaseNavView?.setDatabaseName(databaseName)
            databaseNameView?.text = databaseName
            databaseNavView?.setDatabasePath(it.fileUri?.toString())
            databaseNavView?.setDatabaseVersion(it.version)
            val modified = it.dataModifiedSinceLastLoading
            databaseNavView?.setDatabaseModifiedSinceLastLoading(modified)
            databaseModifiedView?.isVisible = modified
            val customColor = it.customColor
            databaseNavView?.setDatabaseColor(customColor)
            if (customColor != null) {
                databaseColorView?.visibility = View.VISIBLE
                databaseColorView?.setColorFilter(
                    customColor,
                    PorterDuff.Mode.SRC_IN
                )
            } else {
                databaseColorView?.visibility = View.GONE
            }
        }
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

    /**
     * Transform the AUTO_SEARCH_KEY in ACTION_SEARCH, return true if AUTO_SEARCH_KEY was present
     */
    private fun transformSearchInfoIntent(intent: Intent) {
        // To relaunch the activity as ACTION_SEARCH
        val searchInfo: SearchInfo? = EntrySelectionHelper.retrieveSearchInfoFromIntent(intent)
        val autoSearch = intent.getBooleanExtra(AUTO_SEARCH_KEY, false)
        intent.removeExtra(AUTO_SEARCH_KEY)
        if (searchInfo != null && autoSearch) {
            intent.action = Intent.ACTION_SEARCH
            intent.putExtra(SearchManager.QUERY, searchInfo.toString())
        }
    }

    private fun manageIntent(intent: Intent?) {
        intent?.let {
            if (intent.extras?.containsKey(GROUP_STATE_KEY) == true) {
                mMainGroupState = intent.getParcelableExtraCompat(GROUP_STATE_KEY)
                intent.removeExtra(GROUP_STATE_KEY)
            }
            // To transform KEY_SEARCH_INFO in ACTION_SEARCH
            transformSearchInfoIntent(intent)
            // Get search query
            if (intent.action == Intent.ACTION_SEARCH) {
                mAutoSearch = true
                val stringQuery =
                    intent.getStringExtra(SearchManager.QUERY)?.trim { it <= ' ' } ?: ""
                intent.action = Intent.ACTION_DEFAULT
                intent.removeExtra(SearchManager.QUERY)
            } else if (mRequestStartupSearch
                && PreferencesUtil.automaticallyFocusSearch(this@GroupActivity)
            ) {
                // Expand the search view if defined in settings
                // To request search only one time
                mRequestStartupSearch = false
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "setNewIntent: $intent")
        setIntent(intent)
        manageIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableList(PREVIOUS_GROUPS_IDS_KEY, mPreviousGroupsIds)
        mOldGroupToUpdate?.let {
            outState.putParcelable(OLD_GROUP_TO_UPDATE_KEY, it)
        }
        outState.putBoolean(REQUEST_STARTUP_SEARCH_KEY, mRequestStartupSearch)
        outState.putBoolean(CLOSE_DRAWER_ARG, CLOSE_DRAWER)
        super.onSaveInstanceState(outState)
    }

    override fun onGroupRefreshed() {
        val group = mCurrentGroup
        // Assign title
        if (group?.isVirtual == false) {
            // Add breadcrumb
            setBreadcrumbNode(group)
            refreshDatabaseViews()
            invalidateOptionsMenu()
        }
        initAddButton(group)
    }

    private fun setBreadcrumbNode(group: Group?) {
        mBreadcrumbAdapter?.apply {
            setNode(group)
            breadcrumbListView?.scrollToPosition(itemCount - 1)
        }
    }

    private fun initAddButton(group: Group?) {
        addNodeButtonView?.apply {
            closeButtonIfOpen()
            // To enable add button
            val addGroupEnabled = !mDatabaseReadOnly && group?.isVirtual != true
            var addEntryEnabled = !mDatabaseReadOnly && group?.isVirtual != true
            group?.let {
                if (!it.allowAddEntryIfIsRoot)
                    addEntryEnabled = it != mRootGroup && addEntryEnabled
            }
            enableAddGroup(addGroupEnabled)
            enableAddEntry(addEntryEnabled)
            if (group?.isVirtual == true)
                hideButton()
            else if (actionNodeMode == null)
                showButton()
        }
    }

    override fun onScrolled(dy: Int) {
        if (actionNodeMode == null)
            addNodeButtonView?.hideOrShowButtonOnScrollListener(dy)
    }

    override fun onNodeClick(
        database: ContextualDatabase,
        node: Node
    ) {
        when (node.type) {
            Type.GROUP -> try {
                val group = node as Group
                // Save the last not virtual group and it's position
                if (mCurrentGroup?.isVirtual == false) {
                    mMainGroupState?.let {
                        mPreviousGroupsIds.add(it)
                    }
                }
                // Open child group
                loadMainGroup(GroupState(group.nodeId, 0))
            } catch (e: ClassCastException) {
                Log.e(TAG, "Node can't be cast in Group")
            }

            Type.ENTRY -> try {
                val entryVersioned = node as Entry
                EntrySelectionHelper.doSpecialAction(intent,
                    {
                        EntryActivity.launch(
                            this@GroupActivity,
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
                            && PreferencesUtil.isKeyboardSaveSearchInfoEnable(this@GroupActivity)
                        ) {
                            updateEntryWithSearchInfo(database, entryVersioned, searchInfo)
                        }
                        entrySelectedForKeyboardSelection(database, entryVersioned)
                        loadGroup()
                    },
                    { searchInfo, _ ->
                        if (!database.isReadOnly
                            && searchInfo != null
                            && PreferencesUtil.isAutofillSaveSearchInfoEnable(this@GroupActivity)
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
            this@GroupActivity,
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
            this@GroupActivity,
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
                mGroupFragment.actionNodesCallback(
                    database,
                    nodes,
                    this
                ) { _ ->
                    actionNodeMode = null
                    addNodeButtonView?.showButton()
                }.let {
                    actionNodeMode = toolbarAction?.startSupportActionMode(it)
                }
            } else {
                actionNodeMode?.invalidate()
            }
            addNodeButtonView?.hideButton()
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
            Type.GROUP -> {
                launchDialogForGroupUpdate(node as Group)
            }

            Type.ENTRY -> {
                EntryEditActivity.launchToUpdate(
                    this@GroupActivity,
                    database,
                    (node as Entry).nodeId,
                    mEntryActivityResultLauncher
                )
            }
        }
        return true
    }

    private fun launchDialogToShowGroupInfo(group: Group) {
        GroupDialogFragment.launch(group.getGroupInfo())
            .show(supportFragmentManager, GroupDialogFragment.TAG_SHOW_GROUP)
    }

    private fun launchDialogForGroupCreation(group: Group) {
        GroupEditDialogFragment.create(GroupInfo().apply {
            if (group.allowAddNoteInGroup) {
                notes = ""
            }
        }).show(supportFragmentManager, GroupEditDialogFragment.TAG_CREATE_GROUP)
    }

    private fun launchDialogForGroupUpdate(group: Group) {
        mOldGroupToUpdate = group
        GroupEditDialogFragment.update(group.getGroupInfo())
            .show(supportFragmentManager, GroupEditDialogFragment.TAG_CREATE_GROUP)
    }

    private fun launchDialogToAskMainCredential(uri: Uri?) {
        MainCredentialDialogFragment.getInstance(uri)
            .show(supportFragmentManager, MainCredentialDialogFragment.TAG_ASK_MAIN_CREDENTIAL)
    }

    override fun onCopyMenuClick(
        database: ContextualDatabase,
        nodes: List<Node>
    ): Boolean {
        actionNodeMode?.invalidate()
        loadGroup()
        return true
    }

    override fun onMoveMenuClick(
        database: ContextualDatabase,
        nodes: List<Node>
    ): Boolean {
        actionNodeMode?.invalidate()
        loadGroup()
        return true
    }

    override fun onPasteMenuClick(
        database: ContextualDatabase,
        pasteMode: GroupFragment.PasteMode?,
        nodes: List<Node>
    ): Boolean {
        when (pasteMode) {
            GroupFragment.PasteMode.PASTE_FROM_COPY -> {
                // Copy
                mMainGroup?.let { newParent ->
                    copyNodes(nodes, newParent)
                }
            }

            GroupFragment.PasteMode.PASTE_FROM_MOVE -> {
                // Move
                mMainGroup?.let { newParent ->
                    moveNodes(nodes, newParent)
                }
            }

            else -> {
            }
        }
        finishNodeAction()
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

        // Show the lock button
        lockView?.visibility = if (PreferencesUtil.showLockDatabaseButton(this)) {
            View.VISIBLE
        } else {
            View.GONE
        }
        // Padding if lock button visible
        toolbarAction?.updateLockPaddingLeft()

        loadGroup()

        //close drawer and reset mainLayout position
        if (CLOSE_DRAWER) {
            drawerLayout?.closeDrawer(GravityCompat.START, false)
            mainContainer?.translationX = 0f
            CLOSE_DRAWER = false
        }
    }

    override fun onPause() {
        super.onPause()

        finishNodeAction()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // hide or show nav menu
        databaseNavView?.apply {
            //  depending on current mode
            val modeCondition = mSpecialMode == SpecialMode.DEFAULT
            navItemSettings?.isVisible = modeCondition
            navItemMergeFrom?.isVisible = mMergeDataAllowed && modeCondition
            navItemSaveCopyTo?.isVisible = modeCondition
            navItemAbout?.isVisible = modeCondition
        }
        return true
    }

    override fun hideHomeButtonIfModeIsNotDefault(): Boolean {
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                drawerLayout?.openDrawer(GravityCompat.START)
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

    override fun startActivity(intent: Intent) {
        // Get the intent, verify the action and get the query
        if (Intent.ACTION_SEARCH == intent.action) {
            // manually launch the same search activity
            val searchIntent = getIntent().apply {
                // add query to the Intent Extras
                action = Intent.ACTION_SEARCH
                putExtra(SearchManager.QUERY, intent.getStringExtra(SearchManager.QUERY))
            }
            setIntent(searchIntent)
            onNewIntent(searchIntent)
        } else {
            super.startActivity(intent)
        }
    }

    override fun onDatabaseBackPressed() {
        if (mGroupFragment.nodeActionSelectionMode) {
            finishNodeAction()
        } else if (databaseNavView?.navItemEntries?.isChecked == true) {
            databaseNavView?.navItemGroups?.let {
                selectNavigationItem(NavigationItem.GROUPS)
            }
        } else {
            // Normal way when we are not in root
            if (mRootGroup != null && mRootGroup != mCurrentGroup) {
                when {
                    Intent.ACTION_SEARCH == intent.action -> {
                        loadGroup()
                    }

                    mPreviousGroupsIds.isEmpty() -> {
                        super.onRegularBackPressed()
                    }

                    else -> {
                        // Load the previous group
                        loadMainGroup(mPreviousGroupsIds.removeLast())
                    }
                }
            }
            // Else in root, lock if needed
            else {
                EntrySelectionHelper.removeModesFromIntent(intent)
                EntrySelectionHelper.removeInfoFromIntent(intent)
                if (PreferencesUtil.isLockDatabaseWhenBackButtonOnRootClicked(this)) {
                    lockAndExit()
                } else {
                    backToTheAppCaller()
                }
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

    enum class NavigationItem {
        GROUPS, ENTRIES
    }

    companion object {

        private val TAG = GroupActivity::class.java.name

        private const val REQUEST_STARTUP_SEARCH_KEY = "REQUEST_STARTUP_SEARCH_KEY"
        private const val GROUP_STATE_KEY = "GROUP_STATE_KEY"
        private const val PREVIOUS_GROUPS_IDS_KEY = "PREVIOUS_GROUPS_IDS_KEY"
        private const val GROUP_FRAGMENT_TAG = "GROUP_FRAGMENT_TAG"
        private const val ENTRIES_FRAGMENT_TAG = "ENTRIES_FRAGMENT_TAG"
        private const val OLD_GROUP_TO_UPDATE_KEY = "OLD_GROUP_TO_UPDATE_KEY"
        private const val AUTO_SEARCH_KEY = "AUTO_SEARCH_KEY"
        private const val CLOSE_DRAWER_ARG = "close_drawer"

        private fun buildIntent(
            context: Context,
            groupState: GroupState?,
            intentBuildLauncher: (Intent) -> Unit
        ) {
            val intent = Intent(context, GroupActivity::class.java)
            if (groupState != null) {
                intent.putExtra(GROUP_STATE_KEY, groupState)
            }
            intentBuildLauncher.invoke(intent)
        }

        private fun checkTimeAndBuildIntent(
            activity: Activity,
            groupState: GroupState?,
            intentBuildLauncher: (Intent) -> Unit
        ) {
            if (TimeoutHelper.checkTimeAndLockIfTimeout(activity)) {
                buildIntent(activity, groupState, intentBuildLauncher)
            }
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

        /*
         * -------------------------
         * 		Standard Launch
         * -------------------------
         */
        fun launch(
            context: Context,
            database: ContextualDatabase,
            autoSearch: Boolean = false
        ) {
            if (database.loaded) {
                checkTimeAndBuildIntent(context, null) { intent ->
                    intent.putExtra(AUTO_SEARCH_KEY, autoSearch)
                    context.startActivity(intent)
                }
            }
        }

        /*
         * -------------------------
         * 		Search Launch
         * -------------------------
         */
        fun launchForSearchResult(
            context: Context,
            database: ContextualDatabase,
            searchInfo: SearchInfo,
            autoSearch: Boolean = false
        ) {
            if (database.loaded) {
                checkTimeAndBuildIntent(context, null) { intent ->
                    intent.putExtra(AUTO_SEARCH_KEY, autoSearch)
                    EntrySelectionHelper.addSearchInfoInIntent(
                        intent,
                        searchInfo
                    )
                    context.startActivity(intent)
                }
            }
        }

        /*
         * -------------------------
         * 		Search save Launch
         * -------------------------
         */
        fun launchForSaveResult(
            context: Context,
            database: ContextualDatabase,
            searchInfo: SearchInfo,
            autoSearch: Boolean = false
        ) {
            if (database.loaded && !database.isReadOnly) {
                checkTimeAndBuildIntent(context, null) { intent ->
                    intent.putExtra(AUTO_SEARCH_KEY, autoSearch)
                    EntrySelectionHelper.startActivityForSaveModeResult(
                        context,
                        intent,
                        searchInfo
                    )
                }
            }
        }

        /*
         * -------------------------
         * 		Keyboard Launch
         * -------------------------
         */
        fun launchForKeyboardSelectionResult(
            context: Context,
            database: ContextualDatabase,
            searchInfo: SearchInfo? = null,
            autoSearch: Boolean = false
        ) {
            if (database.loaded) {
                checkTimeAndBuildIntent(context, null) { intent ->
                    intent.putExtra(AUTO_SEARCH_KEY, autoSearch)
                    EntrySelectionHelper.startActivityForKeyboardSelectionModeResult(
                        context,
                        intent,
                        searchInfo
                    )
                }
            }
        }

        /*
         * -------------------------
         * 		Autofill Launch
         * -------------------------
         */
        @RequiresApi(api = Build.VERSION_CODES.O)
        fun launchForAutofillResult(
            activity: AppCompatActivity,
            database: ContextualDatabase,
            activityResultLaunch: ActivityResultLauncher<Intent>?,
            autofillComponent: AutofillComponent,
            searchInfo: SearchInfo? = null,
            autoSearch: Boolean = false
        ) {
            if (database.loaded) {
                checkTimeAndBuildIntent(activity, null) { intent ->
                    intent.putExtra(AUTO_SEARCH_KEY, autoSearch)
                    AutofillHelper.startActivityForAutofillResult(
                        activity,
                        intent,
                        activityResultLaunch,
                        autofillComponent,
                        searchInfo
                    )
                }
            }
        }

        /*
         * -------------------------
         * 		Registration Launch
         * -------------------------
         */
        fun launchForRegistration(
            context: Context,
            database: ContextualDatabase,
            registerInfo: RegisterInfo? = null
        ) {
            if (database.loaded && !database.isReadOnly) {
                checkTimeAndBuildIntent(context, null) { intent ->
                    intent.putExtra(AUTO_SEARCH_KEY, false)
                    EntrySelectionHelper.startActivityForRegistrationModeResult(
                        context,
                        intent,
                        registerInfo
                    )
                }
            }
        }

        /*
         * -------------------------
         * 		Global Launch
         * -------------------------
         */
        fun launch(
            activity: AppCompatActivity,
            database: ContextualDatabase,
            onValidateSpecialMode: () -> Unit,
            onCancelSpecialMode: () -> Unit,
            onLaunchActivitySpecialMode: () -> Unit,
            autofillActivityResultLauncher: ActivityResultLauncher<Intent>?
        ) {
            EntrySelectionHelper.doSpecialAction(activity.intent,
                {
                    // Default action
                    launch(
                        activity,
                        database,
                        true
                    )
                },
                { searchInfo ->
                    // Search action
                    if (database.loaded) {
                        launchForSearchResult(
                            activity,
                            database,
                            searchInfo,
                            true
                        )
                        onLaunchActivitySpecialMode()
                    } else {
                        // Simply close if database not opened
                        onCancelSpecialMode()
                    }
                },
                { searchInfo ->
                    // Save info
                    if (database.loaded) {
                        if (!database.isReadOnly) {
                            launchForSaveResult(
                                activity,
                                database,
                                searchInfo,
                                false
                            )
                            onLaunchActivitySpecialMode()
                        } else {
                            Toast.makeText(
                                activity.applicationContext,
                                R.string.autofill_read_only_save,
                                Toast.LENGTH_LONG
                            )
                                .show()
                            onCancelSpecialMode()
                        }
                    }
                },
                { searchInfo ->
                    // Keyboard selection
                    SearchHelper.checkAutoSearchInfo(activity,
                        database,
                        searchInfo,
                        { _, items ->
                            MagikeyboardService.performSelection(
                                items,
                                { entryInfo ->
                                    // Keyboard populated
                                    MagikeyboardService.populateKeyboardAndMoveAppToBackground(
                                        activity,
                                        entryInfo
                                    )
                                    onValidateSpecialMode()
                                },
                                { autoSearch ->
                                    launchForKeyboardSelectionResult(
                                        activity,
                                        database,
                                        searchInfo,
                                        autoSearch
                                    )
                                    onLaunchActivitySpecialMode()
                                }
                            )
                        },
                        {
                            // Here no search info found, disable auto search
                            launchForKeyboardSelectionResult(
                                activity,
                                database,
                                searchInfo,
                                false
                            )
                            onLaunchActivitySpecialMode()
                        },
                        {
                            // Simply close if database not opened, normally not happened
                            onCancelSpecialMode()
                        }
                    )
                },
                { searchInfo, autofillComponent ->
                    // Autofill selection
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        SearchHelper.checkAutoSearchInfo(activity,
                            database,
                            searchInfo,
                            { openedDatabase, items ->
                                // Response is build
                                AutofillHelper.buildResponseAndSetResult(
                                    activity,
                                    openedDatabase,
                                    items
                                )
                                onValidateSpecialMode()
                            },
                            {
                                // Here no search info found, disable auto search
                                launchForAutofillResult(
                                    activity,
                                    database,
                                    autofillActivityResultLauncher,
                                    autofillComponent,
                                    searchInfo,
                                    false
                                )
                                onLaunchActivitySpecialMode()
                            },
                            {
                                // Simply close if database not opened, normally not happened
                                onCancelSpecialMode()
                            }
                        )
                    } else {
                        onCancelSpecialMode()
                    }
                },
                { registerInfo ->
                    // Autofill registration
                    if (!database.isReadOnly) {
                        SearchHelper.checkAutoSearchInfo(activity,
                            database,
                            registerInfo?.searchInfo,
                            { _, _ ->
                                // No auto search, it's a registration
                                launchForRegistration(
                                    activity,
                                    database,
                                    registerInfo
                                )
                                onLaunchActivitySpecialMode()
                            },
                            {
                                // Here no search info found, disable auto search
                                launchForRegistration(
                                    activity,
                                    database,
                                    registerInfo
                                )
                                onLaunchActivitySpecialMode()
                            },
                            {
                                // Simply close if database not opened, normally not happened
                                onCancelSpecialMode()
                            }
                        )
                    } else {
                        Toast.makeText(
                            activity.applicationContext,
                            R.string.autofill_read_only_save,
                            Toast.LENGTH_LONG
                        )
                            .show()
                        onCancelSpecialMode()
                    }
                })
        }
    }
}
