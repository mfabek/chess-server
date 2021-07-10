package com.pmf.chessgame.storage.repository;

import com.pmf.chessgame.storage.model.entity.GameEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameEntityRepository extends JpaRepository<GameEntity, Long> {
}
