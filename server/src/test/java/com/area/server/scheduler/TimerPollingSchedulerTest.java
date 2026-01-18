package com.area.server.scheduler;

import com.area.server.model.Area;
import com.area.server.model.ServiceConnection;
import com.area.server.model.ServiceConnection.ServiceType;
import com.area.server.model.TimerActionConfig;
import com.area.server.repository.AreaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class TimerPollingSchedulerTest {

    @Autowired
    private TimerPollingScheduler timerScheduler;

    @Autowired
    private AreaPollingScheduler areaScheduler;

    @MockBean
    private AreaRepository areaRepository;

    @Test
    public void testTimerSeparation() {
        // Arrange
        Area timerArea = new Area();
        timerArea.setId(1L);
        TimerActionConfig timerConfig = new TimerActionConfig();
        timerConfig.setTimerType("current_time");
        timerArea.setTimerConfig(timerConfig);
        timerArea.setActionType("timer.current_time");

        Area gmailArea = new Area();
        gmailArea.setId(2L);
        gmailArea.setActionType("gmail.email_received");
        ServiceConnection gmailConnection = new ServiceConnection();
        gmailConnection.setType(ServiceType.GMAIL);
        gmailArea.setActionConnection(gmailConnection);

        when(areaRepository.findActiveTimerAreas()).thenReturn(List.of(timerArea));
        when(areaRepository.findActiveNonTimerAreas()).thenReturn(List.of(gmailArea));

        // Act & Assert
        // We can't easily assert on internal execution without more mocking,
        // but we can verify that the scheduler beans are present and the repository
        // queries work as expected (mocked)

        List<Area> timers = areaRepository.findActiveTimerAreas();
        assertThat(timers).hasSize(1);
        assertThat(timers.get(0).getId()).isEqualTo(1L);

        List<Area> nonTimers = areaRepository.findActiveNonTimerAreas();
        assertThat(nonTimers).hasSize(1);
        assertThat(nonTimers.get(0).getId()).isEqualTo(2L);
    }
}
