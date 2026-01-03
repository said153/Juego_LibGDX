package com.tron3d.debug

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3

/**
 * Controlador para un punto de debug movible en 3D CON FLECHAS DE MOVIMIENTO
 * ✅ PUNTO PEQUEÑO con indicadores de dirección visibles
 * ✅ X, Y, Z completamente movibles
 * ✅ MOVIMIENTO LIBRE COMPLETO - SIN NINGÚN LÍMITE
 */
class DebugPointController {

    private var debugPointModel: Model? = null
    private var debugPointInstance: ModelInstance? = null
    private val debugPointPosition = Vector3(0f, 5f, 0f) // Posición inicial en el centro

    // ✅ VELOCIDAD AJUSTABLE
    private var moveSpeed = 5.0f
    private val rotationSpeed = 0.5f

    // Para mostrar coordenadas
    private var isEnabled = false
    private var showGrid = false  // Grid oculto por defecto para movimiento libre
    private var gridModel: Model? = null
    private var gridInstance: ModelInstance? = null

    // ✅ MODELOS PARA FLECHAS DE DIRECCIÓN
    private var arrowForwardModel: Model? = null
    private var arrowBackwardModel: Model? = null
    private var arrowLeftModel: Model? = null
    private var arrowRightModel: Model? = null
    private var arrowForwardInstance: ModelInstance? = null
    private var arrowBackwardInstance: ModelInstance? = null
    private var arrowLeftInstance: ModelInstance? = null
    private var arrowRightInstance: ModelInstance? = null

    // ✅ MODELOS PARA MOVIMIENTO VERTICAL (Y)
    private var arrowUpModel: Model? = null
    private var arrowDownModel: Model? = null
    private var arrowUpInstance: ModelInstance? = null
    private var arrowDownInstance: ModelInstance? = null

    // ✅ SIN LÍMITES - ELIMINAMOS TODA LÓGICA DE LÍMITES
    private val INFINITE_BOUNDS = 1000000f  // Valor muy grande para grid visual

    // ✅ Y VARIABLE (para explorar altura también)
    private var isFixedY = false
    private var initialYValue = 5f

    // ✅ CONTADOR DE FRAMES LOCAL
    private var frameCount = 0

    // ✅ VISIBILIDAD DE FLECHAS
    private var showDirectionArrows = true

    // ✅ CONTROL DE CÁMARA
    private var cameraFollow = true
    private val cameraOffset = Vector3(0f, 25f, 35f)

    init {
        // ✅ CREAR MODELOS - SIN NINGÚN LÍMITE
        createDebugPoint()
        createDirectionArrows()
        createVerticalArrows()
        createGrid()

        Gdx.app.log("DebugPoint", "✅ Punto de debug creado (pequeño)")
        Gdx.app.log("DebugPoint", "🎯 Flechas de dirección disponibles")
        Gdx.app.log("DebugPoint", "🌍 MOVIMIENTO LIBRE COMPLETO ACTIVADO")
        Gdx.app.log("DebugPoint", "🚀 Puedes moverte infinitamente en TODAS direcciones")
        Gdx.app.log("DebugPoint", "🎮 Controles: A/D (X), Q/E (Y), W/S (Z), Shift=rapido, Ctrl=lento")
        Gdx.app.log("DebugPoint", "📷 Cámara sigue automáticamente al punto")
        Gdx.app.log("DebugPoint", "📌 Grid oculto por defecto (presiona G para mostrarlo)")
    }

    /**
     * ✅ CREAR PUNTO DE DEBUG MÁS PEQUEÑO
     */
    private fun createDebugPoint() {
        val modelBuilder = ModelBuilder()
        modelBuilder.begin()

        // ✅ PUNTO ROJO VISIBLE PERO PEQUEÑO
        val material = Material(ColorAttribute.createDiffuse(Color.RED))

        // Esfera principal MÁS PEQUEÑA
        modelBuilder.part("debug_sphere",
            com.badlogic.gdx.graphics.GL20.GL_TRIANGLES,
            VertexAttributes.Usage.Position.toLong() or VertexAttributes.Usage.Normal.toLong(),
            material).apply {
            setColor(Color.RED)
            sphere(0.3f, 0.3f, 0.3f, 10, 10)
        }

        // ✅ ANILLO AMARILLO PARA VISIBILIDAD
        val ringMaterial = Material(ColorAttribute.createDiffuse(Color(1f, 1f, 0f, 0.7f)))
        modelBuilder.part("debug_ring",
            com.badlogic.gdx.graphics.GL20.GL_LINES,
            VertexAttributes.Usage.Position.toLong(),
            ringMaterial).apply {
            setColor(Color.YELLOW)
            circle(0.4f, 12, 0f, 0f, 0f, 0f, 1f, 0f)
            circle(0.4f, 12, 0f, 0f, 0f, 1f, 0f, 0f)
            circle(0.4f, 12, 0f, 0f, 0f, 0f, 0f, 1f)
        }

        // ✅ INDICADOR DE DIRECCIÓN (flecha verde)
        val arrowMaterial = Material(ColorAttribute.createDiffuse(Color(0f, 1f, 0f, 0.9f)))
        modelBuilder.part("debug_arrow",
            com.badlogic.gdx.graphics.GL20.GL_TRIANGLES,
            VertexAttributes.Usage.Position.toLong() or VertexAttributes.Usage.Normal.toLong(),
            arrowMaterial).apply {
            setColor(Color.GREEN)
            cone(0.15f, 0.5f, 0.15f, 8)
        }

        debugPointModel = modelBuilder.end()
        debugPointInstance = ModelInstance(debugPointModel)
        debugPointInstance?.transform?.setToTranslation(debugPointPosition)
    }

    /**
     * ✅ CREAR FLECHAS DE DIRECCIÓN PARA CONTROLES VISUALES
     */
    private fun createDirectionArrows() {
        // ✅ FLECHA ADELANTE (VERDE - Z negativo)
        arrowForwardModel = createArrowModel(Color(0f, 1f, 0f, 0.8f), 0f, 0f, -0.5f)
        arrowForwardInstance = ModelInstance(arrowForwardModel)
        updateArrowPositions()

        // ✅ FLECHA ATRÁS (ROJO - Z positivo)
        arrowBackwardModel = createArrowModel(Color(1f, 0f, 0f, 0.8f), 0f, 0f, 0.5f)
        arrowBackwardInstance = ModelInstance(arrowBackwardModel)
        arrowBackwardInstance?.transform?.rotate(0f, 1f, 0f, 180f)
        updateArrowPositions()

        // ✅ FLECHA IZQUIERDA (AZUL - X negativo)
        arrowLeftModel = createArrowModel(Color(0f, 0f, 1f, 0.8f), -0.5f, 0f, 0f)
        arrowLeftInstance = ModelInstance(arrowLeftModel)
        arrowLeftInstance?.transform?.rotate(0f, 1f, 0f, 90f)
        updateArrowPositions()

        // ✅ FLECHA DERECHA (AMARILLO - X positivo)
        arrowRightModel = createArrowModel(Color(1f, 1f, 0f, 0.8f), 0.5f, 0f, 0f)
        arrowRightInstance = ModelInstance(arrowRightModel)
        arrowRightInstance?.transform?.rotate(0f, 1f, 0f, -90f)
        updateArrowPositions()
    }

    /**
     * ✅ CREAR FLECHAS VERTICALES (para movimiento en Y)
     */
    private fun createVerticalArrows() {
        // ✅ FLECHA ARRIBA (MAGENTA - Y positivo)
        arrowUpModel = createVerticalArrowModel(Color(1f, 0f, 1f, 0.8f), 0f, 0.5f, 0f)
        arrowUpInstance = ModelInstance(arrowUpModel)
        arrowUpInstance?.transform?.rotate(1f, 0f, 0f, -90f)
        updateArrowPositions()

        // ✅ FLECHA ABAJO (CIAN - Y negativo)
        arrowDownModel = createVerticalArrowModel(Color(0f, 1f, 1f, 0.8f), 0f, -0.5f, 0f)
        arrowDownInstance = ModelInstance(arrowDownModel)
        arrowDownInstance?.transform?.rotate(1f, 0f, 0f, 90f)
        updateArrowPositions()
    }

    /**
     * ✅ CREAR MODELO DE FLECHA
     */
    private fun createArrowModel(color: Color, offsetX: Float, offsetY: Float, offsetZ: Float): Model {
        val modelBuilder = ModelBuilder()
        modelBuilder.begin()

        val material = Material(ColorAttribute.createDiffuse(color))

        modelBuilder.part("arrow",
            com.badlogic.gdx.graphics.GL20.GL_TRIANGLES,
            VertexAttributes.Usage.Position.toLong() or VertexAttributes.Usage.Normal.toLong(),
            material).apply {
            cylinder(0.05f, 0.5f, 0.05f, 8)
            cone(0.1f, 0.2f, 0.1f, 8)
        }

        val model = modelBuilder.end()
        val instance = ModelInstance(model)
        instance.transform.setToTranslation(offsetX, offsetY, offsetZ)
        return model
    }

    /**
     * ✅ CREAR MODELO DE FLECHA VERTICAL
     */
    private fun createVerticalArrowModel(color: Color, offsetX: Float, offsetY: Float, offsetZ: Float): Model {
        val modelBuilder = ModelBuilder()
        modelBuilder.begin()

        val material = Material(ColorAttribute.createDiffuse(color))

        modelBuilder.part("vertical_arrow",
            com.badlogic.gdx.graphics.GL20.GL_TRIANGLES,
            VertexAttributes.Usage.Position.toLong() or VertexAttributes.Usage.Normal.toLong(),
            material).apply {
            cylinder(0.05f, 0.5f, 0.05f, 8)
            cone(0.1f, 0.2f, 0.1f, 8)
        }

        val model = modelBuilder.end()
        val instance = ModelInstance(model)
        instance.transform.setToTranslation(offsetX, offsetY, offsetZ)
        return model
    }

    /**
     * ✅ ACTUALIZAR POSICIONES DE LAS FLECHAS
     */
    private fun updateArrowPositions() {
        val pos = debugPointPosition
        arrowForwardInstance?.transform?.setToTranslation(pos.x, pos.y + 0.3f, pos.z - 5f)
        arrowBackwardInstance?.transform?.setToTranslation(pos.x, pos.y + 0.3f, pos.z + 5f)
        arrowLeftInstance?.transform?.setToTranslation(pos.x - 5f, pos.y + 0.3f, pos.z)
        arrowRightInstance?.transform?.setToTranslation(pos.x + 5f, pos.y + 0.3f, pos.z)
        arrowUpInstance?.transform?.setToTranslation(pos.x, pos.y + 5f, pos.z)
        arrowDownInstance?.transform?.setToTranslation(pos.x, pos.y - 5f, pos.z)
    }

    /**
     * ✅ CONFIGURAR Y FIJO (o variable)
     */
    fun setFixedY(value: Float, fixed: Boolean = true) {
        isFixedY = fixed
        initialYValue = value
        debugPointPosition.y = value
        if (fixed) {
            Gdx.app.log("DebugPoint", "🔧 Y fijado en $value (inmóvil)")
        } else {
            Gdx.app.log("DebugPoint", "🔧 Y inicial en $value (movible)")
        }
    }

    /**
     * ✅ CONFIGURAR VELOCIDAD
     */
    fun setMoveSpeed(speed: Float) {
        moveSpeed = speed
        Gdx.app.log("DebugPoint", "⚡ Velocidad ajustada a $speed")
    }

    /**
     * ✅ TOGGLE SEGUIMIENTO DE CÁMARA
     */
    fun toggleCameraFollow() {
        cameraFollow = !cameraFollow
        Gdx.app.log("DebugPoint", if (cameraFollow) "📷 Cámara sigue al punto" else "📷 Cámara estática")
    }

    /**
     * ✅ TOGGLE VISIBILIDAD DE FLECHAS
     */
    fun toggleDirectionArrows() {
        showDirectionArrows = !showDirectionArrows
        Gdx.app.log("DebugPoint", if (showDirectionArrows) "🎯 Flechas visibles" else "🎯 Flechas ocultas")
    }

    /**
     * ✅ MÉTODO PARA OBTENER COORDENADAS COMPLETAS X, Y, Z
     */
    fun getPositionXYZString(): String {
        return "X=${debugPointPosition.x.format(2)}, Y=${debugPointPosition.y.format(2)}, Z=${debugPointPosition.z.format(2)}"
    }

    /**
     * ✅ MÉTODO PARA OBTENER COORDENADAS X y Z (compatibilidad)
     */
    fun getPositionXZString(): String {
        return "X=${debugPointPosition.x.format(2)}, Z=${debugPointPosition.z.format(2)}"
    }

    /**
     * ✅ OBTENER COORDENADAS POR SEPARADO
     */
    fun getX(): Float = debugPointPosition.x
    fun getY(): Float = debugPointPosition.y
    fun getZ(): Float = debugPointPosition.z

    /**
     * ✅ OBTENER POSICIÓN COMO VECTOR3
     */
    fun getPosition(): Vector3 = Vector3(debugPointPosition)

    /**
     * ✅ OBTENER INFO DE LÍMITES (siempre sin límites)
     */
    fun getCurrentBoundsString(): String {
        return "SIN LÍMITES - MOVIMIENTO LIBRE COMPLETO"
    }

    private fun createGrid() {
        val modelBuilder = ModelBuilder()
        modelBuilder.begin()

        val gridMaterial = Material(ColorAttribute.createDiffuse(Color(0.3f, 0.3f, 0.5f, 0.08f)))
        val meshBuilder = modelBuilder.part("grid",
            com.badlogic.gdx.graphics.GL20.GL_LINES,
            VertexAttributes.Usage.Position.toLong() or VertexAttributes.Usage.ColorPacked.toLong(),
            gridMaterial)

        meshBuilder.setColor(Color.LIGHT_GRAY)

        // Grid de 1000x1000 unidades (solo visual)
        val gridSize = 1000f
        val divisions = 20

        // Líneas en X
        for (i in -divisions..divisions) {
            val x = i * (gridSize / divisions)
            meshBuilder.line(x, 0.05f, -gridSize, x, 0.05f, gridSize)
        }

        // Líneas en Z
        for (i in -divisions..divisions) {
            val z = i * (gridSize / divisions)
            meshBuilder.line(-gridSize, 0.05f, z, gridSize, 0.05f, z)
        }

        // Borde del grid
        meshBuilder.setColor(Color(1f, 0f, 0f, 0.3f))
        meshBuilder.line(-gridSize, 0.1f, -gridSize, gridSize, 0.1f, -gridSize)
        meshBuilder.line(gridSize, 0.1f, -gridSize, gridSize, 0.1f, gridSize)
        meshBuilder.line(gridSize, 0.1f, gridSize, -gridSize, 0.1f, gridSize)
        meshBuilder.line(-gridSize, 0.1f, gridSize, -gridSize, 0.1f, -gridSize)

        gridModel = modelBuilder.end()
        gridInstance = ModelInstance(gridModel)
        gridInstance?.transform?.setToTranslation(0f, 0f, 0f)
    }

    fun update(camera: PerspectiveCamera) {
        if (!isEnabled) return

        val delta = Gdx.graphics.deltaTime

        // ✅ EVITAR MOVIMIENTO DEL PUNTO DEBUG CUANDO HAY 2 DEDOS (ZOOM)
        val touchCount = getTouchCount()
        if (touchCount >= 2) {
            debugPointInstance?.transform?.rotate(0f, 1f, 0f, rotationSpeed * delta * 50)
            return
        }

        // ✅ VELOCIDAD AJUSTABLE
        var currentMoveSpeed = moveSpeed

        // ✅ TECLA SHIFT PARA MOVIMIENTO MÁS RÁPIDO
        if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) {
            currentMoveSpeed *= 5.0f
        }

        // ✅ TECLA CONTROL PARA MOVIMIENTO MÁS LENTO (PRECISIÓN)
        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)) {
            currentMoveSpeed *= 0.05f
        }

        // ✅ CONTROLES CON TECLADO
        val moveMultiplier = currentMoveSpeed * delta * 25

        // ✅ MOVIMIENTO EN X - ¡SIN LÍMITES!
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            debugPointPosition.x -= moveMultiplier
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            debugPointPosition.x += moveMultiplier
        }

        // ✅ MOVIMIENTO EN Y - ¡SIN LÍMITES!
        if (Gdx.input.isKeyPressed(Input.Keys.Q) || Gdx.input.isKeyPressed(Input.Keys.PAGE_UP)) {
            debugPointPosition.y += moveMultiplier
        }
        if (Gdx.input.isKeyPressed(Input.Keys.E) || Gdx.input.isKeyPressed(Input.Keys.PAGE_DOWN)) {
            debugPointPosition.y -= moveMultiplier
        }

        // ✅ MOVIMIENTO EN Z - ¡SIN LÍMITES!
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            debugPointPosition.z -= moveMultiplier
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            debugPointPosition.z += moveMultiplier
        }

        // ✅ MOVIMIENTO CON GESTOS TÁCTILES
        if (touchCount == 1) {
            val touchX = Gdx.input.getX(0)
            val touchY = Gdx.input.getY(0)
            val screenWidth = Gdx.graphics.width
            val screenHeight = Gdx.graphics.height
            val edgeZone = screenWidth * 0.25f

            when {
                touchX < edgeZone -> debugPointPosition.x -= moveMultiplier * 3
                touchX > screenWidth - edgeZone -> debugPointPosition.x += moveMultiplier * 3
                touchY < edgeZone -> debugPointPosition.z -= moveMultiplier * 3
                touchY > screenHeight - edgeZone -> debugPointPosition.z += moveMultiplier * 3
            }
        }

        // ✅ ¡NO HAY LÍMITES! Puedes moverte infinitamente
        // No hay coerceIn, no hay restricciones

        // ✅ ACTUALIZAR CÁMARA SI SIGUE AL PUNTO
        if (cameraFollow) {
            camera.position.set(
                debugPointPosition.x + cameraOffset.x,
                debugPointPosition.y + cameraOffset.y,
                debugPointPosition.z + cameraOffset.z
            )
            camera.lookAt(debugPointPosition)
            camera.update()
        }

        // Actualizar posición del modelo
        debugPointInstance?.transform?.setToTranslation(debugPointPosition)
        updateArrowPositions()
        debugPointInstance?.transform?.rotate(0f, 1f, 0f, rotationSpeed * delta * 10)

        frameCount++
        if (frameCount % 30 == 0) {
            Gdx.app.log("DebugPoint", "📍 Posición: X=${debugPointPosition.x.format(1)}, " +
                "Y=${debugPointPosition.y.format(1)}, Z=${debugPointPosition.z.format(1)}")
            Gdx.app.log("DebugPoint", "🎮 Movimiento libre - Sin límites en ninguna dirección")
        }
    }

    // ✅ MÉTODO PARA CONTAR DEDOS EN PANTALLA
    private fun getTouchCount(): Int {
        var count = 0
        for (i in 0 until 5) {
            if (Gdx.input.isTouched(i)) {
                count++
            }
        }
        return count
    }

    fun render(modelBatch: ModelBatch, environment: com.badlogic.gdx.graphics.g3d.Environment) {
        if (!isEnabled) return

        if (showGrid && gridInstance != null) {
            modelBatch.render(gridInstance, environment)
        }

        if (showDirectionArrows) {
            arrowForwardInstance?.let { modelBatch.render(it, environment) }
            arrowBackwardInstance?.let { modelBatch.render(it, environment) }
            arrowLeftInstance?.let { modelBatch.render(it, environment) }
            arrowRightInstance?.let { modelBatch.render(it, environment) }
            arrowUpInstance?.let { modelBatch.render(it, environment) }
            arrowDownInstance?.let { modelBatch.render(it, environment) }
        }

        debugPointInstance?.let { modelBatch.render(it, environment) }
    }

    fun toggleEnabled() {
        isEnabled = !isEnabled
        Gdx.app.log("DebugPoint", if (isEnabled) "🔧 MODO DEBUG ACTIVADO" else "🔧 MODO DEBUG DESACTIVADO")
        if (isEnabled) {
            Gdx.app.log("DebugPoint", "📍 Controles:")
            Gdx.app.log("DebugPoint", "  • A/D: Izquierda/Derecha (X) - ¡SIN LÍMITES!")
            Gdx.app.log("DebugPoint", "  • Q/E: Arriba/Abajo (Y) - ¡SIN LÍMITES!")
            Gdx.app.log("DebugPoint", "  • W/S: Adelante/Atrás (Z) - ¡SIN LÍMITES!")
            Gdx.app.log("DebugPoint", "  • SHIFT: Velocidad SUPER rápida (5x)")
            Gdx.app.log("DebugPoint", "  • CTRL: Velocidad ultra lenta (0.05x)")
            Gdx.app.log("DebugPoint", "  • C: Seguimiento cámara ON/OFF")
            Gdx.app.log("DebugPoint", "  • F: Mostrar/ocultar flechas")
            Gdx.app.log("DebugPoint", "  • G: Mostrar/ocultar grid")
            Gdx.app.log("DebugPoint", "🌍 MOVIMIENTO LIBRE COMPLETO ACTIVADO")
            Gdx.app.log("DebugPoint", "🚀 Puedes moverte infinitamente en TODAS direcciones")
        }
    }

    fun toggleGrid() {
        showGrid = !showGrid
        Gdx.app.log("DebugPoint", if (showGrid) "📐 Grid visible (solo visual)" else "📐 Grid oculto")
    }

    fun getCurrentPosition(): Vector3 = Vector3(debugPointPosition)

    fun getCurrentPositionString(): String {
        return "X=${debugPointPosition.x.format(2)}, Y=${debugPointPosition.y.format(2)}, Z=${debugPointPosition.z.format(2)}"
    }

    /**
     * ✅ MÉTODO SIMPLIFICADO: Configurar área (solo para grid visual)
     */
    fun setAreaSize(size: Float) {
        createGrid()  // Solo recrea el grid visual
        Gdx.app.log("DebugPoint", "📐 Grid visual ajustado a tamaño $size")
        Gdx.app.log("DebugPoint", "⚠️ Esto NO afecta el movimiento - sigue siendo libre")
    }

    /**
     * ✅ ELIMINAR MÉTODOS RELACIONADOS CON LÍMITES
     * No hay enableBounds, disableBounds, setArenaBounds, etc.
     */

    fun dispose() {
        debugPointModel?.dispose()
        gridModel?.dispose()
        arrowForwardModel?.dispose()
        arrowBackwardModel?.dispose()
        arrowLeftModel?.dispose()
        arrowRightModel?.dispose()
        arrowUpModel?.dispose()
        arrowDownModel?.dispose()
        Gdx.app.log("DebugPoint", "🗑️ Recursos de debug liberados")
    }

    fun getDebugInstances(): List<ModelInstance> {
        val instances = mutableListOf<ModelInstance>()

        if (showGrid && gridInstance != null) {
            instances.add(gridInstance!!)
        }

        if (showDirectionArrows) {
            arrowForwardInstance?.let { instances.add(it) }
            arrowBackwardInstance?.let { instances.add(it) }
            arrowLeftInstance?.let { instances.add(it) }
            arrowRightInstance?.let { instances.add(it) }
            arrowUpInstance?.let { instances.add(it) }
            arrowDownInstance?.let { instances.add(it) }
        }

        debugPointInstance?.let { instances.add(it) }

        return instances
    }

    fun isCameraFollowing(): Boolean = cameraFollow
    fun areArrowsVisible(): Boolean = showDirectionArrows

    private fun Float.format(digits: Int): String = "%.${digits}f".format(this)
}
