package bestpractice.algorithm;

import java.util.ArrayDeque;

/**
 * Created by wuyongbo on 2015/10/23.
 */
public class BinaryTreeTraverse {

    public BinaryTreeNode rootNode = null;

    public BinaryTreeTraverse(int array[]) {
        rootNode = makeBinaryTreeMiddleOrder(array, 1);
    }

    /**
     * 根据中序遍历的输出序列创建一颗二叉树
     * @param array
     * @return 返回根节点
     */
    private BinaryTreeNode makeBinaryTreeMiddleOrder(int array[], int index) {
        if (index < array.length) {
            int value = array[index];
            if (value != 0) {
                BinaryTreeNode node = new BinaryTreeNode(value);
                array[index] = 0;

                node.leftTree = makeBinaryTreeMiddleOrder(array, index*2);
                node.rightTree = makeBinaryTreeMiddleOrder(array, index*2 + 1);

                return node;
            }
        }
        return null;
    }

    /**
     * 深度优先遍历的递归算法实现
     * 辅助数据结构 栈
     * @param treeNode
     */
    private void deepFirstTraverseRecursive(BinaryTreeNode treeNode) {

        System.out.print(treeNode.value + " ");

        if (treeNode.leftTree == null && treeNode.rightTree == null) {
            return;
        }

        if (treeNode.leftTree != null) {
            deepFirstTraverseRecursive(treeNode.leftTree);
        }

        if (treeNode.rightTree != null) {
            deepFirstTraverseRecursive(treeNode.rightTree);
        }
    }

    /**
     * 深度优先遍历的非递归算法实现
     * 辅助数据结构 栈
     * @param rootNode 二叉树的根节点
     */
    ArrayDeque<BinaryTreeNode> stack = new ArrayDeque<BinaryTreeNode>();
    private void deepFirstTraverseNonRecursive(BinaryTreeNode rootNode) {
        if (rootNode == null) {
            System.out.print("Empty tree");
            return;
        }

        ArrayDeque<BinaryTreeNode> treeStack = new ArrayDeque<BinaryTreeNode>();
        stack.push(rootNode);

        while (!stack.isEmpty()) {
            BinaryTreeNode treeNode = stack.pop();
            System.out.print(treeNode.value + " ");

            if (treeNode.rightTree != null)
                stack.push(treeNode.rightTree);

            if (treeNode.leftTree != null)
                stack.push(treeNode.leftTree);
        }
    }

    /**
     * 广度遍历的非递归实现，体现了队列先进先出的特点
     * 辅助数据结构 队列
     * @param node
     */
    private void widthFirstTraverse(BinaryTreeNode node) {
        if (node == null) {
            System.out.print("Empty tree");
            return;
        }
        ArrayDeque<BinaryTreeNode> queue = new ArrayDeque<BinaryTreeNode>();
        queue.add(node);

        while (!queue.isEmpty()) {
            BinaryTreeNode treeNode = queue.remove();
            System.out.print(treeNode.value + " ");

            if (treeNode.leftTree != null)
                queue.add(treeNode.leftTree);

            if (treeNode.rightTree != null) {
                queue.add(treeNode.rightTree);
            }
        }
        System.out.print("\n");
    }

    public static void main(String[] args) {
        int[] arr={0,13,65,5,97,25,0,37,22,0,4,28,0,0,32,0};
        BinaryTreeTraverse treeTraverse = new BinaryTreeTraverse(arr);
        System.out.print("\n------深度优先遍历 递归算法------\n");
        treeTraverse.deepFirstTraverseRecursive(treeTraverse.rootNode);
        System.out.print("\n------深度优先遍历 非递归算法------\n");
        treeTraverse.deepFirstTraverseNonRecursive(treeTraverse.rootNode);
        System.out.print("\n------广度优先遍历------\n");
        treeTraverse.widthFirstTraverse(treeTraverse.rootNode);
    }
}
