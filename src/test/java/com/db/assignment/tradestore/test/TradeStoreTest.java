package com.db.assignment.tradestore.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.db.assignment.tradestore.SpringBootWebApplication;
import com.db.assignment.tradestore.dto.TradeDTO;
import com.db.assignment.tradestore.dto.TradeIncomingDTO;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootWebApplication.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TradeStoreTest {
	
	public static final String TEST_API_PATH="/api/v1/tradestore";
	
	public static final String DEFAULT_DATE_FORMAT = "dd/MM/yyyy";
	
    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    private String getApiUrl() {
        return "http://localhost:" + port + TEST_API_PATH;
    }
    
	@Test
	@Order(1)
	public void contextLoads() {
	}
	
    @Test
    @Order(2)
    public void testSaveNewTradeWithValidCreatedDate() {

    	//Ignoring the rare midnight data change scenario at test execution time, where milliseconds that may occur between save and retrieve operation within this test case may cause a change of calendar date
    	LocalDate todaysDate = LocalDate.now();
    	LocalDate sampleFutureMaturityDate = todaysDate.plusDays(1);
    	String sampleFutureMaturityDateStr = sampleFutureMaturityDate.format(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT));
    	String todaysDateStr = todaysDate.format(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT));
    	
    	saveTradeAndAssertBasic("T1","1","CP-1","B1",sampleFutureMaturityDateStr); 
    	saveTradeAndAssertBasic("T2","1","CP-2","B2",sampleFutureMaturityDateStr);
    	
        //Assert by retrieving subsequently that both the records are saved as new separate records respectively, and all fields matching appropriately
        TradeDTO trade1Refetched = retrieveSavedTrade("T1","1");
        TradeDTO trade2Refetched = retrieveSavedTrade("T2","1");

    	assertEquals(trade1Refetched.getTradeId(), "T1");
    	assertEquals(trade1Refetched.getVersion(), "1");
    	assertEquals(trade1Refetched.getCounterPartyId(), "CP-1");
    	assertEquals(trade1Refetched.getBookId(), "B1");
    	assertEquals(trade1Refetched.getMaturityDate(), sampleFutureMaturityDateStr);
    	//Asset that createdDate should be today only
    	assertEquals(trade1Refetched.getCreatedDate(), todaysDateStr);
        
    	assertEquals(trade2Refetched.getTradeId(), "T2");
    	assertEquals(trade2Refetched.getVersion(), "1");
    	assertEquals(trade2Refetched.getCounterPartyId(), "CP-2");
    	assertEquals(trade2Refetched.getBookId(), "B2");
    	assertEquals(trade2Refetched.getMaturityDate(), sampleFutureMaturityDateStr);
    	//Asset that createdDate should be today only
    	assertEquals(trade2Refetched.getCreatedDate(), todaysDateStr);

    }

    @Test
    @Order(3)
    public void testSaveTradeHigherVersionAllowedAndNewRecordCreated() {

    	String sampleFutureMaturityDateStr = provideSampleFutureMaturityDate();
    	
    	//Saving Trade version 1
        saveTradeAndAssertBasic("T3","1","CP-1","B1",sampleFutureMaturityDateStr);
        
        //Attempting to Save Trade with higher version
        saveTradeAndAssertBasic("T3","2","CP-1","B2",sampleFutureMaturityDateStr);
        
        //Fetch previously saved version again to assert of it also exists, i.e. v2 is saved as a new record
        TradeDTO tradeV1Refetched = retrieveSavedTrade("T3","1");
        assertEquals(tradeV1Refetched.getVersion(), "1");

        TradeDTO tradeV2Refetched = retrieveSavedTrade("T3","2");
        assertEquals(tradeV2Refetched.getVersion(), "2");

    }
    
    @Test
    @Order(4)
    public void testUpdateTradeSameVersionAllowedAndSameRecordUpdated() {

    	String sampleFutureMaturityDateStr = provideSampleFutureMaturityDate();
    	
    	//Saving Trade version 1
        saveTradeAndAssertBasic("T4","1","CP-1","B1",sampleFutureMaturityDateStr);
        
        //Attempting to Save Trade with same version and changed bookId
        saveTradeAndAssertBasic("T4","1","CP-1","B2",sampleFutureMaturityDateStr);
        
        //Fetch saved trade object again to assert if it contains the latest value only for recently updated attributes
        TradeDTO tradeV1UpdatedRefetched = retrieveSavedTrade("T4","1");
        
        assertEquals(tradeV1UpdatedRefetched.getVersion(), "1");
        
        //Asserting with recent updated value  of bookId
        assertEquals(tradeV1UpdatedRefetched.getBookId(), "B2");
        
    }
    
    @Test
    @Order(5)
    public void testUpdateTradeLowerVersionNotAllowed() {
    	String sampleFutureMaturityDateStr = provideSampleFutureMaturityDate();
    	
    	//Saving Trade version 1
        saveTradeAndAssertBasic("T5","1","CP-1","B1",sampleFutureMaturityDateStr);
        
        //Saving Trade version 2
        saveTradeAndAssertBasic("T5","2","CP-1","B2",sampleFutureMaturityDateStr);
        
        //Attempting to Save Trade with lower version and changed bookId
    	TradeIncomingDTO invalidVersionTrade = initializeTradeObject("T5","1","CP-1","B3",sampleFutureMaturityDateStr);
        
        ResponseEntity<TradeDTO> postResponse = null;
       	postResponse = restTemplate.postForEntity(getApiUrl() + "", invalidVersionTrade, TradeDTO.class);

        assertNotNull(postResponse);
        assertNotNull(postResponse.getBody());
        
        //Assert Trade Not saved
        assertNull(postResponse.getBody().getTradeId());

    }
    
    @Test
    @Order(6)
    public void testSaveNewTradePastMaturityDateNotAllowed() {
    	String samplePastMaturityDateStr = provideSamplePastMaturityDate();
    	
        //Attempting to Save a new Trade with a past maturity date. Created Date is assumed as Todays Date on Server.
    	TradeIncomingDTO pastMaturityDateTrade = initializeTradeObject("T6","1","CP-1","B1",samplePastMaturityDateStr);
        
        ResponseEntity<TradeDTO> postResponse = null;
       	postResponse = restTemplate.postForEntity(getApiUrl() + "", pastMaturityDateTrade, TradeDTO.class);

        assertNotNull(postResponse);
        assertNotNull(postResponse.getBody());
        
        //Assert Trade Not saved
        assertNull(postResponse.getBody().getTradeId());

    }
    
    @Test
    @Order(7)
    public void testUpdateTradePastMaturityDateNotAllowed() {
    	String samplePastMaturityDateStr = provideSamplePastMaturityDate();
    	
    	//Saving Trade version 1. Created Date is assumed as Todays Date on Server.
        saveTradeAndAssertBasic("T7","1","CP-1","B1",provideSampleFutureMaturityDate());
        
        //Attempting to Update Trade with past maturity date. 
    	TradeIncomingDTO pastMaturityDateTrade = initializeTradeObject("T7","1","CP-1","B1",samplePastMaturityDateStr);
        
        ResponseEntity<TradeDTO> postResponse = null;
       	postResponse = restTemplate.postForEntity(getApiUrl() + "", pastMaturityDateTrade, TradeDTO.class);

        assertNotNull(postResponse);
        assertNotNull(postResponse.getBody());
        
        //Assert Trade Not saved
        assertNull(postResponse.getBody().getTradeId());

    }
    
    @Test
    @Order(8)
    public void testExpireFlagShouldBeYWhenPastMaturityDate() {
    	
    	//Please note that below past created date record is pre-inserted using data.sql
    	TradeDTO pastCreatedDateTrade = retrieveSavedTrade("T20","1");
    	
    	assertEquals("Y",pastCreatedDateTrade.getExpired());

    }
    
    @Test
    @Order(9)
    public void testExpireFlagShouldBeNWhenNotPastMaturityDate() {
    	
    	//Future Maturity Date Scenario
    	TradeIncomingDTO futureMaturityDateTrade = initializeTradeObject("T9","1","CP-1","B1",provideSampleFutureMaturityDate());
        
        ResponseEntity<TradeDTO> postResponse = null;
       	postResponse = restTemplate.postForEntity(getApiUrl() + "", futureMaturityDateTrade, TradeDTO.class);
    	
        TradeDTO futureMaturityDateTradeRefetched = retrieveSavedTrade("T9","1");
        
    	assertEquals("N",futureMaturityDateTradeRefetched.getExpired());
    	
    	
    	//Today as Maturity Date Scenario
    	TradeIncomingDTO todayAsMaturityDateTrade = initializeTradeObject("T10","1","CP-1","B1",provideSampleValidMaturityDateAsToday());
        
       	postResponse = restTemplate.postForEntity(getApiUrl() + "", todayAsMaturityDateTrade, TradeDTO.class);
    	
        TradeDTO todayAsMaturityDateTradeRefetched = retrieveSavedTrade("T10","1");
        
    	assertEquals("N",todayAsMaturityDateTradeRefetched.getExpired());

    }
    
    /**
     * Reusable Helper method to invoke API for save operation. Also carries basic common assertions on API response.
     */
    protected TradeDTO saveTradeAndAssertBasic(String tradeId, String version, String counterPartyId, String bookId, String maturityDate) {
        
    	TradeIncomingDTO trade = initializeTradeObject(tradeId, version, counterPartyId, bookId, maturityDate);
        
        ResponseEntity<TradeDTO> postResponse = null;
       	postResponse = restTemplate.postForEntity(getApiUrl() + "", trade, TradeDTO.class);

        assertNotNull(postResponse);
        assertNotNull(postResponse.getBody());
        assertEquals(tradeId,postResponse.getBody().getTradeId());
        assertEquals(version,postResponse.getBody().getVersion());
        
        return postResponse.getBody();
    }
    
    protected TradeIncomingDTO initializeTradeObject(String tradeId, String version, String counterPartyId, String bookId, String maturityDate) {
    	TradeIncomingDTO trade = new TradeIncomingDTO();

        trade.setTradeId(tradeId);
        trade.setVersion(version);
        trade.setCounterPartyId(counterPartyId);
        trade.setBookId(bookId);
        trade.setMaturityDate(maturityDate);
        
        return trade;
    }

    
    /**
     * Reusable Helper method to retrieve a saved Trade. Also assertions API response for not null response.
     */
    protected TradeDTO retrieveSavedTrade(String tradeId, String version) {
    	TradeDTO trade = restTemplate.getForObject(getApiUrl() + "/"+tradeId+"/"+version, TradeDTO.class);
        assertNotNull(trade);
        assertNotNull(trade.getTradeId());
        return trade;
    }

    protected String provideSampleFutureMaturityDate() {
    	LocalDate todaysDate = LocalDate.now();
    	LocalDate sampleFutureMaturityDate = todaysDate.plusDays(1);
    	String sampleFutureMaturityDateStr = sampleFutureMaturityDate.format(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT));
    	return sampleFutureMaturityDateStr;
    }
    protected String provideSampleValidMaturityDateAsToday() {
    	LocalDate todaysDate = LocalDate.now();
    	String todaysDateStr = todaysDate.format(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT));
    	return todaysDateStr;
    }
    protected String provideSamplePastMaturityDate() {
    	LocalDate todaysDate = LocalDate.now();
    	LocalDate sampleFutureMaturityDate = todaysDate.minusDays(1);
    	String sampleFutureMaturityDateStr = sampleFutureMaturityDate.format(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT));
    	return sampleFutureMaturityDateStr;
    }
}
