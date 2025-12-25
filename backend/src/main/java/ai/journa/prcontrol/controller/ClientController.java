package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.dto.ClientAliasRequest;
import ai.journa.prcontrol.dto.ClientRequest;
import ai.journa.prcontrol.dto.ClientResponse;
import ai.journa.prcontrol.service.ClientService;
import ai.journa.prcontrol.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/me/clients")
public class ClientController {
  private final ClientService clientService;
  private final CurrentUserService currentUserService;

  public ClientController(ClientService clientService, CurrentUserService currentUserService) {
    this.clientService = clientService;
    this.currentUserService = currentUserService;
  }

  @GetMapping
  public List<ClientResponse> list() {
    return clientService.list(currentUserService.requireCurrentUser());
  }

  @PostMapping
  public ClientResponse create(@Valid @RequestBody ClientRequest request) {
    return clientService.create(currentUserService.requireCurrentUser(), request);
  }

  @PutMapping("/{id}")
  public ClientResponse update(@PathVariable Long id, @Valid @RequestBody ClientRequest request) {
    return clientService.update(currentUserService.requireCurrentUser(), id, request);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    clientService.delete(currentUserService.requireCurrentUser(), id);
  }

  @GetMapping("/{id}/aliases")
  public List<String> listAliases(@PathVariable Long id) {
    return clientService.listAliases(currentUserService.requireCurrentUser(), id);
  }

  @PostMapping("/{id}/aliases")
  public List<String> addAlias(@PathVariable Long id, @Valid @RequestBody ClientAliasRequest request) {
    return clientService.addAlias(currentUserService.requireCurrentUser(), id, request.getAlias());
  }

  @PutMapping("/{id}/aliases/{aliasId}")
  public List<String> updateAlias(@PathVariable Long id, @PathVariable Long aliasId, @Valid @RequestBody ClientAliasRequest request) {
    return clientService.updateAlias(currentUserService.requireCurrentUser(), id, aliasId, request.getAlias());
  }

  @DeleteMapping("/{id}/aliases/{aliasId}")
  public List<String> deleteAlias(@PathVariable Long id, @PathVariable Long aliasId) {
    return clientService.deleteAlias(currentUserService.requireCurrentUser(), id, aliasId);
  }
}
