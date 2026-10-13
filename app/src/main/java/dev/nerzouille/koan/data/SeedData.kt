package dev.nerzouille.koan.data

import java.util.UUID

/**
 * Fake spaces for M1, until M2 pulls the real ones from Zen. Seeded once into an empty database.
 */
object SeedData {
    private class FakeSpace(
        val name: String,
        val icon: String,
        val gradient: List<Long>,
        val essentials: List<Pair<String, String>>,
        val pinned: List<Pair<String, String>>,
        val tabs: List<Pair<String, String>>,
    )

    private val spaces = listOf(
        FakeSpace(
            name = "Personal",
            icon = "🌿",
            gradient = listOf(0xFF3E6B4F, 0xFF1D3A2E),
            essentials = listOf(
                "YouTube" to "https://www.youtube.com/",
                "Gmail" to "https://mail.google.com/",
                "Reddit" to "https://www.reddit.com/",
                "Spotify" to "https://open.spotify.com/",
            ),
            pinned = listOf(
                "Weather · Lyon" to "https://www.meteoblue.com/en/weather/week/lyon_france_2996944",
                "Budget · Sheets" to "https://docs.google.com/spreadsheets/",
            ),
            tabs = listOf(
                "Hacker News" to "https://news.ycombinator.com/",
                "Hyprland Wiki · Configuring" to "https://wiki.hyprland.org/Configuring/",
                "EndeavourOS Forum" to "https://forum.endeavouros.com/",
                "Lofi Girl - YouTube" to "https://www.youtube.com/watch?v=jfKfPfyJRdk",
                "Wikipedia · Koan" to "https://en.wikipedia.org/wiki/K%C5%8Dan",
                "Le Monde" to "https://www.lemonde.fr/",
                "Letterboxd" to "https://letterboxd.com/",
                "Steam Store" to "https://store.steampowered.com/",
                "Amazon.fr · Cart" to "https://www.amazon.fr/",
                "r/androiddev" to "https://www.reddit.com/r/androiddev/",
            ),
        ),
        FakeSpace(
            name = "Work",
            icon = "💼",
            gradient = listOf(0xFF7A4A2E, 0xFF3A2418),
            essentials = listOf(
                "GitHub" to "https://github.com/",
                "Gmail" to "https://mail.google.com/",
                "Notion" to "https://www.notion.so/",
                "Figma" to "https://www.figma.com/",
                "Linear" to "https://linear.app/",
                "Calendar" to "https://calendar.google.com/",
                "Slack" to "https://app.slack.com/",
                "ChatGPT" to "https://chat.openai.com/",
            ),
            pinned = listOf(
                "Notion · Planning" to "https://www.notion.so/planning",
                "Linear · Sprint board" to "https://linear.app/koan/board",
                "Google Calendar" to "https://calendar.google.com/",
            ),
            tabs = listOf(
                "Stripe Docs · Webhooks" to "https://docs.stripe.com/webhooks",
                "Vercel · Deployments" to "https://vercel.com/dashboard",
                "Zenith OS · Docs" to "https://zenith.dev/docs",
                "Tailwind CSS · Flexbox" to "https://tailwindcss.com/docs/flex-grow",
                "Kotlin Coroutines guide" to "https://kotlinlang.org/docs/coroutines-guide.html",
                "Compose · graphicsLayer" to "https://developer.android.com/develop/ui/compose/graphics/draw/modifiers",
                "GeckoView API" to "https://mozilla.github.io/geckoview/javadoc/mozilla-central/",
                "Stack Overflow · RenderEffect blur" to "https://stackoverflow.com/questions/tagged/android",
                "MDN · CSS backdrop-filter" to "https://developer.mozilla.org/en-US/docs/Web/CSS/backdrop-filter",
                "Perfetto UI" to "https://ui.perfetto.dev/",
                "Material 3 · Motion" to "https://m3.material.io/styles/motion/overview",
                "Figma · Koan UI" to "https://www.figma.com/design/koan",
                "Pull request #42 · GitHub" to "https://github.com/nerzouille/koan/pull/42",
            ),
        ),
        FakeSpace(
            name = "Uni",
            icon = "🎓",
            gradient = listOf(0xFF3B3F7A, 0xFF1E2048),
            essentials = listOf(
                "Moodle" to "https://moodle.org/",
                "Overleaf" to "https://www.overleaf.com/",
                "Zotero" to "https://www.zotero.org/",
                "Drive" to "https://drive.google.com/",
            ),
            pinned = listOf(
                "Timetable" to "https://edt.univ.fr/",
                "Assignments · Canvas" to "https://canvas.instructure.com/",
            ),
            tabs = listOf(
                "Lecture 7 · Linear Algebra" to "https://ocw.mit.edu/courses/18-06-linear-algebra/",
                "Attention Is All You Need" to "https://arxiv.org/abs/1706.03762",
                "Thesis draft · Overleaf" to "https://www.overleaf.com/project/thesis",
                "Khan Academy · Eigenvectors" to "https://www.khanacademy.org/math/linear-algebra",
                "Google Scholar" to "https://scholar.google.com/",
                "3Blue1Brown · Essence of LA" to "https://www.3blue1brown.com/topics/linear-algebra",
                "Wolfram Alpha" to "https://www.wolframalpha.com/",
                "DeepL Translator" to "https://www.deepl.com/translator",
                "CS231n notes" to "https://cs231n.github.io/",
            ),
        ),
    )

    fun spaces(): List<SpaceEntity> = spaces.mapIndexed { i, s ->
        SpaceEntity(uuid = uuidFor(s.name), name = s.name, icon = s.icon, gradientColors = s.gradient, order = i)
    }

    fun tabs(now: Long = System.currentTimeMillis()): List<TabEntity> = spaces.flatMap { s ->
        val space = uuidFor(s.name)
        val essentials = s.essentials.mapIndexed { i, (title, url) ->
            TabEntity(UUID.randomUUID().toString(), spaceUuid = space, url = url, title = title, essential = true, order = i)
        }
        val pinned = s.pinned.mapIndexed { i, (title, url) ->
            TabEntity(UUID.randomUUID().toString(), spaceUuid = space, url = url, title = title, pinned = true, order = i)
        }
        val normal = s.tabs.mapIndexed { i, (title, url) ->
            TabEntity(
                UUID.randomUUID().toString(), spaceUuid = space, url = url, title = title, order = i,
                lastAccessed = now - (s.tabs.size - i) * 60_000L,
            )
        }
        essentials + pinned + normal
    }

    /** Extra tabs for the 50-card performance check (ROADMAP M1 "done when"). */
    fun filler(spaceUuid: String, startOrder: Int, count: Int): List<TabEntity> {
        val pool = spaces.flatMap { it.tabs }
        return List(count) { i ->
            val (title, url) = pool[i % pool.size]
            TabEntity(UUID.randomUUID().toString(), spaceUuid = spaceUuid, url = url, title = title, order = startOrder + i)
        }
    }

    private fun uuidFor(name: String) = UUID.nameUUIDFromBytes("koan-seed-$name".toByteArray()).toString()
}
