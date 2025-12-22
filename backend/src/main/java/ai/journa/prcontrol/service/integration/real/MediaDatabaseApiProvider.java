package ai.journa.prcontrol.service.integration.real;

import ai.journa.prcontrol.service.integration.MediaDatabaseProvider;
import ai.journa.prcontrol.service.integration.model.MediaJournalist;

import java.util.Collections;
import java.util.List;

public class MediaDatabaseApiProvider implements MediaDatabaseProvider {
    @Override
    public List<MediaJournalist> searchJournalists(String beat, String outlet, String location, String keywords) {
        String cisionKey = System.getenv("CISION_API_KEY");
        String muckrackKey = System.getenv("MUCKRACK_API_KEY");
        if ((cisionKey == null || cisionKey.isBlank()) && (muckrackKey == null || muckrackKey.isBlank())) {
            throw new IllegalStateException("CISION_API_KEY or MUCKRACK_API_KEY is not configured");
        }
        return Collections.emptyList();
    }

    @Override
    public MediaJournalist getJournalistById(String id) {
        return null;
    }
}
