package com.amazonaws.lambda.app;

import java.io.Serializable;

public class Mail implements Serializable {

    private String subject;
    private String body;
    Mail(){}
    public String getSubject() {
        return subject;
    }
    public void setSubject(String subject) {
        this.subject = subject;
    }
    public String getBody() {
        return body;
    }
    public void setBody(String body) {
        this.body = body;
    }
}
