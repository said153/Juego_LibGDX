package com.tron3d.models

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Vector2

class ArenaCollider(private val arenaInstance: ModelInstance?) {

    // ✅ SEGMENTOS COMPLETOS CORREGIDOS
    private val arenaSegments = listOf(
        // Segmento 1: (44.64, 22.45) a (6.56, 22.45)
        Segment(Vector2(44.64f, 20.45f), Vector2(6.56f, 22.45f)),

        // Segmento 2: (6.56, 22.45) a (6.56, 17.17) - CORREGIDO: era 20.17
        Segment(Vector2(6.56f, 20.45f), Vector2(6.56f, 17.17f)),

        // Segmento 3: (6.56, 20.17) a (-15.04, 17.17) - CORREGIDO: segunda coordenada
        Segment(Vector2(6.56f, 20.17f), Vector2(-15.04f, 17.17f)),

        // Segmento 4: (-15.04, 20.17) a (-19.55, 15.83)
        Segment(Vector2(-15.04f, 20.17f), Vector2(-19.55f, 15.83f)),

        // Segmento 5: (-19.55, 15.83) a (-19.55, 8.54)
        Segment(Vector2(-19.55f, 15.83f), Vector2(-19.55f, 8.54f)),

        // Segmento 6: (-19.55, 8.54) a (-16.86, 6.40)
        Segment(Vector2(-19.55f, 8.54f), Vector2(-16.86f, 6.40f)),

        // Segmento 7: (-16.86, 6.40) a (-17.18, 4.14)
        Segment(Vector2(-16.86f, 6.40f), Vector2(-17.18f, 4.14f)),

        // Segmento 8: (-17.18, 4.14) a (-15.68, 3.74)
        Segment(Vector2(-17.18f, 4.14f), Vector2(-15.68f, 3.74f)),

        // Segmento 9: (-15.68, 3.74) a (-15.68, 1.06)
        Segment(Vector2(-15.68f, 3.74f), Vector2(-15.68f, 1.06f)),

        // Segmento 10: (-15.68, 1.06) a (-5.14, -0.94)
        Segment(Vector2(-15.68f, 1.06f), Vector2(-5.14f, -0.94f)),

        // Segmento 11: (-5.14, -0.94) a (-2.80, 1.76)
        Segment(Vector2(-5.14f, -0.94f), Vector2(-2.80f, 1.76f)),

        // Segmento 12: (-2.80, 1.76) a (10.55, 1.80)
        Segment(Vector2(-2.80f, 1.76f), Vector2(10.55f, 1.80f)),

        // Segmento 13: (10.55, 1.80) a (10.55, 0.05)
        Segment(Vector2(10.55f, 1.80f), Vector2(10.55f, 0.05f)),

        // Segmento 14: (10.55, 0.05) a (11.72, 0.05)
        Segment(Vector2(10.55f, 0.05f), Vector2(11.72f, 0.05f)),

        // Segmento 15: (11.72, 0.05) a (12.88, -3.07)
        Segment(Vector2(11.72f, 0.05f), Vector2(12.88f, -3.07f)),

        // Segmento 16: (12.88, -3.07) a (15, -2.27)
        Segment(Vector2(12.88f, -3.07f), Vector2(15f, -2.27f)),

        // Segmento 17: (15, -2.27) a (15, -4.31)
        Segment(Vector2(15f, -2.27f), Vector2(15f, -4.31f)),

        // Segmento 18: (15, -4.31) a (17.24, -7.32)
        Segment(Vector2(15f, -4.31f), Vector2(17.24f, -7.32f)),

        // Segmento 19: (17.24, -7.32) a (17.24, -9.86f) - CORREGIDO: -9,86 → -9.86f
        Segment(Vector2(17.24f, -7.32f), Vector2(17.24f, -9.86f)),

        // Segmento 20: (17.24, -9.86) a (18.45, -9.86)
        Segment(Vector2(17.24f, -9.86f), Vector2(18.45f, -9.86f)),

        // Segmento 21: (18.45, -9.86) a (19.49, -12.38)
        Segment(Vector2(18.45f, -9.86f), Vector2(19.49f, -12.38f)),

        // Segmento 22: (19.49, -12.38) a (40.61, -12.38)
        Segment(Vector2(19.49f, -12.38f), Vector2(40.61f, -12.38f)),

        // Segmento 23: (40.61, -12.38) a (43.39, -9.94f) - CORREGIDO: -9-94 → -9.94f
        Segment(Vector2(40.61f, -12.38f), Vector2(43.39f, -9.94f)),

        // Segmento 24: (43.39, -9.94) a (44.14, -4.90)
        Segment(Vector2(43.39f, -9.94f), Vector2(44.14f, -4.90f)),

        // Segmento 25: (44.14, -4.90) a (65.62, -3.87)
        Segment(Vector2(44.14f, -4.90f), Vector2(65.62f, -3.87f)),

        // Segmento 26: (65.62, -3.87) a (69.17, 2.26)
        Segment(Vector2(65.62f, -3.87f), Vector2(69.17f, 2.26f)),

        // Segmento 27: (69.17, 2.26) a (67.98, 3.04)
        Segment(Vector2(69.17f, 2.26f), Vector2(67.98f, 3.04f)),

        // Segmento 28: (67.98, 3.04) a (67.98, 6.70)
        Segment(Vector2(67.98f, 3.04f), Vector2(67.98f, 6.70f)),

        // Segmento 29: (67.98, 6.70) a (66.07, 6.70)
        Segment(Vector2(67.98f, 6.70f), Vector2(66.07f, 6.70f)),

        // Segmento 30: (66.07, 6.70) a (66.06, 11.28)
        Segment(Vector2(66.07f, 6.70f), Vector2(66.06f, 11.28f)),

        // Segmento 31: (66.06, 11.28) a (64.16, 11.34)
        Segment(Vector2(66.06f, 11.28f), Vector2(64.16f, 11.34f)),

        // Segmento 32: (64.16, 11.34) a (63.34, 13.47)
        Segment(Vector2(64.16f, 11.34f), Vector2(63.34f, 13.47f)),

        // Segmento 33: (63.34, 13.47) a (62.22, 15.37)
        Segment(Vector2(63.34f, 13.47f), Vector2(62.22f, 15.37f)),

        // Segmento 34: (62.22, 15.37) a (61.11, 17.19)
        Segment(Vector2(62.22f, 15.37f), Vector2(61.11f, 17.19f)),

        // Segmento 35: (61.11, 17.19) a (59.22, 17.19)
        Segment(Vector2(61.11f, 17.19f), Vector2(59.22f, 17.19f)),

        // Segmento 36: (59.22, 17.19) a (56.84, 19.20)
        Segment(Vector2(59.22f, 17.19f), Vector2(56.84f, 19.20f)),

        // Segmento 37: (56.84, 19.20) a (56.84, 22.95)
        Segment(Vector2(56.84f, 19.20f), Vector2(56.84f, 22.95f)),

        // Segmento 38: (56.84, 22.95) a (45.99, 22.95)
        Segment(Vector2(56.84f, 22.95f), Vector2(45.99f, 22.95f)),

        // Segmento 39: Cerrar el perímetro - (45.99, 22.95) a (44.64, 22.45)
        Segment(Vector2(45.99f, 22.95f), Vector2(44.64f, 22.45f))
    )

    // Clase interna para representar segmentos
    private data class Segment(val start: Vector2, val end: Vector2)

    // ✅ Extraer todos los puntos únicos en orden
    private val arenaPoints: List<Vector2> by lazy {
        val points = mutableListOf<Vector2>()

        // Tomar el primer punto del primer segmento
        points.add(arenaSegments.first().start)

        // Para cada segmento, agregar su punto final
        for (segment in arenaSegments) {
            points.add(segment.end)
        }

        points.distinct() // Eliminar duplicados
    }

    // ✅ Bounding box calculado automáticamente
    private val minX: Float = arenaPoints.minOf { it.x }
    private val maxX: Float = arenaPoints.maxOf { it.x }
    private val minZ: Float = arenaPoints.minOf { it.y }
    private val maxZ: Float = arenaPoints.maxOf { it.y }
    private val centerX: Float = (minX + maxX) / 2
    private val centerZ: Float = (minZ + maxZ) / 2

    init {
        Gdx.app.log("ArenaCollider", "🎯 INICIANDO COLLIDER CON ${arenaSegments.size} SEGMENTOS")
        Gdx.app.log("ArenaCollider", "📐 Bounding box calculado:")
        Gdx.app.log("ArenaCollider", "   X: ${"%.2f".format(minX)} a ${"%.2f".format(maxX)}")
        Gdx.app.log("ArenaCollider", "   Z: ${"%.2f".format(minZ)} a ${"%.2f".format(maxZ)}")
        Gdx.app.log("ArenaCollider", "📍 Centro: (${"%.2f".format(centerX)}, ${"%.2f".format(centerZ)})")

        // Mostrar primeros segmentos para debug
        for (i in 0 until minOf(3, arenaSegments.size)) {
            val seg = arenaSegments[i]
            Gdx.app.log("ArenaCollider-DEBUG", "Segmento $i: (${seg.start.x}, ${seg.start.y}) → (${seg.end.x}, ${seg.end.y})")
        }
    }

    /**
     * ✅ Verificar si una posición está dentro de la arena usando segmentos
     */
    fun isOutOfBounds(position: Vector2): Boolean {
        val x = position.x
        val z = position.y

        Gdx.app.log("ArenaCollider-CHECK", "🔍 Verificando: (${"%.2f".format(x)}, ${"%.2f".format(z)})")

        // 1. Verificación rápida con bounding box
        if (x < minX || x > maxX || z < minZ || z > maxZ) {
            Gdx.app.log("ArenaCollider", "🚫 FUERA de bounding box")
            return true
        }

        // 2. Verificación precisa con ray casting sobre segmentos
        val isInside = isPointInsideSegments(x, z)

        return !isInside
    }

    /**
     * ✅ Verificar si un punto está dentro usando el algoritmo de ray casting
     */
    private fun isPointInsideSegments(x: Float, z: Float): Boolean {
        // Crear un rayo desde el punto hacia la derecha (infinito)
        val rayStart = Vector2(x, z)
        val rayEnd = Vector2(maxX + 100f, z) // Rayo muy largo hacia la derecha

        var intersectionCount = 0

        // Contar intersecciones con todos los segmentos
        for (segment in arenaSegments) {
            if (doSegmentsIntersect(rayStart, rayEnd, segment.start, segment.end)) {
                intersectionCount++
            }
        }

        // Si el número de intersecciones es impar, el punto está dentro
        val isInside = intersectionCount % 2 == 1

        if (isInside) {
            Gdx.app.log("ArenaCollider-CHECK", "✅ DENTRO (${intersectionCount} intersecciones)")
        } else {
            Gdx.app.log("ArenaCollider-CHECK", "🚫 FUERA (${intersectionCount} intersecciones)")
        }

        return isInside
    }

    /**
     * ✅ Verificar si dos segmentos se intersectan
     */
    private fun doSegmentsIntersect(a: Vector2, b: Vector2, c: Vector2, d: Vector2): Boolean {
        // Calcular orientaciones
        val orient1 = orientation(a, b, c)
        val orient2 = orientation(a, b, d)
        val orient3 = orientation(c, d, a)
        val orient4 = orientation(c, d, b)

        // Caso general: se intersectan
        if (orient1 != orient2 && orient3 != orient4) {
            return true
        }

        // Casos especiales: colinealidad
        if (orient1 == 0 && isPointOnSegment(a, c, b)) return true
        if (orient2 == 0 && isPointOnSegment(a, d, b)) return true
        if (orient3 == 0 && isPointOnSegment(c, a, d)) return true
        if (orient4 == 0 && isPointOnSegment(c, b, d)) return true

        return false
    }

    /**
     * ✅ Calcular orientación de tres puntos
     */
    private fun orientation(p: Vector2, q: Vector2, r: Vector2): Int {
        val val1 = (q.y - p.y) * (r.x - q.x)
        val val2 = (q.x - p.x) * (r.y - q.y)
        val result = val1 - val2

        return when {
            result > 0.001f -> 1      // Sentido horario (con tolerancia)
            result < -0.001f -> -1    // Sentido antihorario (con tolerancia)
            else -> 0                 // Colineal
        }
    }

    /**
     * ✅ Verificar si el punto q está en el segmento pr
     */
    private fun isPointOnSegment(p: Vector2, q: Vector2, r: Vector2): Boolean {
        return (q.x <= maxOf(p.x, r.x) && q.x >= minOf(p.x, r.x) &&
            q.y <= maxOf(p.y, r.y) && q.y >= minOf(p.y, r.y))
    }

    /**
     * ✅ Obtener el punto más cercano dentro de la arena
     */
    fun getClosestValidPoint(position: Vector2): Vector2 {
        // Si ya está dentro, devolver la misma posición
        if (!isOutOfBounds(position)) {
            return position
        }

        // Buscar el segmento más cercano
        var closestPoint = arenaPoints[0]
        var minDistance = position.dst(closestPoint)

        for (point in arenaPoints) {
            val distance = position.dst(point)
            if (distance < minDistance) {
                minDistance = distance
                closestPoint = point
            }
        }

        // Mover ligeramente hacia adentro desde el punto más cercano
        val directionToCenter = Vector2(centerX, centerZ).sub(closestPoint).nor()
        val adjustedPoint = closestPoint.cpy().add(directionToCenter.scl(0.5f))

        Gdx.app.log("ArenaCollider", "🔄 Ajustando punto fuera: $position → $adjustedPoint")
        return adjustedPoint
    }

    /**
     * ✅ Obtener todos los puntos del perímetro (para debug/visualización)
     */
    fun getPerimeterPoints(): List<Vector2> {
        return arenaPoints
    }

    /**
     * ✅ Obtener todos los segmentos (para debug/visualización)
     */
    fun getPerimeterSegments(): List<Pair<Vector2, Vector2>> {
        return arenaSegments.map { Pair(it.start, it.end) }
    }

    /**
     * ✅ Verificar colisión con segmento (para trails)
     */
    fun checkSegmentCollision(start: Vector2, end: Vector2): Boolean {
        for (segment in arenaSegments) {
            if (doSegmentsIntersect(start, end, segment.start, segment.end)) {
                return true
            }
        }

        return false
    }

    /**
     * ✅ Verificar si un punto está en el borde de la arena
     */
    fun isPointOnEdge(position: Vector2, tolerance: Float = 0.1f): Boolean {
        for (segment in arenaSegments) {
            if (isPointOnSegment(position, segment.start, segment.end, tolerance)) {
                return true
            }
        }
        return false
    }

    /**
     * ✅ Verificar si un punto está en un segmento con tolerancia
     */
    private fun isPointOnSegment(p: Vector2, a: Vector2, b: Vector2, tolerance: Float): Boolean {
        // Verificar distancia del punto a la línea
        val lineLength = a.dst(b)
        if (lineLength == 0f) return p.dst(a) < tolerance

        // Calcular proyección
        val t = ((p.x - a.x) * (b.x - a.x) + (p.y - a.y) * (b.y - a.y)) / (lineLength * lineLength)
        val tClamped = t.coerceIn(0f, 1f)

        // Punto proyectado en el segmento
        val projection = Vector2(
            a.x + tClamped * (b.x - a.x),
            a.y + tClamped * (b.y - a.y)
        )

        // Verificar distancia al segmento
        return p.dst(projection) < tolerance
    }

    // Métodos de utilidad (mantener compatibilidad)
    fun isInsidePlayableArea(position: Vector2): Boolean {
        return !isOutOfBounds(position)
    }

    fun getPlayableWidth(): Float {
        return maxX - minX
    }

    fun getPlayableDepth(): Float {
        return maxZ - minZ
    }

    fun getCenter(): Vector2 {
        return Vector2(centerX, centerZ)
    }

    fun getStartPosition(percentage: Float): Vector2 {
        // Buscar un punto dentro del área jugable
        val searchX = minX + (maxX - minX) * percentage
        val searchZ = centerZ

        val testPoint = Vector2(searchX, searchZ)

        // Si el punto está fuera, buscar el más cercano
        return if (isOutOfBounds(testPoint)) {
            getClosestValidPoint(testPoint)
        } else {
            testPoint
        }
    }

    fun clampPosition(position: Vector2): Vector2 {
        return if (isOutOfBounds(position)) {
            getClosestValidPoint(position)
        } else {
            position
        }
    }

    fun getBounds(): Map<String, Float> {
        return mapOf(
            "minX" to minX,
            "maxX" to maxX,
            "minZ" to minZ,
            "maxZ" to maxZ,
            "centerX" to centerX,
            "centerZ" to centerZ,
            "width" to getPlayableWidth(),
            "depth" to getPlayableDepth()
        )
    }
}
