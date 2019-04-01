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
}