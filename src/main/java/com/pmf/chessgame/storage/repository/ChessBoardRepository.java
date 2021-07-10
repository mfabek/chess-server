package com.pmf.chessgame.storage.repository;

import com.pmf.chessgame.storage.model.entity.ChessBoardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChessBoardRepository extends JpaRepository<ChessBoardEntity, Long> {
}
