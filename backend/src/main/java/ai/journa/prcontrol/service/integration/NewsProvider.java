package ai.journa.prcontrol.service.integration;

import ai.journa.prcontrol.domain.ProviderType;

public interface NewsProvider {
  ProviderType type();

  FetchResult fetch(FetchRequest request, String apiKey) throws ProviderException;
}
