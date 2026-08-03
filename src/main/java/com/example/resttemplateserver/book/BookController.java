package com.example.resttemplateserver.book;

import java.net.URI;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/books")
public class BookController {

	private final BookRepository bookRepository;

	public BookController(BookRepository bookRepository) {
		this.bookRepository = bookRepository;
	}

	@GetMapping
	public List<Book> getBooks() {
		return bookRepository.findAll();
	}

	@GetMapping("/{id}")
	public Book getBook(@PathVariable Integer id) {
		return bookRepository.findById(id)
			.orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Book not found: " + id));
	}

	@PostMapping
	public ResponseEntity<Book> createBook(@RequestBody Book book) {
		book.setId(null);
		Book savedBook = bookRepository.save(book);
		return ResponseEntity
			.created(URI.create("/books/" + savedBook.getId()))
			.body(savedBook);
	}

	@PutMapping("/{id}")
	public Book updateBook(@PathVariable Integer id, @RequestBody Book book) {
		Book existingBook = bookRepository.findById(id)
			.orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Book not found: " + id));

		existingBook.setName(book.getName());
		existingBook.setAuthor(book.getAuthor());
		existingBook.setPrice(book.getPrice());

		return bookRepository.save(existingBook);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteBook(@PathVariable Integer id) {
		if (!bookRepository.existsById(id)) {
			throw new ResponseStatusException(NOT_FOUND, "Book not found: " + id);
		}

		bookRepository.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}