package com.db.assignment.tradestore.jpa.mapper;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.db.assignment.tradestore.constants.ServiceConstants.EXPIRY_FLAG;
import com.db.assignment.tradestore.dto.TradeDTO;
import com.db.assignment.tradestore.dto.TradeIncomingDTO;
import com.db.assignment.tradestore.jpa.entity.TradeEntity;
import com.db.assignment.tradestore.util.DateUtil;

@Component
public class TradeEntityMapper{
	
	DateUtil dateUtil;

	@Autowired
	public TradeEntityMapper(DateUtil dateUtil) {
		this.dateUtil = dateUtil;
	}

	public TradeDTO entityToDto(TradeEntity entity) {
		TradeDTO dto = new TradeDTO();

		dto.setTradeId(entity.getTradeId());
		
		//Preferring String data type for version attribute in DTO, and Integer in Entity to support numeric sorting as default
		dto.setVersion(entity.getVersion().toString());

		dto.setCounterPartyId(entity.getCounterPartyId());

		dto.setBookId(entity.getBookId());

		dto.setMaturityDate(dateUtil.dateToString(entity.getMaturityDate()));

		//Notice that we are not persisting the expiry flag but calculating at runtime 
		// to avoid overhead of a scheduled process to update it if persisted otherwise. 
		//This is unless we have more use-cases, strongly suggesting it to be persisted.
		dto.setCreatedDate(dateUtil.dateToString(entity.getCreatedDate()));

		dto.setExpired(identifyExpiryFlagFromMaturityDate(entity.getMaturityDate()));
		
		return dto;
	}
	
	public TradeEntity dtoToNewEntity(TradeIncomingDTO dto) {
		TradeEntity entity = new TradeEntity();
		
		entity.setTradeId(dto.getTradeId());

		//Converting String to Integer data type for version attribute while saving, to support numeric sorting as default
		entity.setVersion(Integer.parseInt(dto.getVersion()));
		
		entity.setCounterPartyId(dto.getCounterPartyId());
		
		entity.setBookId(dto.getBookId());

		entity.setMaturityDate(dateUtil.stringToDate(dto.getMaturityDate()));

		//If createdDate is not supplied, we assume to use Current date from Server
		if(dto.getCreatedDate()!=null) {
			entity.setCreatedDate(dateUtil.stringToDate(dto.getCreatedDate()));
		}else {
			entity.setCreatedDate(LocalDate.now());
		}
		
		//Not explicitly setting expiry flag, since we are identifying it at runtime during a retrieve operation
		
		return entity;
	}
	
	protected String identifyExpiryFlagFromMaturityDate(LocalDate maturityDate) {
		//Referring expiry flag literal from Constants
		return dateUtil.isPastDate(maturityDate)?EXPIRY_FLAG.Y.toString():EXPIRY_FLAG.N.toString();
	}
}
