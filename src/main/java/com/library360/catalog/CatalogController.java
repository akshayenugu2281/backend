package com.library360.catalog;

import com.library360.catalog.dto.BookRequest;
import com.library360.catalog.dto.BookResponse;
import com.library360.catalog.dto.BookCopyRequest;
import com.library360.catalog.BookCopyRepository;
import com.library360.catalog.CopyStatus;
import com.library360.common.BusinessException;
import com.library360.common.ErrorCode;
import com.library360.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final CategoryRepository categoryRepository;
    private final PublisherRepository publisherRepository;
    private final AuthorRepository authorRepository;
    private final com.library360.reservation.ReservationRepository reservationRepository;

    public record CopyResponse(Long id, String accessionNumber, String barcode, String shelf,
                               String condition, CopyStatus status, String notes) {
        static CopyResponse from(BookCopy copy) {
            return new CopyResponse(copy.getId(), copy.getAccessionNumber(), copy.getBarcode(),
                copy.getShelf(), copy.getCondition() == null ? null : copy.getCondition().name(),
                copy.getStatus(), copy.getNotes());
        }
    }

    public record CopyStatusRequest(
        @jakarta.validation.constraints.NotNull CopyStatus status,
        @jakarta.validation.constraints.Size(max = 300) String note) {}

    @GetMapping("/books")
    @Transactional(readOnly = true)
    public ResponseEntity<PageResponse<BookResponse>> searchBooks(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String language,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Book> books = bookRepository.search(normalizeQuery(q), categoryId, normalizeQuery(language),
            PageRequest.of(Math.max(page, 0), boundedSize(size), Sort.by("title")));
        java.util.Map<Long, BookResponse.Availability> availability = availabilityFor(
            books.getContent().stream().map(Book::getId).toList());
        return ResponseEntity.ok(PageResponse.from(books, book -> toResponse(book, availability)));
    }

    @GetMapping("/books/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<BookResponse> getBook(@PathVariable Long id) {
        Book book = bookRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND));
        return ResponseEntity.ok(toResponse(book, availabilityFor(java.util.List.of(id))));
    }

    /** Staff view of every physical copy of a title: accession number, barcode, status, shelf. */
    @GetMapping("/books/{id}/copies")
    @PreAuthorize("hasAuthority('book:write')")
    @Transactional(readOnly = true)
    public ResponseEntity<java.util.List<CopyResponse>> listCopies(@PathVariable Long id) {
        Book book = bookRepository.findById(id)
            .filter(existing -> !existing.isDeleted())
            .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND));
        return ResponseEntity.ok(bookCopyRepository.findByBookId(book.getId()).stream()
            .sorted(java.util.Comparator.comparing(c -> c.getAccessionNumber() == null ? "" : c.getAccessionNumber()))
            .map(CopyResponse::from).toList());
    }

    /**
     * Staff copy-lifecycle operation with validated transitions. Copies on loan or on hold
     * must be returned / released first; history is preserved via the notes field.
     */
    @PatchMapping("/copies/{barcode}/status")
    @PreAuthorize("hasAuthority('book:write')")
    @Transactional
    public ResponseEntity<CopyResponse> changeCopyStatus(
            @PathVariable String barcode, @Valid @RequestBody CopyStatusRequest request) {
        BookCopy copy = bookCopyRepository.findByBarcode(barcode.trim().toUpperCase(Locale.ROOT))
            .orElseThrow(() -> new BusinessException(ErrorCode.COPY_NOT_FOUND));
        CopyStatus from = copy.getStatus();
        CopyStatus to = request.status();
        // Validated lifecycle: circulation owns AVAILABLE <-> ON_LOAN/ON_HOLD.
        // Staff may retire a free copy, recover it, or withdraw it permanently.
        // Copies on loan/on hold must be returned/released first; WITHDRAWN is terminal.
        boolean allowed;
        if (from == CopyStatus.AVAILABLE) {
            allowed = to == CopyStatus.LOST || to == CopyStatus.DAMAGED
                || to == CopyStatus.MAINTENANCE || to == CopyStatus.WITHDRAWN;
        } else if (from == CopyStatus.LOST || from == CopyStatus.DAMAGED || from == CopyStatus.MAINTENANCE) {
            allowed = to == CopyStatus.AVAILABLE || to == CopyStatus.WITHDRAWN;
        } else {
            allowed = false;
        }
        if (!allowed) {
            throw new BusinessException(ErrorCode.COPY_NOT_AVAILABLE);
        }
        copy.setStatus(to);
        if (request.note() != null && !request.note().isBlank()) {
            String entry = java.time.LocalDate.now() + ": " + request.note().trim();
            copy.setNotes(copy.getNotes() == null || copy.getNotes().isBlank()
                ? entry : copy.getNotes() + " | " + entry);
        }
        return ResponseEntity.ok(CopyResponse.from(bookCopyRepository.save(copy)));
    }

    @PostMapping("/books")
    @PreAuthorize("hasAuthority('book:write')")
    @Transactional
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody BookRequest request) {
        String isbn = blankToNull(request.isbn13());
        if (isbn != null && bookRepository.existsByIsbn13(isbn)) {
            throw new BusinessException(ErrorCode.ISBN_EXISTS);
        }
        Book book = applyRequest(new Book(), request);
        Book saved = bookRepository.save(book);
        if (request.initialCopyBarcode() != null && !request.initialCopyBarcode().isBlank()) {
            createCopy(saved, request.initialCopyBarcode(), request.initialCopyShelf(), null);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @PutMapping("/books/{id}")
    @PreAuthorize("hasAuthority('book:write')")
    @Transactional
    public ResponseEntity<BookResponse> updateBook(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
        Book book = bookRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND));
        String isbn = blankToNull(request.isbn13());
        if (isbn != null && !isbn.equals(book.getIsbn13()) && bookRepository.existsByIsbn13(isbn)) {
            throw new BusinessException(ErrorCode.ISBN_EXISTS);
        }
        return ResponseEntity.ok(toResponse(bookRepository.save(applyRequest(book, request))));
    }

    @PostMapping("/books/{id}/copies")
    @PreAuthorize("hasAuthority('book:write')")
    @Transactional
    public ResponseEntity<BookResponse> addCopy(@PathVariable Long id, @Valid @RequestBody BookCopyRequest request) {
        Book book = bookRepository.findById(id)
            .filter(existing -> !existing.isDeleted())
            .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND));
        createCopy(book, request.barcode(), request.shelf(), request.notes());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(book));
    }

    @DeleteMapping("/books/{id}")
    @PreAuthorize("hasAuthority('book:write')")
    @Transactional
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        Book book = bookRepository.findById(id)
            .filter(existing -> !existing.isDeleted())
            .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND));
        book.setDeleted(true);
        bookRepository.save(book);
        return ResponseEntity.noContent().build();
    }

    private void createCopy(Book book, String rawBarcode, String shelf, String notes) {
        String barcode = rawBarcode.trim().toUpperCase(Locale.ROOT);
        if (bookCopyRepository.existsByBarcode(barcode)) {
            throw new BusinessException(ErrorCode.COPY_BARCODE_EXISTS);
        }
        BookCopy copy = new BookCopy();
        copy.setBook(book);
        copy.setBarcode(barcode);
        // Temporary unique value satisfies NOT NULL + UNIQUE under concurrency;
        // the final deterministic number is assigned once the id exists.
        copy.setAccessionNumber(AccessionNumbers.temporary());
        copy.setShelf(blankToNull(shelf));
        copy.setNotes(blankToNull(notes));
        copy.setCondition(CopyCondition.GOOD);
        copy.setStatus(CopyStatus.AVAILABLE);
        BookCopy saved = bookCopyRepository.saveAndFlush(copy);
        saved.setAccessionNumber(AccessionNumbers.format(
            AccessionNumbers.yearOf(saved.getAcquiredOn(), java.time.LocalDate.now()), saved.getId()));
    }

    private BookResponse toResponse(Book book) {
        return toResponse(book, availabilityFor(java.util.List.of(book.getId())));
    }

    private BookResponse toResponse(Book book, java.util.Map<Long, BookResponse.Availability> availability) {
        return BookResponse.from(book, availability.getOrDefault(book.getId(), BookResponse.Availability.empty()));
    }

    /** One grouped query per dimension — no per-book N+1 fan-out on search pages. */
    private java.util.Map<Long, BookResponse.Availability> availabilityFor(java.util.List<Long> bookIds) {
        java.util.Map<Long, long[]> counts = new java.util.HashMap<>();
        java.util.Map<Long, Long> waitlist = new java.util.HashMap<>();
        if (bookIds == null || bookIds.isEmpty()) {
            return java.util.Map.of();
        }
        for (Object[] row : bookCopyRepository.countByBookIdsGrouped(bookIds)) {
            Long id = (Long) row[0];
            CopyStatus status = (CopyStatus) row[1];
            long n = (Long) row[2];
            long[] slot = counts.computeIfAbsent(id, k -> new long[4]);
            slot[0] += n;
            if (status == CopyStatus.AVAILABLE) slot[1] += n;
            else if (status == CopyStatus.ON_LOAN) slot[2] += n;
            else if (status == CopyStatus.ON_HOLD) slot[3] += n;
        }
        for (Object[] row : reservationRepository.countEligibleQueuedByBookIds(bookIds)) {
            waitlist.put((Long) row[0], (Long) row[1]);
        }
        java.util.Map<Long, BookResponse.Availability> result = new java.util.HashMap<>();
        for (Long id : bookIds) {
            long[] slot = counts.getOrDefault(id, new long[4]);
            result.put(id, new BookResponse.Availability(
                (int) slot[0], (int) slot[1], (int) slot[2], (int) slot[3],
                waitlist.getOrDefault(id, 0L).intValue()));
        }
        return result;
    }

    private Book applyRequest(Book book, BookRequest request) {
        book.setIsbn13(blankToNull(request.isbn13()));
        book.setTitle(request.title().trim());
        book.setSubtitle(blankToNull(request.subtitle()));
        book.setEdition(blankToNull(request.edition()));
        book.setPublishYear(request.publishYear() == null ? null : request.publishYear().shortValue());
        book.setLanguage(request.language() == null || request.language().isBlank() ? "en" : request.language().trim());
        book.setPages(request.pages());
        book.setDescription(blankToNull(request.description()));
        book.setCoverUrl(blankToNull(request.coverUrl()));

        if (request.categoryId() == null) {
            book.setCategory(null);
        } else {
            book.setCategory(categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND)));
        }
        if (request.publisherId() == null) {
            book.setPublisher(null);
        } else {
            book.setPublisher(publisherRepository.findById(request.publisherId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PUBLISHER_NOT_FOUND)));
        }
        if (request.authorIds() == null || request.authorIds().isEmpty()) {
            book.getAuthors().clear();
        } else {
            var authors = authorRepository.findAllById(request.authorIds());
            if (authors.size() != request.authorIds().stream().distinct().count()) {
                throw new BusinessException(ErrorCode.AUTHOR_NOT_FOUND);
            }
            book.getAuthors().clear();
            book.getAuthors().addAll(authors);
        }
        return book;
    }

    private static int boundedSize(int size) {
        return Math.max(1, Math.min(size, 100));
    }

    private static String normalizeQuery(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
