package com.streamsphere.central.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.streamsphere.central.entity.Tag;
import java.util.UUID;

@Repository
public interface TagRepo extends JpaRepository<Tag, UUID> {
    public Tag findByName(String tagName);
}

