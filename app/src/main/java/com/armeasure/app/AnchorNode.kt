package com.armeasure.app

import android.content.Context
import android.graphics.Color
import com.google.ar.core.Anchor
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.node.Node
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale

/**
 * AnchorNode — AR world-এ একটা point represent করে।
 * User যখন screen-এ tap করে, এই node তৈরি হয়।
 */
class AnchorNode(
    val anchor: Anchor?,
    private val sceneView: ArSceneView
) : ArNode() {

    init {
        // Anchor-এর pose থেকে position set করো
        anchor?.let {
            this.pose = it.pose
        }
    }

    /**
     * Visual marker তৈরি করে (একটা ছোট colored dot)
     * যাতে user দেখতে পায় point কোথায় বসেছে।
     */
    fun createVisualMarker(context: Context) {
        // SceneView-তে simple sphere render
        // এখানে একটা small node বসাই যেটা measurement point দেখায়
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
 * LineNode — দুইটা AnchorNode-এর মধ্যে একটা visible line draw করে।
 * Measurement line দেখানোর জন্য ব্যবহার হয়।
 */
class LineNode(
    private val sceneView: ArSceneView,
    private val startNode: AnchorNode,
    private val endNode: AnchorNode
) : Node() {

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
