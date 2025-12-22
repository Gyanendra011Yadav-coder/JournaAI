package ai.journa.prcontrol.service.integration;

import ai.journa.prcontrol.service.integration.model.MediaJournalist;

import java.util.List;

public interface MediaDatabaseProvider {
    List<MediaJournalist> searchJournalists(String beat, String outlet, String location, String keywords);

    MediaJournalist getJournalistById(String id);
}
