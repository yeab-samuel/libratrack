package com.libratrack.repository;
import com.libratrack.entity.Book;
import org.springframework.data.jpa.repository.*;
public interface BookRepository extends JpaRepository<Book,Long>,JpaSpecificationExecutor<Book> {
    boolean existsByIsbn(String isbn);
}
