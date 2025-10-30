package com.tron3d.models

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable

/**
 * Light Cycle TRON LEGACY - Versión detallada por código
 * Basado en el diseño de la película
 */
class LightCycle(
    var colorNeon: Color,
    initialPosition: Vector3 = Vector3.Zero
) : Disposable {

    private val modelBuilder = ModelBuilder()

    val model: Model
    val instance: ModelInstance

    val trailInstances = mutableListOf<TrailSegment>()
    private val trailModel: Model
    private var lastTrailPosition = Vector3(initialPosition)

    var position = Vector3(initialPosition)
    var rotation = 0f
    var velocity = Vector3.Zero

    init {
        model = createTronLegacyBike()
        instance = ModelInstance(model)
        instance.transform.setToTranslation(position)

        trailModel = createTrailModel()

        Gdx.app.log("LightCycle", "Moto TRON Legacy creada")
    }

    /**
     * Crea una Light Cycle más fiel al diseño de TRON Legacy
     */
    private fun createTronLegacyBike(): Model {
        modelBuilder.begin()

        val scale = 4f

        // === MATERIALES ===

        // Cuerpo negro mate
        val bodyMaterial = Material(
            ColorAttribute.createDiffuse(0.03f, 0.03f, 0.05f, 1f),
            ColorAttribute.createSpecular(0.8f, 0.8f, 0.9f, 1f),
            FloatAttribute.createShininess(200f)
        )

        // Neón brillante INTENSO
        val neonMaterial = Material(
            ColorAttribute.createDiffuse(colorNeon),
            ColorAttribute.createEmissive(colorNeon),
            ColorAttribute.createSpecular(Color.WHITE),
            FloatAttribute.createShininess(100f),
            BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE, 1f)
        )

        // Vidrio/Cockpit translúcido
        val glassMaterial = Material(
            ColorAttribute.createDiffuse(0.1f, 0.2f, 0.3f, 0.6f),
            ColorAttribute.createSpecular(1f, 1f, 1f, 1f),
            FloatAttribute.createShininess(150f),
            BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 0.6f)
        )

        // === CONSTRUCCIÓN ESTILO TRON LEGACY ===

        // CHASIS PRINCIPAL (forma aerodinámica)
        val bodyPart = modelBuilder.part(
            "chassis",
            GL20.GL_TRIANGLES,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
            bodyMaterial
        )

        // Base larga y baja
        bodyPart.setVertexTransform(trans(0f, 0.3f * scale, 0f))
        bodyPart.box(0.6f * scale, 0.2f * scale, 3.5f * scale)

        // Nariz puntiaguda
        bodyPart.setVertexTransform(trans(0f, 0.5f * scale, 2f * scale))
        bodyPart.cone(0.4f * scale, 1f * scale, 0.4f * scale, 12)

        // Cuerpo central elevado
        bodyPart.setVertexTransform(trans(0f, 0.7f * scale, 0.3f * scale))
        bodyPart.box(0.7f * scale, 0.5f * scale, 2f * scale)

        // Sección trasera elevada
        bodyPart.setVertexTransform(trans(0f, 1.2f * scale, -1.2f * scale))
        bodyPart.box(0.8f * scale, 0.8f * scale, 0.6f * scale)

        // Extensiones laterales (canopy)
        bodyPart.setVertexTransform(trans(0.5f * scale, 0.8f * scale, 0.2f * scale))
        bodyPart.box(0.12f * scale, 0.6f * scale, 2.5f * scale)

        bodyPart.setVertexTransform(trans(-0.5f * scale, 0.8f * scale, 0.2f * scale))
        bodyPart.box(0.12f * scale, 0.6f * scale, 2.5f * scale)

        // RUEDAS MASIVAS (característica icónica)
        val wheelPart = modelBuilder.part(
            "wheels",
            GL20.GL_TRIANGLES,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
            neonMaterial
        )

        // Rueda delantera ENORME
        wheelPart.setVertexTransform(trans(0f, 0.5f * scale, 1.8f * scale))
        wheelPart.cylinder(1.2f * scale, 0.3f * scale, 1.2f * scale, 32)

        // Aro interior brillante
        wheelPart.setVertexTransform(trans(0f, 0.5f * scale, 1.8f * scale))
        wheelPart.cylinder(1f * scale, 0.35f * scale, 1f * scale, 32)

        // Rueda trasera ENORME
        wheelPart.setVertexTransform(trans(0f, 0.5f * scale, -1.5f * scale))
        wheelPart.cylinder(1.2f * scale, 0.3f * scale, 1.2f * scale, 32)

        wheelPart.setVertexTransform(trans(0f, 0.5f * scale, -1.5f * scale))
        wheelPart.cylinder(1f * scale, 0.35f * scale, 1f * scale, 32)

        // LÍNEAS DE NEÓN (patrón icónico de circuitos)
        val neonPart = modelBuilder.part(
            "neon_lines",
            GL20.GL_TRIANGLES,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
            neonMaterial
        )

        // Línea central continua (muy característica)
        neonPart.setVertexTransform(trans(0f, 0.85f * scale, 0.3f * scale))
        neonPart.box(0.15f * scale, 0.15f * scale, 3.3f * scale)

        // Líneas laterales paralelas
        neonPart.setVertexTransform(trans(0.55f * scale, 0.95f * scale, 0.3f * scale))
        neonPart.box(0.1f * scale, 0.1f * scale, 3f * scale)

        neonPart.setVertexTransform(trans(-0.55f * scale, 0.95f * scale, 0.3f * scale))
        neonPart.box(0.1f * scale, 0.1f * scale, 3f * scale)

        // Líneas en forma de Y (diseño característico)
        neonPart.setVertexTransform(trans(0f, 0.85f * scale, 1.5f * scale))
        neonPart.box(0.8f * scale, 0.1f * scale, 0.1f * scale)

        // Líneas verticales traseras (patrón de circuito)
        neonPart.setVertexTransform(trans(0.35f * scale, 1.3f * scale, -1.4f * scale))
        neonPart.box(0.08f * scale, 1f * scale, 0.08f * scale)

        neonPart.setVertexTransform(trans(-0.35f * scale, 1.3f * scale, -1.4f * scale))
        neonPart.box(0.08f * scale, 1f * scale, 0.08f * scale)

        // Anillos en las ruedas (efecto circuito integrado)
        for (i in 0..3) {
            val angle = i * 90f
            val rad = Math.toRadians(angle.toDouble())
            val x = (0.9f * scale * kotlin.math.cos(rad)).toFloat()
            val y = (0.9f * scale * kotlin.math.sin(rad)).toFloat()

            // Rueda delantera
            neonPart.setVertexTransform(trans(x, 0.5f * scale + y, 1.8f * scale))
            neonPart.box(0.08f * scale, 0.08f * scale, 0.4f * scale)

            // Rueda trasera
            neonPart.setVertexTransform(trans(x, 0.5f * scale + y, -1.5f * scale))
            neonPart.box(0.08f * scale, 0.08f * scale, 0.4f * scale)
        }

        // COCKPIT/VIDRIO
        val cockpitPart = modelBuilder.part(
            "cockpit",
            GL20.GL_TRIANGLES,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
            glassMaterial
        )

        // Burbuja del piloto
        cockpitPart.setVertexTransform(trans(0f, 1.3f * scale, 0.4f * scale))
        cockpitPart.sphere(0.4f * scale, 0.5f * scale, 0.6f * scale, 16, 16)

        // PILOTO (silueta oscura)
        val pilotPart = modelBuilder.part(
            "pilot",
            GL20.GL_TRIANGLES,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
            bodyMaterial
        )

        // Cabeza
        pilotPart.setVertexTransform(trans(0f, 1.5f * scale, 0.3f * scale))
        pilotPart.sphere(0.25f * scale, 0.25f * scale, 0.3f * scale, 12, 12)

        // Torso inclinado
        pilotPart.setVertexTransform(trans(0f, 1f * scale, 0.5f * scale))
        pilotPart.box(0.4f * scale, 0.6f * scale, 0.5f * scale)

        return modelBuilder.end()
    }

    private fun createTrailModel(): Model {
        val trailMaterial = Material(
            ColorAttribute.createDiffuse(colorNeon),
            ColorAttribute.createEmissive(colorNeon),
            BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE, 0.9f)
        )

        modelBuilder.begin()

        val trailPart = modelBuilder.part(
            "trail",
            GL20.GL_TRIANGLES,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
            trailMaterial
        )

        // Pared de luz tipo TRON
        trailPart.setVertexTransform(trans(0f, 2.5f, 0f))
        trailPart.box(3f, 5f, 0.3f)

        return modelBuilder.end()
    }

    private fun trans(x: Float, y: Float, z: Float): Matrix4 {
        return Matrix4().setToTranslation(x, y, z)
    }

    fun update(delta: Float) {
        instance.transform.setToTranslation(position)
        instance.transform.rotate(Vector3.Y, rotation)

        val distance = position.dst(lastTrailPosition)
        if (distance > 0.5f) {
            createTrail()
        }

        val iterator = trailInstances.iterator()
        while (iterator.hasNext()) {
            val trail = iterator.next()
            trail.update(delta)
            if (trail.alpha <= 0f) {
                iterator.remove()
            }
        }

        while (trailInstances.size > 120) {
            trailInstances.removeAt(0)
        }
    }

    private fun createTrail() {
        val trailInstance = ModelInstance(trailModel)
        trailInstance.transform.setToTranslation(position.cpy())
        trailInstance.transform.rotate(Vector3.Y, rotation)

        trailInstances.add(TrailSegment(trailInstance, colorNeon.cpy()))
        lastTrailPosition.set(position)
    }

    fun renderTrail(batch: ModelBatch) {
        trailInstances.forEach { trail ->
            val color = trail.color.cpy()
            color.a = trail.alpha

            trail.instance.materials.first().set(
                BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE, trail.alpha)
            )
            trail.instance.materials.first().set(ColorAttribute.createDiffuse(color))
            trail.instance.materials.first().set(ColorAttribute.createEmissive(color))

            batch.render(trail.instance)
        }
    }

    inner class TrailSegment(
        val instance: ModelInstance,
        val color: Color
    ) {
        var alpha = 1f
        private val fadeSpeed = 0.4f

        fun update(delta: Float) {
            alpha -= fadeSpeed * delta
            if (alpha < 0f) alpha = 0f
        }
    }

    override fun dispose() {
        model.dispose()
        trailModel.dispose()
    }
}
