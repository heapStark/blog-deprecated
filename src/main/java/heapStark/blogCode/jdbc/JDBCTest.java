package heapStark.blogCode.jdbc;

import heapStark.blogCode.jdbc.utils.JdbcUtils;
import heapStark.blogCode.utils.MultiThreadTestUtil;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

/**
 * blogcode
 * Created by wangzhilei3 on 2018/1/7.
 */
public class JDBCTest {
    @Test
    public void testShow() {
        Connection conn = JdbcUtils.getConnection("transaction");
        try {
            //创建一个Statement对象
            Statement stmt = conn.createStatement(); //创建Statement对象
            String sql = "show tables";
            ResultSet resultSet = stmt.executeQuery(sql);
            while (resultSet.next()) {
                String s = resultSet.getString(1);
                assert (s.equals("student"));
            }
            System.out.print("成功连接到数据库！");
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * autoCommit 测试
     */
    @Test
    public void insertTest() {
        Connection conn = JdbcUtils.getConnection("transaction");
        try {
            assert (conn.getAutoCommit() == true);
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.print("成功连接到数据库！");
        try {
            //创建一个Statement对象
            Statement stmt = conn.createStatement(); //创建Statement对象
            String sql = "INSERT INTO student (id, NAME,gender,score,age,birthday)VALUES('125','liu','0','123','15',NOW())";
            boolean result = stmt.execute(sql);
            System.out.print("执行结果！" + result);
            stmt.close();
            conn.commit();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void modifyTest() throws Exception {
        Connection conn = JdbcUtils.getConnection("transaction");

        System.out.println("成功连接到数据库！");

        //创建一个Statement对象
        Statement stmt = conn.createStatement(); //创建Statement对象
        String sql = "UPDATE student SET id =221 WHERE id = 220";
        int result = stmt.executeUpdate(sql);
        stmt.close();
        conn.close();
    }


    /**
     * 脏读测试 不可重复读 Connection.TRANSACTION_READ_UNCOMMITTED
     *
     * @throws Exception
     */
    @Test
    public void dirtyReadTest() throws Exception {
        MultiThreadTestUtil.multiThreadRun(() -> {

            Connection conn = JdbcUtils.getConnection("transaction");
            try {
                assert (conn.getAutoCommit() == true);
                conn.setAutoCommit(false);
                //conn.setTransactionIsolation(Connection.TRANSACTION_NONE);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            System.out.println("成功连接到数据库！");
            try {
                //创建一个Statement对象
                Statement stmt = conn.createStatement(); //创建Statement对象
                String sql = "UPDATE student SET id =221 WHERE id = 220";
                int result = stmt.executeUpdate(sql);
                System.out.println("修改执行结果：" + result);
                stmt.close();
                TimeUnit.SECONDS.sleep(7);
                conn.rollback();
                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 1);
        //脏读读取未提交数据
        Connection conn = JdbcUtils.getConnection("transaction");
        conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
        System.out.println("成功连接到数据库！");
        try {
            TimeUnit.SECONDS.sleep(2);
            //创建一个Statement对象
            Statement stmt = conn.createStatement(); //创建Statement对象
            String sql = "SELECT * FROM student WHERE id = 221";
            ResultSet result = stmt.executeQuery(sql);
            result.next();
            assert (result.getString("id").equals("221"));
            System.out.println("执行结果！" + result.getString("id"));
            stmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //不读取未提交数据
        Connection conn2 = JdbcUtils.getConnection("transaction");
        conn2.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        System.out.println("成功连接到数据库！");
        try {
            TimeUnit.SECONDS.sleep(2);
            //创建一个Statement对象
            Statement stmt = conn2.createStatement(); //创建Statement对象
            String sql = "SELECT * FROM student WHERE id = 220";
            ResultSet result = stmt.executeQuery(sql);
            result.next();
            assert (result.getString("id").equals("220"));
            System.out.println("执行结果！" + result.getString("id"));
            stmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 幻读测试，insert
     *
     * @throws Exception
     */
    @Test
    public void committedReadTest() throws Exception {
        MultiThreadTestUtil.multiThreadRun(() -> {
            Connection connection = JdbcUtils.getConnection("transaction");
            try {
                connection.setAutoCommit(false);
                Statement statement = connection.createStatement();
                String sql = "INSERT INTO student (id, NAME,gender,score,age,birthday)VALUES('125','liu','0','123','15',NOW())";
                statement.execute(sql);
                statement.close();
                TimeUnit.SECONDS.sleep(7);
                System.out.println("rollback");
                connection.rollback();
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 1);

        TimeUnit.SECONDS.sleep(2);
        //可重复读读到新行
        Connection conn = JdbcUtils.getConnection("transaction");
        //conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);//不可读取新行
        //conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);//可以读取新行
        conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);//可以读取新行
        System.out.println("成功连接到数据库！");
        try {
            TimeUnit.SECONDS.sleep(2);
            //创建一个Statement对象
            Statement stmt = conn.createStatement(); //创建Statement对象
            String sql = "SELECT COUNT(*) FROM student WHERE id = 125";
            ResultSet result = stmt.executeQuery(sql);
            result.next();
            assert (result.getString(1).equals("1"));
            System.out.println("执行结果！" + result.getString(1));
            stmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //串行化不读取新行
        Connection conn2 = JdbcUtils.getConnection("transaction");
        conn2.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        System.out.println("成功连接到数据库！");
        try {
            TimeUnit.SECONDS.sleep(2);
            //创建一个Statement对象
            Statement stmt = conn2.createStatement(); //创建Statement对象
            String sql = "SELECT COUNT(*) FROM student";
            ResultSet result = stmt.executeQuery(sql);
            result.next();
            assert (result.getString(1).equals("1"));
            System.out.println("串行化执行执行结果！" + result.getString(1));
            stmt.close();
            conn.close();
            TimeUnit.SECONDS.sleep(3);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 幻读测试，insert 之后两次查询结果不一致
     * 改为串行执行实际存在数据
     * @throws Exception
     */
    @Test
    public void REPEATABLEReadTest() throws Exception {
        MultiThreadTestUtil.multiThreadRun(() -> {
            Connection connection = JdbcUtils.getConnection("transaction");
            Connection connectiondel = JdbcUtils.getConnection("transaction");
            //清理数据
            try {
                Statement statementdel = connectiondel.createStatement();
                statementdel.execute("DELETE FROM student WHERE id = 125");
                connectiondel.close();
            } catch (SQLException e) {

            }
            try {
                connection.setAutoCommit(false);

                Statement statement = connection.createStatement();
                System.out.println("开始添加数据");
                String sql = "INSERT INTO student (id, NAME,gender,score,age,birthday)VALUES('125','liu','0','123','15',NOW())";
                boolean b = statement.execute(sql);
                System.out.println("执行结果：" + b);
                statement.close();
                TimeUnit.SECONDS.sleep(3);
                connection.commit();
                System.out.println("commit");
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 1);
        Connection conn = JdbcUtils.getConnection("transaction");
        conn.setAutoCommit(false);

        /*conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);//可读取新行
        conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);//可以读取新行
        conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);//可以读取新行*/
        conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);//无法读取新行
        System.out.println("成功连接到数据库！");
        try {
            //创建一个Statement对象
            Statement stmt = conn.createStatement(); //创建Statement对象
            String sql = "SELECT COUNT(*) FROM student ";
            ResultSet result = stmt.executeQuery(sql);
            result.next();
            //assert (result.getString(1).equals("0"));
            System.out.println("事务提交前执行结果：" + result.getString(1));
            TimeUnit.SECONDS.sleep(8);
            //同一事务第二次操作
            result = stmt.executeQuery(sql);
            result.next();
            //assert (result.getString(1).equals("1"));
            System.out.println("事务提交后执行结果：" + result.getString(1));
            stmt.close();
            conn.close();
            TimeUnit.SECONDS.sleep(4);
        } catch (Exception e) {
            e.printStackTrace();
        }
        TimeUnit.SECONDS.sleep(10);
    }

}
