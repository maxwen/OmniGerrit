package org.omnirom.omnigerrit

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import org.omnirom.omnigerrit.ui.theme.OmniGerritTheme
import org.omnirom.omniota.model.RetrofitManager
import java.text.DateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
class MainActivity : ComponentActivity() {
    val TAG = "MainActivity"
    private val viewModel by viewModels<MainViewModel>()
    private lateinit var changesPager: LazyPagingItems<Change>
    private lateinit var listState: LazyListState
    private lateinit var changeDetail: State<Change?>

    var bottomSheetExpanded = BottomSheetValue.Collapsed
    lateinit var bottomSheetScaffoldState: BottomSheetScaffoldState
    lateinit var localDateTimeFormat: DateFormat
    lateinit var localDateFormat: DateFormat
    private var queryListenerStarted = false
    private var messageExpanded = mutableStateOf(false)

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
                viewModel.reloadFlow.collectLatest {
                    if (viewModel.isConnected.value) {
                        changesPager.refresh()
                    }
                }
            }
        }

        setContent {
            OmniGerritTheme {
                bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = BottomSheetState(initialValue = bottomSheetExpanded)
                )
                changeDetail = viewModel.changeFlow.collectAsState()
                changesPager = viewModel.changesPager.collectAsLazyPagingItems()

                if (!queryListenerStarted) {
                    startQueryListener()
                    queryListenerStarted = true
                }

                listState = rememberLazyListState()

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
                            })
                        },
                        bottomBar = {
                            BottomAppBar(actions = {
                                IconButton(
                                    onClick = {
                                        // TODO
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_settings),
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
                        }
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
            if (connected.value) {
                LazyColumn(modifier = Modifier.padding(top = 10.dp), state = listState) {
                    itemsIndexed(items = changesPager) { index, change ->
                        val selected =
                            changeDetail.value != null && changeDetail.value!!.id == change!!.id
                        val changeTime = change!!.updatedInMillis
                        ChangeItem(index, change, changeTime, selected)
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
                    }
                }
            } else {
                NoNetworkScreen()
            }
        }
    }

    @Composable
    fun ChangeItem(
        index: Int,
        change: Change,
        changeTime: Long,
        selected: Boolean
    ) {
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
                .clickable(onClick = {
                    coroutineScope.launch {
                        if (bottomSheetScaffoldState.bottomSheetState.isCollapsed) {
                            bottomSheetScaffoldState.bottomSheetState.animateTo(
                                BottomSheetValue.Expanded,
                                tween(300)
                            )
                            bottomSheetExpanded = BottomSheetValue.Expanded
                            // TODO
                            listState.animateScrollToItem(index)
                        }
                    }
                    messageExpanded.value = false
                    viewModel.loadChange(change)
                }),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column() {
                Text(
                    text = change.subject,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = date + " " + change.owner.name,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
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
        Column(
            modifier = Modifier
                .height(height = 200.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(
                        3.dp
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            ) {
                //val coroutineScope = rememberCoroutineScope()

                /*Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                if (bottomSheetScaffoldState.bottomSheetState.isExpanded) {
                                    bottomSheetScaffoldState.bottomSheetState.animateTo(
                                        BottomSheetValue.Collapsed,
                                        tween(300)
                                    )
                                    bottomSheetExpanded =
                                        BottomSheetValue.Collapsed
                                }
                            }
                        }) {
                        Icon(
                            Icons.Outlined.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }*/

                if (changeDetail.value != null) {
                    val change = changeDetail.value!!
                    val message = change.commit?.trimmedMessage() ?: change.subject
                    val messageLines = message.split("\n").size
                    val firstMessageLine = message.split("\n").first()
                    var expanded by remember { messageExpanded }
                    var scrollState = rememberScrollState()

                    Row(
                        modifier = Modifier
                            .weight(1f, true)
                            .padding(start = 14.dp, end = 14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f, true)
                                .clipScrollableContainer(orientation = Orientation.Vertical)
                                .verticalScroll(state = scrollState),
                        ) {
                            Text(
                                text = if (expanded) message else firstMessageLine,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (messageLines > 2) {
                            IconButton(
                                onClick = {
                                    messageExpanded.value = !messageExpanded.value
                                }) {
                                Icon(
                                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                    if (!expanded) {
                        Row(modifier = Modifier.padding(start = 14.dp, end = 14.dp)) {
                            Text(
                                text = "Project:" + change.project,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        /*Row(modifier = Modifier.padding(start = 14.dp, end = 14.dp)) {
                            Text(
                                text = "Branch:" + change.branch,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }*/

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 14.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(onClick = {
                                val uri = Uri.parse(
                                    RetrofitManager.gerritBaseUrl + "#/c/" + changeDetail.value!!._number
                                )
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                startActivity(intent)
                            }) {
                                Text(text = "Show")
                            }
                        }
                    }
                }
            }
        }
    }
}