package com.example.a3dmodelswithfilament

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Choreographer
import android.view.SurfaceView
import com.google.android.filament.Skybox
import com.google.android.filament.utils.*
import java.nio.ByteBuffer


class MainActivity : AppCompatActivity() {

    companion object {
        init { Utils.init() }
    }

    private lateinit var surfaceView: SurfaceView
    private lateinit var choreographer: Choreographer
    private lateinit var modelViewer: ModelViewer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        surfaceView = SurfaceView(this).apply { setContentView(this) }
        choreographer = Choreographer.getInstance()
        modelViewer = ModelViewer(surfaceView)
        surfaceView.setOnTouchListener(modelViewer)

        loadGlb("toyota_supra_a80_1993_2K")
        //loadGltf("BusterDrone")

        modelViewer.scene.skybox = Skybox.Builder().build(modelViewer.engine)
        modelViewer.view.blendMode = com.google.android.filament.View.BlendMode.OPAQUE
        modelViewer.scene.skybox = Skybox.Builder().color(1.0f, 1.0f, 1.0f, 1.0f).build(modelViewer.engine)
        loadEnvironment("venetian_crossroads_2k")

    }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(currentTime: Long) {
            choreographer.postFrameCallback(this)
            modelViewer.render(currentTime)
        }
    }

/*    private val frameCallback = object : Choreographer.FrameCallback {
        private val startTime = System.nanoTime()
        override fun doFrame(currentTime: Long) {
            val seconds = (currentTime - startTime).toDouble() / 1_000_000_000
            choreographer.postFrameCallback(this)
            modelViewer.animator?.apply {
                if (animationCount > 0) {
                    applyAnimation(0, seconds.toFloat())
                }
                updateBoneMatrices()
            }
            modelViewer.render(currentTime)
            // Reset the root transform, then rotate it around the Z axis.
            //drone move
            modelViewer.asset?.apply {
                modelViewer.transformToUnitCube()
                val rootTransform = this.root.getTransform()
                val degrees = 20f * seconds.toFloat()
                val zAxis = Float3(0f, 0f, 1f)
                this.root.setTransform(rootTransform * rotation(zAxis, degrees))
            }
        }
    }*/

    private fun Int.getTransform(): Mat4 {
        val tm = modelViewer.engine.transformManager
        return Mat4.of(*tm.getTransform(tm.getInstance(this), null))
    }

    private fun Int.setTransform(mat: Mat4) {
        val tm = modelViewer.engine.transformManager
        tm.setTransform(tm.getInstance(this), mat.toFloatArray())
    }

    override fun onResume() {
        super.onResume()
        choreographer.postFrameCallback(frameCallback)
    }

    override fun onPause() {
        super.onPause()
        choreographer.removeFrameCallback(frameCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        choreographer.removeFrameCallback(frameCallback)
    }

    private fun loadGlb(name: String) {
        val buffer = readAsset("models/${name}.glb")
        modelViewer.loadModelGlb(buffer)
        modelViewer.transformToUnitCube()
    }

    private fun readAsset(assetName: String): ByteBuffer {
        val input = assets.open(assetName)
        val bytes = ByteArray(input.available())
        input.read(bytes)
        return ByteBuffer.wrap(bytes)
    }

    private fun loadEnvironment(ibl: String) {
        // Create the indirect light source and add it to the scene.
        var buffer = readAsset("envs/$ibl/${ibl}_ibl.ktx")
        KtxLoader.createIndirectLight(modelViewer.engine, buffer).apply {
            intensity = 50_000f
            modelViewer.scene.indirectLight = this
        }

        // Create the sky box and add it to the scene.
        buffer = readAsset("envs/$ibl/${ibl}_skybox.ktx")
        KtxLoader.createSkybox(modelViewer.engine, buffer).apply {
            modelViewer.scene.skybox = this
        }
    }

    private fun loadGltf(name: String) {
        val buffer = readAsset("models/${name}.gltf")
        modelViewer.loadModelGltf(buffer) { uri -> readAsset("models/$uri") }
        modelViewer.transformToUnitCube()
    }

}