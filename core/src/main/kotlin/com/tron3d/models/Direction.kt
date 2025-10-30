package com.tron3d.models

/**
 * Direcciones posibles para el movimiento de las motos
 */
enum class Direction {
    UP,
    DOWN,
    LEFT,
    RIGHT;

    /**
     * Retorna el ángulo de rotación en grados para esta dirección
     * Usado para orientar el modelo 3D de la moto
     */
    fun getRotationAngle(): Float {
        return when (this) {
            UP -> 90f      // Mira hacia arriba (eje Z+)
            DOWN -> -90f   // Mira hacia abajo (eje Z-)
            LEFT -> 180f   // Mira hacia la izquierda (eje X-)
            RIGHT -> 0f    // Mira hacia la derecha (eje X+)
        }
    }

    /**
     * Retorna la dirección opuesta
     */
    fun opposite(): Direction {
        return when (this) {
            UP -> DOWN
            DOWN -> UP
            LEFT -> RIGHT
            RIGHT -> LEFT
        }
    }

    /**
     * Verifica si esta dirección es opuesta a otra
     */
    fun isOpposite(other: Direction): Boolean {
        return this == other.opposite()
    }

    /**
     * Retorna el vector de movimiento unitario para esta dirección
     * Útil para calcular la siguiente posición
     */
    fun toVector(): Pair<Int, Int> {
        return when (this) {
            UP -> Pair(0, 1)      // Y aumenta
            DOWN -> Pair(0, -1)   // Y disminuye
            LEFT -> Pair(-1, 0)   // X disminuye
            RIGHT -> Pair(1, 0)   // X aumenta
        }
    }

    /**
     * Retorna el vector 3D de movimiento para LibGDX
     */
    fun toVector3D(): com.badlogic.gdx.math.Vector3 {
        return when (this) {
            UP -> com.badlogic.gdx.math.Vector3(0f, 0f, 1f)
            DOWN -> com.badlogic.gdx.math.Vector3(0f, 0f, -1f)
            LEFT -> com.badlogic.gdx.math.Vector3(-1f, 0f, 0f)
            RIGHT -> com.badlogic.gdx.math.Vector3(1f, 0f, 0f)
        }
    }
}
