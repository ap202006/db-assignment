package com.db.assignment.tradestore.jpa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.db.assignment.tradestore.jpa.entity.TradeEntity;

@Repository
public interface TradeJpaRepository extends JpaRepository<TradeEntity, Long>, JpaSpecificationExecutor<TradeEntity>  {

	Optional<TradeEntity> findFirstByTradeIdOrderByVersionDesc(String tradeId);
	
	Optional<TradeEntity> findByTradeIdAndVersion(String tradeId, Integer version);
}

