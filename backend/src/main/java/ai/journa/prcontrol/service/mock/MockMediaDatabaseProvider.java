package ai.journa.prcontrol.service.mock;

import ai.journa.prcontrol.service.integration.MediaDatabaseProvider;
import ai.journa.prcontrol.service.integration.model.MediaJournalist;

import java.util.ArrayList;
import java.util.List;

public class MockMediaDatabaseProvider implements MediaDatabaseProvider {
    @Override
    public List<MediaJournalist> searchJournalists(String beat, String outlet, String location, String keywords) {
        List<MediaJournalist> journalists = new ArrayList<>();
        for (int i = 1; i <= 2; i++) {
            MediaJournalist journalist = new MediaJournalist();
            journalist.setId("mock-" + beat + "-" + i);
            journalist.setName("Alex Reporter " + i);
            journalist.setOutlet(outlet != null ? outlet : "Daily Ledger");
            journalist.setBeatTags(beat + ",policy");
            journalist.setLocation(location != null ? location : "New York");
            journalist.setEmail("alex" + i + "@dailyledger.com");
            journalists.add(journalist);
        }
        return journalists;
    }

    @Override
    public MediaJournalist getJournalistById(String id) {
        MediaJournalist journalist = new MediaJournalist();
        journalist.setId(id);
        journalist.setName("Alex Reporter");
        journalist.setOutlet("Daily Ledger");
        journalist.setBeatTags("taxation,policy");
        journalist.setLocation("New York");
        journalist.setEmail("alex@dailyledger.com");
        return journalist;
    }
}
