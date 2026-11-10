package dev.nerzouille.koan.ui.history

import dev.nerzouille.koan.data.ClosedTabEntity
import dev.nerzouille.koan.data.VisitEntity
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.kit.SelectionAction

/** What History's selection bar can do (a tap already opens the page: in this tab from a page). */
class HistoryActions(
    val openNew: (String) -> Unit,
    val copy: (String) -> Unit,
    /** Shares text: one link, or several, one per line. */
    val share: (String) -> Unit,
    val remove: (List<VisitEntity>) -> Unit,
    val restore: (String) -> Unit,
)

/** A row of History that selection mode can check. */
internal sealed interface Checkable {
    val key: String

    class Page(override val key: String, val visit: VisitEntity, val all: List<VisitEntity>) : Checkable
    class Group(override val key: String, val all: List<VisitEntity>) : Checkable
    class Closed(override val key: String, val tab: ClosedTabEntity) : Checkable
}

/** The bar's actions for what is checked ([done] leaves selection mode first). */
internal fun HistoryActions.forChecked(checked: List<Checkable>, done: () -> Unit): List<SelectionAction> {
    fun act(run: () -> Unit) = { done(); run() }
    val one = checked.singleOrNull()
    return when (one) {
        is Checkable.Page -> listOf(
            SelectionAction(KoanIcons.Plus, "New tab", onClick = act { openNew(one.visit.url) }),
            SelectionAction(KoanIcons.Link, "Copy link", onClick = act { copy(one.visit.url) }),
            SelectionAction(KoanIcons.Share, "Share link", onClick = act { share(one.visit.url) }),
            SelectionAction(KoanIcons.Trash, "Remove", destructive = true, onClick = act { remove(one.all) }),
        )
        is Checkable.Group -> listOf(
            SelectionAction(KoanIcons.Trash, "Remove ${one.all.size} visits", destructive = true, onClick = act { remove(one.all) }),
        )
        is Checkable.Closed -> listOf(
            SelectionAction(KoanIcons.Retry, "Reopen", onClick = act { restore(one.tab.koanTabId) }),
            SelectionAction(KoanIcons.Plus, "New tab", onClick = act { openNew(one.tab.tab.url) }),
            SelectionAction(KoanIcons.Link, "Copy link", onClick = act { copy(one.tab.tab.url) }),
            SelectionAction(KoanIcons.Share, "Share link", onClick = act { share(one.tab.tab.url) }),
        )
        null -> {
            val visits = checked.flatMap {
                when (it) {
                    is Checkable.Page -> it.all
                    is Checkable.Group -> it.all
                    is Checkable.Closed -> emptyList()
                }
            }.distinctBy { it.url }
            // A group shares each of its pages.
            val links = visits.map { it.url }
            listOf(
                SelectionAction(KoanIcons.Share, "Share", onClick = act { share(links.joinToString("\n")) }),
                SelectionAction(KoanIcons.Trash, "Remove", destructive = true, onClick = act { remove(visits) }),
            )
        }
    }
}
