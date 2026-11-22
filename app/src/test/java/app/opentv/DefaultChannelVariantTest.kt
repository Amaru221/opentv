/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv

import app.opentv.data.model.Channel
import app.opentv.data.repo.defaultChannelVariant
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The channel's opening quality: lower of two, middle of three or more. */
class DefaultChannelVariantTest {

    private fun variant(rank: Int, label: String) = Channel(
        id = rank.toLong(),
        sourceId = 1,
        streamId = label,
        name = label,
        displayName = "Channel",
        groupKey = "channel",
        qualityRank = rank,
        qualityLabel = label,
        categoryId = null,
        logoUrl = null,
        epgChannelId = null,
        number = 1,
        streamUrl = "http://example.test/$label",
    )

    @Test
    fun `one quality is the default`() {
        val variants = listOf(variant(300, "FHD"))

        assertThat(defaultChannelVariant(variants).qualityLabel).isEqualTo("FHD")
    }

    @Test
    fun `two qualities default to the lower one`() {
        val variants = listOf(variant(300, "FHD"), variant(200, "HD"))

        assertThat(defaultChannelVariant(variants).qualityLabel).isEqualTo("HD")
    }

    @Test
    fun `three qualities default to the middle one`() {
        val variants = listOf(variant(300, "FHD"), variant(200, "HD"), variant(100, "SD"))

        assertThat(defaultChannelVariant(variants).qualityLabel).isEqualTo("HD")
    }

    @Test
    fun `four qualities default to the lower-middle one`() {
        val variants = listOf(
            variant(400, "4K"),
            variant(300, "FHD"),
            variant(200, "HD"),
            variant(100, "SD"),
        )

        assertThat(defaultChannelVariant(variants).qualityLabel).isEqualTo("HD")
    }

    @Test
    fun `five qualities default to the middle one`() {
        val variants = listOf(
            variant(400, "4K"),
            variant(300, "FHD"),
            variant(200, "HD"),
            variant(100, "SD"),
            variant(50, "LQ"),
        )

        assertThat(defaultChannelVariant(variants).qualityLabel).isEqualTo("HD")
    }
}
