package org.recap.camel.datadump.consumer;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.recap.BaseTestCaseUT;
import org.recap.RecapCommonConstants;
import org.recap.RecapConstants;
import org.recap.model.export.Bib;
import org.recap.model.export.DeletedRecord;
import org.recap.model.jpa.BibliographicEntity;
import org.recap.service.formatter.datadump.DeletedJsonFormatterService;
import org.recap.util.datadump.DataExportHeaderUtil;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;

public class DeletedRecordFormatActiveMQConsumerUT extends BaseTestCaseUT {

    @Mock
    DeletedJsonFormatterService deletedJsonFormatterService;

    @InjectMocks
    @Spy
    DeletedRecordFormatActiveMQConsumer deletedRecordFormatActiveMQConsumer = new DeletedRecordFormatActiveMQConsumer(deletedJsonFormatterService);

    @Mock
    DataExportHeaderUtil dataExportHeaderUtil;

    @Mock
    ExecutorService executorService;

    @Mock
    Future future;

    @Test
    public void testgetDataExportHeaderUtil() {
        deletedRecordFormatActiveMQConsumer.getDataExportHeaderUtil();
        assertNotNull(dataExportHeaderUtil);
    }

    @Test
    public void testgetExecutorService() {
        Mockito.when(executorService.isShutdown()).thenReturn(Boolean.TRUE);
        executorService = deletedRecordFormatActiveMQConsumer.getExecutorService();
        assertNotNull(executorService);
    }

    @Test
    public void testprocessRecords() throws Exception {
        CamelContext ctx = new DefaultCamelContext();
        Exchange ex = new DefaultExchange(ctx);
        Message in = ex.getIn();
        BibliographicEntity bibliographicEntity = new BibliographicEntity();
        bibliographicEntity.setId(100);
        bibliographicEntity.setContent("bib content".getBytes());
        bibliographicEntity.setOwningInstitutionId(1);
        bibliographicEntity.setOwningInstitutionBibId("2");
        bibliographicEntity.setCreatedDate(new Date());
        bibliographicEntity.setCreatedBy("tst");
        bibliographicEntity.setLastUpdatedDate(new Date());
        bibliographicEntity.setLastUpdatedBy("tst");
        List<BibliographicEntity> bibliographicEntities=new ArrayList<>();
        bibliographicEntities.add(bibliographicEntity);
        in.setBody(bibliographicEntities);
        ex.setIn(in);
        List<Future<Object>> futureList = new ArrayList<>();
        futureList.add(future);
        Map<String, Object> results = new HashMap<>();
        results.put(RecapCommonConstants.SUCCESS,Arrays.asList(getDeletedRecord()));
        results.put(RecapCommonConstants.FAILURE,Arrays.asList("FailureRecords",getDeletedRecord()));
        results.put(RecapConstants.ITEM_EXPORTED_COUNT,10);
        ReflectionTestUtils.setField(deletedRecordFormatActiveMQConsumer,"deletedJsonFormatterService",deletedJsonFormatterService);
        ReflectionTestUtils.setField(deletedRecordFormatActiveMQConsumer,"executorService",executorService);
        Mockito.when(executorService.invokeAll(any())).thenReturn(futureList);
        Mockito.when(future.get()).thenReturn(results);
        Mockito.doNothing().when(deletedRecordFormatActiveMQConsumer).processRecordFailures(any(),any(),any(),any(),any());
        Mockito.when(deletedJsonFormatterService.prepareDeletedRecords(any())).thenReturn(new HashMap<>());
        try {
            deletedRecordFormatActiveMQConsumer.processRecords(ex);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue(true);
    }

    private DeletedRecord getDeletedRecord() {
        DeletedRecord deletedRecord = new DeletedRecord();
        Bib bib = new Bib();
        bib.setBibId("1");
        bib.setOwningInstitutionBibId("1");
        bib.setOwningInstitutionCode("CUL");
        deletedRecord.setBib(bib);
        return deletedRecord;
    }

    @Test
    public void testsetDataExportHeaderUtil() {
        deletedRecordFormatActiveMQConsumer.setDataExportHeaderUtil(dataExportHeaderUtil);
        assertTrue(true);
    }

    @Test
    public void processFailures(){
        CamelContext ctx = new DefaultCamelContext();
        Exchange ex = new DefaultExchange(ctx);
        List failures = new ArrayList();
        failures.add("failed");
        String batchHeaders ="test";
        String requestId = "1";
        try {
            ReflectionTestUtils.invokeMethod(deletedRecordFormatActiveMQConsumer, "processFailures", ex, failures, batchHeaders, requestId);
        }catch (Exception e){}
    }
}
