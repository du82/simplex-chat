package chat.simplex.app.views.chatlist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import chat.simplex.app.R
import chat.simplex.app.model.*
import chat.simplex.app.ui.theme.Indigo
import chat.simplex.app.views.helpers.*
import chat.simplex.app.views.newchat.NewChatSheet
import chat.simplex.app.views.onboarding.MakeConnection
import chat.simplex.app.views.usersettings.SettingsView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ScaffoldController(val scope: CoroutineScope) {
  lateinit var state: BottomSheetScaffoldState
  val expanded = mutableStateOf(false)

  fun expand() {
    expanded.value = true
    scope.launch { state.bottomSheetState.expand() }
  }

  fun collapse() {
    expanded.value = false
    scope.launch { state.bottomSheetState.collapse() }
  }

  fun toggleSheet() {
    if (state.bottomSheetState.isExpanded) collapse() else expand()
  }

  fun toggleDrawer() = scope.launch {
    state.drawerState.apply { if (isClosed) open() else close() }
  }
}

@Composable
fun scaffoldController(): ScaffoldController {
  val ctrl = ScaffoldController(scope = rememberCoroutineScope())
  val bottomSheetState = rememberBottomSheetState(
    BottomSheetValue.Collapsed,
    confirmStateChange = {
      ctrl.expanded.value = it == BottomSheetValue.Expanded
      true
    }
  )
  ctrl.state = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)
  return ctrl
}

@Composable
fun ChatListView(chatModel: ChatModel, setPerformLA: (Boolean) -> Unit, stopped: Boolean) {
  val scaffoldCtrl = scaffoldController()
  LaunchedEffect(chatModel.clearOverlays.value) {
    if (chatModel.clearOverlays.value && scaffoldCtrl.expanded.value) scaffoldCtrl.collapse()
  }
  var searchInList by rememberSaveable { mutableStateOf("") }
  BottomSheetScaffold(
    topBar = { ChatListToolbar(chatModel, scaffoldCtrl, stopped) { searchInList = it.trim() } },
    scaffoldState = scaffoldCtrl.state,
    drawerContent = { SettingsView(chatModel, setPerformLA) },
    sheetPeekHeight = 0.dp,
    sheetContent = { NewChatSheet(chatModel, scaffoldCtrl) },
    sheetShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
  ) {
    Box {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .background(MaterialTheme.colors.background)
      ) {
        if (chatModel.chats.isNotEmpty()) {
          ChatList(chatModel, search = searchInList)
        } else {
          MakeConnection(chatModel)
        }
      }
      if (scaffoldCtrl.expanded.value) {
        Surface(
          Modifier
            .fillMaxSize()
            .clickable { scaffoldCtrl.collapse() },
          color = Color.Black.copy(alpha = 0.12F)
        ) {}
      }
    }
  }
}

@Composable
fun ChatListToolbar(chatModel: ChatModel, scaffoldCtrl: ScaffoldController, stopped: Boolean, onSearchValueChanged: (String) -> Unit) {
  var showSearch by rememberSaveable { mutableStateOf(false) }
  val hideSearchOnBack = { onSearchValueChanged(""); showSearch = false }
  if (showSearch) {
    BackHandler(onBack = hideSearchOnBack)
  }
  val barButtons = arrayListOf<@Composable RowScope.() -> Unit>()
  if (chatModel.chats.size >= 8) {
    barButtons.add {
      IconButton({ showSearch = true }) {
        Icon(Icons.Outlined.Search, stringResource(android.R.string.search_go).capitalize(Locale.current), tint = MaterialTheme.colors.primary)
      }
    }
  }
  if (!stopped) {
    barButtons.add {
      IconButton(onClick = { scaffoldCtrl.toggleSheet() }) {
        Icon(
          Icons.Outlined.AddCircle,
          stringResource(R.string.add_contact),
          tint = MaterialTheme.colors.primary,
        )
      }
    }
  } else {
    barButtons.add {
      IconButton(onClick = { AlertManager.shared.showAlertMsg(generalGetString(R.string.chat_is_stopped_indication),
        generalGetString(R.string.you_can_start_chat_via_setting_or_by_restarting_the_app)) }) {
        Icon(
          Icons.Filled.Report,
          generalGetString(R.string.chat_is_stopped_indication),
          tint = Color.Red,
        )
      }
    }
  }

  DefaultTopAppBar(
    navigationButton = { if (showSearch) NavigationButtonBack(hideSearchOnBack) else NavigationButtonMenu { scaffoldCtrl.toggleDrawer() } },
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          stringResource(R.string.your_chats),
          color = MaterialTheme.colors.onBackground,
          fontWeight = FontWeight.SemiBold,
        )
        if (chatModel.incognito.value) {
          Icon(
            Icons.Filled.TheaterComedy,
            stringResource(R.string.incognito),
            tint = Indigo,
            modifier = Modifier.padding(10.dp).size(26.dp)
          )
        }
      }
    },
    onTitleClick = null,
    showSearch = showSearch,
    onSearchValueChanged = onSearchValueChanged,
    buttons = barButtons
  )
  Divider()
}

@Composable
fun ChatList(chatModel: ChatModel, search: String) {
  val filter: (Chat) -> Boolean = { chat: Chat ->
    chat.chatInfo.chatViewName.lowercase().contains(search.lowercase())
  }
  val chats by remember(search) { derivedStateOf { if (search.isEmpty()) chatModel.chats else chatModel.chats.filter(filter) } }
  LazyColumn(
    modifier = Modifier.fillMaxWidth()
  ) {
    items(chats) { chat ->
      ChatListNavLinkView(chat, chatModel)
    }
  }
}