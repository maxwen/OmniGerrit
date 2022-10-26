package org.omnirom.omnigerrit

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
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
import org.omnirom.omnigerrit.model.MainViewModel
import org.omnirom.omnigerrit.model.Settings
import org.omnirom.omnigerrit.ui.theme.OmniGerritTheme
import org.omnirom.omnigerrit.ui.theme.isTablet
import org.omnirom.omniota.model.RetrofitManager
import java.text.DateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
class MainActivity : ComponentActivity() {
    val TAG = "MainActivity"
    private val viewModel by viewModels<MainViewModel>()
    private lateinit var changesPager: LazyPagingItems<Change>
    private lateinit var changesListState: LazyListState
    private lateinit var changeDetail: State<Change?>

    var bottomSheetExpanded = BottomSheetValue.Collapsed
    lateinit var bottomSheetScaffoldState: BottomSheetScaffoldState
    lateinit var localDateTimeFormat: DateFormat
    lateinit var localDateFormat: DateFormat
    private var queryListenerStarted = false
    private val snackbarHostState = SnackbarHostState()
    private val changeDetailScrollState = ScrollState(0)

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
                        changesPager.refresh()
                    }
                }
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.snackbarShow.collect { message ->
                    snackbarHostState.showSnackbar(
                        message = message,
                    )
                }
            }
        }

        setContent {
            OmniGerritTheme {
                bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = BottomSheetState(initialValue = bottomSheetExpanded)
                )
                changeDetail = viewModel.changeDetail.collectAsState()
                changesPager = viewModel.changesPager.collectAsLazyPagingItems()

                if (!queryListenerStarted) {
                    startQueryListener()
                    queryListenerStarted = true
                }

                changesListState = rememberLazyListState()
                val projectFilter = viewModel.projectFilter.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(title = {
                                Text(
                                    text = stringResource(id = R.string.app_name)
                                )
                            }, actions = {
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
                            })
                        },
                        bottomBar = {
                            BottomAppBar(actions = {
                                IconButton(
                                    onClick = {
                                        viewModel.toggleProjectFilter()
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(id = if (projectFilter.value) R.drawable.ic_filter_off else R.drawable.ic_filter),
                                        contentDescription = "",
                                    )
                                }
                            }, floatingActionButton = {
                                FloatingActionButton(onClick = {
                                    if (viewModel.isConnected.value) {
                                        viewModel.reload()
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
            Row(modifier = Modifier.padding(4.dp)) {
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
                LazyColumn(modifier = Modifier.padding(top = 10.dp), state = changesListState) {
                    itemsIndexed(items = changesPager) { index, change ->
                        val changeTime = change!!.updatedInMillis
                        ChangeItem(index, change, changeTime)
                    }
                    when {
                        changesPager.loadState.refresh is LoadState.Loading -> {
                            item { LoadingView(modifier = Modifier.fillParentMaxSize()) }
                        }
                        changesPager.loadState.append is LoadState.Loading -> {
                            item { LoadingView(modifier = Modifier.fillParentMaxSize()) }
                        }
                        changesPager.loadState.refresh is LoadState.Error -> {
                        }
                        changesPager.loadState.append is LoadState.Error -> {
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
                        if (bottomSheetScaffoldState.bottomSheetState.isExpanded) {
                            bottomSheetScaffoldState.bottomSheetState.animateTo(
                                BottomSheetValue.Collapsed,
                                tween(300)
                            )
                            bottomSheetExpanded = BottomSheetValue.Collapsed
                        }
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
        var bgColor = MaterialTheme.colorScheme.background
        if (selected) {
            bgColor = MaterialTheme.colorScheme.secondaryContainer
        } else if (change.id.isEmpty()) {
            bgColor = MaterialTheme.colorScheme.tertiaryContainer
        }

        val date = localDateTimeFormat.format(changeTime)
        Row(
            modifier = Modifier
                .background(bgColor)
                .padding(top = 4.dp, bottom = 4.dp)
                .combinedClickable(onClick = {
                    if (change.id.isNotEmpty()) {
                        coroutineScope.launch {
                            if (bottomSheetScaffoldState.bottomSheetState.isCollapsed) {
                                bottomSheetScaffoldState.bottomSheetState.animateTo(
                                    BottomSheetValue.Expanded,
                                    tween(300)
                                )
                                bottomSheetExpanded = BottomSheetValue.Expanded
                                // TODO
                                changesListState.animateScrollToItem(index)
                            }
                            changeDetailScrollState.scrollTo(0)
                        }
                        viewModel.loadChange(change)

                        coroutineScope.launch {
                            if (!Settings.isDetailsHintShown()) {
                                // TODO snackbar might not be the optimal way to do this
                                viewModel.showSnackbarMessage("Click to show change in browser")
                                Settings.setDetailsHintShown(true)
                            }
                        }
                    }
                }, onLongClick = {
                    if (change._number.isNotEmpty()) {
                        showChangeInGerrit(change._number)
                    }
                }),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column() {
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
                /*Text(
                    text = change.project,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )*/
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

    private fun startQueryListener() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.queryString.collectLatest {
                    if (viewModel.isConnected.value) {
                        changesPager.refresh()
                    }
                }
            }
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

        Column(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(
                        10.dp
                    )
                )
                .height(height = if (isLandscapeSpacing()) 140.dp else 210.dp)
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
                                        if (bottomSheetScaffoldState.currentFraction < 0.4) {
                                            bottomSheetScaffoldState.bottomSheetState.animateTo(
                                                BottomSheetValue.Collapsed,
                                                tween(300)
                                            )
                                            bottomSheetExpanded = BottomSheetValue.Collapsed
                                        } else {
                                            bottomSheetScaffoldState.bottomSheetState.animateTo(
                                                BottomSheetValue.Expanded,
                                                tween(300)
                                            )
                                            bottomSheetExpanded = BottomSheetValue.Expanded
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
                            .verticalScroll(state = changeDetailScrollState)
                            .combinedClickable(onClick = {
                                if (number.isNotEmpty()) {
                                    showChangeInGerrit(number)
                                }
                            })
                    ) {
                        Row() {
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Project:",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = change.project,
                                style = MaterialTheme.typography.bodyMedium,
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
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    /*Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Button(onClick = {
                            if (number.isNotEmpty()) {
                                showChangeInGerrit(number)
                            }
                        }) {
                            Text(text = "Show")
                        }
                    }*/
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

    @Composable
    fun isLandscapeSpacing(): Boolean {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        return isLandscape && !isTablet
    }


}