package com.db.assignment.tradestore.rest;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.db.assignment.tradestore.dto.TradeDTO;
import com.db.assignment.tradestore.dto.TradeIncomingDTO;
import com.db.assignment.tradestore.service.TradeStoreService;

@RestController
@RequestMapping(path = {"/api/v1/tradestore"}, produces = MediaType.APPLICATION_JSON_VALUE)
public class TradeStoreController {
	
	private static final Logger logger = LoggerFactory.getLogger(TradeStoreController.class);
	
	private static final String TRADE_SAVE_LOG = "Trade was saved with id:{}";
	private static final String TRADE_SAVE_ERROR_LOG = "Service Failure while saving a Trade";
	
	private TradeStoreService tradeStoreService;
	
	@Autowired
	public TradeStoreController(TradeStoreService tradeStoreService) {
		this.tradeStoreService = tradeStoreService;
	}
	
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<TradeDTO> saveTrade(
			@Valid @RequestBody TradeIncomingDTO tradeIncomingDto) {
		
		final TradeDTO savedTrade = tradeStoreService.saveTrade(tradeIncomingDto);
		
		if(savedTrade!=null) {
			logger.info(TRADE_SAVE_LOG, savedTrade.toString());
		}else {
			logger.error(TRADE_SAVE_ERROR_LOG);
		}
		
		return ResponseEntity.status(HttpStatus.CREATED).body(savedTrade);
	}
	
	@GetMapping(path = "/{tradeId}")
	public ResponseEntity<TradeDTO> retrieveTrade(@PathVariable(value = "tradeId") String tradeId) {
		
		final TradeDTO trade = tradeStoreService.retrieveTrade(tradeId);
		
		if (trade==null) {
			return ResponseEntity.notFound().build();
		}
		
		return ResponseEntity.ok(trade);
	}
	
	@GetMapping(path = "/{tradeId}/{version}")
	public ResponseEntity<TradeDTO> retrieveTrade(@PathVariable(value = "tradeId") String tradeId, @PathVariable(value = "version") String version) {
		
		final TradeDTO trade = tradeStoreService.retrieveTrade(tradeId, version);
		
		if (trade==null) {
			return ResponseEntity.notFound().build();
		}
		
		return ResponseEntity.ok(trade);
	}
}
