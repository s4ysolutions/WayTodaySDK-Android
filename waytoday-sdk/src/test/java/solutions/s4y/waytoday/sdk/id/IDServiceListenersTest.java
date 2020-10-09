package solutions.s4y.waytoday.sdk.id;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class IDServiceListenersTest {
    @Test
    public void idService_canAddAndRemoveListener() {
        assertThat(TrackIDJobService.sListeners.size()).isEqualTo(0);
        ITrackIDChangeListener l = trackID -> {

        };

        TrackIDJobService.addOnTrackIDChangeListener(l);
        assertThat(TrackIDJobService.sListeners.size()).isEqualTo(1);
        TrackIDJobService.removeOnTrackIDChangeListener(l);
        assertThat(TrackIDJobService.sListeners.size()).isEqualTo(0);

    }

    @Test
    public void idService_canAddAndRemoveListeneres() {
        assertThat(TrackIDJobService.sListeners.size()).isEqualTo(0);

        ITrackIDChangeListener l1 = trackID -> {

        };
        ITrackIDChangeListener l2 = trackID -> {

        };

        TrackIDJobService.addOnTrackIDChangeListener(l1);
        assertThat(TrackIDJobService.sListeners.size()).isEqualTo(1);
        TrackIDJobService.addOnTrackIDChangeListener(l2);
        assertThat(TrackIDJobService.sListeners.size()).isEqualTo(2);
        TrackIDJobService.removeOnTrackIDChangeListener(l1);
        assertThat(TrackIDJobService.sListeners.size()).isEqualTo(1);
        TrackIDJobService.removeOnTrackIDChangeListener(l2);
        assertThat(TrackIDJobService.sListeners.size()).isEqualTo(0);
    }

}
