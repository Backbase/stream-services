package com.backbase.stream.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CdpTaskTest {

    @Test
    void testCdpTask() {
        CdpTask cdpTask = new CdpTask();
        assertThat(cdpTask.getName()).isEqualTo("cdpProfilesIngestionTask");
    }

}
