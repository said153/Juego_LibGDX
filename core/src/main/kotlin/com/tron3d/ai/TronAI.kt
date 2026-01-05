package com.tron3d.ai

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector2
import com.tron3d.models.Direction
import com.tron3d.models.GameState
import kotlin.random.Random
import kotlin.math.abs

/**
 * IA DEFINITIVA para Tron 3D
 * Con detección correcta de colisiones y estrategias por heurísticas
 */
class TronAI(private val difficulty: AIDifficulty = AIDifficulty.NORMAL) {

    private var lastDecisionTime = 0f
    private val decisionDelay = when (difficulty) {
        AIDifficulty.FACIL -> 0.3f
        AIDifficulty.NORMAL -> 0.2f
        AIDifficulty.DIFICIL -> 0.1f
    }

    fun decideMove(gameState: GameState, deltaTime: Float): Direction? {
        lastDecisionTime += deltaTime

        if (lastDecisionTime < decisionDelay) {
            return null
        }

        lastDecisionTime = 0f

        return when (difficulty) {
            AIDifficulty.FACIL -> decideMoveEasy(gameState)
            AIDifficulty.NORMAL -> decideMoveNormal(gameState)
            AIDifficulty.DIFICIL -> decideMoveHard(gameState)
        }
    }

    /**
     * IA FÁCIL - Heurística simple: Evita paredes y busca espacio
     */
    private fun decideMoveEasy(gameState: GameState): Direction {
        val currentPos = gameState.player2Position
        val currentDir = gameState.player2Direction
        val playerPos = gameState.player1Position

        val possibleDirs = getPossibleDirections(currentDir)

        // Filtrar direcciones seguras (mirar 2 pasos adelante)
        val safeDirs = possibleDirs.filter { dir ->
            isPathSafe(currentPos, dir, gameState, steps = 2)
        }

        if (safeDirs.isEmpty()) {
            // EMERGENCIA: buscar cualquier dirección que no colisione inmediatamente
            Gdx.app.log("TronAI", "⚠️ SIN OPCIONES SEGURAS - Modo emergencia")
            val emergencyDirs = possibleDirs.filter { dir ->
                isPathSafe(currentPos, dir, gameState, steps = 1)
            }
            return emergencyDirs.randomOrNull() ?: possibleDirs.random()
        }

        // 50% probabilidad de ir hacia el jugador
        if (Random.nextFloat() < 0.5f) {
            val towardsPlayer = getDirectionTowards(currentPos, playerPos, safeDirs)
            if (towardsPlayer != null) {
                Gdx.app.log("TronAI", "🎯 FÁCIL: Hacia jugador $towardsPlayer")
                return towardsPlayer
            }
        }

        // Elegir dirección con más espacio
        return chooseMostSpaceDirection(currentPos, safeDirs, gameState)
    }

    /**
     * IA NORMAL - Heurística intermedia: Persigue y bloquea rutas
     */
    private fun decideMoveNormal(gameState: GameState): Direction {
        val currentPos = gameState.player2Position
        val currentDir = gameState.player2Direction
        val playerPos = gameState.player1Position

        val possibleDirs = getPossibleDirections(currentDir)

        // Filtrar direcciones seguras (mirar 3 pasos adelante)
        val safeDirs = possibleDirs.filter { dir ->
            isPathSafe(currentPos, dir, gameState, steps = 3)
        }

        if (safeDirs.isEmpty()) {
            Gdx.app.log("TronAI", "⚠️ SIN OPCIONES SEGURAS - Modo emergencia")
            val emergencyDirs = possibleDirs.filter { dir ->
                isPathSafe(currentPos, dir, gameState, steps = 1)
            }
            return emergencyDirs.randomOrNull() ?: possibleDirs.random()
        }

        val distance = currentPos.dst(playerPos)

        // Si está lejos, acercarse agresivamente
        if (distance > 20f) {
            val towardsPlayer = getDirectionTowards(currentPos, playerPos, safeDirs)
            if (towardsPlayer != null) {
                Gdx.app.log("TronAI", "🎯 NORMAL: Persiguiendo $towardsPlayer (dist: ${distance.toInt()})")
                return towardsPlayer
            }
        }

        // Si está cerca, intentar bloquear
        if (distance < 15f) {
            val blockingMove = findBlockingMove(currentPos, playerPos, gameState.player1Direction, safeDirs, gameState)
            if (blockingMove != null) {
                Gdx.app.log("TronAI", "🚫 NORMAL: Bloqueando $blockingMove")
                return blockingMove
            }
        }

        // Por defecto: buscar espacio
        return chooseMostSpaceDirection(currentPos, safeDirs, gameState)
    }

    /**
     * IA DIFÍCIL - Heurística avanzada: Minimax simplificado + bloqueo agresivo
     */
    private fun decideMoveHard(gameState: GameState): Direction {
        val currentPos = gameState.player2Position
        val currentDir = gameState.player2Direction
        val playerPos = gameState.player1Position

        val possibleDirs = getPossibleDirections(currentDir)

        // Filtrar direcciones seguras (mirar 4 pasos adelante)
        val safeDirs = possibleDirs.filter { dir ->
            isPathSafe(currentPos, dir, gameState, steps = 4)
        }

        if (safeDirs.isEmpty()) {
            Gdx.app.log("TronAI", "⚠️ SIN OPCIONES SEGURAS - Modo emergencia")
            val emergencyDirs = possibleDirs.filter { dir ->
                isPathSafe(currentPos, dir, gameState, steps = 1)
            }
            return emergencyDirs.randomOrNull() ?: possibleDirs.random()
        }

        val distance = currentPos.dst(playerPos)

        // ESTRATEGIA 1: Si está muy lejos (>25), optimizar espacio propio
        if (distance > 25f) {
            Gdx.app.log("TronAI", "📊 DIFÍCIL: Maximizando territorio propio")
            return evaluateBestMove(currentPos, safeDirs, gameState)
        }

        // ESTRATEGIA 2: Si está cerca (10-25), bloquear agresivamente
        if (distance > 10f) {
            val blockingMove = findBlockingMove(currentPos, playerPos, gameState.player1Direction, safeDirs, gameState)
            if (blockingMove != null) {
                Gdx.app.log("TronAI", "🚫 DIFÍCIL: Bloqueando ruta $blockingMove")
                return blockingMove
            }
        }

        // ESTRATEGIA 3: Si está muy cerca (<10), intentar encerrar
        if (distance <= 10f) {
            val trapMove = findTrapMove(currentPos, playerPos, safeDirs, gameState)
            if (trapMove != null) {
                Gdx.app.log("TronAI", "🕸️ DIFÍCIL: Encerrando $trapMove")
                return trapMove
            }
        }

        // Por defecto: mejor evaluación heurística
        return evaluateBestMove(currentPos, safeDirs, gameState)
    }

    /**
     * Verificar si un camino es seguro por N pasos
     */
    private fun isPathSafe(startPos: Vector2, direction: Direction, gameState: GameState, steps: Int): Boolean {
        var pos = Vector2(startPos)

        for (step in 0 until steps) {
            pos = moveInDirection(pos, direction)

            // Verificar límites de arena
            if (pos.x < -20f || pos.x > 70f || pos.y < -15f || pos.y > 25f) {
                return false
            }

            // Verificar colisión con trail del jugador 1
            for (trailPoint in gameState.player1Trail) {
                if (trailPoint.dst(pos) < 1.0f) {
                    return false
                }
            }

            // ✅ IMPORTANTE: Verificar colisión con PROPIO trail (player 2)
            for (trailPoint in gameState.player2Trail) {
                if (trailPoint.dst(pos) < 1.0f) {
                    return false
                }
            }
        }

        return true
    }

    /**
     * Obtener direcciones posibles (no dar marcha atrás)
     */
    private fun getPossibleDirections(currentDir: Direction): List<Direction> {
        return when (currentDir) {
            Direction.UP -> listOf(Direction.LEFT, Direction.UP, Direction.RIGHT)
            Direction.DOWN -> listOf(Direction.RIGHT, Direction.DOWN, Direction.LEFT)
            Direction.LEFT -> listOf(Direction.DOWN, Direction.LEFT, Direction.UP)
            Direction.RIGHT -> listOf(Direction.UP, Direction.RIGHT, Direction.DOWN)
        }
    }

    /**
     * Obtener dirección hacia el jugador (si está en opciones seguras)
     */
    private fun getDirectionTowards(aiPos: Vector2, playerPos: Vector2, safeDirs: List<Direction>): Direction? {
        val dx = playerPos.x - aiPos.x
        val dy = playerPos.y - aiPos.y

        val preferredDir = if (abs(dx) > abs(dy)) {
            if (dx > 0) Direction.RIGHT else Direction.LEFT
        } else {
            if (dy > 0) Direction.UP else Direction.DOWN
        }

        return if (preferredDir in safeDirs) preferredDir else null
    }

    /**
     * Elegir dirección con más espacio libre
     */
    private fun chooseMostSpaceDirection(pos: Vector2, dirs: List<Direction>, gameState: GameState): Direction {
        val scores = dirs.map { dir ->
            val space = calculateFreeSpace(pos, dir, gameState)
            Pair(dir, space)
        }

        val best = scores.maxByOrNull { it.second }
        Gdx.app.log("TronAI", "📊 Espacios: ${scores.joinToString { "${it.first}=${it.second}" }}")
        return best?.first ?: dirs.random()
    }

    /**
     * Calcular espacio libre en una dirección
     */
    private fun calculateFreeSpace(pos: Vector2, dir: Direction, gameState: GameState): Int {
        var currentPos = Vector2(pos)
        var space = 0

        for (step in 0 until 20) {
            currentPos = moveInDirection(currentPos, dir)

            // Verificar límites
            if (currentPos.x < -20f || currentPos.x > 70f || currentPos.y < -15f || currentPos.y > 25f) {
                break
            }

            // Verificar colisión con cualquier trail
            var collision = false
            for (trailPoint in gameState.player1Trail + gameState.player2Trail) {
                if (trailPoint.dst(currentPos) < 1.0f) {
                    collision = true
                    break
                }
            }

            if (collision) break

            space++
        }

        return space
    }

    /**
     * Encontrar movimiento que bloquee al jugador
     */
    private fun findBlockingMove(
        aiPos: Vector2,
        playerPos: Vector2,
        playerDir: Direction,
        safeDirs: List<Direction>,
        gameState: GameState
    ): Direction? {
        // Predecir dónde estará el jugador
        val predictedPos = moveInDirection(playerPos, playerDir)

        // Buscar dirección que nos acerque a bloquear su ruta
        return safeDirs.minByOrNull { dir ->
            val nextPos = moveInDirection(aiPos, dir)
            nextPos.dst(predictedPos)
        }
    }

    /**
     * Encontrar movimiento que encierre al jugador
     */
    private fun findTrapMove(
        aiPos: Vector2,
        playerPos: Vector2,
        safeDirs: List<Direction>,
        gameState: GameState
    ): Direction? {
        // Intentar rodear al jugador - ir al lado opuesto
        val dx = playerPos.x - aiPos.x
        val dy = playerPos.y - aiPos.y

        // Dirección perpendicular para rodear
        val perpDir = if (abs(dx) > abs(dy)) {
            if (dy > 0) Direction.UP else Direction.DOWN
        } else {
            if (dx > 0) Direction.RIGHT else Direction.LEFT
        }

        return if (perpDir in safeDirs) perpDir else null
    }

    /**
     * Evaluar mejor movimiento con heurística compleja (para DIFÍCIL)
     */
    private fun evaluateBestMove(pos: Vector2, dirs: List<Direction>, gameState: GameState): Direction {
        val scores = dirs.map { dir ->
            val nextPos = moveInDirection(pos, dir)

            // Heurística: espacio libre + distancia al centro + seguridad
            val space = calculateFreeSpace(pos, dir, gameState)
            val centerDist = Vector2(35f, 5f).dst(nextPos) // centro de la arena
            val safetyScore = if (isPathSafe(pos, dir, gameState, steps = 5)) 10 else 0

            val totalScore = space * 2 + (50 - centerDist) + safetyScore

            Pair(dir, totalScore)
        }

        val best = scores.maxByOrNull { it.second }
        Gdx.app.log("TronAI", "🧮 Evaluación: ${scores.joinToString { "${it.first}=${it.second.toInt()}" }}")
        return best?.first ?: dirs.random()
    }

    /**
     * Mover posición en una dirección
     */
    private fun moveInDirection(pos: Vector2, dir: Direction): Vector2 {
        return when (dir) {
            Direction.UP -> Vector2(pos.x, pos.y + 1f)
            Direction.DOWN -> Vector2(pos.x, pos.y - 1f)
            Direction.LEFT -> Vector2(pos.x - 1f, pos.y)
            Direction.RIGHT -> Vector2(pos.x + 1f, pos.y)
        }
    }

    fun reset() {
        lastDecisionTime = 0f
    }
}

enum class AIDifficulty {
    FACIL,
    NORMAL,
    DIFICIL
}
