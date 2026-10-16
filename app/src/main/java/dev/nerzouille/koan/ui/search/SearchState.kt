package dev.nerzouille.koan.ui.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import dev.nerzouille.koan.data.KoanDao
import dev.nerzouille.koan.data.SpaceWithTabs
import dev.nerzouille.koan.search.Hit
import dev.nerzouille.koan.search.Results
import dev.nerzouille.koan.search.SearchEngine
import dev.nerzouille.koan.search.Source
import dev.nerzouille.koan.search.Suggestions
import dev.nerzouille.koan.search.completion
import dev.nerzouille.koan.search.parseQuery
import dev.nerzouille.koan.search.rank
import dev.nerzouille.koan.ui.overview.SpaceModel
import kotlinx.coroutines.delay

/** The sheet's content for what is typed in [SearchField]. */
@Stable
class SearchState {
    var results by mutableStateOf(Results())
        internal set
    var suggestions by mutableStateOf<List<String>>(emptyList())
        internal set

    /** Whose suggestions they are (the chips' label). */
    var suggestionsFrom by mutableStateOf("")
        internal set

    /** The completion the field shows for [typed] (from the latest results). */
    fun complete(typed: String): String? = completion(typed, results.top)
}

/**
 * Searches as the owner types: open tabs in every space (not the one on screen), local history
 * and bookmarks (Room), and DuckDuckGo's suggestions a moment after typing pauses.
 */
@Composable
fun rememberSearch(
    field: SearchField,
    dao: KoanDao,
    spaces: List<SpaceWithTabs>,
    models: List<SpaceModel>,
    currentTabId: String?,
    engine: SearchEngine,
    suggest: Boolean,
): SearchState {
    val state = remember { SearchState() }
    val latestSpaces by rememberUpdatedState(spaces)
    val latestModels by rememberUpdatedState(models)
    val current by rememberUpdatedState(currentTabId)
    LaunchedEffect(field.typed, field.edited) {
        val parsed = parseQuery(field.typed)
        if (!field.edited || parsed.picking || parsed.shortcut != null) {
            state.results = Results()
            return@LaunchedEffect
        }
        // Let a burst of typing settle first.
        delay(QUERY_DELAY_MS)
        val q = parsed.rest
        val visits = (if (q.isEmpty()) dao.recentVisits(LIMIT) else dao.searchVisits(q, LIMIT))
            .map { Hit(it.url, it.title, Source.History, visits = it.visitCount, lastUsed = it.lastVisit) }
        val marks = (if (q.isEmpty()) dao.recentBookmarks(LIMIT) else dao.searchBookmarks(q, LIMIT))
            .map { Hit(it.url, it.title, Source.Bookmark, lastUsed = it.createdAt) }
        val tabs = latestSpaces.flatMapIndexed { i, s ->
            val model = latestModels.getOrNull(i)
            (s.stack.flatMap { it.tabs } + s.pinnedTabs + s.essentials)
                .filter { it.url.startsWith("http") && it.koanTabId != current }
                .distinctBy { it.koanTabId }
                .map { t ->
                    Hit(
                        t.url, t.title, Source.Tab, lastUsed = t.lastAccessed,
                        payload = TabTarget(i, t.koanTabId, model?.name ?: s.space.name, model?.icon ?: s.space.icon),
                    )
                }
        }
        state.results = rank(q, tabs, visits, marks, parsed.scope)
        field.refresh(state::complete)
    }
    // Suggestions run on their own: local results never wait for them, a failure just shows none.
    LaunchedEffect(field.typed, field.edited, engine, suggest) {
        val parsed = parseQuery(field.typed)
        val shortcut = parsed.shortcut
        // "@yt koan": YouTube's own; a plain search: the default engine's; a scope: none.
        val url = when {
            !suggest || !field.edited || parsed.picking || parsed.scope != null -> null
            shortcut != null -> shortcut.suggestUrl(parsed.rest)
            else -> engine.suggestUrl(parsed.rest)
        }
        if (url == null) {
            state.suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(SUGGEST_DELAY_MS)
        state.suggestions = Suggestions.fetch(parsed.rest, url)
        state.suggestionsFrom = shortcut?.label ?: engine.label
    }
    return state
}

private const val QUERY_DELAY_MS = 40L
private const val SUGGEST_DELAY_MS = 180L
private const val LIMIT = 40
