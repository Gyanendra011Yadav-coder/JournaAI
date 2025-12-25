package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.User;
import ai.journa.prcontrol.domain.UserClient;
import ai.journa.prcontrol.domain.UserClientAlias;
import ai.journa.prcontrol.dto.ClientRequest;
import ai.journa.prcontrol.dto.ClientResponse;
import ai.journa.prcontrol.dto.RegisterRequest;
import ai.journa.prcontrol.repository.UserClientAliasRepository;
import ai.journa.prcontrol.repository.UserClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClientService {
  private final UserClientRepository userClientRepository;
  private final UserClientAliasRepository userClientAliasRepository;

  public ClientService(UserClientRepository userClientRepository, UserClientAliasRepository userClientAliasRepository) {
    this.userClientRepository = userClientRepository;
    this.userClientAliasRepository = userClientAliasRepository;
  }

  public List<ClientResponse> list(User user) {
    return userClientRepository.findByUser_Id(user.getId()).stream().map(this::toResponse).toList();
  }

  @Transactional
  public ClientResponse create(User user, ClientRequest request) {
    UserClient client = new UserClient();
    client.setUser(user);
    client.setDisplayName(request.getDisplayName());
    client.setShortName(request.getShortName());
    userClientRepository.save(client);
    if (request.getAliases() != null) {
      for (String alias : request.getAliases()) {
        if (alias == null || alias.isBlank()) {
          continue;
        }
        UserClientAlias entity = new UserClientAlias();
        entity.setClient(client);
        entity.setAlias(alias.trim());
        userClientAliasRepository.save(entity);
      }
    }
    return toResponse(client);
  }

  @Transactional
  public ClientResponse update(User user, Long id, ClientRequest request) {
    UserClient client = userClientRepository.findById(id)
        .filter(existing -> existing.getUser().getId().equals(user.getId()))
        .orElseThrow(() -> new IllegalStateException("Client not found"));
    client.setDisplayName(request.getDisplayName());
    client.setShortName(request.getShortName());
    userClientRepository.save(client);
    userClientAliasRepository.deleteAll(userClientAliasRepository.findByClientId(client.getId()));
    if (request.getAliases() != null) {
      for (String alias : request.getAliases()) {
        if (alias == null || alias.isBlank()) {
          continue;
        }
        UserClientAlias entity = new UserClientAlias();
        entity.setClient(client);
        entity.setAlias(alias.trim());
        userClientAliasRepository.save(entity);
      }
    }
    return toResponse(client);
  }

  @Transactional
  public void delete(User user, Long id) {
    UserClient client = userClientRepository.findById(id)
        .filter(existing -> existing.getUser().getId().equals(user.getId()))
        .orElseThrow(() -> new IllegalStateException("Client not found"));
    userClientRepository.delete(client);
  }

  public List<String> listAliases(User user, Long clientId) {
    UserClient client = userClientRepository.findById(clientId)
        .filter(existing -> existing.getUser().getId().equals(user.getId()))
        .orElseThrow(() -> new IllegalStateException("Client not found"));
    return userClientAliasRepository.findByClientId(client.getId()).stream()
        .map(UserClientAlias::getAlias)
        .toList();
  }

  @Transactional
  public List<String> addAlias(User user, Long clientId, String alias) {
    UserClient client = userClientRepository.findById(clientId)
        .filter(existing -> existing.getUser().getId().equals(user.getId()))
        .orElseThrow(() -> new IllegalStateException("Client not found"));
    UserClientAlias entity = new UserClientAlias();
    entity.setClient(client);
    entity.setAlias(alias.trim());
    userClientAliasRepository.save(entity);
    return listAliases(user, clientId);
  }

  @Transactional
  public List<String> updateAlias(User user, Long clientId, Long aliasId, String alias) {
    UserClientAlias existing = userClientAliasRepository.findById(aliasId)
        .orElseThrow(() -> new IllegalStateException("Alias not found"));
    if (!existing.getClient().getId().equals(clientId) || !existing.getClient().getUser().getId().equals(user.getId())) {
      throw new IllegalStateException("Alias not found");
    }
    existing.setAlias(alias.trim());
    userClientAliasRepository.save(existing);
    return listAliases(user, clientId);
  }

  @Transactional
  public List<String> deleteAlias(User user, Long clientId, Long aliasId) {
    UserClientAlias existing = userClientAliasRepository.findById(aliasId)
        .orElseThrow(() -> new IllegalStateException("Alias not found"));
    if (!existing.getClient().getId().equals(clientId) || !existing.getClient().getUser().getId().equals(user.getId())) {
      throw new IllegalStateException("Alias not found");
    }
    userClientAliasRepository.delete(existing);
    return listAliases(user, clientId);
  }

  @Transactional
  public void createFromRegister(User user, RegisterRequest request) {
    if (request.getClients() == null) {
      return;
    }
    for (RegisterRequest.ClientRequest clientRequest : request.getClients()) {
      if (clientRequest.getDisplayName() == null || clientRequest.getDisplayName().isBlank()) {
        continue;
      }
      ClientRequest dto = new ClientRequest();
      dto.setDisplayName(clientRequest.getDisplayName());
      dto.setShortName(clientRequest.getShortName());
      dto.setAliases(clientRequest.getAliases());
      create(user, dto);
    }
  }

  private ClientResponse toResponse(UserClient client) {
    ClientResponse response = new ClientResponse();
    response.setId(client.getId());
    response.setDisplayName(client.getDisplayName());
    response.setShortName(client.getShortName());
    response.setAliases(client.getAliases().stream().map(UserClientAlias::getAlias).toList());
    return response;
  }
}
