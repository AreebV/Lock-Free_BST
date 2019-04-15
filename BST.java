
package bst;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 *
 * @author Areeb Vaid & Ty Abbott
 */
public class BST
{

    public static void main(String[] args)
    {
        BinarySearchTree bst = new BinarySearchTree();
        boolean check;

        bst.createStart();
        check = bst.insert(10);
        System.out.println("RETURN: " + check);

        check = bst.insert(10);
        System.out.println("RETURN: " + check);

        check = bst.search(10);
        System.out.println("RETURN: " + check);

        check = bst.search(25);
        System.out.println("RETURN: " + check);
    }

}

class BinarySearchTree
{
    private class node
    {
        private AtomicInteger mKey = new AtomicInteger();
        private AtomicBoolean markFlag = new AtomicBoolean();

        private AtomicReference<node> leftChild = new AtomicReference<node>();
        private AtomicReference<node> rightChild = new AtomicReference<node>();

        private AtomicBoolean intentFlag = new AtomicBoolean();
        private AtomicBoolean deleteFlag = new AtomicBoolean();
        private AtomicBoolean promoteFlag = new AtomicBoolean();
        private AtomicBoolean nullFlag = new AtomicBoolean();

        private AtomicBoolean readyToReplace = new AtomicBoolean();
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
    private node R = new node();
    private node S = new node();
    private node T = new node();

    // Creates a new node (used for insert and createStart)
    public void newNode(int key, node newNode)
    {
        newNode.mKey.set(key);

        node leftNode = new node();
        node rightNode = new node();
        leftNode.nullFlag.set(true);
        rightNode.nullFlag.set(true);

        newNode.leftChild.set(leftNode);
        newNode.rightChild.set(rightNode);
    }

    // Function allocates the start nodes, done in main before any operations take place
    public void createStart()
    {
        newNode(0, R);
        newNode(1, S);
        newNode(2, T);
        R.rightChild.set(S);
        S.rightChild.set(T);
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

    public void copyNode(node node1, node node2)
    {
        node1.deleteFlag.set(node2.deleteFlag.get());
        node1.intentFlag.set(node2.intentFlag.get());
        node1.promoteFlag.set(node2.promoteFlag.get());
        node1.deleteFlag.set(node2.deleteFlag.get());
        node1.leftChild.set(node2.leftChild.get());
        node1.rightChild.set(node2.rightChild.get());
        node1.mKey.set(node2.mKey.get());
        node1.markFlag.set(node2.markFlag.get());
        node1.readyToReplace.set(node2.readyToReplace.get());
    }

    // Function that sets one seekRecord equal to another
    public void setSeekRecord(seekRecord first, seekRecord second)
    {
        setEdge(first.injectionEdge, second.injectionEdge);
        setEdge(first.lastEdge, second.lastEdge);
        setEdge(first.pLastEdge, second.pLastEdge);

    }

    // RDCSS that compares two values and sets 1
    public boolean RDCSS(AtomicBoolean a1, boolean o1, AtomicReference<node> a2, node o2, node n2)
    {
        boolean check;

        if (a1.get() == o1)
        {
            check = RDCSSComplete(a1, o1, a2, o2, n2);
        }
        else
            return false;

        return check;
    }

    public boolean RDCSSComplete(AtomicBoolean a1, boolean o1, AtomicReference<node> a2, node o2, node n2)
    {
        boolean v = a1.get();

        if (v == o1)
        {
            return(a2.compareAndSet(o2, n2));
        }

        else return (false);
    }

    // Search through tree and find if there exists a node with the given key
    public void seek(int key)
    {
        anchorRecord pAnchorRecord = new anchorRecord();   //D
        anchorRecord anchorRecord = new anchorRecord();    //D
        seekRecord pSeekRecord = new seekRecord();         //D
        edge pLastEdge = new edge();                       //D
        edge lastEdge = new edge();                        //D
        edge tempEdge = new edge();
        int cKey;
        int aKey;
        int which;
        node temp;                                         //D
        node next;                                         //D
        node curr;                                         //D
        boolean nullCheck = false;
        boolean deleteCheck = false;
        boolean promoteCheck = false;

        seekRecord seeker = new seekRecord();

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
                    makeEdge(tempEdge, curr, next, which);
                    seeker.injectionEdge = tempEdge;
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
                if (pAnchorRecord.node.equals(anchorRecord.node) && pAnchorRecord.key == anchorRecord.key)
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
        node oldValue = new node();
        node newValue = new node();
        edge edge;
        edge helpeeEdge = new edge();
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
                makeEdge(helpeeEdge, node, address, which);
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
            oldValue = address;
            copyNode(oldValue, address);
            if (nullCheck)
                oldValue.nullFlag.set(true);

            if (flag == 0)
            {
                copyNode(newValue, oldValue);
                newValue.deleteFlag.set(true);
                if (nullCheck)
                    newValue.nullFlag.set(true);
            }
            else
            {
                copyNode(newValue, oldValue);
                newValue.promoteFlag.set(true);
                if (nullCheck)
                    newValue.nullFlag.set(true);
            }

//            if (which == 0)
//            {
//                if ((nullCheck == true && node.leftChild.get().nullFlag.get() == true) ||
//                     (nullCheck == false && node.leftChild.get().nullFlag.get() == false))
//                {
//                    result = node.leftChild.compareAndSet(oldValue, newValue);
//                }
//                else
//                    result = false;
//            }
//
//            else
//            {
//                if ((nullCheck == true && node.rightChild.get().nullFlag.get() == true) ||
//                     (nullCheck == false && node.rightChild.get().nullFlag.get() == false))
//                {
//                    result = node.rightChild.compareAndSet(oldValue, newValue);
//                }
//                else
//                    result = false;
//            }

//            if (which == 0)
//                result = node.leftChild.compareAndSet(oldValue, newValue);
//            else
//                result = node.rightChild.compareAndSet(oldValue, newValue);

            if (which == 0)
                result = RDCSS(node.leftChild.get().nullFlag, nullCheck, node.leftChild, oldValue, newValue);
            else
                result = RDCSS(node.rightChild.get().nullFlag, nullCheck, node.rightChild, oldValue, newValue);

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
        edge lastEdge = new edge();
        edge pLastEdge = new edge();
        edge injectionEdge = new edge();

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
        node tempLeft = new node();
        node tempNode= new node();
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
//            copyNode(tempLeft, leftNode);
//            tempLeft.nullFlag.set(true);

            copyNode(tempNode, node);
            tempNode.nullFlag.set(true);
            tempNode.promoteFlag.set(true);

//            if (successorEdge.child.leftChild.get().nullFlag.get())
//                result = successorEdge.child.leftChild.compareAndSet(leftNode, tempNode);
//            else
//                result = false;

            result = RDCSS(successorEdge.child.leftChild.get().nullFlag, true, successorEdge.child.leftChild, leftNode, tempNode);

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
        node oldValue = new node();
        node newValue = new node();
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

//            copyNode(oldValue, successorEdge.child);
//            oldValue.nullFlag.set(false);
//            oldValue.intentFlag.set(intentCheck);
//            oldValue.deleteFlag.set(dFlag);
//            oldValue.promoteFlag.set(false);

            oldValue = successorEdge.child;

            if (nullCheck == true)
            {
                copyNode(newValue, successorEdge.child);
                newValue.nullFlag.set(true);
            }
            else
            {
                copyNode(newValue, rightNode);
                newValue.nullFlag.set(false);
            }
            newValue.intentFlag.set(false);
            newValue.deleteFlag.set(dFlag);
            newValue.promoteFlag.set(false);

//            if (which == 0)
//                result = successorEdge.parent.leftChild.compareAndSet(oldValue, newValue);
//            else
//                result = successorEdge.parent.rightChild.compareAndSet(oldValue, newValue);

            if (which == 0)
            {
                if (successorEdge.parent.leftChild.get().intentFlag.get() == intentCheck)
                    result = RDCSS(successorEdge.parent.leftChild.get().deleteFlag, dFlag, successorEdge.parent.leftChild, oldValue, newValue);
                else
                    result = false;
            }
            else
            {
                if (successorEdge.parent.rightChild.get().intentFlag.get() == intentCheck)
                    result = RDCSS(successorEdge.parent.rightChild.get().deleteFlag, dFlag, successorEdge.parent.rightChild, oldValue, newValue);
                else
                    result = false;
            }

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
        node oldValue = new node();
        node newValue = new node();
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
//            copyNode(oldValue, node);
//            oldValue.intentFlag.set(true);
            oldValue = node;
            copyNode(newValue, newNode);
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
//            copyNode(oldValue, node);
//            oldValue.intentFlag.set(true);

            oldValue = node;

            if (nWhich == 0)
                address = node.leftChild.get();
            else
                address = node.rightChild.get();

            if (address.nullFlag.get() == true)
            {
               copyNode(newValue, node);
                newValue.nullFlag.set(true);
            }
            else
                copyNode(newValue, address);
        }

        // Attempt to the set the correct value
//        if (pWhich == 0)
//            result = parent.leftChild.compareAndSet(oldValue, newValue);
//        else
//            result = parent.rightChild.compareAndSet(oldValue, newValue);

        if (pWhich == 0)
            result = RDCSS(parent.leftChild.get().intentFlag, true, parent.leftChild, oldValue, newValue);
        else
            result = RDCSS(parent.rightChild.get().intentFlag, true, parent.rightChild, oldValue, newValue);

        return result;
    }

    // Help deletion finish
    public void helpTargetNode(edge helpeeEdge)
    {
        stateRecord state = new stateRecord();
        boolean result;

        // Intent flag set on edge, get state record and initialize
        copyNode(state.targetEdge.child, helpeeEdge.child);
        copyNode(state.targetEdge.parent, helpeeEdge.parent);
        state.targetEdge.which = helpeeEdge.which;

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
        copyNode(seeker.lastEdge.child, helpeeEdge.child);
        copyNode(seeker.lastEdge.parent, helpeeEdge.parent);
        seeker.lastEdge.which = helpeeEdge.which;
        makeEdge(seeker.pLastEdge, null, parent, 0);

        // Promote key and remove successor
        removeSuccessor(state);
    }

    // Function returns true if key is in tree and false if not
    public boolean search(int key)
    {
        node node;
        int nKey;

        seek(key);
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
        node newNode = new node();
        node address;
        node temp;
        //node tempAddress = new node(); // NOT USED
        edge targetEdge;
        int nKey;
        int which;
        boolean result;

        while (true)
        {
            seek(key);

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
            newNode(key, newNode);
            newNode.readyToReplace.set(false);

            which = targetRecord.get().injectionEdge.which;
            address = targetRecord.get().injectionEdge.child;
//            copyNode(tempAddress, address);
//            tempAddress.nullFlag.set(true);

            // Attempt to modify the BST by adding in the new node
            if (address.nullFlag.get())
            {
                if (which == 0)
                    result = node.leftChild.compareAndSet(address, newNode);
                else
                    result = node.rightChild.compareAndSet(address, newNode);
            }
            else
                result = false;



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
