package com.olcayaras.figures

/**
 * Utility functions for traversing and manipulating the Joint hierarchy.
 */

/**
 * Finds a joint by its ID in the hierarchy starting from [root].
 * Returns null if no joint with the given ID is found.
 */
fun findJointById(root: Joint, id: String): Joint? {
    if (root.id == id) return root
    for (child in root.children) {
        findJointById(child, id)?.let { return it }
    }
    return null
}

/**
 * Finds the parent joint of the joint with [targetId].
 * Returns null if the target is the root or not found.
 */
fun findParentOf(root: Joint, targetId: String): Joint? {
    for (child in root.children) {
        if (child.id == targetId) return root
        findParentOf(child, targetId)?.let { return it }
    }
    return null
}

/**
 * Collects all joint IDs in the hierarchy starting from [joint].
 */
fun collectAllJointIds(joint: Joint): Set<String> {
    return setOf(joint.id) + joint.children.flatMap { collectAllJointIds(it) }
}

/**
 * Generates a unique joint ID for the given [figure].
 * Returns IDs in the format "joint_1", "joint_2", etc.
 */
fun generateUniqueJointId(figure: Figure): String {
    val existingIds = collectAllJointIds(figure.root)
    var counter = 1
    while ("joint_$counter" in existingIds) counter++
    return "joint_$counter"
}

/**
 * Removes the joint with [targetId] from the hierarchy.
 * Returns true if the joint was found and removed, false otherwise.
 * Note: Cannot remove the root joint.
 */
fun removeJoint(root: Joint, targetId: String): Boolean {
    val iterator = root.children.iterator()
    while (iterator.hasNext()) {
        val child = iterator.next()
        if (child.id == targetId) {
            iterator.remove()
            return true
        }
        if (removeJoint(child, targetId)) return true
    }
    return false
}
