package com.armeasure.app

import android.content.Context
import com.google.ar.core.Anchor
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import io.github.sceneview.node.Node

/**
 * AnchorNode — AR world-এ একটা point represent করে।
 * User যখন screen-এ tap করে, এই node তৈরি হয়।
 *
 * SceneView 2.x-e AR node-er base class holo plain [Node] —
 * engine scene view theke neowa hoy, anchor-er pose theke position set kora hoy.
 */
class AnchorNode(
    val anchor: Anchor?,
    sceneView: ARSceneView
) : Node(sceneView.engine) {

    init {
        // Anchor-এর pose থেকে node-এর position set করো
        anchor?.let {
            this.position = Position(it.pose.tx(), it.pose.ty(), it.pose.tz())
        }
    }

    /**
     * Visual marker তৈরি করে (একটা ছোট colored dot)
     * যাতে user দেখতে পায় point কোথায় বসেছে।
     */
    fun createVisualMarker(context: Context) {
        // Marker-কে small scale-এ set করো
        this.scale = Scale(0.02f, 0.02f, 0.02f)
    }

    /**
     * Anchor ও node destroy করে cleanup।
     */
    override fun destroy() {
        anchor?.detach()
        super.destroy()
    }
}

/**
 * LineNode — দুইটা AnchorNode-এর moddhe ekta line draw করে।
 * Measurement line দেখানোর জন্য ব্যবহার হয়।
 */
class LineNode(
    sceneView: ARSceneView,
    private val startNode: AnchorNode,
    private val endNode: AnchorNode
) : Node(sceneView.engine) {

    init {
        // Line position calculate করো (midpoint)
        val startPose = startNode.anchor?.pose
        val endPose = endNode.anchor?.pose

        if (startPose != null && endPose != null) {
            val midX = (startPose.tx() + endPose.tx()) / 2f
            val midY = (startPose.ty() + endPose.ty()) / 2f
            val midZ = (startPose.tz() + endPose.tz()) / 2f

            this.position = Position(midX, midY, midZ)
        }
    }
}
