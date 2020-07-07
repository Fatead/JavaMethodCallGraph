package com.se.process;

import com.se.DAO.ClassInfoDAO;
import com.se.DAO.MethodInvocationInViewDAO;
import com.se.entity.GraphNode;
import com.se.entity.MethodInvocationInView;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

//统计每个类的被调用次数和调用深度

public class CountInvocation {


    //统计每个类的调用次数与被调用次数
    public static void countInvokeCounts(List<String> projectNameList, Connection conn) throws SQLException {
        System.out.println("正在进行调用次数统计");
        for(String projectName:projectNameList){
            System.out.println("正在进行调用次数统计的项目名称为：" + projectName);
            Map<Integer,String> idMap = ClassInfoDAO.getClassInfoByProjectName(projectName,conn);
            List<List<Integer>> invokeInfoList = new ArrayList<>();
            List<MethodInvocationInView> methodInvocationInViewList = MethodInvocationInViewDAO.getMethodInvocationInViewByProjectName(projectName,conn);
            HashMap<String,Integer> callCountsMap = new HashMap<>();
            HashMap<String,Integer> calledCountsMap = new HashMap<>();
            for(MethodInvocationInView methodInvocationInView:methodInvocationInViewList){
                if(callCountsMap.containsKey(methodInvocationInView.getCallClassName())){
                    int count = callCountsMap.get(methodInvocationInView.getCallClassName());
                    callCountsMap.put(methodInvocationInView.getCallClassName(),count+1);
                }else {
                    callCountsMap.put(methodInvocationInView.getCallClassName(),1);
                }
                if(calledCountsMap.containsKey(methodInvocationInView.getCalledClassName())){
                    int count = calledCountsMap.get(methodInvocationInView.getCalledClassName());
                    calledCountsMap.put(methodInvocationInView.getCalledClassName(),count+1);
                }else {
                    calledCountsMap.put(methodInvocationInView.getCalledClassName(),1);
                }
            }
            for(Integer Id:idMap.keySet()){
                List<Integer> list = new ArrayList<>();
                String className = idMap.get(Id);
                int invokedCount = 0,invokeCount = 0;
                if(calledCountsMap.containsKey(className)){
                    invokedCount = calledCountsMap.get(className);
                }
                if(callCountsMap.containsKey(className)){
                    invokeCount = callCountsMap.get(className);
                }
                list.add(invokedCount);
                list.add(invokeCount);
                list.add(Id);
                invokeInfoList.add(list);
            }
            ClassInfoDAO.updateInvokeCounts(invokeInfoList,conn);
        }
    }

    //统计每个类被调用的深度
    public static void countInvocationDept(List<String> projectNameList,Connection conn) throws SQLException {
        System.out.println("正在统计调用深度");
        Map<String,GraphNode> graphNodeMap = new HashMap<>();
        Map<String,GraphNode> calledMethodMap = new HashMap<>();
        Map<String,GraphNode> callMethodMap = new HashMap<>();
        for(String projectName:projectNameList){
            System.out.println("正在进行调用深度统计的项目名为：" + projectName);
            List<MethodInvocationInView> methodInvocationInViewList = MethodInvocationInViewDAO.getMethodInvocationInViewByProjectName(projectName,conn);
            for(MethodInvocationInView methodInvocationInView:methodInvocationInViewList){
                //过滤类内调用
                if(methodInvocationInView.getCallClassName().equals(methodInvocationInView.getCalledClassName()))continue;
                graphNodeMap.put(methodInvocationInView.getCallClassName(),new GraphNode(methodInvocationInView.getCallClassID(),methodInvocationInView.getCallClassName(), 2));
                graphNodeMap.put(methodInvocationInView.getCalledClassName(),new GraphNode(methodInvocationInView.getCalledClassID(),methodInvocationInView.getCalledClassName(),2));
                //计算入度
                calledMethodMap.put(methodInvocationInView.getCalledClassName(),new GraphNode(methodInvocationInView.getCalledClassID(),methodInvocationInView.getCalledClassName(), 2));
                callMethodMap.put(methodInvocationInView.getCallClassName(),new GraphNode(methodInvocationInView.getCallClassID(),methodInvocationInView.getCallClassName(), 2));
            }
//            System.out.println("全部类的个数为" + graphNodeMap.size());
//            System.out.println("调用类的个数为" + callMethodMap.size());
//            System.out.println("被调用类的个数为" + calledMethodMap.size());
            Map<String, GraphNode> rootGraphNode = new HashMap<>();
            for(String name:graphNodeMap.keySet()){
                boolean addFlag = true;
                for(String className:calledMethodMap.keySet()){
                    if (name.equals(className)) {
                        addFlag = false;
                        break;
                    }
                }
                if(addFlag)rootGraphNode.put(name,graphNodeMap.get(name));
            }
            System.out.println("根节点类的个数为：" + rootGraphNode.size());
            //使用宽度优先搜索确定每个结点的调用深度，若一个结点同时存在多个调用深度，取值最大的深度
            Queue<GraphNode> graphNodeQueue = new LinkedList<>(rootGraphNode.values());
            Set<String> calledNodeNameSet = new HashSet<>();
            while(!graphNodeQueue.isEmpty()){
                GraphNode graphNode = graphNodeQueue.poll();
                for(MethodInvocationInView methodInvocationInView:methodInvocationInViewList){
                    //过滤类内调用
                    if(methodInvocationInView.getCallClassName().equals(methodInvocationInView.getCalledClassName()))continue;
                    if(methodInvocationInView.getCallClassName().equals(graphNode.getName())){
                        GraphNode graphNode1  = graphNodeMap.get(methodInvocationInView.getCalledClassName());
                        if(!calledNodeNameSet.contains(graphNode1.getName())){
                            int dept = graphNode.getCalledDept() + 1;
                            if(dept>graphNode1.getCalledDept()){
                                graphNode1.setCalledDept(dept);
                            }
                            graphNodeMap.put(graphNode1.getName(),graphNode1);
                            graphNodeQueue.add(graphNode1);
                            calledNodeNameSet.add(graphNode1.getName());
                        }
                    }
                }
            }
            Map<String,Integer> invocationDeptMap = new HashMap<>();
            for(GraphNode graphNode:graphNodeMap.values()){
                invocationDeptMap.put(graphNode.getName(),graphNode.getCalledDept());
            }
            ClassInfoDAO.updateInvocationDept(invocationDeptMap,conn);
        }
        ClassInfoDAO.updateDefaultInvokeDept(conn);
        System.out.println("调用深度统计完成");
    }

}
