package dev.nerzouille.koan.ui.history

import dev.nerzouille.koan.data.ClosedTabEntity
import dev.nerzouille.koan.data.VisitEntity
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.kit.RowMenu
import dev.nerzouille.koan.ui.kit.RowMenuItem

/** The long-press menus of History's rows (a tap already opens the page: in this tab from a page). */
class HistoryMenus(
    val openNew: (String) -> Unit,
    val copy: (String) -> Unit,
    val share: (String) -> Unit,
    val remove: (List<VisitEntity>) -> Unit,
    val restore: (String) -> Unit,
) {
    fun page(v: VisitEntity, all: List<VisitEntity>) = RowMenu(
        title = v.title.ifBlank { displayHost(v.url) },
        subtitle = v.url,
        iconUrl = v.url,
        groups = listOf(
            listOf(RowMenuItem(KoanIcons.Plus, "Open in new tab") { openNew(v.url) }),
            listOf(
                RowMenuItem(KoanIcons.Link, "Copy link") { copy(v.url) },
                RowMenuItem(KoanIcons.Share, "Share link") { share(v.url) },
            ),
            listOf(RowMenuItem(KoanIcons.Trash, "Remove from history", destructive = true) { remove(all) }),
        ),
    )

    fun closed(c: ClosedTabEntity) = RowMenu(
        title = c.tab.title.ifBlank { displayHost(c.tab.url) },
        subtitle = c.tab.url,
        iconUrl = c.tab.url,
        groups = listOf(
            listOf(
                RowMenuItem(KoanIcons.Retry, "Reopen tab") { restore(c.koanTabId) },
                RowMenuItem(KoanIcons.Plus, "Open in new tab") { openNew(c.tab.url) },
            ),
            listOf(
                RowMenuItem(KoanIcons.Link, "Copy link") { copy(c.tab.url) },
                RowMenuItem(KoanIcons.Share, "Share link") { share(c.tab.url) },
            ),
        ),
    )

    fun group(host: String, url: String, all: List<VisitEntity>) = RowMenu(
        title = host,
        subtitle = "${all.size} visits",
        iconUrl = url,
        groups = listOf(listOf(RowMenuItem(KoanIcons.Trash, "Remove ${all.size} visits", destructive = true) { remove(all) })),
    )
}
