package com.amazonaws.lambda.app;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.sql.*;

public class Service {

    private Logger log = LoggerFactory.getLogger(Service.class);

    private static final AmazonKinesis AMAZON_KINESIS;
    static {
        AMAZON_KINESIS = AmazonKinesisClientBuilder.standard()
                .withRegion(Regions.US_WEST_2)
                .build();
    }

    public Connection getConnection() {
        Connection con = null;
        try {
            con = DriverManager.getConnection("jdbc:mysql://mydb.cse1mvnkc219.us-west-2.rds.amazonaws.com:3306/case","admin","Jaguar789");
        }
        catch(SQLException s) {
            log.error("Error", s);
        }
        return con;
    }
    public Response select(Request req) {
        Response res = new Response();
        switch(req.getResource()) {
            case("/review/{id}"):return review(req,res);
            case("/approve/{id}"): return approve(req,res);
            default:break;
        }
        res.setStatusCode(500);
        res.setBody("Not an expected resource");
        return res;
    }
    public Response review(Request req, Response res) {
        Connection con = getConnection();
        String[] ar = req.getPath().split("/");
        String id = ar[ar.length -1];
        String q = "insert into workflow(workflow_name, id, status) values(?,?,?)";
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(q);
            ps.setString(1, "caseapproval");
            ps.setString(2,id);
            ps.setString(3,"REVIEWED");
            if(ps.executeUpdate() == 1) {
                res.setBody("Case id  "+ id + " has been REVIEWED");
                res.setStatusCode(200);
            }
            else {res.setBody("Problem in saving data to DB"); res.setStatusCode(500);}
        }
        catch(Exception e) {
            log.error("error", e);
            res.setStatusCode(500);
            res.setBody(e.getMessage());
        }
        finally {
            safeClose(con);
            safeClose(ps);
        }
        return res;
    }
    public Response approve(Request req, Response res) {
        Connection con = getConnection();
        String[] ar = req.getPath().split("/");
        String id = ar[ar.length -1];
        String q = "insert into workflow(workflow_name, id, status) values(?,?,?)";
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(q);
            ps.setString(1,"caseapproval");
            ps.setString(2,id);
            ps.setString(3,"APPROVED");
            if(ps.executeUpdate() > 0) {
                Mail m = new Mail();
                m.setSubject("Review" +" "+ id);
                m.setBody(new StringBuilder().append("Click on this line to after Review : http://127.0.0.1:3000/review/"+id).append('\n').append("Click on this link to Reject : http://127.0.0.1:3000/reject/"+id).toString());
                String kinesispayload = new ObjectMapper().writeValueAsString(m);
                messageToKinesis(id, kinesispayload);
                res.setBody("Case id  "+ id + "  has been APPROVED");
                res.setStatusCode(200); }
            else {res.setBody("Problem in saving data to DB"); res.setStatusCode(500);}

        }
        catch(Exception e) {
            log.error("error", e);
            res.setStatusCode(500);
            res.setBody(e.getMessage());
        }
        finally {
            safeClose(con);
            safeClose(ps);
      }
       return res;
    }

    public void messageToKinesis(String id, String body){
        PutRecordRequest recordRequest = new PutRecordRequest();
        recordRequest.setStreamName("EmailStream");
        recordRequest.setPartitionKey(id);
        recordRequest.withData(ByteBuffer.wrap(body.getBytes()));
        AMAZON_KINESIS.putRecord(recordRequest);
    }

    public void safeClose(Connection c) {
        if(c != null) {
            try {
                c.close();
            } catch (SQLException e) {
                log.error("", e);
            }
        }
    }

    private void safeClose(PreparedStatement stmt) {
        if(stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                log.error("", e);
            }
        }
    }


}
