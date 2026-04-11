package com.example.magictime

data class Pos(
    var x: Float = 0f,
    var y: Float = 0f,
    var scale: Float = 1f
)

data class LayoutConfig(
    var clock: Pos = Pos(),
    var operator: Pos = Pos(),
    var marquee: Pos = Pos(),
    var statusBar: Pos = Pos(),
    var date: Pos = Pos(),
    var lockIcon: Pos? = null,
    var phoneButton: Pos? = null,
    var cameraButton: Pos? = null
)