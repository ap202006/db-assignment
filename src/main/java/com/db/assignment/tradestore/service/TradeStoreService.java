package com.db.assignment.tradestore.service;

import java.time.LocalDate;
import java.util.Optional;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.db.assignment.tradestore.constants.ServiceConstants;
import com.db.assignment.tradestore.dto.TradeDTO;
import com.db.assignment.tradestore.dto.TradeIncomingDTO;
import com.db.assignment.tradestore.exceptions.ValidationFailureException;
import com.db.assignment.tradestore.jpa.entity.TradeEntity;
import com.db.assignment.tradestore.jpa.mapper.TradeEntityMapper;
import com.db.assignment.tradestore.jpa.repository.TradeJpaRepository;
import com.db.assignment.tradestore.util.DateUtil;

@Service
public class TradeStoreService implements ServiceConstants{
	
	private static final Logger logger = LoggerFactory.getLogger(TradeStoreService.class);
	
	TradeEntityMapper tradeEntityMapper;
	
	TradeJpaRepository tradeJpaRepository;
	
	DateUtil dateUtil;

	@Autowired
	public TradeStoreService(TradeEntityMapper tradeEntityMapper, TradeJpaRepository tradeJpaRepository, DateUtil dateUtil) {
		this.tradeEntityMapper = tradeEntityMapper;
		this.tradeJpaRepository = tradeJpaRepository;
		this.dateUtil = dateUtil;
	}

	public TradeDTO saveTrade(@Valid TradeIncomingDTO tradeIncomingDto) {
		TradeEntity entity = null;
		
		//TODO: Code improvisation feasible by refactoring validations to a separate Validator Service Bean class. 
		
		if(!isMaturityDateValid(tradeIncomingDto)) {
			logger.info("Invalid Maturity Date scenario detected for: "+tradeIncomingDto.toString());
			throw new ValidationFailureException("Maturity Date not allowed to be less than current date");
		}
		
		Optional<TradeEntity> optionalForVersion = tradeJpaRepository.findByTradeIdAndVersion(
																	tradeIncomingDto.getTradeId(), 
																	Integer.parseInt(tradeIncomingDto.getVersion()));
		if(optionalForVersion.isPresent()) {

			entity = optionalForVersion.get();
			
			Optional<TradeEntity> optionalForLatestVersion = tradeJpaRepository.findFirstByTradeIdOrderByVersionDesc(tradeIncomingDto.getTradeId());
			
			if(optionalForVersion.get().getVersion().compareTo(optionalForLatestVersion.get().getVersion())==0){
				//Scenario Identified: Update of same record scenario, since supplied version is same as the latest version of respective tradeId in store
				
				//Applying update to limited attributes only, taking an assumption on createdDate to be server date only at the time of new record creation
				// expiry flag is anyway being calculated at runtime only
				entity.setCounterPartyId(tradeIncomingDto.getCounterPartyId());
				entity.setBookId(tradeIncomingDto.getBookId());
				entity.setMaturityDate(dateUtil.stringToDate(tradeIncomingDto.getMaturityDate()));
				
				entity = tradeJpaRepository.save(entity);
				
			}else if(optionalForVersion.get().getVersion().compareTo(optionalForLatestVersion.get().getVersion())<0){
				logger.info("Invalid/Lower Version scenario detected for: "+tradeIncomingDto.toString());
				//Scenario Identified: Not allowed scenario, since supplied version is less than the latest version of respective tradeId in store
				throw new ValidationFailureException("Not allowed scenario, since supplied version is less than the latest version of respective tradeId in store");
			}

		}else {
			//Scenario Identified: New version to be saved scenario, since supplied version of respective tradeId is not available so far in store
			
			entity = tradeEntityMapper.dtoToNewEntity(tradeIncomingDto);
			entity = tradeJpaRepository.save(entity);
		}
		return tradeEntityMapper.entityToDto(entity);
	}

	public TradeDTO retrieveTrade(String tradeId) {
		Optional<TradeEntity> optional = tradeJpaRepository.findFirstByTradeIdOrderByVersionDesc(tradeId);
		if(optional.isPresent()) {
			TradeEntity entity = optional.get();
			return tradeEntityMapper.entityToDto(entity);
		}else {
			//Deferring response type to Controller layer, if no respective record found
			return null;	
		}
	}
	 
	public TradeDTO retrieveTrade(String tradeId, String version) {
		Optional<TradeEntity> optional = tradeJpaRepository.findByTradeIdAndVersion(tradeId, Integer.parseInt(version));
		if(optional.isPresent()) {
			TradeEntity entity = optional.get();
			return tradeEntityMapper.entityToDto(entity);
		}else {
			//Deferring response type to Controller layer, if no respective record found
			return null;	
		}
	}

	protected boolean isMaturityDateValid(TradeIncomingDTO tradeIncomingDto) {
		LocalDate suppliedMaturityDate = dateUtil.stringToDate(tradeIncomingDto.getMaturityDate());
		LocalDate currentDate = LocalDate.now();
		return suppliedMaturityDate.isBefore(currentDate)?false:true;
	}

}
