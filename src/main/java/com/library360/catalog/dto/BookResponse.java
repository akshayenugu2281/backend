package com.library360.catalog.dto;

import com.library360.catalog.Author;
import com.library360.catalog.Book;
import com.library360.catalog.Category;
import com.library360.catalog.Publisher;

import java.util.Comparator;
import java.util.List;

public record BookResponse(
    Long id,
    String isbn13,
    String title,
    String subtitle,
    String edition,
    Short publishYear,
    String language,
    Integer pages,
    String description,
    String coverUrl,
    int availableCopies,
    int totalCopies,
    int issuedCopies,
    int onHoldCopies,
    int waitlistCount,
    CategorySummary category,
    PublisherSummary publisher,
    List<AuthorSummary> authors
) {
    public record CategorySummary(Long id, String name, String slug) {}
    public record PublisherSummary(Long id, String name, String website) {}
    public record AuthorSummary(Long id, String fullName, String bio) {}

    /** Copy counts computed from persisted copy rows — never hand-edited. */
    public record Availability(int total, int available, int issued, int onHold, int waitlist) {
        public static Availability empty() { return new Availability(0, 0, 0, 0, 0); }
    }

    public static BookResponse from(Book book, int availableCopies) {
        return from(book, new Availability(0, availableCopies, 0, 0, 0));
    }

    public static BookResponse from(Book book, Availability availability) {
        Category category = book.getCategory();
        Publisher publisher = book.getPublisher();
        List<AuthorSummary> authors = book.getAuthors().stream()
            .sorted(Comparator.comparing(Author::getFullName, String.CASE_INSENSITIVE_ORDER))
            .map(author -> new AuthorSummary(author.getId(), author.getFullName(), author.getBio()))
            .toList();
        return new BookResponse(
            book.getId(), book.getIsbn13(), book.getTitle(), book.getSubtitle(), book.getEdition(),
            book.getPublishYear(), book.getLanguage(), book.getPages(), book.getDescription(), book.getCoverUrl(),
            availability.available(), availability.total(), availability.issued(), availability.onHold(), availability.waitlist(),
            category == null ? null : new CategorySummary(category.getId(), category.getName(), category.getSlug()),
            publisher == null ? null : new PublisherSummary(publisher.getId(), publisher.getName(), publisher.getWebsite()),
            authors
        );
    }
}
