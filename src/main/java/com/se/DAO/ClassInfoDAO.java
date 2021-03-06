package com.se.DAO;

import com.alibaba.fastjson.JSON;
import com.se.entity.ClassInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ClassInfoDAO {

    public static Map<Integer,String> getClassInfoByProjectName(String projectName, Connection conn) throws SQLException {
        String sql = "select ID,className from classinfo where projectName = ? and is_delete = 0";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setString(1,projectName);
        ResultSet resultSet = pst.executeQuery();
        Map<Integer,String> idMap = new HashMap<>();
        while(resultSet.next()){
            idMap.put(resultSet.getInt("ID"),resultSet.getString("className"));
        }
        return idMap;
    }

    public static Map<String,Integer> getClassName2IDMapByProjectName(String projectName, Connection conn) throws SQLException {
        String sql = "select ID,className from classinfo where projectName = ? and is_delete = 0";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setString(1,projectName);
        ResultSet resultSet = pst.executeQuery();
        Map<String, Integer> idMap = new HashMap<>();
        while(resultSet.next()){
            idMap.put(resultSet.getString("className"), resultSet.getInt("ID"));
        }
        return idMap;
    }

    public static List<ClassInfo> getClassListByProjectName(String projectName,Connection connection) throws SQLException{
        String sql = "select ID, className,filePath from classinfo where projectName = ? and is_delete = 0";
        PreparedStatement pst = connection.prepareStatement(sql);
        pst.setString(1,projectName);
        ResultSet resultSet = pst.executeQuery();
        List<ClassInfo> classInfoList = new ArrayList<>();
        while(resultSet.next()){
            ClassInfo classInfo = new ClassInfo();
            classInfo.setID(resultSet.getInt("ID"));
            classInfo.setProjectName(projectName);
            classInfo.setClassName(resultSet.getString("className"));
            classInfo.setFilePath(resultSet.getString("filePath"));
            classInfoList.add(classInfo);
        }
        return classInfoList;
    }

    public static Map<String,Integer> getClassInfoMapByProjectName(String projectName, Connection conn) throws SQLException {
        String sql = "select ID,className from classinfo where projectName = ? and is_delete = 0";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setString(1,projectName);
        ResultSet resultSet = pst.executeQuery();
        Map<String,Integer> idMap = new HashMap<>();
        while(resultSet.next()){
            idMap.put(resultSet.getString("className"),resultSet.getInt("ID"));
        }
        return idMap;
    }

    public static List<String> getAllClassInfoList(String projectName, Connection conn) throws SQLException {
        String sql = "select * from classinfo where projectName = ? and is_delete = 0";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setString(1, projectName);
        ResultSet resultSet = pst.executeQuery();
        List<String> classInfoList = new ArrayList<>();
        while(resultSet.next()){
            classInfoList.add(resultSet.getString("className"));
        }
        return classInfoList;
    }

    public static void updateSuperClass(String superClassName, String className, String projectName, Connection conn) throws SQLException {
        String sql = "UPDATE classinfo SET super_class = ? WHERE className = ? and projectName = ? and is_delete = 0";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setString(1,superClassName);
        pst.setString(2,className);
        pst.setString(3,projectName);
        pst.executeUpdate();
    }

    public static List<ClassInfo> getSubClassList(String projectName, Connection conn)throws SQLException{
        String sql = "select * from classinfo where projectName = ? and is_delete = 0 and (super_class is not null or interfaces is not null)";
        PreparedStatement pst =  conn.prepareStatement(sql);
        pst.setString(1, projectName);
        ResultSet resultSet = pst.executeQuery();

        List<ClassInfo> classInfoList = new ArrayList<>();
        while(resultSet.next()){
            ClassInfo classInfo = new ClassInfo();
            classInfo.setID(resultSet.getInt("ID"));
            classInfo.setClassName(resultSet.getString("className"));
            classInfo.setSuperClass(resultSet.getString("super_class"));
            classInfo.setInterfaces(resultSet.getString("interfaces"));
            classInfoList.add(classInfo);
        }
        return classInfoList;

    }

    public static void updateImplInterfaces(List<String> implInterfaces, String className, String projectName, Connection conn) throws SQLException {
        String sql = "UPDATE classinfo SET interfaces = ? WHERE className = ? and projectName = ? and is_delete = 0";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setString(1, JSON.toJSONString(implInterfaces));
        pst.setString(2,className);
        pst.setString(3,projectName);
        pst.executeUpdate();
    }

    /**
     * 存储一个项目内的所有类
     * @param classInfoList
     */
    public synchronized static void saveClassInfoList(List<ClassInfo> classInfoList, Connection conn) throws SQLException{
        String sql = "insert into classinfo (projectName,className,isInterface,filePath, create_time, update_time,layer) values (?,?,?,?,?,?,?)";
        Date currentDate = new Date();
        java.sql.Date currentDateInSql = new java.sql.Date(currentDate.getTime());
        if(classInfoList != null && !classInfoList.isEmpty()){
            PreparedStatement pst = conn.prepareStatement(sql);
            for(ClassInfo classInfo : classInfoList){
                //过滤过长的方法名，过滤匿名函数，过滤链式调用
                if(classInfo == null||classInfo.getClassName().length()>100 || classInfo.getClassName().contains("{")||classInfo.getClassName().contains("}")||classInfo.getClassName().contains("(")
                        ||classInfo.getClassName().contains(")"))continue;
                pst.setString(1,classInfo.getProjectName());
                pst.setString(2,classInfo.getClassName());
                pst.setString(3,classInfo.getInterface().toString());
                pst.setString(4,classInfo.getFilePath());
                pst.setDate(5, currentDateInSql);
                pst.setDate(6, currentDateInSql);
                pst.setString(7,classInfo.getLayer());
                pst.addBatch();
            }
            pst.executeBatch();
            pst.clearBatch();
        }
    }

    public static void updateInvokeCounts(List<List<Integer>> invokeInfoList, Connection conn) throws SQLException {
        String sql = "UPDATE classinfo SET invokedCounts = ?,invokeCounts = ? WHERE ID = ? and is_delete = 0";
        PreparedStatement pst = conn.prepareStatement(sql);
        for(List<Integer> list:invokeInfoList){
            pst.setInt(1,list.get(0));
            pst.setInt(2,list.get(1));
            pst.setInt(3,list.get(2));
            pst.addBatch();
        }
        pst.executeBatch();
        pst.clearBatch();
    }

    public static void updateDefaultInvokeDept(Connection conn) throws SQLException {
        String sql = "update classinfo set invocationDept = '0' where invocationDept is null and is_delete = 0";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.executeUpdate();
    }


    public static void updateInvocationDept(Map<String,Integer> invocationDeptMap, Connection conn) throws SQLException {
        String sql = "UPDATE classinfo SET invocationDept = ? WHERE className = ? and is_delete = 0";
        PreparedStatement pst = conn.prepareStatement(sql);
        for(String className:invocationDeptMap.keySet()){
            pst.setInt(1,invocationDeptMap.get(className));
            pst.setString(2,className);
            pst.addBatch();
        }
        pst.executeBatch();
        pst.clearBatch();
    }

    public static String getClassIDByProjectNameAndClassName(String projectName,String className,Connection conn) throws SQLException{
        String sql = "select ID from classinfo where projectName = ? and className = ? and is_delete = 0";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setString(1,projectName);
        pst.setString(2,className);
        ResultSet resultSet = pst.executeQuery();
        if(resultSet.next()){
            return resultSet.getString("ID");
        }
        return null;
    }

    public static List<String> getAllProjectNameFromDB(Connection conn) throws SQLException {
        List<String> projectNameList = new ArrayList<>();
        String sql = "select distinct projectName from classinfo where is_delete = 0";
        PreparedStatement preparedStatement = conn.prepareStatement(sql);
        ResultSet resultSet = preparedStatement.executeQuery();
        while(resultSet.next()){
            projectNameList.add(resultSet.getString("projectName"));
        }
        return projectNameList;
    }

    public static List<ClassInfo> getClassInfoByFilePath(String filePath, Connection connection) throws SQLException {
        List<ClassInfo> classInfoList = new ArrayList<>();
        String sql = "select ID,className from classinfo where filePath = ? and is_delete = 0";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1,filePath);
        ResultSet resultSet = preparedStatement.executeQuery();
        while(resultSet.next()){
            ClassInfo classInfo = new ClassInfo();
            classInfo.setID(resultSet.getInt("ID"));
            classInfo.setClassName(resultSet.getString("className"));
            classInfoList.add(classInfo);
        }
        return classInfoList;
    }

    public static ClassInfo getClassInfoByFilePath(String projectName, String filePath, Connection connection) throws SQLException {
        List<ClassInfo> classInfoList = new ArrayList<>();
        String sql = "select ID,className from classinfo where projectName = ? and filePath = ? and is_delete = 0";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1,projectName);
        preparedStatement.setString(2,filePath);
        ResultSet resultSet = preparedStatement.executeQuery();
        ClassInfo classInfo = new ClassInfo();
        while(resultSet.next()){
            classInfo.setID(resultSet.getInt("ID"));
            classInfo.setClassName(resultSet.getString("className"));
            classInfoList.add(classInfo);
        }
        return classInfo;
    }

    public static ClassInfo getClassInfoByClassName(String className, Connection connection) throws SQLException {
        String sql = "select ID,invokeCounts,invokedCounts from classinfo where className = ? and is_delete = 0";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1,className);
        ResultSet resultSet = preparedStatement.executeQuery();
        ClassInfo classInfo = new ClassInfo();
        while(resultSet.next()){
            classInfo.setID(resultSet.getInt("ID"));
            classInfo.setClassName(className);
            classInfo.setInvokeCounts(resultSet.getInt("invokeCounts"));
            classInfo.setInvokedCounts(resultSet.getInt("invokedCounts"));
        }
        return classInfo;
    }


    public static Map<String,List<Integer>> getAllClassInvokeInfo(Connection connection) throws SQLException {
        String sql = "select className,invokeCounts,invokedCounts from classinfo where is_delete = 0";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        ResultSet resultSet = preparedStatement.executeQuery();
        Map<String,List<Integer>> classInvokeMap = new HashMap<>();
        while(resultSet.next()){
            List<Integer> integerList = new ArrayList<>();
            integerList.add(resultSet.getInt("invokeCounts"));
            integerList.add(resultSet.getInt("invokedCounts"));
            classInvokeMap.put(resultSet.getString("className"),integerList);
        }
        return classInvokeMap;
    }


    public static List<ClassInfo> getDiscardClassList(Connection connection) throws SQLException {
        String sql = "select className,filePath from classinfo where invocationDept = 0 and invokedCounts = 0 and invokeCounts = 0 and is_delete = 0";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        ResultSet resultSet = preparedStatement.executeQuery();
        List<ClassInfo> classInfoList = new ArrayList<>();
        while(resultSet.next()){
            ClassInfo classInfo = new ClassInfo();
            classInfo.setClassName(resultSet.getString("className"));
            String filePath = resultSet.getString("filePath");
            if(filePath == null)continue;
            filePath = filePath.replace("|","\\");
            classInfo.setFilePath(filePath);
            classInfoList.add(classInfo);
        }
        return classInfoList;
    }

    public static double getAvgInvokeCounts(Connection connection) throws SQLException {
        String sql = "select AVG(classinfo.invokeCounts) as avgInvokeCounts from classinfo where invokeCounts!=0 and is_delete = 0";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        ResultSet resultSet = preparedStatement.executeQuery();
        double avgInvokeCounts = 0;
        while(resultSet.next()){
            avgInvokeCounts = resultSet.getDouble("avgInvokeCounts");
        }
        return avgInvokeCounts;
    }

    public static double getAvgInvokedCounts(Connection connection) throws SQLException {
        String sql = "select AVG(classinfo.invokedCounts) as avgInvokedCounts from classinfo where invokedCounts!=0 and is_delete = 0";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        ResultSet resultSet = preparedStatement.executeQuery();
        double avgInvokedCounts = 0;
        while(resultSet.next()){
            avgInvokedCounts = resultSet.getDouble("avgInvokedCounts");
        }
        return avgInvokedCounts;
    }

    public static void updateDiscardClass(List<String> classPathList,Connection connection) throws SQLException{
        String sql = "update classinfo set isDiscardClass = 1 where filePath = ? and is_delete = 0";
        PreparedStatement pst = connection.prepareStatement(sql);
        for(String filePath:classPathList){
            filePath = filePath.replace("\\","|");
            pst.setString(1,filePath);
            pst.addBatch();
        }
        pst.executeBatch();
        pst.clearBatch();
    }

    public static void deleteClassInfoRecords(List<Integer> deleteClassIDs, Connection conn) throws SQLException{
//        conn.setAutoCommit(false);
//        String cInfoSQL = "delete from classinfo where ID = ?";
        Date currentDate = new Date();
        java.sql.Date currentDateInSql = new java.sql.Date(currentDate.getTime());

        String cInfoSQL = "update classinfo set is_delete = 1, update_time = ? where ID = ?";

        PreparedStatement pst = conn.prepareStatement(cInfoSQL);

        if(deleteClassIDs != null){
            for(Integer classInfoID : deleteClassIDs){
                pst.setDate(1, currentDateInSql);
                pst.setInt(2, classInfoID);
                pst.addBatch();
            }
            pst.executeBatch();
            pst.clearBatch();
//            conn.commit();
        }

    }


    public static void updateGodClass(List<String> classPathList, Connection connection) throws SQLException {
        String sql = "update classinfo set isGodClass = 1 where filePath = ? and is_delete = 0";
        PreparedStatement pst = connection.prepareStatement(sql);
        for(String filePath:classPathList){
            filePath = filePath.replace("\\","|");
            pst.setString(1,filePath);
            pst.addBatch();
        }
        pst.executeBatch();
        pst.clearBatch();
    }




}