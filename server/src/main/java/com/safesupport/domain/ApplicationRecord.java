package com.safesupport.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "applications")
public class ApplicationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(length = 200)
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // 允许为空：兼容升级前已存在的数据（视作 PENDING / 0 次重试）
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RecordStatus status = RecordStatus.PENDING;

    /** 已尝试发送的轮次，用于失败重试上限 */
    private Integer attempts = 0;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "application_contacts", joinColumns = @JoinColumn(name = "application_id"))
    private List<Contact> contacts = new ArrayList<>();

    protected ApplicationRecord() {
    }

    public ApplicationRecord(LocalDate effectiveDate, String note, Instant createdAt) {
        this.effectiveDate = effectiveDate;
        this.note = note;
        this.createdAt = createdAt;
        this.status = RecordStatus.PENDING;
    }

    public void addContact(Contact contact) {
        contacts.add(contact);
    }

    public void markAttempt(Instant at) {
        this.attempts = getAttempts() + 1;
        this.lastAttemptAt = at;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public RecordStatus getStatus() {
        return status == null ? RecordStatus.PENDING : status;
    }

    public void setStatus(RecordStatus status) {
        this.status = status;
    }

    public int getAttempts() {
        return attempts == null ? 0 : attempts;
    }

    public Instant getLastAttemptAt() {
        return lastAttemptAt;
    }

    public List<Contact> getContacts() {
        return contacts;
    }
}
