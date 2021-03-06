package dev.spring.framework.springstudy.book;

import javax.annotation.PostConstruct;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Setter
@NoArgsConstructor
@Service
public class BookService {

    private BookRepository bookRepository;

    @Autowired
    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public Book save(Book bookInput) {
        Book book = new Book();
        return bookRepository.save(book);
    }

    @PostConstruct
    public void postConstruct() {
        System.out.println("===============");
        System.out.println("Hello");
    }
}
