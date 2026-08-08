package cn.maian.media.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MediaGarbageCollector {

    private final MediaStorageService mediaStorageService;

    public MediaGarbageCollector(MediaStorageService mediaStorageService) {
        this.mediaStorageService = mediaStorageService;
    }

    @Scheduled(initialDelayString = "${app.media.gc-initial-delay-ms:300000}", fixedDelayString = "${app.media.gc-interval-ms:3600000}")
    public void collect() {
        mediaStorageService.garbageCollectOrphans();
    }
}
