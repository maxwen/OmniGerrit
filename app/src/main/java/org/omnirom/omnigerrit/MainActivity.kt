package org.omnirom.omnigerrit

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemsIndexed
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.omnirom.omnigerrit.model.Change
import org.omnirom.omnigerrit.model.Device
import org.omnirom.omnigerrit.model.MainViewModel
import org.omnirom.omnigerrit.ui.theme.OmniGerritTheme
import org.omnirom.omnigerrit.ui.theme.isTablet
import org.omnirom.omnigerrit.utils.BuildImageUtils
import org.omnirom.omniota.model.RetrofitManager
import java.text.DateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
class MainActivity : ComponentActivity() {
    val TAG = "MainActivity"
    private val viewModel by viewModels<MainViewModel>()
    private var changesPager: LazyPagingItems<Change>? = null
    private lateinit var changesListState: LazyListState
    private lateinit var changeDetail: State<Change?>

    lateinit var bottomSheetScaffoldState: BottomSheetScaffoldState
    lateinit var localDateTimeFormat: DateFormat
    lateinit var localDateFormat: DateFormat
    private val snackbarHostState = SnackbarHostState()
    private val changeDetailScrollState = ScrollState(0)
    private val bottomSheetValue = mutableStateOf(BottomSheetValue.Collapsed)

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        localDateTimeFormat =
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG, Locale.getDefault())
        localDateTimeFormat.timeZone = TimeZone.getTimeZone("UTC")
        localDateFormat =
            DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
        localDateFormat.timeZone = TimeZone.getTimeZone("UTC")

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.triggerReload.collectLatest {
                    if (viewModel.isConnected.value) {
                        changesPager?.refresh()
                    }
                }
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.snackbarShow.collectLatest { message ->
                    snackbarHostState.showSnackbar(
                        message = message,
                    )
                }
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.queryFilter.collectLatest {
                    if (viewModel.isConnected.value) {
                        changesPager?.refresh()
                    }
                }
            }
        }

        setContent {
            OmniGerritTheme {
                val bottomSheetValue by rememberSaveable { bottomSheetValue }

                bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = BottomSheetState(initialValue = bottomSheetValue)
                )
                changeDetail = viewModel.changeDetail.collectAsState()
                changesPager = viewModel.changesPager.collectAsLazyPagingItems()

                changesListState = rememberLazyListState()
                val projectFilter by viewModel.projectFilter.collectAsState()
                var queryDateAfterExpanded by remember { mutableStateOf(false) }
                val queryDateAfter by viewModel.queryDateAfter.collectAsState()
                var projectFilterExpanded by remember { mutableStateOf(false) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        /*topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = stringResource(id = R.string.app_name)
                                    )
                                },
                                actions = {
                                    IconButton(
                                        onClick = {
                                            viewModel.showSnackbarMessage("Nothing to see here yet")
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_settings),
                                            contentDescription = "",
                                        )
                                    }
                                },
                            )
                        },*/
                        bottomBar = {
                            BottomAppBar(
                                actions = {
                                    IconButton(
                                        onClick = {
                                            projectFilterExpanded = true
                                        }
                                    ) {
                                        if (projectFilter) {
                                            BadgedBox(
                                                badge = {
                                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                                        Icon(
                                                            Icons.Outlined.Done,
                                                            contentDescription = "",
                                                            tint = MaterialTheme.colorScheme.onPrimary,
                                                            modifier = Modifier.size(8.dp)
                                                        )
                                                    }
                                                },
                                            ) {
                                                Icon(
                                                    painterResource(id = R.drawable.ic_devices),
                                                    contentDescription = ""
                                                )
                                            }
                                        } else {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_devices),
                                                contentDescription = "",
                                            )
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = projectFilterExpanded,
                                        onDismissRequest = { projectFilterExpanded = false },
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .height(48.dp)
                                                .fillMaxWidth()
                                                .background(color = MaterialTheme.colorScheme.primary)
                                                .padding(start = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val header = if (!projectFilter) {
                                                "All devices"
                                            } else {
                                                "Device " + BuildImageUtils.device
                                            }
                                            Text(
                                                text = header,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                        Divider()
                                        DropdownMenuItem(
                                            onClick = {
                                                projectFilterExpanded = false
                                                viewModel.setProjectFilter(false)
                                            }, leadingIcon = {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_filter_off),
                                                    contentDescription = "",
                                                )
                                            }, text = {
                                                Text(
                                                    text = "Show all",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            })

                                        DropdownMenuItem(
                                            onClick = {
                                                projectFilterExpanded = false
                                                viewModel.setProjectFilter(true)
                                            }, leadingIcon = {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_devices),
                                                    contentDescription = "",
                                                )
                                            }, text = {
                                                Text(
                                                    text = "Filter for " + BuildImageUtils.device,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            })
                                    }
                                    IconButton(
                                        onClick = {
                                            queryDateAfterExpanded = true
                                        }
                                    ) {
                                        if (queryDateAfter != 0L) {
                                            BadgedBox(badge = {
                                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                                    Icon(
                                                        Icons.Outlined.Done,
                                                        contentDescription = "",
                                                        tint = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.size(8.dp)
                                                    )
                                                }
                                            }) {
                                                Icon(
                                                    painterResource(id = R.drawable.show_since),
                                                    contentDescription = ""
                                                )
                                            }
                                        } else {
                                            Icon(
                                                painter = painterResource(id = R.drawable.show_since),
                                                contentDescription = "",
                                            )
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = queryDateAfterExpanded,
                                        onDismissRequest = { queryDateAfterExpanded = false },
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .height(48.dp)
                                                .fillMaxWidth()
                                                .background(color = MaterialTheme.colorScheme.primary)
                                                .padding(start = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val header = if (queryDateAfter == 0L) {
                                                "All changes"
                                            } else {
                                                "Since " + localDateFormat.format(
                                                    queryDateAfter
                                                )
                                            }
                                            Text(
                                                text = header,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                        Divider()
                                        DropdownMenuItem(
                                            onClick = {
                                                queryDateAfterExpanded = false
                                                viewModel.setQueryDateAfter(0)
                                            }, leadingIcon = {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_filter_off),
                                                    contentDescription = "",
                                                )
                                            }, text = {
                                                Text(
                                                    text = "Show all",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                )
                                            })
                                        if (Device.getBuildDateInMillis(applicationContext) != 0L) {
                                            DropdownMenuItem(
                                                onClick = {
                                                    queryDateAfterExpanded = false
                                                    viewModel.setQueryDateAfter(
                                                        Device.getBuildDateInMillis(applicationContext))
                                                }, leadingIcon = {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.show_since),
                                                        contentDescription = "",
                                                    )
                                                }, text = {
                                                    Text(
                                                        text = "Since this build " + localDateFormat.format(
                                                            Device.getBuildDateInMillis(
                                                                applicationContext
                                                            )
                                                        ),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                    )
                                                })
                                        }
                                        DropdownMenuItem(
                                            onClick = {
                                                queryDateAfterExpanded = false
                                                doSelectStartTime()
                                            }, leadingIcon = {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.show_since),
                                                    contentDescription = "",
                                                )
                                            }, text = {
                                                Text(
                                                    text = "Select start date",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                )
                                            })
                                    }
                                },
                                floatingActionButton = {
                                    FloatingActionButton(onClick = {
                                        if (viewModel.isConnected.value) {
                                            viewModel.reload()
                                            // TODO - hide bottomsheet on refresh?
                                            /*coroutineScope.launch {
                                                updateBottomSheetState(
                                                    BottomSheetValue.Collapsed
                                                )
                                            }*/
                                        }
                                    }, containerColor = MaterialTheme.colorScheme.primary) {
                                        Icon(
                                            Icons.Outlined.Refresh,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                })
                        },
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    )
                    {
                        Column(modifier = Modifier.padding(it)) {
                            BottomSheetScaffold(
                                scaffoldState = bottomSheetScaffoldState,
                                sheetPeekHeight = 0.dp,
                                sheetContent = {
                                    ChangeDetails()
                                },
                                sheetShape = RoundedCornerShape(
                                    topEnd = 28.dp,
                                    topStart = 28.dp
                                ),
                                backgroundColor = MaterialTheme.colorScheme.background,
                                sheetGesturesEnabled = false
                            )
                            {
                                BoxWithConstraints {
                                    Column() {
                                        Changes()
                                    }
                                }
                            }
                        }
                        //DateSelectDialog()
                    }
                }
            }
        }
    }

    @Composable
    fun Changes() {
        val queryString = viewModel.queryString.collectAsState()

        Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp)) {
            Row() {
                OutlinedTextField(
                    value = queryString.value,
                    onValueChange = {
                        viewModel.setQueryString(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null
                        )
                    },
                    placeholder = {
                        Text(
                            text = "Enter word to match",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    maxLines = 1,
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }
            val connected = viewModel.isConnected.collectAsState()
            val buildsMapLoaded = viewModel.buildsMapLoaded.collectAsState()

            if (connected.value) {
                LazyColumn(modifier = Modifier.padding(top = 4.dp), state = changesListState) {
                    itemsIndexed(items = changesPager!!) { index, change ->
                        val changeTime = change!!.updatedInMillis
                        ChangeItem(index, change, changeTime)
                    }
                    when {
                        changesPager!!.loadState.refresh is LoadState.Loading -> {
                            item { LoadingView(modifier = Modifier.fillParentMaxSize()) }
                        }
                        changesPager!!.loadState.append is LoadState.Loading -> {
                            item { LoadingView(modifier = Modifier.fillParentMaxSize()) }
                        }
                        changesPager!!.loadState.refresh is LoadState.Error -> {
                        }
                        changesPager!!.loadState.append is LoadState.Error -> {
                        }
                        !buildsMapLoaded.value -> {
                            item { LoadingView(modifier = Modifier.fillParentMaxSize()) }
                        }
                    }
                }
            } else {
                NoNetworkScreen()

                // close bottom sheet
                LaunchedEffect(key1 = connected, block = {
                    if (!connected.value) {
                        updateBottomSheetState(BottomSheetValue.Collapsed)
                    }
                })
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun ChangeItem(
        index: Int,
        change: Change,
        changeTime: Long
    ) {
        val selected =
            changeDetail.value != null && changeDetail.value!!.id == change.id
        val coroutineScope = rememberCoroutineScope()
        var bgColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
            1.dp
        )
        if (selected) {
            bgColor = MaterialTheme.colorScheme.secondaryContainer
        } else if (change.id.isEmpty()) {
            bgColor = MaterialTheme.colorScheme.tertiaryContainer
        }

        var itemMenuExpanded by remember { mutableStateOf(false) }

        val date = localDateTimeFormat.format(changeTime)
        Row(
            modifier = Modifier
                .padding(top = 4.dp, bottom = 4.dp)
                .background(bgColor, shape = RoundedCornerShape(size = 4.dp))
                .heightIn(min = 88.dp)
                .combinedClickable(onClick = {
                    if (change.id.isNotEmpty()) {
                        coroutineScope.launch {
                            // scroll up if behind bottom sheet
                            if (bottomSheetScaffoldState.bottomSheetState.isCollapsed) {
                                if (isLandscapeSpacing()) {
                                    changesListState.animateScrollToItem(0)
                                } else {
                                    val vsisibleItems =
                                        changesListState.layoutInfo.viewportSize.height / changesListState.layoutInfo.visibleItemsInfo[0].size
                                    if (index - changesListState.firstVisibleItemIndex > vsisibleItems / 2) {
                                        changesListState.animateScrollToItem(0.coerceAtLeast(index - vsisibleItems / 2))
                                    }
                                }
                            }
                            updateBottomSheetState(BottomSheetValue.Expanded)
                            changeDetailScrollState.scrollTo(0)
                        }
                        viewModel.loadChange(change)
                    }
                }, onLongClick = {
                    itemMenuExpanded = true
                }),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.padding(start = 10.dp, end = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = change.subject,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, true),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                if (change.id.isNotEmpty()) {
                    Text(
                        text = date + " by " + change.owner.name,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = change.project,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            DropdownMenu(
                expanded = itemMenuExpanded,
                onDismissRequest = { itemMenuExpanded = false }) {
                DropdownMenuItem(
                    onClick = {
                        itemMenuExpanded = false

                        if (change._number.isNotEmpty()) {
                            showChangeInGerrit(change._number)
                        } else {
                            showOtaBuildDirInBrowser()
                        }
                    }, text = {
                        Text(
                            text = if (change._number.isNotEmpty()) {
                                "Show in Gerrit"
                            } else {
                                "Show available builds"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    })
                DropdownMenuItem(
                    onClick = {
                        itemMenuExpanded = false
                        viewModel.setQueryDateAfter(change.updatedInMillis)
                    }, text = {
                        Text(
                            text = "Show since this change",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    })
            }
        }
    }

    @Composable
    fun NoNetworkScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .wrapContentSize(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No network connection",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center,
                fontSize = 20.sp,
            )
        }
    }

    @Composable
    private fun LoadingView(
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    val BottomSheetScaffoldState.currentFraction: Float
        get() {
            val fraction = bottomSheetState.progress.fraction
            val targetValue = bottomSheetState.targetValue
            val currentValue = bottomSheetState.currentValue

            return when {
                currentValue == BottomSheetValue.Collapsed && targetValue == BottomSheetValue.Collapsed -> 0f
                currentValue == BottomSheetValue.Expanded && targetValue == BottomSheetValue.Expanded -> 1f
                currentValue == BottomSheetValue.Collapsed && targetValue == BottomSheetValue.Expanded -> fraction
                else -> 1f - fraction
            }
        }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun ChangeDetails() {
        /*val linerHeightInDp = with(LocalDensity.current) {
            MaterialTheme.typography.bodyMedium.lineHeight.toDp()
        }*/
        val coroutineScope = rememberCoroutineScope()

        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    return available
                }
            }
        }

        var selectedTab by rememberSaveable { mutableStateOf<Int>(0) }
        Column(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(
                        3.dp
                    )
                )
                .height(height = if (isLandscapeSpacing()) 140.dp else 220.dp)
                .padding(start = 14.dp, end = 14.dp)
                .nestedScroll(nestedScrollConnection)
        ) {
            Column() {

                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(4.dp)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(onDragEnd = {
                                    coroutineScope.launch {
                                        if (bottomSheetScaffoldState.currentFraction < 0.5) {
                                            forceUpdateBottomSheetState(BottomSheetValue.Collapsed)
                                        } else {
                                            forceUpdateBottomSheetState(BottomSheetValue.Expanded)
                                        }
                                    }
                                }) { _, dragAmount ->
                                    bottomSheetScaffoldState.bottomSheetState.performDrag(dragAmount)
                                }
                            }
                            .clip(shape = RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
                if (changeDetail.value != null) {
                    val change = changeDetail.value!!
                    val message = change.commit?.trimmedMessage() ?: change.subject
                    val number = change._number
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, true)
                    ) {
                        TabRow(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                                3.dp
                            ),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            selectedTabIndex = selectedTab,
                            divider = {},
                            indicator = { tabPositions ->
                                Box(
                                    Modifier
                                        .tabIndicatorOffset(tabPositions[selectedTab])
                                        .padding(5.dp)
                                        .fillMaxSize()
                                        .border(
                                            BorderStroke(
                                                1.dp,
                                                MaterialTheme.colorScheme.outline
                                            ), RoundedCornerShape(20.dp)
                                        )
                                )
                            }
                        ) {
                            Tab(
                                selected = false,
                                text = {
                                    Text(
                                        text = "Message",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                },
                                onClick = { selectedTab = 0 })
                            Tab(
                                selected = false,
                                text = {
                                    Text(
                                        text = "Details",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                },
                                onClick = { selectedTab = 1 })
                            Tab(
                                selected = false,
                                text = {
                                    Text(
                                        text = "More",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                },
                                onClick = { selectedTab = 2 })
                        }
                        when (selectedTab) {
                            0 -> {
                                Column(
                                    modifier = Modifier
                                        .verticalScroll(state = changeDetailScrollState)
                                        .padding(top = 4.dp)
                                        .fillMaxWidth()
                                ) {
                                    Row() {
                                        Text(
                                            text = message,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                }
                            }
                            1 -> {
                                Column(
                                    modifier = Modifier
                                        .verticalScroll(state = changeDetailScrollState)
                                        .padding(top = 4.dp)
                                        .fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "Project:",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = change.project,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Branch:",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = change.branch,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                    if (!change.owner.name.isNullOrEmpty()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Owner:",
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = change.owner.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(start = 4.dp)
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Updated:",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = localDateTimeFormat.format(change.updatedInMillis),
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Number:",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = change._number,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                }
                            }
                            2 -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                        .weight(1f, true),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(onClick = {
                                        if (number.isNotEmpty()) {
                                            showChangeInGerrit(number)
                                        }
                                    }) {
                                        Icon(
                                            painterResource(id = R.drawable.ic_web),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                        )
                                        Text(
                                            text = "Show",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier
                                                .padding(start = 14.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showChangeInGerrit(number: String) {
        val uri = Uri.parse(
            RetrofitManager.gerritBaseUrl + "#/c/" + number
        )
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }

    private fun showOtaBuildDirInBrowser() {
        val uri = Uri.parse(
            RetrofitManager.otaBaseUrl + (if (RetrofitManager.deviceRootDir!!.isNotEmpty()) RetrofitManager.deviceRootDir!! + "/" else "") + BuildImageUtils.device
        )
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }

    private fun isLandscapeSpacing(): Boolean {
        val configuration = this.resources.configuration
        val isLandscape =
            configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        return isLandscape && !isTablet
    }

    private fun doSelectStartTime() {
        val c = Calendar.getInstance()
        c.timeInMillis = viewModel.queryDateAfter.value
        val day = c[Calendar.DAY_OF_MONTH]
        val year = c[Calendar.YEAR]
        val month = c[Calendar.MONTH]
        val datePickerDialog = DatePickerDialog(
            ContextThemeWrapper(this, R.style.Theme_OmniGerrit_DatePickerDialog),
            { view, year, monthOfYear, dayOfMonth ->
                val c = Calendar.getInstance()
                c.timeZone = TimeZone.getTimeZone("UTC")
                c[Calendar.DAY_OF_MONTH] = dayOfMonth
                c[Calendar.YEAR] = year
                c[Calendar.MONTH] = monthOfYear
                c[Calendar.HOUR_OF_DAY] = 0
                c[Calendar.MINUTE] = 0
                viewModel.setQueryDateAfter(c.timeInMillis)
            }, year, month, day
        )
        datePickerDialog.show()
    }

    private suspend fun updateBottomSheetState(bottomSheetState: BottomSheetValue) {
        if (bottomSheetState == BottomSheetValue.Collapsed) {
            if (bottomSheetScaffoldState.bottomSheetState.isExpanded) {
                bottomSheetScaffoldState.bottomSheetState.collapse()
                bottomSheetValue.value = bottomSheetState
            }
        } else {
            if (bottomSheetScaffoldState.bottomSheetState.isCollapsed) {
                bottomSheetScaffoldState.bottomSheetState.expand()
                bottomSheetValue.value = bottomSheetState
            }
        }
    }

    private suspend fun forceUpdateBottomSheetState(bottomSheetState: BottomSheetValue) {
        if (bottomSheetState == BottomSheetValue.Collapsed) {
            bottomSheetScaffoldState.bottomSheetState.collapse()
        } else {
            bottomSheetScaffoldState.bottomSheetState.expand()
        }
        bottomSheetValue.value = bottomSheetState
    }
}