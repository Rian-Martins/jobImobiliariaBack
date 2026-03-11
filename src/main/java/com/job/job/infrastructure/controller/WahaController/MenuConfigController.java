package com.job.job.infrastructure.controller.WahaController;

import com.job.job.domain.model.wahaEntities.MenuOption;
import com.job.job.infrastructure.persistence.ChatStatePersistance.MenuOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/waha/config/menu")
@RequiredArgsConstructor
public class MenuConfigController {

    private final MenuOptionRepository repository;

    @PostMapping
    public ResponseEntity<MenuOption> criarOpcao(@RequestBody MenuOption option) {
        return ResponseEntity.ok(repository.save(option));
    }

    @GetMapping
    public ResponseEntity<List<MenuOption>> listarOpcoes() {
        return ResponseEntity.ok(repository.findAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarOpcao(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
