package dev.nerzouille.koan.ui.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * The root's navigation state, moved out of `KoanRootContent` as it was: which space and tab, the
 * overview, the address field and start page, sign-in, Private's way back. The values that
 * survive a process death are saved one by one ([rememberRootState]); the transitions that move
 * them stay with the root, which reads them through these properties.
 */
@Stable
class RootState internal constructor(
    signingIn: MutableState<Boolean>,
    /** Each space's current tab (uuid → card id last opened there): the overview centres on it. */
    val currentTabs: HashMap<String, String>,
    currentSpace: MutableIntState,
    currentSpaceUuid: MutableState<String?>,
    overviewOpen: MutableState<Boolean>,
    private val editingState: MutableState<Boolean>,
    landing: MutableState<String?>,
    startPage: MutableState<Boolean>,
    overviewSettled: MutableState<Boolean>,
    placeRestored: MutableState<Boolean>,
    focusZenTop: MutableState<Boolean>,
    heroPick: MutableState<String?>,
    closedFrom: MutableState<String?>,
    normalSpaceUuid: MutableState<String?>,
    firstRunHandled: MutableState<Boolean>,
) {
    var signingIn by signingIn
    var currentSpace by currentSpace
    var currentSpaceUuid by currentSpaceUuid
    var overviewOpen by overviewOpen
    /**
     * The address field is open. The start page goes with it: closing the field leaves the start
     * page in the same step (an effect keyed on the field missed a close that came in the same
     * frame as the opening, and the start page stayed up for good).
     */
    var editing: Boolean
        get() = editingState.value
        set(value) {
            editingState.value = value
            if (!value) startPage = false
        }
    var landing by landing
    var startPage by startPage
    var overviewSettled by overviewSettled
    var placeRestored by placeRestored
    var focusZenTop by focusZenTop
    var heroPick by heroPick
    var closedFrom by closedFrom
    var normalSpaceUuid by normalSpaceUuid
    var firstRunHandled by firstRunHandled
}

/** The root's [RootState]: saved values through `rememberSaveable`, the others for the composition. */
@Composable
fun rememberRootState(): RootState {
    val signingIn = rememberSaveable { mutableStateOf(false) }
    val currentTabs = rememberSaveable { HashMap<String, String>() }
    val currentSpace = rememberSaveable { mutableIntStateOf(0) }
    val currentSpaceUuid = rememberSaveable { mutableStateOf<String?>(null) }
    val overviewOpen = rememberSaveable { mutableStateOf(false) }
    val editing = remember { mutableStateOf(false) }
    val landing = remember { mutableStateOf<String?>(null) }
    val startPage = remember { mutableStateOf(false) }
    val overviewSettled = remember { mutableStateOf(false) }
    val placeRestored = rememberSaveable { mutableStateOf(false) }
    val focusZenTop = remember { mutableStateOf(false) }
    val heroPick = remember { mutableStateOf<String?>(null) }
    val closedFrom = rememberSaveable { mutableStateOf<String?>(null) }
    val normalSpaceUuid = rememberSaveable { mutableStateOf<String?>(null) }
    val firstRunHandled = rememberSaveable { mutableStateOf(false) }
    return remember {
        RootState(
            signingIn, currentTabs, currentSpace, currentSpaceUuid, overviewOpen, editing, landing, startPage, overviewSettled, placeRestored, focusZenTop, heroPick, closedFrom, normalSpaceUuid, firstRunHandled,
        )
    }
}
