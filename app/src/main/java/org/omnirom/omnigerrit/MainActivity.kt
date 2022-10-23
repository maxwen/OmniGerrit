package org.omnirom.omnigerrit

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.omnirom.omnigerrit.model.BuildImage
import org.omnirom.omnigerrit.model.Change
import org.omnirom.omnigerrit.model.ChangeFilter
import org.omnirom.omnigerrit.model.MainViewModel
import org.omnirom.omnigerrit.ui.theme.OmniGerritTheme
import org.omnirom.omniota.model.ConnectivityObserver
import org.omnirom.omniota.model.NetworkActivityObserver
import org.omnirom.omniota.model.RetrofitManager
import java.text.DateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
class MainActivity : ComponentActivity() {
    val TAG = "MainActivity"
    private val viewModel by viewModels<MainViewModel>()
    var changesPager: LazyPagingItems<Change>? = null
    lateinit var connectivityObserver: ConnectivityObserver
    private val isConnected = MutableStateFlow<Boolean>(false)
    var bottomSheetExpanded = BottomSheetValue.Collapsed
    lateinit var bottomSheetScaffoldState: BottomSheetScaffoldState
    lateinit var localDateTimeFormat: DateFormat
    lateinit var localDateFormat: DateFormat

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        localDateTimeFormat =
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())
        localDateTimeFormat.timeZone = TimeZone.getDefault()
        localDateFormat =
            DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault())

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                connectivityObserver = NetworkActivityObserver(applicationContext)
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                connectivityObserver.observe().collectLatest { status ->
                    isConnected.value = status == ConnectivityObserver.Status.Available
                }
            }
        }

        setContent {
            OmniGerritTheme {
                bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = BottomSheetState(initialValue = bottomSheetExpanded)
                )
                val changeDetail = viewModel.changeFlow.collectAsState()
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
                                        // TODO
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_settings),
                                        contentDescription = "",
                                    )
                                }
                            })
                        })
                    {
                        Column(modifier = Modifier.padding(it)) {
                            BottomSheetScaffold(
                                scaffoldState = bottomSheetScaffoldState,
                                sheetPeekHeight = 0.dp,
                                sheetContent = {
                                    Column(
                                        modifier = Modifier
                                            .heightIn(min = 200.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.surfaceColorAtElevation(
                                                    3.dp
                                                )
                                            )
                                            .padding(top = 12.dp, start = 10.dp, end = 10.dp)
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            if (changeDetail.value != null) {
                                                val change = changeDetail.value!!
                                                val date = localDateTimeFormat.format(change.updatedInMillis)
                                                Text(
                                                    text = change.subject
                                                )
                                                Text(
                                                    text = "Change-Id:" + change.change_id
                                                )
                                                Text(
                                                    text = "Project:" + change.project
                                                )
                                                Text(
                                                    text = "Branch:" + change.branch
                                                )
                                                Text(
                                                    text = "Modified:" + date
                                                )

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
                                },
                                sheetShape = RoundedCornerShape(
                                    topEnd = 12.dp,
                                    topStart = 12.dp
                                ),
                                backgroundColor = MaterialTheme.colorScheme.background,
                            )
                            {
                                BoxWithConstraints {
                                    Column() {
                                        changesPager =
                                            viewModel.changesPager!!.collectAsLazyPagingItems()
                                        Changes(changesPager!!, changeDetail)
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
    fun Changes(changesPager: LazyPagingItems<Change>, changeDetail: State<Change?>) {
        Column(modifier = Modifier.padding(start = 10.dp, end = 10.dp)) {
            val connected = isConnected.collectAsState()
            if (connected.value) {
                LazyColumn(modifier = Modifier.padding(top = 10.dp)) {
                    items(items = changesPager) { change ->
                        val selected =
                            changeDetail.value != null && changeDetail.value!!.id == change!!.id
                        val changeTime = change!!.updatedInMillis
                        ChangeItem(change, changeTime, selected)
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
        change: Change,
        changeTime: Long,
        selected: Boolean
    ) {
        val coroutineScope = rememberCoroutineScope()
        Row(
            modifier = Modifier
                .clickable(onClick = {
                    coroutineScope.launch {
                        if (bottomSheetScaffoldState.bottomSheetState.isCollapsed) {
                            bottomSheetScaffoldState.bottomSheetState.animateTo(
                                BottomSheetValue.Expanded,
                                tween(300)
                            )
                            bottomSheetExpanded = BottomSheetValue.Expanded
                        }
                    }
                    viewModel.loadChange(change)
                })
                .background(
                    if (selected || change.id.isEmpty()) MaterialTheme.colorScheme.secondaryContainer else {
                        MaterialTheme.colorScheme.background
                    }
                )
        ) {
            val date = if (change.id.isEmpty()) localDateFormat.format(changeTime) else localDateTimeFormat.format(changeTime)
            Text(text = date, modifier = Modifier.width(140.dp), maxLines = 1)
            Text(
                text = change.subject, modifier = Modifier
                    .padding(start = 8.dp)
                    .fillMaxWidth(), maxLines = 1
            )
        }
    }

    @Composable
    fun BuildItem(build: BuildImage, selected: Boolean) {
        val coroutineScope = rememberCoroutineScope()
        Row(
            modifier = Modifier
                .clickable(onClick = {
                    coroutineScope.launch {
                        if (bottomSheetScaffoldState.bottomSheetState.isCollapsed) {
                            bottomSheetScaffoldState.bottomSheetState.animateTo(
                                BottomSheetValue.Expanded,
                                tween(300)
                            )
                            bottomSheetExpanded = BottomSheetValue.Expanded
                        }
                    }
                })
                .background(
                    MaterialTheme.colorScheme.secondaryContainer
                )
        ) {
            val date = localDateFormat.format(build.getBuildDateInMillis())
            Text(text = date, modifier = Modifier.width(140.dp), maxLines = 1)
            Text(
                text = build.filename, modifier = Modifier
                    .padding(start = 8.dp)
                    .fillMaxWidth(), maxLines = 1
            )
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

    @Composable
    fun SnackbarDemo() {
        Column {
            val (snackbarVisibleState, setSnackBarState) = remember { mutableStateOf(false) }

            Button(onClick = { setSnackBarState(!snackbarVisibleState) }) {
                if (snackbarVisibleState) {
                    Text("Hide Snackbar")
                } else {
                    Text("Show Snackbar")
                }
            }
            if (snackbarVisibleState) {
                Snackbar(

                    action = {
                        Button(onClick = {}) {
                            Text("MyAction")
                        }
                    },
                    modifier = Modifier.padding(8.dp)
                ) { Text(text = "This is a snackbar!") }
            }
        }
    }
}