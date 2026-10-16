package dev.nerzouille.koan.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/** Bookmarks saved on the phone (local only until Sync bookmarks arrive). */
class Bookmarks(private val dao: KoanDao, private val scope: CoroutineScope) {
    val urls: StateFlow<Set<String>> =
        dao.observeBookmarkedUrls().map { it.toSet() }.stateIn(scope, SharingStarted.Eagerly, emptySet())

    /** Bookmarks [url] if it isn't yet, otherwise removes it. */
    fun toggle(url: String, title: String) = scope.launch {
        if (url in urls.value) {
            dao.deleteBookmark(url)
        } else {
            dao.insertBookmark(BookmarkEntity(UUID.randomUUID().toString(), url, title.ifBlank { url }, System.currentTimeMillis()))
        }
    }
}
