package com.example.virtuallab

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.Anchor
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.assets.RenderableSource
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.rendering.Texture
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlin.math.cos
import kotlin.math.sin

class CameraActivity : AppCompatActivity() {
    private lateinit var arFragment: ArFragment
    private val modelNodes = mutableListOf<TransformableNode>()
    private var selectedNode: TransformableNode? = null
    private var pendingModelPath: String? = null
    private val movementSpeed = 0.05f
    private val leftJoystickSpeed = 0.02f
    private val rotationSpeed = 2.0f
    private var isAutoRotating = false
    private val autoRotationSpeed = 1.0f
    private val originalMaterials = mutableMapOf<TransformableNode, Material>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_layout)

        val modelsButton: Button = findViewById(R.id.modelsButton)
        val autoRotateButton: Button = findViewById(R.id.autoRotateButton)
        arFragment = supportFragmentManager.findFragmentById(R.id.arFragment) as ArFragment

        setupJoysticks()
        setupPlaneDetection()
        setupSceneTapListener()

        autoRotateButton.setOnClickListener {
            isAutoRotating = !isAutoRotating
            autoRotateButton.text = if (isAutoRotating) "Auto Rotate: ON" else "Auto Rotate: OFF"
        }
        modelsButton.setOnClickListener {
            showListOfModels()
        }

        if (savedInstanceState != null) {
            pendingModelPath = savedInstanceState.getString("pendingModelPath")
            isAutoRotating = savedInstanceState.getBoolean("isAutoRotating", false)
            autoRotateButton.text = if (isAutoRotating) "Auto Rotate: ON" else "Auto Rotate: OFF"
        }
    }

    private fun setupPlaneDetection() {
        arFragment.arSceneView.scene.addOnUpdateListener {
            if (pendingModelPath != null) {
                val session = arFragment.arSceneView.session ?: return@addOnUpdateListener

                val plane = session.getAllTrackables(Plane::class.java)
                    ?.firstOrNull { it.trackingState == TrackingState.TRACKING }

                if (plane != null) {
                    val modelPath = pendingModelPath
                    pendingModelPath = null
                    if (modelPath != null) {
                        loadAndPlaceModel(modelPath, plane)
                    }
                }
            }
            if (isAutoRotating) {
                selectedNode?.let { node ->
                    val currentRotation = node.localRotation
                    val rotationChange = Quaternion.axisAngle(Vector3(0f, 1f, 0f), autoRotationSpeed)
                    node.localRotation = Quaternion.multiply(currentRotation, rotationChange)
                }
            }
        }
    }

    private fun showListOfModels() {
        // Use .gltf files in subdirectories
        val models = listOf(
            Pair("Chicken", "Chicken/chicken.gltf"),
            Pair("Deer", "Deer/deer.gltf"),
            Pair("Dog", "Dog/dog.gltf"),
            Pair("Horse", "Horse/horse.gltf"),
            Pair("Cat", "Cat/cat.gltf"),
            Pair("Penguin", "Penguin/penguin.gltf"),
            Pair("Tiger", "Tiger/tiger.gltf")
        )

        val modelNames = models.map { it.first }.toTypedArray()

        val dialog = AlertDialog.Builder(this)
            .setTitle("Choose a Model")
            .setItems(modelNames) { _, which ->
                val selectedModelPath = models[which].second
                placeModel(selectedModelPath)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }

    private fun placeModel(modelPath: String) {
        pendingModelPath = modelPath
        val session = arFragment.arSceneView.session
        if (session == null) {
            Toast.makeText(this, "AR session not ready, please try again.", Toast.LENGTH_LONG).show()
            return
        }

        val plane = session.getAllTrackables(Plane::class.java)
            ?.firstOrNull { it.trackingState == TrackingState.TRACKING }
        if (plane != null) {
            pendingModelPath = null
            loadAndPlaceModel(modelPath, plane)
        } else {
            Toast.makeText(this, "Move your device to detect a plane.", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadAndPlaceModel(modelPath: String, plane: Plane) {
        val modelUri = Uri.parse("file:///android_asset/$modelPath")
        try {
            assets.open(modelPath).use { inputStream ->
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Model file not found: $modelPath", Toast.LENGTH_LONG).show()
            return
        }

        // Use RenderableSource to load the .gltf file
        ModelRenderable.builder()
            .setSource(
                this,
                RenderableSource.builder()
                    .setSource(this, modelUri, RenderableSource.SourceType.GLTF2)
                    .setRecenterMode(RenderableSource.RecenterMode.ROOT)
                    .build()
            )
            .setRegistryId(modelPath)
            .build()
            .thenAccept { modelRenderable ->
                applyTextureToModel(modelRenderable, "deer_diffuse.png")
                addNodeToScene(arFragment, plane.createAnchor(plane.centerPose), modelRenderable)
            }
            .exceptionally { throwable ->
                Toast.makeText(this, "Error loading model: $modelPath - ${throwable.message}", Toast.LENGTH_LONG).show()
                null
            }
    }

    private fun applyTextureToModel(modelRenderable: ModelRenderable, texturePath: String) {
        Texture.builder()
            .setSource(this, Uri.parse("file:///android_asset/$texturePath"))
            .build()
            .thenAccept { texture ->
                modelRenderable.material?.let { material: Material ->
                    material.setTexture("baseColor", texture)
                } ?: run {
                }
            }
            .exceptionally { throwable ->
                Toast.makeText(this, "Error loading texture: $texturePath", Toast.LENGTH_LONG).show()
                null
            }
    }

    private fun addNodeToScene(arFragment: ArFragment, anchor: Anchor, renderable: ModelRenderable) {
        val anchorNode = AnchorNode(anchor)
        val node = TransformableNode(arFragment.transformationSystem).apply {
            renderable.isShadowCaster = true
            renderable.isShadowReceiver = true
            setRenderable(renderable)
            setParent(anchorNode)
            localScale = Vector3(1f, 1f, 1f)
        }

        arFragment.arSceneView.scene.addChild(anchorNode)

        node.setOnTapListener { _, _ ->
            highlightModel(node)
        }

        modelNodes.add(node)
        highlightModel(node)
    }

    private fun highlightModel(node: TransformableNode) {
        resetHighlight()
        selectedNode = node

        if (!originalMaterials.containsKey(node)) {
            node.renderable?.material?.let { material ->
                originalMaterials[node] = material.makeCopy()
            }
        }

        MaterialFactory.makeOpaqueWithColor(this, Color(1.0f, 1.0f, 0.0f))
            .thenAccept { yellowMaterial ->
                node.renderable?.material = yellowMaterial
            }
            .exceptionally {
                null
            }
    }

    private fun resetHighlight() {
        selectedNode?.let { node ->
            originalMaterials[node]?.let { originalMaterial ->
                node.renderable?.material = originalMaterial
            }
        }
        selectedNode = null
    }
    private fun setupSceneTapListener() {
        // Add a touch listener to the scene to detect taps anywhere
        arFragment.arSceneView.scene.addOnPeekTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                val hitResults = arFragment.arSceneView.scene.hitTest(motionEvent)
                val hitNode = hitResults?.node
                if (hitNode == null || !modelNodes.any { it == hitNode || it.children.contains(hitNode) }) {
                    resetHighlight()
                }
            }
        }
    }
    private fun setupJoysticks() {
        val leftJoystick = findViewById<CircularJoystickView>(R.id.joystickLeft)
        val rightJoystick = findViewById<VerticalJoystickView>(R.id.joystickRight)
        val rotateJoystick = findViewById<HorizontalJoystickView>(R.id.joystickRotate)

        leftJoystick.setOnMoveListener { angle, strength ->
            selectedNode?.let { node ->
                val camera = arFragment.arSceneView.scene.camera
                val right = camera.right // Camera's right vector (X-axis in camera space)
                val up = camera.up       // Camera's up vector (Y-axis in camera space)

                val radians = Math.toRadians(angle.toDouble())
                val moveX = strength * leftJoystickSpeed * cos(radians).toFloat() // Horizontal component (left/right)
                val moveY = -strength * leftJoystickSpeed * sin(radians).toFloat() // Vertical component (up/down, inverted)

                val scaledRight = Vector3(right.x * moveX, right.y * moveX, right.z * moveX)
                val scaledUp = Vector3(up.x * moveY, up.y * moveY, up.z * moveY)

                val moveVector = Vector3.add(scaledRight, scaledUp)

                val currentPosition = node.worldPosition

                val newPosition = Vector3.add(currentPosition, moveVector)
                node.worldPosition = newPosition
            }
        }

        rightJoystick.setOnMoveListener { strength ->
            selectedNode?.let { node ->
                val camera = arFragment.arSceneView.scene.camera
                val forward = camera.forward

                // Remove any Y-component to keep movement horizontal (in the X-Z plane)
                val forwardHorizontal = Vector3(forward.x, 0f, forward.z).normalized()

                val moveVector = Vector3(
                    forwardHorizontal.x * strength * movementSpeed,
                    forwardHorizontal.y * strength * movementSpeed,
                    forwardHorizontal.z * strength * movementSpeed
                )

                val currentPosition = node.worldPosition

                val newPosition = Vector3.add(currentPosition, moveVector)
                node.worldPosition = newPosition

            }
        }

        rotateJoystick.setOnMoveListener { strength ->
            selectedNode?.let { node ->
                // Disable auto-rotation if the user manually rotates the model
                if (isAutoRotating) {
                    isAutoRotating = false
                    findViewById<Button>(R.id.autoRotateButton).text = "Auto Rotate: OFF"
                }

                val rotationChange = strength * rotationSpeed
                val rotationDelta = Quaternion.axisAngle(Vector3(0f, 1f, 0f), rotationChange)

                val currentRotation = node.localRotation
                node.localRotation = Quaternion.multiply(currentRotation, rotationDelta)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("pendingModelPath", pendingModelPath)
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        modelNodes.forEach { it.parent?.let { parent -> arFragment.arSceneView.scene.removeChild(parent as AnchorNode) } }
        modelNodes.clear()
        originalMaterials.clear()
    }
}