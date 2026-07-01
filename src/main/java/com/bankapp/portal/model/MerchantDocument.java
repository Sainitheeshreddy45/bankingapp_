package com.bankapp.portal.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "merchant_documents",
        indexes = {
                @Index(name = "idx_doc_lookup", columnList = "merchant_id, document_type")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Back-reference to parent merchant container profile.
     * Declared LAZY to maximize performance during criteria searches.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(name = "document_type", nullable = false, length = 32)
    private String documentType; // Matches your service parameters: e.g., "PAN", "GST"

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "status", nullable = false, length = 32)
    private String status; // DRAFT, PENDING, APPROVED, REJECTED

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;
}