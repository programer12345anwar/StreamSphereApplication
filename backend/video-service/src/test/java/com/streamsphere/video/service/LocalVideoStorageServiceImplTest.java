package com.streamsphere.video.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalVideoStorageServiceImplTest {

    @TempDir
    Path storageDirectory;

    @Test
    void missingLocalMediaReturnsNotFound() {
        LocalVideoStorageServiceImpl storage = new LocalVideoStorageServiceImpl();
        ReflectionTestUtils.setField(storage, "storageLocation", storageDirectory.toString());
        storage.init();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> storage.loadVideoAsResource("missing.mp4"));

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void rejectsPathsOutsideTheStorageDirectory() {
        LocalVideoStorageServiceImpl storage = new LocalVideoStorageServiceImpl();
        ReflectionTestUtils.setField(storage, "storageLocation", storageDirectory.toString());
        storage.init();

        assertThrows(IllegalArgumentException.class, () -> storage.loadVideoAsResource("..\\secret.mp4"));
    }
}
