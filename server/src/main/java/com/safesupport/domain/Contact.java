package com.safesupport.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Contact {

    @Column(name = "contact_name", nullable = false, length = 20)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(length = 120)
    private String email;

    /** 实际发送地址：家属邮箱，或兜底的测试邮箱 */
    @Column(length = 120)
    private String target;

    /** SENT / SKIPPED / FAILED */
    @Column(length = 20)
    private String status;

    @Column(length = 200)
    private String message;

    protected Contact() {
    }

    public Contact(String name, String phone, String email) {
        this.name = name;
        this.phone = phone;
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
