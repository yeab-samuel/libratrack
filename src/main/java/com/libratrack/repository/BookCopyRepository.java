package com.libratrack.repository;
import com.libratrack.entity.*;
import com.libratrack.enums.CopyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface BookCopyRepository extends JpaRepository<BookCopy,Long> {
    List<BookCopy> findAllByBook(Book book);
    Optional<BookCopy> findFirstByBookAndStatus(Book book,CopyStatus status);
    long countByBookAndStatus(Book book,CopyStatus status);
    boolean existsByCopyNumber(String copyNumber);

    List<BookCopy> findByBookAndStatus(Book book, CopyStatus status);
}
