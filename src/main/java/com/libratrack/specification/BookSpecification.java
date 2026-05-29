package com.libratrack.specification;
import com.libratrack.entity.*;
import com.libratrack.enums.*;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import java.util.*;
public class BookSpecification {
    private BookSpecification(){}
    public static Specification<Book> withFilters(String title,String author,BookCategory category,Integer publishedYear,Boolean available){
        return(root,query,cb)->{
            List<Predicate> ps=new ArrayList<>();
            if(title!=null&&!title.isBlank()) ps.add(cb.like(cb.lower(root.get("title")),"%" +title.toLowerCase()+"%"));
            if(author!=null&&!author.isBlank()) ps.add(cb.like(cb.lower(root.get("author")),"%" +author.toLowerCase()+"%"));
            if(category!=null) ps.add(cb.equal(root.get("category"),category));
            if(publishedYear!=null) ps.add(cb.equal(root.get("publishedYear"),publishedYear));
            if(Boolean.TRUE.equals(available)){
                Subquery<Long> sub=query.subquery(Long.class);
                Root<BookCopy> cr=sub.from(BookCopy.class);
                sub.select(cb.count(cr)).where(cb.and(cb.equal(cr.get("book"),root),cb.equal(cr.get("status"),CopyStatus.AVAILABLE)));
                ps.add(cb.greaterThan(sub,0L));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }
}
