package com.tron3d.models

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox

class ArenaCollider(private val arenaInstance: ModelInstance) {

    private val min = Vector3()
    private val max = Vector3()

    private val safetyMargin = 2f

    init {
        calculateBoundsManually()
    }

    /**
     * ✅ Calcular límites MANUALMENTE aplicando el transform
     */
    private fun calculateBoundsManually() {
        // Obtener bounding box original (sin transform)
        val boundingBox = BoundingBox()
        arenaInstance.calculateBoundingBox(boundingBox)
        val rawMin = Vector3()
        val rawMax = Vector3()
        boundingBox.getMin(rawMin)
        boundingBox.getMax(rawMax)

        // Obtener transform de la instancia
        val transform = arenaInstance.transform
        val translation = Vector3()
        val scale = Vector3()
        transform.getTranslation(translation)
        transform.getScale(scale)

        // ✅ APLICAR MANUALMENTE: escala + traslación
        min.set(
            rawMin.x * scale.x + translation.x,
            rawMin.y * scale.y + translation.y,
            rawMin.z * scale.z + translation.z
        )

        max.set(
            rawMax.x * scale.x + translation.x,
            rawMax.y * scale.y + translation.y,
            rawMax.z * scale.z + translation.z
        )

        Gdx.app.log("ArenaCollider", "═══════════════════════════════════")
        Gdx.app.log("ArenaCollider", "🎯 LÍMITES DE COLISIÓN (MANUALES):")
        Gdx.app.log("ArenaCollider", "   Escala aplicada: $scale")
        Gdx.app.log("ArenaCollider", "   Traslación: $translation")
        Gdx.app.log("ArenaCollider", "   Min: X=${min.x} Z=${min.z}")
        Gdx.app.log("ArenaCollider", "   Max: X=${max.x} Z=${max.z}")
        Gdx.app.log("ArenaCollider", "   Ancho: ${max.x - min.x}")
        Gdx.app.log("ArenaCollider", "   Profundidad: ${max.z - min.z}")
        Gdx.app.log("ArenaCollider", "   Centro: (${(min.x + max.x)/2}, ${(min.z + max.z)/2})")
        Gdx.app.log("ArenaCollider", "═══════════════════════════════════")
    }

    fun isOutOfBounds(position: Vector2): Boolean {
        val x = position.x
        val z = position.y

        return x < (min.x + safetyMargin) ||
            x > (max.x - safetyMargin) ||
            z < (min.z + safetyMargin) ||
            z > (max.z - safetyMargin)
    }

    fun isInsidePlayableArea(position: Vector2): Boolean {
        return !isOutOfBounds(position)
    }

    fun getPlayableWidth(): Float {
        return max.x - min.x - safetyMargin * 2
    }

    fun getPlayableDepth(): Float {
        return max.z - min.z - safetyMargin * 2
    }

    fun getCenter(): Vector2 {
        return Vector2(
            (min.x + max.x) / 2f,
            (min.z + max.z) / 2f
        )
    }

    fun getStartPosition(percentage: Float): Vector2 {
        val center = getCenter()
        val width = getPlayableWidth()

        val x = min.x + safetyMargin + width * percentage
        val z = center.y

        return Vector2(x, z)
    }

    fun clampPosition(position: Vector2): Vector2 {
        val clampedX = position.x.coerceIn(
            min.x + safetyMargin,
            max.x - safetyMargin
        )
        val clampedZ = position.y.coerceIn(
            min.z + safetyMargin,
            max.z - safetyMargin
        )

        return Vector2(clampedX, clampedZ)
    }
}
