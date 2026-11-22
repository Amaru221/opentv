/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.opentv.data.db.OpenTvDatabase
import app.opentv.data.model.Category
import app.opentv.data.model.Channel
import app.opentv.data.model.EpgFeed
import app.opentv.data.model.Source
import app.opentv.data.model.SourceKind
import app.opentv.data.model.StreamKind
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The read-side half of provider enable/disable: an off provider must not leak its channels,
 * categories, favourites, search hits or guide feed into the app. Datasets are tiny; what matters
 * is that the SQL joins (not a UI-only filter) do the excluding.
 */
@RunWith(RobolectricTestRunner::class)
// The real OpenTvApp schedules WorkManager and opens the on-disk database in onCreate; neither
// belongs in a DAO test. A plain Application keeps the test to the in-memory database.
@Config(sdk = [33], application = Application::class)
class EnabledSourceFilterTest {

    private lateinit var db: OpenTvDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OpenTvDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun disabledProviderDisappearsEverywhereAndComesBackWhenEnabled() = runTest {
        val sources = db.sources()
        val channels = db.channels()
        val categories = db.categories()
        val feeds = db.epgFeeds()

        val onId = sources.insert(Source(name = "On", kind = SourceKind.M3U, url = "http://on/list.m3u"))
        val offId = sources.insert(
            Source(name = "Off", kind = SourceKind.M3U, url = "http://off/list.m3u", enabled = false),
        )

        categories.upsertAll(
            listOf(
                Category(id = "on-cat", sourceId = onId, name = "On Cat", kind = StreamKind.LIVE),
                Category(id = "off-cat", sourceId = offId, name = "Off Cat", kind = StreamKind.LIVE),
            ),
        )
        channels.upsertAll(
            listOf(
                channel(onId, "on-1", "Alpha", "on-cat", favourite = true),
                channel(offId, "off-1", "Beta", "off-cat", favourite = true),
            ),
        )
        feeds.insert(EpgFeed(name = "On guide", providerSourceId = onId))
        feeds.insert(EpgFeed(name = "Off guide", providerSourceId = offId))
        feeds.insert(EpgFeed(name = "Free guide", url = "http://guide/xmltv.xml"))

        // Disabled provider: absent from every read path.
        assertThat(channels.observe(null, null).first().map { it.name }).containsExactly("Alpha")
        assertThat(channels.observeInCategories(listOf("on-cat", "off-cat")).first().map { it.name })
            .containsExactly("Alpha")
        assertThat(
            channels.observeInCategoriesIncludingHidden(null, listOf("on-cat", "off-cat"))
                .first().map { it.name },
        ).containsExactly("Alpha")
        assertThat(channels.search("Beta").first()).isEmpty()
        assertThat(channels.searchIncludingHidden("Beta").first()).isEmpty()
        assertThat(channels.observeFavourites().first().map { it.name }).containsExactly("Alpha")
        assertThat(categories.observe(StreamKind.LIVE).first().map { it.name }).containsExactly("On Cat")
        assertThat(categories.allByKind(StreamKind.LIVE).map { it.name }).containsExactly("On Cat")
        assertThat(channels.observeVisibleCount().first()).isEqualTo(1)
        assertThat(channels.channelCountsByCategory().map { it.categoryId }).containsExactly("on-cat")
        assertThat(feeds.observeAll().first().map { it.name })
            .containsExactly("On guide", "Free guide")

        // Re-enable: everything returns live, with no re-sync or restart.
        sources.update(sources.byId(offId)!!.copy(enabled = true))

        assertThat(channels.observe(null, null).first().map { it.name })
            .containsExactly("Alpha", "Beta")
        assertThat(channels.search("Beta").first().map { it.name }).containsExactly("Beta")
        assertThat(channels.observeFavourites().first().map { it.name })
            .containsExactly("Alpha", "Beta")
        assertThat(categories.observe(StreamKind.LIVE).first().map { it.name })
            .containsExactly("On Cat", "Off Cat")
        assertThat(feeds.observeAll().first().map { it.name })
            .containsExactly("On guide", "Off guide", "Free guide")
    }

    private fun channel(
        sourceId: Long,
        streamId: String,
        name: String,
        categoryId: String,
        favourite: Boolean = false,
    ) = Channel(
        sourceId = sourceId,
        streamId = streamId,
        name = name,
        displayName = name,
        categoryId = categoryId,
        logoUrl = null,
        epgChannelId = null,
        number = null,
        streamUrl = "http://stream/$streamId.ts",
        favourite = favourite,
    )
}
