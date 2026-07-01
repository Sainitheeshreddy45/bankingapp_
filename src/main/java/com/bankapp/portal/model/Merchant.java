package com.bankapp.portal.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.VarbinaryJdbcType;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "merchants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_email", unique = true, nullable = false)
    private String ownerEmail;

    @Column(name = "business_name", nullable = false)
    private String businessName;

    @Column(name = "company_type")
    private String companyType;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "ifsc_code", length = 11)
    private String ifscCode;

    @Column(name = "director_details", columnDefinition = "TEXT")
    private String directorDetails;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 32)
    private KycStatus kycStatus;

    // --- Module 1 Fine-Grained Document Management & Storage ---

    @Column(name = "pan_file_name")
    private String panFileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "pan_status", length = 32)
    private DocStatus panStatus;

    @Column(name = "gst_file_name")
    private String gstFileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "gst_status", length = 32)
    private DocStatus gstStatus;

    @Lob
    @Column(name = "pan_data")
    private byte[] panData;

    @Lob
    @Column(name = "gst_data")
    private byte[] gstData;

    /**
     * Bidirectional relationship mapping to any downstream historical sub-document tables.
     */
    @OneToMany(mappedBy = "merchant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<MerchantDocument> documents = new ArrayList<>();
}