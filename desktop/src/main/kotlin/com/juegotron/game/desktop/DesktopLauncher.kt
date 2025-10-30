package com.juegotron.game.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.tron3d.game.Tron3DGame  // ← CAMBIO

fun main() {
    Lwjgl3Application(Tron3DGame(), Lwjgl3ApplicationConfiguration().apply {
        setTitle("Tron 3D Game")
        setWindowedMode(1280, 720)
        useVsync(true)
    })
}
