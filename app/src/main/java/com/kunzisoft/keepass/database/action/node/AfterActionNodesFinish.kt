package com.kunzisoft.keepass.database.action.node

import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.tasks.ActionRunnable

/**
 * Callback method who return the node(s) modified after an action
 * - Add : @param oldNodes empty, @param newNodes CreatedNodes
 * - Copy : @param oldNodes NodesToCopy, @param newNodes NodesCopied
 * - Delete : @param oldNodes NodesToDelete, @param newNodes empty
 * - Move : @param oldNodes empty, @param newNodes NodesToMove
 * - Update : @param oldNodes NodesToUpdate, @param newNodes NodesUpdated
 */
class ActionNodesValues(val oldNodes: List<Node>, val newNodes: List<Node>)

abstract class AfterActionNodesFinish {
    abstract fun onActionNodesFinish(result: ActionRunnable.Result, actionNodesValues: ActionNodesValues)
}
