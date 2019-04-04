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

// ------------------------------ NOT DONE ------------------------------------------------------- |
    // Function to insert value into the bst
    public boolean insert(int key)  // targetRecord.remove(); Help target and succcessor
    {
        node node;
        node newNode;
        node address;
        node temp;
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
                return (false);

            // Make a newNode and initialize
            newNode = newNode(key);
            newNode.readyToReplace.set(false);

            which = targetRecord.get().injectionEdge.which;
            address = targetRecord.get().injectionEdge.child;
            address.nullFlag.set(true);

            // Attempt to modify the BST by adding in the new node
            if (which == 0)
                result = node.leftChild.compareAndSet(address, newNode);
            else
                result = node.rightChild.compareAndSet(address, newNode);

            // Successful insertion
            if (result)
                return (true);

            // Unsuccessful insertion, help is needed
            if (which == 0)
                temp = node.leftChild.get();
            else
                temp = node.rightChild.get();

//            if (temp.deleteFlag.get())
//                // Help Target Node
//
//            else if (temp.promoteFlag.get())
//                // Help Successor Node
        }
    }
}
