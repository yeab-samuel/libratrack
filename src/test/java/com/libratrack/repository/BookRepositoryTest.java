package com.libratrack.repository;

import com.libratrack.entity.Book;
import com.libratrack.entity.BookCopy;
import com.libratrack.enums.BookCategory;
import com.libratrack.enums.CopyCondition;
import com.libratrack.enums.CopyStatus;
import com.libratrack.specification.BookSpecification;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class BookRepositoryTest {

    @Autowired BookRepository bookRepository;
    @Autowired BookCopyRepository bookCopyRepository;

    // ── 1. search_byTitle_returnsMatchingBook ────────────────────────────────
    @Test
    void search_byTitle_returnsMatchingBook() {
        bookRepository.save(Book.builder()
                .isbn("978-0001").title("Clean Code").author("Robert Martin")
                .category(BookCategory.SCIENCE).publishedYear(2008).totalCopies(1).build());

        Specification<Book> spec = BookSpecification.withFilters("clean", null, null, null, null);
        List<Book> results = bookRepository.findAll(spec);

        assertEquals(1, results.size());
        assertEquals("Clean Code", results.get(0).getTitle());
    }

    // ── 2. search_byAuthor_returnsMatchingBook ───────────────────────────────
    @Test
    void search_byAuthor_returnsMatchingBook() {
        bookRepository.save(Book.builder()
                .isbn("978-0002").title("Clean Code").author("Robert Martin")
                .category(BookCategory.SCIENCE).publishedYear(2008).totalCopies(1).build());

        Specification<Book> spec = BookSpecification.withFilters(null, "robert", null, null, null);
        List<Book> results = bookRepository.findAll(spec);

        assertEquals(1, results.size());
        assertEquals("Robert Martin", results.get(0).getAuthor());
    }

    // ── 3. search_byCategory_returnsOnlyThatCategory ────────────────────────
    @Test
    void search_byCategory_returnsOnlyThatCategory() {
        bookRepository.save(Book.builder()
                .isbn("978-0010").title("Physics 101").author("Author A")
                .category(BookCategory.SCIENCE).publishedYear(2020).totalCopies(1).build());
        bookRepository.save(Book.builder()
                .isbn("978-0011").title("Engineering Basics").author("Author B")
                .category(BookCategory.ENGINEERING).publishedYear(2019).totalCopies(1).build());

        Specification<Book> spec = BookSpecification.withFilters(null, null, BookCategory.SCIENCE, null, null);
        List<Book> results = bookRepository.findAll(spec);

        assertEquals(1, results.size());
        assertEquals(BookCategory.SCIENCE, results.get(0).getCategory());
    }

    // ── 4. search_byPublishedYear_returnsCorrectBook ─────────────────────────
    @Test
    void search_byPublishedYear_returnsCorrectBook() {
        bookRepository.save(Book.builder()
                .isbn("978-0020").title("Refactoring").author("Martin Fowler")
                .category(BookCategory.SCIENCE).publishedYear(2008).totalCopies(1).build());

        // year matches
        Specification<Book> specMatch = BookSpecification.withFilters(null, null, null, 2008, null);
        List<Book> found = bookRepository.findAll(specMatch);
        assertEquals(1, found.size());
        assertEquals(2008, found.get(0).getPublishedYear());

        // year does not match
        Specification<Book> specMiss = BookSpecification.withFilters(null, null, null, 2099, null);
        List<Book> notFound = bookRepository.findAll(specMiss);
        assertTrue(notFound.isEmpty());
    }

    // ── 5. search_byAvailableTrue_excludesFullyLoanedBook ───────────────────
    @Test
    void search_byAvailableTrue_excludesFullyLoanedBook() {
        Book bookA = bookRepository.save(Book.builder()
                .isbn("978-0030").title("Book A").author("Author A")
                .category(BookCategory.SCIENCE).publishedYear(2021).totalCopies(1).build());
        Book bookB = bookRepository.save(Book.builder()
                .isbn("978-0031").title("Book B").author("Author B")
                .category(BookCategory.LAW).publishedYear(2021).totalCopies(1).build());

        bookCopyRepository.save(BookCopy.builder()
                .book(bookA).copyNumber("CA-001")
                .condition(CopyCondition.GOOD).status(CopyStatus.AVAILABLE).build());
        bookCopyRepository.save(BookCopy.builder()
                .book(bookB).copyNumber("CB-001")
                .condition(CopyCondition.GOOD).status(CopyStatus.ON_LOAN).build());

        Specification<Book> spec = BookSpecification.withFilters(null, null, null, null, true);
        List<Book> results = bookRepository.findAll(spec);

        assertEquals(1, results.size());
        assertEquals("978-0030", results.get(0).getIsbn());
    }

    // ── 6. search_noParams_returnsAllBooks ───────────────────────────────────
    @Test
    void search_noParams_returnsAllBooks() {
        bookRepository.save(Book.builder()
                .isbn("978-0040").title("Book One").author("Author 1")
                .category(BookCategory.SCIENCE).publishedYear(2020).totalCopies(1).build());
        bookRepository.save(Book.builder()
                .isbn("978-0041").title("Book Two").author("Author 2")
                .category(BookCategory.LAW).publishedYear(2019).totalCopies(1).build());
        bookRepository.save(Book.builder()
                .isbn("978-0042").title("Book Three").author("Author 3")
                .category(BookCategory.ENGINEERING).publishedYear(2018).totalCopies(1).build());

        Specification<Book> spec = BookSpecification.withFilters(null, null, null, null, null);
        List<Book> results = bookRepository.findAll(spec);

        assertEquals(3, results.size());
    }
}
