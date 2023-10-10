package edu.illinois.confuzz.internal;

import java.util.*;

public class ParamTree {
    public static final ParamTreeNode root = new ParamTreeNode();
    public static final Map<String, ParamTreeNode> paramTreeNodeMap = new HashMap<>();

    public static void insert(String name) {
        if (!paramTreeNodeMap.containsKey(name)) {
            String[] segments = name.split("\\.");
            ParamTreeNode node = root;
            // steps down the tree
            for (String segment: segments) {
                if (!node.children.containsKey(segment)) {
                    node = node.addChild(segment);
                } else {
                    node = node.children.get(segment);
                }
            }
            node.isParam = true;
            paramTreeNodeMap.put(name, node);
        }
    }

    public static void remove(String name) {
        ParamTreeNode node = paramTreeNodeMap.get(name);
        node.isParam = false;
        while (node.children.size() == 0 && node != root) {
            ParamTreeNode father = node.father;
            father.children.remove(node.segment);
            node = father;
        }
        paramTreeNodeMap.remove(name);
    }

    public static boolean contains(String name) {
        return paramTreeNodeMap.containsKey(name);
    }

    // TODO: add a method for sampling from vicinity of a param
    // We choose 0.5 here because we observe that the most common dependencies are between two params
    // But we also want to mutate only one config per feature sometimes
    private static final int ONE_OVER_MEAN_SAMPLE_FACTOR = 2; // This is just for computational efficiency
    public static List<String> sample(Random random, Set<String> names, Collection<String> allParams) {
        Map<String, Integer> commonPrefixCnt = new LinkedHashMap<>();
        if (allParams.size() == 0 || names.size() == 0) {
            return new ArrayList<>();
        }
        for (String name: names) {
            switch(name.split("\n").length) {
                case 1: case 2: case 3:
                    markPath(name, 1); break;
                case 4: case 5: case 6:
                    markPath(name, 2); break;
                default:
                    markPath(name, 3);
            }
        }
        int norm = 0;
        for (String param: allParams) {
            if (names.contains(param) || !paramTreeNodeMap.containsKey(param)
                    || ConfParamGenerator.getDullConfigs().contains(param)) {
                continue;
            }
            int sum = 0;
            ParamTreeNode node = paramTreeNodeMap.get(param);
            while (node != root) {
                sum += node.cnt;
                node = node.father;
            }
            if (sum > 0) {
                norm += sum;
                commonPrefixCnt.put(param, sum);
            }
        }
        for (String name: names) {
            clearPath(name);
        }
        if (commonPrefixCnt.size() == 0) {
            return new ArrayList<>();
        }
        List<String> ret = new ArrayList<>();
        // This code only works when MEAN_SAMPLE < size of commonPrefixCnt
        for (Map.Entry<String, Integer> entry: commonPrefixCnt.entrySet()) {
            if (random.nextInt(norm) * ONE_OVER_MEAN_SAMPLE_FACTOR < entry.getValue() * names.size()) {
                ret.add(entry.getKey());
            }
        }
        LogUtils.println("Sampled " + ret.size() + "/" + paramTreeNodeMap.size() + " params from the config param tree:");
        LogUtils.println(String.join(", ", ret) + "\n");
        return ret;
    }

    private static void clearPath(String name) {
        if (!paramTreeNodeMap.containsKey(name)) {
            throw new IllegalArgumentException("ParamTree does not contain param: " + name);
        }
        ParamTreeNode node = paramTreeNodeMap.get(name);
        while (node.cnt == 0 && node != root) {
            node = node.father;
        }
        node.cnt = 0;
    }

    private static void markPath(String name, int depth) {
        if (!paramTreeNodeMap.containsKey(name)) {
            throw new IllegalArgumentException("ParamTree does not contain param: " + name);
        }
        ParamTreeNode node = paramTreeNodeMap.get(name);
        for (int i = 0; i < depth; i++) {
            node = node.father;
        }
        node.cnt ++;
    }

    private static class ParamTreeNode {
        public boolean isParam;
        public ParamTreeNode father;
        public final Map<String, ParamTreeNode> children = new HashMap<>();
        public String segment = null;

        public int cnt = 0;

        public ParamTreeNode() {
            this(false, null);
        }

        public ParamTreeNode(boolean isParam, ParamTreeNode father) {
            this.isParam = isParam;
            this.father = father;
        }
        public ParamTreeNode addChild(String segment) {
            ParamTreeNode child = new ParamTreeNode(false, this);
            children.put(segment, child);
            child.segment = segment;
            return child;
        }
    };
}
