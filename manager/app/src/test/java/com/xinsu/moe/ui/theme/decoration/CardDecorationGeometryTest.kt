package com.xinsu.moe.ui.theme.decoration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CardDecorationGeometryTest {
    @Test
    fun `five card roles have distinct geometry and a bounded content safe rect`() {
        val geometries = DecoratedCardRole.entries.map { role ->
            CardDecorationGeometryPolicy.resolve(
                role = role,
                width = 400f,
                height = 240f,
                safeInsetFraction = 0.12f,
                fontScale = 1f,
                layoutDirection = LogicalLayoutDirection.Ltr,
            )
        }

        assertEquals(5, geometries.map { it.structuralSignature() }.toSet().size)
        geometries.forEach { geometry ->
            val safe = geometry.contentSafeRect
            assertTrue(safe.left >= 0f)
            assertTrue(safe.top >= 0f)
            assertTrue(safe.right <= geometry.width)
            assertTrue(safe.bottom <= geometry.height)
            assertTrue(safe.left < safe.right)
            assertTrue(safe.top < safe.bottom)
            BadgeAnchor.entries.forEach { anchor ->
                assertFalse(safe.contains(geometry.badgeCenter(anchor)), "$anchor must stay outside content")
            }
        }
    }

    @Test
    fun `large fonts hide secondary ornaments without removing the primary frame`() {
        val normal = geometry(fontScale = 1f)
        val large = geometry(fontScale = 1.5f)

        assertTrue(normal.showSecondaryOrnaments)
        assertFalse(large.showSecondaryOrnaments)
        assertEquals(normal.frameScale, large.frameScale)
        assertTrue(large.contentSafeRect.width >= normal.contentSafeRect.width)
        assertTrue(large.contentSafeRect.height >= normal.contentSafeRect.height)
    }

    @Test
    fun `logical start and end mirror card geometry and badge anchors in rtl`() {
        val ltr = geometry(layoutDirection = LogicalLayoutDirection.Ltr)
        val rtl = geometry(layoutDirection = LogicalLayoutDirection.Rtl)

        assertEquals(ltr.contentSafeRect.left, ltr.width - rtl.contentSafeRect.right, 0.0001f)
        assertEquals(ltr.contentSafeRect.right, ltr.width - rtl.contentSafeRect.left, 0.0001f)
        BadgeAnchor.entries.forEach { anchor ->
            val ltrPoint = ltr.badgeCenter(anchor)
            val rtlPoint = rtl.badgeCenter(anchor)
            assertEquals(ltrPoint.x, ltr.width - rtlPoint.x, 0.0001f, anchor.name)
            assertEquals(ltrPoint.y, rtlPoint.y, 0.0001f, anchor.name)
        }
    }

    @Test
    fun `start to end and end to start use logical coordinates in both directions`() {
        assertEquals(
            0.25f,
            horizontalEnergyFraction(
                EnergyDirection.StartToEnd,
                0.25f,
                towardEnd = true,
                layoutDirection = LogicalLayoutDirection.Ltr,
            ),
        )
        assertEquals(
            0.75f,
            horizontalEnergyFraction(
                EnergyDirection.StartToEnd,
                0.25f,
                towardEnd = true,
                layoutDirection = LogicalLayoutDirection.Rtl,
            ),
        )
        assertEquals(
            0.75f,
            horizontalEnergyFraction(
                EnergyDirection.EndToStart,
                0.25f,
                towardEnd = true,
                layoutDirection = LogicalLayoutDirection.Ltr,
            ),
        )
        assertEquals(
            0.25f,
            horizontalEnergyFraction(
                EnergyDirection.EndToStart,
                0.25f,
                towardEnd = true,
                layoutDirection = LogicalLayoutDirection.Rtl,
            ),
        )
    }

    @Test
    fun `every horizontal track grows monotonically within bounds in ltr and rtl`() {
        val expectedPaths = setOf(
            EnergyPathStyle.BrushSweep,
            EnergyPathStyle.TwinConverge,
            EnergyPathStyle.RailCharge,
            EnergyPathStyle.PawSteps,
            EnergyPathStyle.ElasticConverge,
            EnergyPathStyle.SlashCut,
            EnergyPathStyle.RibbonFlip,
            EnergyPathStyle.FieldSweep,
            EnergyPathStyle.LampMarch,
            EnergyPathStyle.ThornGrow,
            EnergyPathStyle.TideFill,
            EnergyPathStyle.ChoiceConfirm,
            EnergyPathStyle.VineGrow,
            EnergyPathStyle.DataPulse,
            EnergyPathStyle.LayerSweep,
            EnergyPathStyle.FuseBurn,
        )
        val actualPaths = EnergyPathStyle.entries.filter(HorizontalTrackPolicy::isHorizontal)
        val progresses = listOf(0f, 0.5f, 1f)

        assertEquals(expectedPaths, actualPaths.toSet())
        actualPaths.forEach { path ->
            listOf(EnergyDirection.StartToEnd, EnergyDirection.EndToStart).forEach { direction ->
                val ltr = horizontalTrack(path, direction, LogicalLayoutDirection.Ltr)
                val rtl = horizontalTrack(path, direction, LogicalLayoutDirection.Rtl)
                val ltrCoordinates = progresses.map { progress -> ltr.renderCoordinatesAt(progress) }
                val rtlCoordinates = progresses.map { progress -> rtl.renderCoordinatesAt(progress) }
                val ltrLengths = ltrCoordinates.map { kotlin.math.abs(it.deltaX) }
                val rtlLengths = rtlCoordinates.map { kotlin.math.abs(it.deltaX) }

                assertEquals(0f, ltrLengths.first(), 0.0001f, "$path/$direction/LTR")
                assertEquals(0f, rtlLengths.first(), 0.0001f, "$path/$direction/RTL")
                assertTrue(ltrLengths.zipWithNext().all { (before, after) -> before <= after })
                assertTrue(rtlLengths.zipWithNext().all { (before, after) -> before <= after })

                progresses.indices.forEach { index ->
                    val progress = progresses[index]
                    val ltrPlan = ltrCoordinates[index]
                    val rtlPlan = rtlCoordinates[index]
                    val ltrEnd = ltrPlan.endX
                    val rtlEnd = rtlPlan.endX
                    assertTrue(ltrPlan.startX in 0f..TrackWidth)
                    assertTrue(rtlPlan.startX in 0f..TrackWidth)
                    assertTrue(ltrEnd in 0f..TrackWidth, "$path/$direction/LTR/$progress")
                    assertTrue(rtlEnd in 0f..TrackWidth, "$path/$direction/RTL/$progress")
                    assertEquals(ltrPlan.startX, TrackWidth - rtlPlan.startX, 0.0001f)
                    assertEquals(ltrEnd, TrackWidth - rtlEnd, 0.0001f)
                }
            }
        }
    }

    @Test
    fun `ribbon flip and choice confirm grow from their logical ornament anchor`() {
        listOf(EnergyPathStyle.RibbonFlip, EnergyPathStyle.ChoiceConfirm).forEach { path ->
            val ltr = horizontalTrack(
                path = path,
                direction = EnergyDirection.StartToEnd,
                layoutDirection = LogicalLayoutDirection.Ltr,
            )
            val expectedStart = TrackEdgeInset + TrackCorner * 0.36f
            val start = ltr.renderCoordinatesAt(0f)
            val middle = ltr.renderCoordinatesAt(0.5f)
            val end = ltr.renderCoordinatesAt(1f)

            assertEquals(expectedStart, start.startX, 0.0001f, path.name)
            assertEquals(expectedStart, start.endX, 0.0001f, path.name)
            assertEquals((expectedStart + TrackWidth - TrackEdgeInset) * 0.5f, middle.endX, 0.0001f)
            assertEquals(TrackWidth - TrackEdgeInset, end.endX, 0.0001f, path.name)
            assertEquals(start.startX, ltr.renderCoordinatesAt(-1f).endX, 0.0001f)
            assertEquals(TrackWidth - TrackEdgeInset, ltr.renderCoordinatesAt(2f).endX, 0.0001f)
        }
    }

    @Test
    fun `ambient motion is bounded and axis aware`() {
        AmbientStyle.entries.forEach { style ->
            assertTrue(AmbientMotionPolicy.cycleDurationMillis(style) in 6_000..12_000)
        }
        assertNotEquals(
            AmbientMotionPolicy.offsetX(DriftAxis.Horizontal, 0.2f, 0.25f, LogicalLayoutDirection.Ltr),
            AmbientMotionPolicy.offsetX(DriftAxis.Horizontal, 0.2f, 0.25f, LogicalLayoutDirection.Rtl),
        )
        assertEquals(0f, AmbientMotionPolicy.offsetY(DriftAxis.Horizontal, 0.2f, 0.25f), 0.0001f)
        assertNotEquals(0f, AmbientMotionPolicy.offsetY(DriftAxis.Vertical, 0.2f, 0.25f))
        assertEquals(0f, AmbientMotionPolicy.offsetX(DriftAxis.None, 0.2f, 0.25f, LogicalLayoutDirection.Ltr))
    }

    private fun geometry(
        fontScale: Float = 1f,
        layoutDirection: LogicalLayoutDirection = LogicalLayoutDirection.Ltr,
    ) = CardDecorationGeometryPolicy.resolve(
        role = DecoratedCardRole.Hero,
        width = 400f,
        height = 240f,
        safeInsetFraction = 0.12f,
        fontScale = fontScale,
        layoutDirection = layoutDirection,
    )

    private fun horizontalTrack(
        path: EnergyPathStyle,
        direction: EnergyDirection,
        layoutDirection: LogicalLayoutDirection,
    ) = HorizontalTrackPolicy.resolve(
        path = path,
        width = TrackWidth,
        edgeInset = TrackEdgeInset,
        corner = TrackCorner,
        direction = direction,
        layoutDirection = layoutDirection,
    )

    private fun HorizontalTrackGeometry.renderCoordinatesAt(progress: Float): TestRenderCoordinates {
        var result: TestRenderCoordinates? = null
        withRenderCoordinatesAt(progress) { startX, endX, deltaX, direction ->
            result = TestRenderCoordinates(startX, endX, deltaX, direction)
        }
        return checkNotNull(result)
    }

    private data class TestRenderCoordinates(
        val startX: Float,
        val endX: Float,
        val deltaX: Float,
        val direction: Float,
    )

    private companion object {
        const val TrackWidth = 1_000f
        const val TrackEdgeInset = 35f
        const val TrackCorner = 80f
    }
}
