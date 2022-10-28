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
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import org.omnirom.omnigerrit.model.ChangeFilter
import org.omnirom.omnigerrit.model.MainViewModel
import org.omnirom.omnigerrit.ui.theme.OmniGerritTheme
import org.omnirom.omnigerrit.ui.theme.isTablet
import org.omnirom.omniota.model.RetrofitManager
import java.lang.Math.max
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
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
        localDateTimeFormat.timeZone = TimeZone.getDefault()
        localDateFormat =
            DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault())

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
                val bottomSheetValue = rememberSaveable { bottomSheetValue }

                bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = BottomSheetState(initialValue = bottomSheetValue.value)
                )
                changeDetail = viewModel.changeDetail.collectAsState()
                changesPager = viewModel.changesPager.collectAsLazyPagingItems()

                changesListState = rememberLazyListState()
                val projectFilter = viewModel.projectFilter.collectAsState()
                var queryDateAfterExpanded by remember { mutableStateOf(false) }
                val queryDateAfter = viewModel.queryDateAfter.collectAsState()

                val coroutineScope = rememberCoroutineScope()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        topBar = {
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
                        },
                        bottomBar = {
                            BottomAppBar(
                                actions = {
                                    IconButton(
                                        onClick = {
                                            viewModel.toggleProjectFilter()
                                            // TODO - hide bottomsheet on refresh?
                                            /*coroutineScope.launch {
                                                updateBottomSheetState(
                                                    BottomSheetValue.Collapsed
                                                )
                                            }*/
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(id = if (projectFilter.value) R.drawable.ic_filter_off else R.drawable.ic_filter),
                                            contentDescription = "",
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            queryDateAfterExpanded = true
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.show_since),
                                            contentDescription = "",
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = queryDateAfterExpanded,
                                        onDismissRequest = { queryDateAfterExpanded = false },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.background)
                                    ) {
                                        DropdownMenuItem(
                                            onClick = {
                                                viewModel.setQueryDateAfter("")
                                                // TODO - hide bottomsheet on refresh?
                                                /*coroutineScope.launch {
                                                    updateBottomSheetState(
                                                        BottomSheetValue.Collapsed
                                                    )
                                                }*/
                                                queryDateAfterExpanded = false
                                            }, leadingIcon = {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_filter_off),
                                                    contentDescription = "",
                                                )
                                            }, text = {
                                                Text(
                                                    text = "Disable",
                                                )
                                            })
                                        DropdownMenuItem(
                                            onClick = {
                                                doSelectStartTime()
                                                queryDateAfterExpanded = false
                                            }, leadingIcon = {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.show_since),
                                                    contentDescription = "",
                                                )
                                            }, text = {
                                                Text(
                                                    text = "Since" + if (queryDateAfter.value.isNotEmpty()) " - " + queryDateAfter.value else "",
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
                                    }) {
                                        Icon(
                                            Icons.Outlined.Refresh,
                                            contentDescription = null,
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
                    }
                }
            }
        }
    }

    @Composable
    fun Changes() {
        val queryString = viewModel.queryString.collectAsState()

        Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp)) {
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
                var lastChange: Change? = null
                LazyColumn(modifier = Modifier.padding(top = 4.dp), state = changesListState) {
                    itemsIndexed(items = changesPager!!) { index, change ->
                        val changeTime = change!!.updatedInMillis
                        ChangeItem(index, change, changeTime, lastChange)
                        lastChange = change
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
        changeTime: Long,
        lastChange: Change?
    ) {
        val selected =
            changeDetail.value != null && changeDetail.value!!.id == change.id
        val coroutineScope = rememberCoroutineScope()
        var bgColor = MaterialTheme.colorScheme.background
        if (selected) {
            bgColor = MaterialTheme.colorScheme.secondaryContainer
        } else if (change.id.isEmpty()) {
            bgColor = MaterialTheme.colorScheme.tertiaryContainer
        }

        val date = localDateTimeFormat.format(changeTime)
        var newDay = false
        /*if (lastChange != null) {
            if (localDateFormat.format(lastChange.updatedInMillis) != localDateFormat.format(change.updatedInMillis)) {
                newDay = true
            }
        }*/
        Row(
            modifier = Modifier
                .background(bgColor)
                .padding(top = if (newDay) 16.dp else 4.dp, bottom = 4.dp)
                .combinedClickable(onClick = {
                    if (change.id.isNotEmpty()) {
                        coroutineScope.launch {
                            // scroll up if behind bottom sheet
                            if (bottomSheetScaffoldState.bottomSheetState.isCollapsed) {
                                if (isLandscapeSpacing()) {
                                    changesListState.animateScrollToItem(0)
                                } else {
                                    if (index - changesListState.firstVisibleItemIndex > 5) {
                                        changesListState.animateScrollToItem(max(0, index - 5))
                                    }
                                }
                            }
                            updateBottomSheetState(BottomSheetValue.Expanded)
                            changeDetailScrollState.scrollTo(0)
                        }
                        viewModel.loadChange(change)
                    }
                }, onLongClick = {
                    if (change._number.isNotEmpty()) {
                        showChangeInGerrit(change._number)
                    }
                }),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = change.subject,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = date + " " + change.owner.name,
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

        val selectedTab = rememberSaveable { mutableStateOf<Int>(0) }
        Column(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(
                        10.dp
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
                                10.dp
                            ),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            selectedTabIndex = selectedTab.value,
                            divider = {},
                            indicator = { tabPositions ->
                                Box(
                                    Modifier
                                        .tabIndicatorOffset(tabPositions[selectedTab.value])
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
                                onClick = { selectedTab.value = 0 })
                            Tab(
                                selected = false,
                                text = {
                                    Text(
                                        text = "Details",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                },
                                onClick = { selectedTab.value = 1 })
                            Tab(
                                selected = false,
                                text = {
                                    Text(
                                        text = "More",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                },
                                onClick = { selectedTab.value = 2 })
                        }
                        when (selectedTab.value) {
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
                                            tint = Color.White,
                                        )
                                        Text(
                                            text = "Show", color = Color.White,
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

    private fun isLandscapeSpacing(): Boolean {
        val configuration = this.resources.configuration
        val isLandscape =
            configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        return isLandscape && !isTablet
    }

    private fun doSelectStartTime() {
        val c = Calendar.getInstance()
        if (viewModel.queryDateAfter.value.isNotEmpty()) {
            c.timeInMillis =
                ChangeFilter.gerritDateFormat.parse(viewModel.queryDateAfter.value)?.time ?: 0
        }
        val day = c[Calendar.DAY_OF_MONTH]
        val year = c[Calendar.YEAR]
        val month = c[Calendar.MONTH]
        val datePickerDialog = DatePickerDialog(
            ContextThemeWrapper(this, R.style.Theme_OmniGerrit),
            { view, year, monthOfYear, dayOfMonth ->
                val c = Calendar.getInstance()
                c.timeZone = TimeZone.getTimeZone("UTC")
                c[Calendar.DAY_OF_MONTH] = dayOfMonth
                c[Calendar.YEAR] = year
                c[Calendar.MONTH] = monthOfYear
                c[Calendar.HOUR_OF_DAY] = 0
                c[Calendar.MINUTE] = 0
                bottomSheetValue.value = BottomSheetValue.Collapsed
                viewModel.setQueryDateAfter(ChangeFilter.gerritDateFormat.format(c.timeInMillis))
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