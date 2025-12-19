package com.tron3d.models

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3

class LightCycle(
    val colorNeon: Color,
    initialPosition: Vector3,
    val playerId: Int = 1,
    modelPath: String? = null
) {
    val position = Vector3(initialPosition)
    val direction = Vector3(1f, 0f, 0f)
    var speed = 5f
    var rotation = 0f

    private val modelInstance: ModelInstance

    init {
        Gdx.app.log("LightCycle", "🏍️ Inicializando moto - Player: $playerId")
        Gdx.app.log("LightCycle", "📍 Posición inicial: $initialPosition")
        Gdx.app.log("LightCycle", "🎨 Color neón: $colorNeon")
        Gdx.app.log("LightCycle", "📦 Modelo path: $modelPath")

        modelInstance = if (modelPath != null) {
            val internalPath = if (modelPath.startsWith("models/")) {
                modelPath
            } else {
                "models/$modelPath"
            }

            if (Gdx.files.internal(internalPath).exists()) {
                Gdx.app.log("LightCycle", "✅ Archivo encontrado: $internalPath")
                createFromFile(internalPath)
            } else {
                Gdx.app.log("LightCycle", "⚠️ Archivo NO encontrado: $internalPath")
                Gdx.app.log("LightCycle", "🔧 Usando modelo simple")
                createSimpleModel()
            }
        } else {
            Gdx.app.log("LightCycle", "🔧 Sin path - usando modelo simple")
            createSimpleModel()
        }

        updateTransform()
        Gdx.app.log("LightCycle", "✅ Moto inicializada correctamente")
    }

    private fun createFromFile(path: String): ModelInstance {
        try {
            val loader = if (path.endsWith(".g3dj")) {
                Gdx.app.log("LightCycle", "📄 Cargando formato G3DJ (JSON)")
                com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader(
                    com.badlogic.gdx.utils.JsonReader()
                )
            } else {
                Gdx.app.log("LightCycle", "📦 Cargando formato G3DB (binario)")
                com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader(
                    com.badlogic.gdx.utils.UBJsonReader()
                )
            }

            val model = loader.loadModel(Gdx.files.internal(path))
            val instance = ModelInstance(model)

            Gdx.app.log("LightCycle", "📊 Materiales encontrados: ${instance.materials.size}")

            // ✅ REMOVER NODOS NO DESEADOS
            val nodesToRemove = mutableListOf<com.badlogic.gdx.graphics.g3d.model.Node>()

            instance.nodes.forEach { node ->
                Gdx.app.log("LightCycle", "🔍 Evaluando nodo: ${node.id}")

                if (node.id == "Plane" ||
                    node.id == "Plane.001" ||
                    node.id == "Camera" ||
                    node.id == "Spot" ||
                    node.id == "Empty") {

                    Gdx.app.log("LightCycle", "🗑️ Marcando para remover: ${node.id}")
                    nodesToRemove.add(node)
                }
            }

            nodesToRemove.forEach { node ->
                instance.nodes.removeValue(node, true)
                Gdx.app.log("LightCycle", "✅ Nodo removido: ${node.id}")
            }

            Gdx.app.log("LightCycle", "🎯 Nodos restantes: ${instance.nodes.size}")

            // ✅ PRESERVAR COLORES ORIGINALES + Solo añadir glow del equipo
            instance.materials.forEach { material ->
                Gdx.app.log("LightCycle", "🎨 Procesando material: ${material.id}")

                // Obtener el color original (si existe)
                val originalDiffuse = material.get(ColorAttribute::class.java, ColorAttribute.Diffuse)

                if (originalDiffuse != null) {
                    // ✅ MANTENER el color original tal cual
                    Gdx.app.log("LightCycle", "   Color original: ${originalDiffuse.color}")
                    // NO hacer nada con el diffuse, dejarlo como está

                } else {
                    // Si el material no tiene color, ponerle uno neutral
                    material.set(ColorAttribute.createDiffuse(Color.LIGHT_GRAY))
                    Gdx.app.log("LightCycle", "   ⚠️ Sin color original, usando gris")
                }

                // ✅ SOLO añadir emisión/glow del color del equipo (MUY SUTIL)
                material.set(ColorAttribute.createEmissive(
                    colorNeon.r * 0.15f,  // Muy suave
                    colorNeon.g * 0.15f,
                    colorNeon.b * 0.15f,
                    1f
                ))
            }

            Gdx.app.log("LightCycle", "✅ Modelo 3D cargado (colores preservados)")
            return instance

        } catch (e: Exception) {
            Gdx.app.error("LightCycle", "❌ Error cargando modelo: ${e.message}")
            e.printStackTrace()
            return createSimpleModel()
        }
    }

    private fun createSimpleModel(): ModelInstance {
        Gdx.app.log("LightCycle", "🔨 Creando modelo simple procedural")

        val builder = ModelBuilder()
        builder.begin()

        // Material con emisión para visibilidad
        val material = Material().apply {
            set(ColorAttribute.createDiffuse(colorNeon))
            set(ColorAttribute.createEmissive(
                colorNeon.r * 0.6f,
                colorNeon.g * 0.6f,
                colorNeon.b * 0.6f,
                1f
            ))
        }

        // Cuerpo principal
        val bodyNode = builder.node()
        bodyNode.id = "body"
        val bodyPart = builder.part(
            "body",
            GL20.GL_TRIANGLES,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
            material
        )
        bodyPart.box(3f, 1f, 1.5f)

        // Rueda delantera
        val frontWheelNode = builder.node()
        frontWheelNode.id = "frontWheel"
        frontWheelNode.translation.set(1.2f, -0.3f, 0f)
        val wheelMaterial = Material(ColorAttribute.createDiffuse(Color.WHITE))
        val frontWheelPart = builder.part(
            "frontWheel",
            GL20.GL_TRIANGLES,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
            wheelMaterial
        )
        frontWheelPart.cylinder(0.5f, 0.3f, 0.5f, 16)

        // Rueda trasera
        val rearWheelNode = builder.node()
        rearWheelNode.id = "rearWheel"
        rearWheelNode.translation.set(-1.2f, -0.3f, 0f)
        val rearWheelPart = builder.part(
            "rearWheel",
            GL20.GL_TRIANGLES,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
            Material(ColorAttribute.createDiffuse(Color.WHITE))
        )
        rearWheelPart.cylinder(0.5f, 0.3f, 0.5f, 16)

        val model = builder.end()
        Gdx.app.log("LightCycle", "✅ Modelo simple creado")
        return ModelInstance(model)
    }

    fun update(delta: Float) {
        updateTransform()
    }

    fun moveForward(distance: Float) {
        position.add(
            direction.x * distance,
            0f,
            direction.z * distance
        )
        updateTransform()
        Gdx.app.log("LightCycle", "🏃 Moviendo a: (${position.x}, ${position.z})")
    }

    private fun updateTransform() {
        modelInstance.transform.setToTranslation(position)
        modelInstance.transform.rotate(Vector3.Y, rotation)
        modelInstance.transform.scale(0.001f, 0.001f, 0.001f)
    }

    fun turnLeft() {
        direction.rotate(Vector3.Y, 90f).nor()
        rotation += 90f
        updateTransform()
        Gdx.app.log("LightCycle", "↪️ Girando izquierda - Rotación: $rotation°")
    }

    fun turnRight() {
        direction.rotate(Vector3.Y, -90f).nor()
        rotation -= 90f
        updateTransform()
        Gdx.app.log("LightCycle", "↩️ Girando derecha - Rotación: $rotation°")
    }

    fun render(modelBatch: ModelBatch, environment: Environment) {
        // ✅ SOLO renderizar el modelo, SIN glow
        modelBatch.render(modelInstance, environment)
    }

    fun dispose() {
        modelInstance.model.dispose()
        Gdx.app.log("LightCycle", "🗑️ Modelo disposed")
    }
}
