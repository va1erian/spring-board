package com.example.board.jpa;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface StoredNoteRepository extends CrudRepository<StoredNote, Long> {
    @Modifying
    @Query("update StoredNote sn set sn.x = ?1, sn.y = ?2, sn.angle = ?3 where sn.id = ?4")
    int setPositionForNote(Integer x, Integer y, Float angle, Long id);
}
