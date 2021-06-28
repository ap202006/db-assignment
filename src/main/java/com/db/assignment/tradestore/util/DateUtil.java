package com.db.assignment.tradestore.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.db.assignment.tradestore.constants.ServiceConstants;

@Component
public class DateUtil implements ServiceConstants{
	
	public String dateToString(LocalDate date) {
		String strDate= date.format(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT));
		return strDate;
	}
	
	public LocalDate stringToDate(String strDate) {
	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT);
	    LocalDate localDate = LocalDate.parse(strDate, formatter);
	    return localDate;
	}
	
	public boolean isPastDate(LocalDate date) {
		LocalDate today = LocalDate.now();
		return date.isBefore(today)?true:false;
	}
}
