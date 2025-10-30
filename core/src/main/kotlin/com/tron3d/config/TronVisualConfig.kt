package com.tron3d.config

import com.badlogic.gdx.graphics.Color

/**
 * Configuración centralizada de efectos visuales TRON
 * FIJO: Usa var en lugar de val para permitir modificación
 */
object TronVisualConfig {

    // === COLORES DE NEÓN (inmutables) ===
    object NeonColors {
        val CYAN = Color(0f, 0.9f, 1f, 1f)
        val ORANGE = Color(1f, 0.5f, 0f, 1f)
        val GREEN = Color(0.2f, 1f, 0.3f, 1f)
        val PURPLE = Color(0.9f, 0.2f, 1f, 1f)
        val RED = Color(1f, 0.1f, 0.2f, 1f)
        val YELLOW = Color(1f, 0.9f, 0.1f, 1f)
    }

    // === MATERIALES DE LA MOTO (mutables con var) ===
    object BikeAppearance {
        val BODY_COLOR = Color(0.05f, 0.08f, 0.12f, 1f)
        var BODY_SHININESS = 128f
        var NEON_INTENSITY = 1.5f
        var NEON_SHININESS = 64f
        var NEON_ALPHA = 0.9f
        val WHEEL_COLOR = Color(0.08f, 0.1f, 0.13f, 1f)
        var WHEEL_SHININESS = 96f
        val PILOT_COLOR = Color(0.1f, 0.12f, 0.15f, 1f)
        var PILOT_SHININESS = 32f
    }

    // === SISTEMA DE TRAILS ===
    object TrailSettings {
        var SPAWN_INTERVAL = 0.1f
        var FADE_SPEED = 0.8f
        var MAX_TRAILS = 50
        var TRAIL_ALPHA = 0.7f
        var TRAIL_HEIGHT = 3f
        var TRAIL_WIDTH = 1.8f
        var TRAIL_THICKNESS = 0.3f
    }

    // === ILUMINACIÓN ===
    object Lighting {
        val AMBIENT_LIGHT = Color(0.15f, 0.15f, 0.25f, 1f)
        val MAIN_LIGHT_COLOR = Color(0.3f, 0.3f, 0.4f, 1f)
        val MAIN_LIGHT_DIRECTION = floatArrayOf(-0.3f, -0.8f, -0.2f)
        val ACCENT_LIGHT_COLOR = Color(0.2f, 0.3f, 0.4f, 1f)
        val ACCENT_LIGHT_DIRECTION = floatArrayOf(0.5f, -0.3f, 0.3f)
        val BACKGROUND_COLOR = Color(0f, 0f, 0f, 1f)
    }

    // === SUELO ===
    object Floor {
        val BASE_COLOR = Color(0.02f, 0.05f, 0.1f, 1f)
        val GRID_COLOR = Color(0.15f, 0.6f, 0.9f, 0.9f)
        val GRID_GLOW_COLOR = Color(0.4f, 0.9f, 1f, 0.6f)
        var REFLECTION_INTENSITY = 0.3f
    }

    // === EFECTOS DE SHADER ===
    object ShaderEffects {
        var GLOW_INTENSITY = 1.5f
        var FRESNEL_POWER = 2.5f
        var FRESNEL_MULTIPLIER = 3.0f
        var SPECULAR_STRENGTH = 0.5f
        var OVERALL_BRIGHTNESS = 1.3f
    }

    // === POST-PROCESAMIENTO ===
    object PostProcessing {
        var ENABLE_BLOOM = false  // Desactivado por defecto para performance
        var BLOOM_INTENSITY = 0.8f
        var BLOOM_THRESHOLD = 0.7f
        var BLOOM_BLUR_PASSES = 2
    }

    // === CÁMARA ===
    object Camera {
        var FOV = 67f
        var NEAR_PLANE = 0.1f
        var FAR_PLANE = 300f
        var DEFAULT_HEIGHT = 25f
        var DEFAULT_DISTANCE = 50f
    }

    // === PERFORMANCE ===
    object Performance {
        var MAX_TRAILS_PER_BIKE = 50
        var ENABLE_SHADOWS = false
        var ENABLE_REFLECTIONS = true
        var PARTICLE_COUNT = 20
    }

    // === FÍSICA Y MOVIMIENTO ===
    object Movement {
        var DEFAULT_SPEED = 5f
        var ACCELERATION = 10f
        var MAX_SPEED = 15f
        var TURN_SPEED = 180f
    }

    // === PRESETS ===
    fun applyIntenseNeonPreset() {
        BikeAppearance.NEON_INTENSITY = 2.5f
        TrailSettings.TRAIL_ALPHA = 0.9f
        ShaderEffects.GLOW_INTENSITY = 2.0f
        PostProcessing.BLOOM_INTENSITY = 1.2f
    }

    fun applyDarkAtmosphericPreset() {
        Lighting.AMBIENT_LIGHT.set(0.05f, 0.05f, 0.1f, 1f)
        BikeAppearance.NEON_INTENSITY = 2.0f
        TrailSettings.FADE_SPEED = 0.5f
        ShaderEffects.OVERALL_BRIGHTNESS = 1.0f
    }

    fun applyPerformancePreset() {
        Performance.MAX_TRAILS_PER_BIKE = 30
        Performance.ENABLE_SHADOWS = false
        Performance.PARTICLE_COUNT = 10
        PostProcessing.ENABLE_BLOOM = false
        TrailSettings.MAX_TRAILS = 30
    }
}
