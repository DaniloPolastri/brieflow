package com.briefflow.controller;

import com.briefflow.dto.client.ClientRequestDTO;
import com.briefflow.dto.client.ClientResponseDTO;
import com.briefflow.service.ClientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    public ResponseEntity<ClientResponseDTO> create(
            @Valid @RequestBody ClientRequestDTO dto,
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestAttribute("userId") Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(dto, workspaceId, userId));
    }

    @GetMapping
    public ResponseEntity<List<ClientResponseDTO>> list(
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(clientService.list(workspaceId, search, active));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponseDTO> getById(
            @PathVariable Long id,
            @RequestAttribute("workspaceId") Long workspaceId) {
        return ResponseEntity.ok(clientService.getById(id, workspaceId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody ClientRequestDTO dto,
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(clientService.update(id, dto, workspaceId, userId));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ClientResponseDTO> toggleActive(
            @PathVariable Long id,
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(clientService.toggleActive(id, workspaceId, userId));
    }

    @PostMapping("/{id}/logo")
    public ResponseEntity<ClientResponseDTO> uploadLogo(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(clientService.uploadLogo(id, file, workspaceId, userId));
    }

    @DeleteMapping("/{id}/logo")
    public ResponseEntity<Void> removeLogo(
            @PathVariable Long id,
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestAttribute("userId") Long userId) {
        clientService.removeLogo(id, workspaceId, userId);
        return ResponseEntity.noContent().build();
    }
}
