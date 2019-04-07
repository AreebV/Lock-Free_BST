
package bst;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 *
 * @author Areeb Vaid & Ty Abbot
 */
public class BST
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
    }

}

class BinarySearchTree
{
    private class node
    {
        private AtomicInteger mKey;
        private AtomicBoolean markFlag;

        private AtomicReference<node> leftChild;
        private AtomicReference<node> rightChild;

        private AtomicBoolean intentFlag;
        private AtomicBoolean deleteFlag;
        private AtomicBoolean promoteFlag;
        private AtomicBoolean nullFlag;

        private AtomicBoolean readyToReplace;
    }

    private class edge
    {
        private node parent;
        private node child;
        private int which;  // left = 0, 1 = right
    }

    private class seekRecord
    {
        private edge lastEdge;
        private edge pLastEdge;
        private edge injectionEdge;
    }

    private class anchorRecord
    {
        private node node;
        private int key;
    }

    private class stateRecord
    {
        private edge targetEdge;
        private edge pTargetEdge;
        private int targetKey;
        private int currentKey;
        private int depth;

        private int mode; // 0 = injection, 1 = discovery, 2 = cleanup
        private int type; // 0 = simple, 1 = complex

        private seekRecord successorRecord;
    }

    // Object to store info about tree traversal for seek

    ThreadLocal<seekRecord> targetRecord = new ThreadLocal<seekRecord>();
    private stateRecord myState = new stateRecord();

    // 3 Sentinal Nodes
    private node R;
    private node S;
    private node T;

    // Creates a new node (used for insert and createStart)
    public node newNode(int key)
    {
        node newNode = new node();
        newNode.mKey.set(key);

        node leftNode = new node();
        node rightNode = new node();
        leftNode.nullFlag.set(true);
        rightNode.nullFlag.set(true);
        leftNode = null;
        rightNode = null;

        newNode.leftChild.set(leftNode);
        newNode.rightChild.set(rightNode);

        return (newNode);
    }

    // Function allocates the start nodes, done in main before any operations take place
    public void createStart()
    {
        R = newNode(0);
        R.rightChild.set(newNode(1));
        S = R.rightChild.get();
        S.rightChild.set(newNode(2));
        T = S.rightChild.get();
    }

    // Function that fills in the edge between nodes
    public void makeEdge(edge edge, node parent, node child, int which)
    {
        edge.parent = parent;
        edge.child = child;
        edge.which = which;
    }

    // Function that sets one edge equal to another
    public void setEdge(edge first, edge second)
    {
        first.parent = second.parent;
        first.child = second.child;
        first.which = second.which;
    }

    // Function that sets one seekRecord equal to another
    public void setSeekRecord(seekRecord first, seekRecord second)
    {
        setEdge(first.injectionEdge, second.injectionEdge);
        setEdge(first.lastEdge, second.lastEdge);
        setEdge(first.pLastEdge, second.pLastEdge);

    }

    // Search through tree and find if there exists a node with the given key
    public void seek(int key, seekRecord seeker)
    {
        anchorRecord pAnchorRecord = null;
        anchorRecord anchorRecord = null;
        seekRecord pSeekRecord = null;
        edge pLastEdge = null;
        edge lastEdge = null;
        int cKey;
        int aKey;
        int which;
        node temp;
        node next;
        node curr;
        boolean nullCheck = false;
        boolean deleteCheck = false;
        boolean promoteCheck = false;

        pAnchorRecord.key = S.mKey.get();
        pAnchorRecord.node = S;

        while (true)
        {
            // Initialize all variables for traversal
            makeEdge(pLastEdge, R, S, 1);
            makeEdge(lastEdge, S, T, 1);
            curr = T;
            anchorRecord.node = S;
            anchorRecord.key = S.mKey.get();

            while (true)
            {
                // Read key stored in current
                cKey = curr.mKey.get();

                // Find the next edge to follow ( 0 is left, 1 is right)
                which = key<cKey ? 0 : 1;

                temp = (which == 0) ? curr.leftChild.get() : curr.rightChild.get();

                if (temp.nullFlag.get() == true)
                    nullCheck = true;
                if (temp.deleteFlag.get() == true)
                    deleteCheck = true;
                if (temp.promoteFlag.get() == true)
                    promoteCheck = true;
                next = temp;

                // If key has been found or the next node is null, stop traversal
                if (key == cKey || nullCheck)
                {
                    seeker.pLastEdge = pLastEdge;
                    seeker.lastEdge = lastEdge;
                    makeEdge(seeker.injectionEdge, curr, next, which);
                    targetRecord.set(seeker);

                    // If key has been found return, else break to check if node has moved up the tree
                    if (key == cKey)
                        return;
                    else
                        break;
                }

                // If the next edge to be traversed is right, then set up anchor node
                if (which == 1)
                {
                    anchorRecord.node = curr;
                    anchorRecord.key = cKey;
                }

                // Move to the next set of nodes and edge
                pLastEdge = lastEdge;
                makeEdge(lastEdge, curr, next, which);
                curr = next;
            }

            // If the key has not been found use the anchor record to make sure that the key has not moved up in tree
            aKey = anchorRecord.node.mKey.get();
            if (anchorRecord.key == aKey)
            {
                temp = anchorRecord.node.rightChild.get();
                if (temp.deleteFlag.get() == true)
                    deleteCheck = true;
                if (temp.promoteFlag.get() == true)
                    promoteCheck = true;

                // If the deleteFlag and promoteFlag are not set, the anchor is part of tree
                if (!deleteCheck && !promoteCheck)
                    return;

                // If anchor record equals previous anchor record, return previous access path
                if (pAnchorRecord.node == anchorRecord.node && pAnchorRecord.key == anchorRecord.key)
                {
                    setSeekRecord(seeker, pSeekRecord);
                    targetRecord.set(seeker);
                    return;
                }
            }

            // Store current seek traversal, and try seek again
            setSeekRecord(pSeekRecord, seeker);
            pAnchorRecord.node = anchorRecord.node;
            pAnchorRecord.key = anchorRecord.key;
        }
    }

    // Set the flags for the child edge
    // Mode: Injection = 0, Discovery = 1, Cleanup = 2
    public boolean markChildEdge(stateRecord state, int which)
    {
        node node;
        node temp;
        node address;
        node oldValue;
        node newValue;
        node tempValue;
        edge edge;
        edge helpeeEdge = null;
        boolean nullCheck = false;
        boolean intentCheck = false;
        boolean deleteCheck = false;
        boolean promoteCheck = false;
        boolean result;
        int flag; // 0 = DELETE, 1 = PROMOTE

        if (state.mode == 0)
        {
            edge = state.targetEdge;
            flag = 0; // DELETE
        }
        else
        {
            edge = state.successorRecord.lastEdge;
            flag = 1; // PROMOTE
        }
        node = edge.child;

        while (true)
        {
            // Set temp and check flags
            if (which == 0)
                temp = node.leftChild.get();
            else
                temp = node.rightChild.get();

            if (temp.nullFlag.get()) nullCheck = true;
            if (temp.intentFlag.get()) intentCheck = true;
            if (temp.deleteFlag.get()) deleteCheck = true;
            if (temp.promoteFlag.get()) promoteCheck = true;
            address = temp;

            if (intentCheck == true)
            {
                makeEdge(helpeeEdge, node, address, which); // CHECK: NOT ALLOCATED?
                helpTargetNode(helpeeEdge);
                continue;
            }

            else if (deleteCheck == true)
            {
                // If flag is promote
                if (flag == 1)
                {
                    helpTargetNode(edge);
                    return false;
                }
                else
                    return true;
            }

            else if (promoteCheck == true)
            {
                // Delete flag
                if (flag == 0)
                {
                    helpSuccessorNode(edge);
                    return false;
                }
                else
                    return true;
            }

            // Set null for oldValue if nullCheck is true
            if (nullCheck)
                address.nullFlag.set(true);
            oldValue = address;

            if (flag == 0)
            {
                tempValue = oldValue;
                tempValue.deleteFlag.set(true);
                newValue = tempValue;
            }
            else
            {
                tempValue = oldValue;
                tempValue.promoteFlag.set(true);
                newValue = tempValue;
            }

            if (which == 0)
                result = node.leftChild.compareAndSet(oldValue, newValue);
            else
                result = node.rightChild.compareAndSet(oldValue, newValue);

            // If CAS failed retry, otherwise break out of loop and return true
            if (!result)
                continue;
            else
                break;
        }
        return true;
    }

    // Set the type of delete operation and update the mode of the operation
    public void initializeTypeAndUpdateMode(stateRecord state)
    {
        node node;

        node = state.targetEdge.child;

        // Checck if either children are null
        if (node.leftChild.get().nullFlag.get() == true ||
            node.rightChild.get().nullFlag.get() == true)
        {
            // Simple delete = 0, complex delete = 1
            if(node.markFlag.get() == true)
                state.type = 1;

            else
                state.type = 0;
        }

        // Both children are non null
        else
        {
            state.type = 1;
        }
        updateMode(state);
    }

    public void updateMode (stateRecord state)
    {
        node node;

        // If it is a simple delete then move straight to cleanup (no discovery)
        if (state.type == 0)
            state.mode = 2; // Cleanup = 2

        else
        {
            node = state.targetEdge.child;

            // If dicovery has been done, move to cleanup, otherwise move to discovery
            if (node.readyToReplace.get() == true)
                state.mode = 2; // Cleanup = 2
            else
                state.mode = 1; // Discovery = 1
        }
        return;
    }

    // Find the node with the smallest key in the right subtree of target
    public boolean findSmallest(stateRecord state, seekRecord seeker)
    {
        node node;
        node rightNode;
        node leftNode;
        node current;
        edge lastEdge = null;
        edge pLastEdge = null;
        edge injectionEdge = null;

        node = state.targetEdge.child;
        rightNode = node.rightChild.get();

        // Right tree is empty
        if (rightNode.nullFlag.get() == true)
            return false;

        // Set up the traversal variables
        makeEdge(lastEdge, node, rightNode, 1);
        makeEdge(pLastEdge, node, rightNode, 1);

        while (true)
        {
            current = lastEdge.child;
            leftNode = current.leftChild.get();

            if (leftNode.nullFlag.get() == true)
            {
                makeEdge(injectionEdge, current, leftNode, 0);
                break;
            }

            // Move to the next edge
            pLastEdge = lastEdge;
            makeEdge(lastEdge, current, leftNode, 0);
        }

        // Set up the seekRecord for the calling function
        seeker.lastEdge = lastEdge;
        seeker.pLastEdge = pLastEdge;
        seeker.injectionEdge = injectionEdge;
        return true;
    }

    // Find the successor node and for the deletion operation
    public void findAndMarkSuccessor(stateRecord state)
    {
        node node;
        node leftNode;
        node temp;
        node tempLeft;
        node tempNode;
        edge successorEdge;
        seekRecord seeker;
        boolean markFlag;
        boolean result;

        node = state.targetEdge.child;
        seeker = state.successorRecord;

        while (true)
        {
            markFlag = node.markFlag.get();

            // Get smallest node in right subtree
            result = findSmallest(state, seeker);

            // Either successor node preselected or right subtree empty
            if (markFlag == true || result == false)
                break;

            // Grab seek record information
            successorEdge = seeker.lastEdge;
            leftNode = seeker.injectionEdge.child;

            // Check if node being deleted is marked
            markFlag = node.markFlag.get();

            // If successor has been selected retry while
            if (markFlag == true)
                continue;

            // Attempt to set the promote flag for left edge
            tempLeft = leftNode;
            tempNode = node;
            tempLeft.nullFlag.set(true);
            tempNode.nullFlag.set(true);
            tempNode.promoteFlag.set(true);
            result = successorEdge.child.leftChild.compareAndSet(tempLeft, tempNode);

            if (result == true)
                break;

            // If the attempt to mark edge failed, recover and retry
            temp = successorEdge.child.leftChild.get();

            // Help with deletion
            if ((temp.nullFlag.get() == true) && (temp.deleteFlag.get() == true))
                helpTargetNode(successorEdge);
        }
        updateMode(state);
        return;
    }

    // Removes the successor node used in delete operations
    public void removeSuccessor(stateRecord state)
    {
        node node;
        node address;
        node temp;
        node rightNode;
        node oldValue;
        node newValue;
        edge successorEdge;
        edge pLastEdge;
        edge lastEdge;
        seekRecord seeker;
        int which;
        boolean promoteCheck;
        boolean intentCheck;
        boolean nullCheck;
        boolean deleteCheck;
        boolean dFlag;
        boolean result;

        node = state.targetEdge.child;
        seeker = state.successorRecord;
        successorEdge = seeker.lastEdge;

        // Check for valid information
        address = successorEdge.child.leftChild.get();
        promoteCheck = address.promoteFlag.get();

        if (promoteCheck == false || address != node)
        {
            node.readyToReplace.set(true);
            updateMode(state);
            return;
        }

        // If the right edge is still not set for promotion, do so
        temp = successorEdge.child.rightChild.get();
        if (temp.promoteFlag.get() == false)
            markChildEdge(state, 1);

        // Promote the key
        node.mKey = successorEdge.child.mKey;
        node.markFlag.set(true);

        while (true)
        {
            // Check if successor is right child of target
            if (successorEdge.parent == node)
            {
                dFlag = true;
                which = 1; // Right
            }
            else
            {
                dFlag = false;
                which = 0; // Left
            }

            if (which == 0)
                intentCheck = successorEdge.parent.leftChild.get().intentFlag.get();
            else
                intentCheck = successorEdge.parent.rightChild.get().intentFlag.get();

            rightNode = successorEdge.child.rightChild.get();
            nullCheck = rightNode.nullFlag.get();

            oldValue = successorEdge.child;
            oldValue.nullFlag.set(false);
            oldValue.intentFlag.set(intentCheck);
            oldValue.deleteFlag.set(dFlag);
            oldValue.promoteFlag.set(false);

            if (nullCheck == true)
            {
                newValue = successorEdge.child;
                newValue.nullFlag.set(true);
            }
            else
            {
                newValue = rightNode;
                newValue.nullFlag.set(false);
            }
            newValue.intentFlag.set(false);
            newValue.deleteFlag.set(dFlag);
            newValue.promoteFlag.set(false);

            if (which == 0)
                result = successorEdge.parent.leftChild.compareAndSet(oldValue, newValue);
            else
                result = successorEdge.parent.rightChild.compareAndSet(oldValue, newValue);

            if (result == true || dFlag == true)
                break;

            if (which == 0)
                temp = successorEdge.parent.leftChild.get();
            else
                temp = successorEdge.parent.rightChild.get();
            deleteCheck = temp.deleteFlag.get();

            pLastEdge = seeker.pLastEdge;
            if (deleteCheck == true && pLastEdge != null)
                helpTargetNode(pLastEdge);

            result = findSmallest(state, seeker);
            lastEdge = seeker.lastEdge;

            // Successor node has been removed
            if ((result == false) || (lastEdge.child != successorEdge.child))
                break;
            else
                successorEdge = seeker.lastEdge;
        }

        node.readyToReplace.set(true);
        updateMode(state);
        return;
    }

    // Last stage of delete operation, used by both simple and complex delete
    public boolean cleanup(stateRecord state)
    {
        node node;
        node parent;
        node leftNode;
        node rightNode;
        node oldValue;
        node newValue;
        node address;
        node newNode = new node();
        int pWhich;
        int nWhich;
        boolean nullCheck;
        boolean result;

        // Grab addresses to set up function
        parent = state.targetEdge.parent;
        node = state.targetEdge.child;
        pWhich = state.targetEdge.which;

        // If the type is a complex delete
        if (state.type == 1)
        {
            // Replace node with new unmarked copy
            newNode.mKey = node.mKey;
            newNode.markFlag.set(false);

            leftNode = node.leftChild.get();
            rightNode = node.rightChild.get();
            newNode.leftChild.set(leftNode);

            nullCheck = rightNode.nullFlag.get();
            if (nullCheck == true)
            {
                newNode.rightChild.set(null);
                newNode.rightChild.get().nullFlag.set(true);
            }
            else
                newNode.rightChild.set(node.rightChild.get());

            // Initialize arguments of CAS
            oldValue = node;
            oldValue.intentFlag.set(true);
            newValue = newNode;
        }

        // Type is simple delete, remove the node
        else
        {
            // Find which edge will be switched
            if (node.leftChild.get().nullFlag.get() == true)
                nWhich = 1;
            else
                nWhich = 0;

            // Set up for CAS
            oldValue = node;
            oldValue.intentFlag.set(true);

            if (nWhich == 0)
                address = node.leftChild.get();
            else
                address = node.rightChild.get();

            if (address.nullFlag.get() == true)
            {
                newValue = node;
                newValue.nullFlag.set(true);
            }
            else
                newValue = address;
        }

        // Attempt to the set the correct value
        if (pWhich == 0)
            result = parent.leftChild.compareAndSet(oldValue, newValue);
        else
            result = parent.rightChild.compareAndSet(oldValue, newValue);

        return result;
    }

    // Help deletion finish
    public void helpTargetNode(edge helpeeEdge)
    {
        stateRecord state = new stateRecord();
        boolean result;

        // Intent flag set on edge, get state record and initialize
        state.targetEdge = helpeeEdge;
        state.mode = 0; // Injection = 0

        // If either the left or right edges are not marked, then mark them
        result = markChildEdge(state, 0);
        if (result == false)
            return;

        markChildEdge(state, 1);
        initializeTypeAndUpdateMode(state);

        // Finish delete
        if (state.mode == 1)
            findAndMarkSuccessor(state);

        if (state.mode == 1)
            removeSuccessor(state);

        if (state.mode == 2)
            cleanup(state);

        return;
    }

    // Help promotion finish
    public void helpSuccessorNode(edge helpeeEdge)
    {
        node node;
        node parent;
        node leftNode;
        stateRecord state = new stateRecord();
        seekRecord seeker;

        parent = helpeeEdge.parent;
        node = helpeeEdge.child;

        // Grab target node
        leftNode = node.leftChild.get();

        // Get new stateRecord and seekRecord and set them up
        makeEdge(state.targetEdge, null, leftNode, 0);
        state.mode = 1; // Discovery

        seeker = state.successorRecord;
        seeker.lastEdge = helpeeEdge;
        makeEdge(seeker.pLastEdge, null, parent, 0);

        // Promote key and remove successor
        removeSuccessor(state);
    }

    // Function returns true if key is in tree and false if not
    public boolean search(int key)
    {
        node node;
        int nKey;

        seek(key, targetRecord.get());
        node = targetRecord.get().lastEdge.child;
        nKey = node.mKey.get();

        targetRecord.remove();

        if (nKey == key)
            return (true);
        else
            return (false);
    }


    // Function to insert value into the bst
    public boolean insert(int key)
    {
        node node;
        node newNode;
        node address;
        node temp;
        node tempAddress;
        edge targetEdge;
        int nKey;
        int which;
        boolean result;

        while (true)
        {
            seek(key, targetRecord.get());

            // The target edge and target node found from seek
            targetEdge = targetRecord.get().lastEdge;
            node = targetEdge.child;
            nKey = node.mKey.get();

            if (nKey == key)
            {
                targetRecord.remove();
                return (false);
            }

            // Make a newNode and initialize
            newNode = newNode(key);
            newNode.readyToReplace.set(false);

            which = targetRecord.get().injectionEdge.which;
            address = targetRecord.get().injectionEdge.child;
            tempAddress = address;
            tempAddress.nullFlag.set(true);

            // Attempt to modify the BST by adding in the new node
            if (which == 0)
                result = node.leftChild.compareAndSet(tempAddress, newNode);
            else
                result = node.rightChild.compareAndSet(tempAddress, newNode);

            // Successful insertion
            if (result)
            {
                targetRecord.remove();
                return (true);
            }

            // Unsuccessful insertion, help is needed
            if (which == 0)
                temp = node.leftChild.get();
            else
                temp = node.rightChild.get();

            if (temp.deleteFlag.get())
                helpTargetNode(targetEdge);

            else if (temp.promoteFlag.get())
                helpSuccessorNode(targetEdge);

        }
    }
}
