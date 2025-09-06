package org.example;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chapters")
public class ChapterDbController {

    private final ChapterRepository repository;

    public ChapterDbController(ChapterRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{title}")
    public Chapter getChapter(@PathVariable String title) {
        return repository.findByTitle(title);
    }
}
