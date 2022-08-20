package org.wordpress.android.sharedlogin

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class JetpackPublicDataTest {
    private val classToTest = JetpackPublicData()

    @Test
    fun `Should return correct release public hash key`() {
        val actual = classToTest.currentPublicKeyHash()
        val expected = "60fca11c59c6933610146f40a1296250abff640dc5da2b85fc8e5aa411dd17d6"
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should return correct current package ID`() {
        val actual = classToTest.currentPackageId()
        val expected = "com.jetpack.android.prealpha"
        assertThat(actual).isEqualTo(expected)
    }
}
