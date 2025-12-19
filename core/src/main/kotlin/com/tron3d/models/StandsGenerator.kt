package com.tron3d.models

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.utils.Disposable

/**
 * Generador de gradas estilo Tron - Ajustado para arena con escala 0.01
 */
class StandsGenerator : Disposable {

    private val models = mutableListOf<Model>()
    val instances = mutableListOf<ModelInstance>()
    private var crowdTexture: Texture? = null

    /**
     * Generar gradas adaptadas a la arena escalada a 0.01
     */
    fun generate(arenaInstance: ModelInstance? = null) {
        Gdx.app.log("StandsGenerator", "🏟️ Generando gradas (escala 0.01)...")

        crowdTexture = loadTexture("models/portal_alpha_1.png")
        val modelBuilder = ModelBuilder()

        // ✅ Calcular dimensiones reales de la arena DESPUÉS de escalarla
        var arenaMinX = 0f
        var arenaMaxX = 50f
        var arenaMinZ = 0f
        var arenaMaxZ = 30f
        var arenaWidth = 50f
        var arenaDepth = 30f
        var centerX = 25f
        var centerZ = 15f

        if (arenaInstance != null) {
            val boundingBox = BoundingBox()
            arenaInstance.calculateBoundingBox(boundingBox)
            val min = Vector3()
            val max = Vector3()
            boundingBox.getMin(min)
            boundingBox.getMax(max)

            arenaMinX = min.x
            arenaMaxX = max.x
            arenaMinZ = min.z
            arenaMaxZ = max.z
            arenaWidth = max.x - min.x
            arenaDepth = max.z - min.z
            centerX = (min.x + max.x) / 2f
            centerZ = (min.z + max.z) / 2f

            Gdx.app.log("StandsGenerator", "📐 Arena detectada:")
            Gdx.app.log("StandsGenerator", "   Ancho: $arenaWidth")
            Gdx.app.log("StandsGenerator", "   Profundidad: $arenaDepth")
            Gdx.app.log("StandsGenerator", "   Centro: ($centerX, $centerZ)")
            Gdx.app.log("StandsGenerator", "   Min: ($arenaMinX, $arenaMinZ)")
            Gdx.app.log("StandsGenerator", "   Max: ($arenaMaxX, $arenaMaxZ)")
        }

        // Distancia y tamaño de las gradas proporcional a la arena
        val standOffset = arenaWidth * 0.25f  // 25% del ancho de la arena
        val standDepth = arenaWidth * 0.15f   // 15% del ancho

        Gdx.app.log("StandsGenerator", "📏 Configuración gradas:")
        Gdx.app.log("StandsGenerator", "   Offset: $standOffset")
        Gdx.app.log("StandsGenerator", "   Profundidad: $standDepth")

        // ✅ GRADAS EN LOS 4 LADOS - ADAPTADAS AL TAMAÑO REAL

        // Izquierda (X negativo)
        createTronStand(
            modelBuilder,
            x = arenaMinX - standOffset,
            z = centerZ,
            width = standDepth,
            length = arenaDepth * 1.3f,
            side = "LEFT"
        )

        // Derecha (X positivo)
        createTronStand(
            modelBuilder,
            x = arenaMaxX + standOffset,
            z = centerZ,
            width = standDepth,
            length = arenaDepth * 1.3f,
            side = "RIGHT"
        )

        // Atrás (Z negativo)
        createTronStand(
            modelBuilder,
            x = centerX,
            z = arenaMinZ - standOffset,
            width = arenaWidth * 1.3f,
            length = standDepth,
            side = "BACK"
        )

        // Frente (Z positivo)
        createTronStand(
            modelBuilder,
            x = centerX,
            z = arenaMaxZ + standOffset,
            width = arenaWidth * 1.3f,
            length = standDepth,
            side = "FRONT"
        )

        // Luces superiores
        createTopLights(modelBuilder, arenaMinX, arenaMaxX, arenaMinZ, arenaMaxZ, arenaWidth, arenaDepth)

        Gdx.app.log("StandsGenerator", "✅ ${instances.size} elementos generados")
    }

    private fun createTronStand(
        modelBuilder: ModelBuilder,
        x: Float, z: Float,
        width: Float, length: Float,
        side: String
    ) {
        val numLevels = 8
        val levelHeight = 2.5f
        val levelDepth = 1.8f

        for (level in 0 until numLevels) {
            val currentHeight = level * levelHeight
            val currentDepth = level * levelDepth

            val offsetX = when (side) {
                "LEFT" -> currentDepth
                "RIGHT" -> -currentDepth
                else -> 0f
            }

            val offsetZ = when (side) {
                "BACK" -> currentDepth
                "FRONT" -> -currentDepth
                else -> 0f
            }

            val baseColor = Color(0.08f, 0.12f, 0.18f, 1f)

            val material = if (crowdTexture != null && level >= 2) {
                Material(
                    TextureAttribute.createDiffuse(crowdTexture),
                    ColorAttribute.createDiffuse(Color(0.7f, 0.7f, 0.8f, 1f)),
                    ColorAttribute.createEmissive(0.04f, 0.08f, 0.12f, 1f)
                )
            } else {
                Material(
                    ColorAttribute.createDiffuse(baseColor),
                    ColorAttribute.createEmissive(0.01f, 0.05f, 0.08f, 1f)
                )
            }

            val levelWidth = if (side == "LEFT" || side == "RIGHT") {
                (width - level * 0.25f).coerceAtLeast(width * 0.3f)
            } else {
                (width - level * 1.0f).coerceAtLeast(width * 0.2f)
            }

            val levelLength = if (side == "BACK" || side == "FRONT") {
                (length - level * 0.25f).coerceAtLeast(length * 0.3f)
            } else {
                (length - level * 1.0f).coerceAtLeast(length * 0.2f)
            }

            val model = modelBuilder.createBox(
                levelWidth, levelHeight * 0.9f, levelLength,
                material,
                (VertexAttributes.Usage.Position or
                    VertexAttributes.Usage.Normal or
                    VertexAttributes.Usage.TextureCoordinates or
                    VertexAttributes.Usage.ColorPacked).toLong()
            )

            models.add(model)

            val instance = ModelInstance(model)
            instance.transform.setToTranslation(
                x + offsetX,
                currentHeight + levelHeight * 0.45f,
                z + offsetZ
            )
            instances.add(instance)

            // Bordes luminosos
            if (level > 0 && level < numLevels - 1) {
                createEdgeLight(
                    modelBuilder,
                    x + offsetX,
                    currentHeight + levelHeight * 0.9f,
                    z + offsetZ,
                    levelWidth,
                    levelLength,
                    side
                )
            }
        }
    }

    private fun createEdgeLight(
        modelBuilder: ModelBuilder,
        x: Float, y: Float, z: Float,
        width: Float, length: Float,
        side: String
    ) {
        val material = Material(
            ColorAttribute.createDiffuse(Color(0.4f, 1f, 1f, 1f)),
            ColorAttribute.createEmissive(0.9f, 1f, 1f, 1f)
        )

        val edgeThickness = 0.25f
        val edgeHeight = 0.4f

        val model = modelBuilder.createBox(
            if (side == "LEFT" || side == "RIGHT") edgeThickness else width,
            edgeHeight,
            if (side == "BACK" || side == "FRONT") edgeThickness else length,
            material,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal or VertexAttributes.Usage.ColorPacked).toLong()
        )

        models.add(model)

        val offsetAmount = if (side == "LEFT" || side == "RIGHT") width / 2f else length / 2f
        val offsetX = when (side) {
            "LEFT" -> offsetAmount
            "RIGHT" -> -offsetAmount
            else -> 0f
        }
        val offsetZ = when (side) {
            "BACK" -> offsetAmount
            "FRONT" -> -offsetAmount
            else -> 0f
        }

        val instance = ModelInstance(model)
        instance.transform.setToTranslation(x + offsetX, y, z + offsetZ)
        instances.add(instance)
    }

    private fun createTopLights(
        modelBuilder: ModelBuilder,
        minX: Float, maxX: Float,
        minZ: Float, maxZ: Float,
        width: Float, depth: Float
    ) {
        val material = Material(
            ColorAttribute.createDiffuse(Color.WHITE),
            ColorAttribute.createEmissive(1f, 1f, 1f, 1f)
        )

        val lightHeight = 30f
        val margin = width * 0.1f
        val numLightsWidth = 12
        val numLightsDepth = 8

        // Luces laterales (a lo largo del eje Z)
        for (i in 0 until numLightsDepth) {
            val progress = i.toFloat() / (numLightsDepth - 1)
            val zPos = minZ + margin + progress * (depth - margin * 2)

            createSingleLight(modelBuilder, material, minX - margin * 0.5f, lightHeight, zPos)
            createSingleLight(modelBuilder, material, maxX + margin * 0.5f, lightHeight, zPos)
        }

        // Luces frontales/traseras (a lo largo del eje X)
        for (i in 0 until numLightsWidth) {
            val progress = i.toFloat() / (numLightsWidth - 1)
            val xPos = minX + margin + progress * (width - margin * 2)

            createSingleLight(modelBuilder, material, xPos, lightHeight, minZ - margin * 0.5f)
            createSingleLight(modelBuilder, material, xPos, lightHeight, maxZ + margin * 0.5f)
        }

        Gdx.app.log("StandsGenerator", "💡 ${(numLightsWidth + numLightsDepth) * 2} luces generadas")
    }

    private fun createSingleLight(
        modelBuilder: ModelBuilder,
        material: Material,
        x: Float, y: Float, z: Float
    ) {
        // Luz (esfera brillante)
        val model = modelBuilder.createSphere(
            0.7f, 0.7f, 0.7f, 10, 10,
            material,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal or VertexAttributes.Usage.ColorPacked).toLong()
        )

        models.add(model)
        instances.add(ModelInstance(model).apply {
            transform.setToTranslation(x, y, z)
        })

        // Cable/soporte
        val cableMaterial = Material(
            ColorAttribute.createDiffuse(Color(0.15f, 0.25f, 0.35f, 1f)),
            ColorAttribute.createEmissive(0.03f, 0.05f, 0.08f, 1f)
        )

        val cable = modelBuilder.createCylinder(
            0.12f, y - 2f, 0.12f, 6,
            cableMaterial,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal or VertexAttributes.Usage.ColorPacked).toLong()
        )

        models.add(cable)
        instances.add(ModelInstance(cable).apply {
            transform.setToTranslation(x, y / 2f, z)
        })
    }

    private fun loadTexture(path: String): Texture? {
        return try {
            if (Gdx.files.internal(path).exists()) {
                Texture(Gdx.files.internal(path))
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun dispose() {
        models.forEach { it.dispose() }
        models.clear()
        instances.clear()
        crowdTexture?.dispose()
    }
}
