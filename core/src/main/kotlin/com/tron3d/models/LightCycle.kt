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

    val trailSegments = mutableListOf<TrailSegment>()
    private var lastTrailPosition: Vector3? = null
    private val minTrailDistance = 0.5f

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

            // ✅ APLICAR COLORES DEL EQUIPO (CYAN o NARANJA)
            instance.materials.forEach { material ->
                Gdx.app.log("LightCycle", "🎨 Procesando material: ${material.id}")

                // ✅ Si es una rueda o material negro, mantener negro
                if (material.id == "Material.004" ||
                    material.id == "Material.007" ||
                    material.id?.contains("wheel", ignoreCase = true) == true) {

                    material.set(ColorAttribute.createDiffuse(Color.BLACK))
                    Gdx.app.log("LightCycle", "   ⚫ Rueda/detalle negro preservado")

                } else {
                    // ✅ Resto del cuerpo: COLOR DEL EQUIPO (cyan o naranja)
                    material.set(ColorAttribute.createDiffuse(colorNeon))
                    material.set(ColorAttribute.createEmissive(
                        colorNeon.r * 0.10f,  // ✅ Emisión fuerte para visibilidad
                        colorNeon.g * 0.10f,
                        colorNeon.b * 0.10f,
                        1f
                    ))

                    Gdx.app.log("LightCycle", "   ✅ Color equipo aplicado: $colorNeon")
                }
            }

            Gdx.app.log("LightCycle", "✅ Modelo 3D cargado con colores del equipo")
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
        updateTrail()  // ✅ NUEVO

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
        modelInstance.transform.scale(0.002f, 0.002f, 0.002f)
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
        // ✅ Renderizar rastro PRIMERO (para que quede detrás)
        trailSegments.forEach { segment ->
            modelBatch.render(segment.modelInstance, environment)
        }
        // ✅ SOLO renderizar el modelo, SIN glow
        modelBatch.render(modelInstance, environment)
    }

    fun dispose() {
        clearTrail()
        modelInstance.model.dispose()
        Gdx.app.log("LightCycle", "🗑️ Modelo disposed")
    }

    private fun updateTrail() {
        val currentPos = Vector3(position)

        if (lastTrailPosition == null) {
            lastTrailPosition = Vector3(currentPos)
            return
        }

        val distance = currentPos.dst(lastTrailPosition!!)

        if (distance >= minTrailDistance) {
            // Crear nuevo segmento de rastro
            val segment = TrailSegment(
                start = Vector3(lastTrailPosition!!),
                end = Vector3(currentPos),
                color = colorNeon,
                width = 0.3f,
                height = 2.5f
            )

            trailSegments.add(segment)
            lastTrailPosition!!.set(currentPos)

            Gdx.app.log("LightCycle", "✨ Rastro creado: ${trailSegments.size} segmentos")
        }
    }

    // ✅ NUEVO: Limpiar rastros al reiniciar
    fun clearTrail() {
        trailSegments.forEach { it.dispose() }
        trailSegments.clear()
        lastTrailPosition = null
        Gdx.app.log("LightCycle", "🗑️ Rastro limpiado")
    }

}
