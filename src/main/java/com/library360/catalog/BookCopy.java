package com.library360.catalog;

import com.library360.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "book_copies")
@Getter @Setter
@NoArgsConstructor
public class BookCopy extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id")
    private Book book;

    @Column(nullable = false, unique = true, length = 40)
    private String barcode;

    @Column(name = "accession_number", unique = true, length = 40)
    private String accessionNumber;

    @Column(length = 40)
    private String shelf;

    @Enumerated(EnumType.STRING)
    @Column(name = "`condition`", nullable = false)
    private CopyCondition condition = CopyCondition.GOOD;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CopyStatus status = CopyStatus.AVAILABLE;

    @Column(name = "acquired_on")
    private LocalDate acquiredOn;

    @Column(length = 300)
    private String notes;
}
