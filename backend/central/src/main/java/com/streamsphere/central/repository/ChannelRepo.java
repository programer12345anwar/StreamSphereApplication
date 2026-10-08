package com.streamsphere.central.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.streamsphere.central.entity.Channel;


@Repository
public interface ChannelRepo extends JpaRepository<Channel, UUID> {

    @Query("SELECT c FROM Channel c WHERE size(c.videos) > 20 AND c.totalSubs > 50")
    List<Channel> findPopularChannels();

    @Query(
            value = "SELECT * FROM channels c WHERE c.total_subs > 50 AND " +
                    "(SELECT COUNT(*) FROM video v WHERE v.channel_id = c.id) > 20",
            nativeQuery = true)
    List<Channel> findPopularChannelsNative();

    Channel findByUserEmail(String email);

}

