package com.safesupport.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "application_contacts", joinColumns = @JoinColumn(name = "application_id"))
    private List<Contact> contacts = new ArrayList<>();

    protected ApplicationRecord() {
    }

    public ApplicationRecord(LocalDate effectiveDate, String note, Instant createdAt) {
        this.effectiveDate = effectiveDate;
        this.note = note;
        this.createdAt = createdAt;
    }

    public void addContact(Contact contact) {
        contacts.add(contact);
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

    public List<Contact> getContacts() {
        return contacts;
    }
}
