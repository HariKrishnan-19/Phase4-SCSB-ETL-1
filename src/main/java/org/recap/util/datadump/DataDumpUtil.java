package org.recap.util.datadump;

import org.apache.commons.lang3.StringUtils;
import org.recap.RecapCommonConstants;
import org.recap.RecapConstants;
import org.recap.model.export.DataDumpRequest;
import org.recap.model.jpa.CollectionGroupEntity;
import org.recap.model.jpa.ETLRequestLogEntity;
import org.recap.model.jpa.ExportStatusEntity;
import org.recap.model.jpa.ImsLocationEntity;
import org.recap.repository.CollectionGroupDetailsRepository;
import org.recap.repository.ETLRequestLogDetailsRepository;
import org.recap.repository.ExportStatusDetailsRepository;
import org.recap.repository.ImsLocationDetailsRepository;
import org.recap.service.DataExportDBService;
import org.recap.service.preprocessor.DataDumpExportService;
import org.recap.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class DataDumpUtil {

    private static final Logger logger = LoggerFactory.getLogger(DataDumpUtil.class);

    @Autowired CollectionGroupDetailsRepository collectionGroupDetailsRepository;
    @Autowired ETLRequestLogDetailsRepository etlRequestLogDetailsRepository;
    @Autowired ExportStatusDetailsRepository exportStatusDetailsRepository;
    @Autowired DataDumpExportService dataDumpExportService;
    @Autowired ImsLocationDetailsRepository imsLocationDetailsRepository;
    @Autowired DataExportDBService dataExportDBService;

    public String getFetchType(String fetchTypeNumber) {
        String fetchType ="";
        switch (fetchTypeNumber) {
            case RecapConstants.DATADUMP_FETCHTYPE_FULL:
                fetchType= RecapConstants.EXPORT_TYPE_FULL;
                break;
            case RecapConstants.DATADUMP_FETCHTYPE_INCREMENTAL:
                fetchType= RecapConstants.INCREMENTAL;
                break;
            case RecapConstants.DATADUMP_FETCHTYPE_DELETED:
                fetchType= RecapConstants.DELETED;
                break;
            default:
                fetchType= "Export";
        }
        return fetchType;
    }

    public String getOutputformat(String outputFileFormat) {
        String format ="";
        switch (outputFileFormat) {
            case RecapConstants.DATADUMP_XML_FORMAT_MARC:
                format= RecapConstants.MARC;
                break;
            case RecapConstants.DATADUMP_XML_FORMAT_SCSB:
                format= RecapConstants.SCSB;
                break;
            case RecapConstants.DATADUMP_DELETED_JSON_FORMAT:
                format= RecapConstants.JSON;
                break;
        }
        return format;
    }

    public String getTransmissionType(String transmissionType) {
        String type ="";
        switch (transmissionType) {
            case RecapConstants.DATADUMP_TRANSMISSION_TYPE_S3:
                type= "FTP";
                break;
            case RecapConstants.DATADUMP_TRANSMISSION_TYPE_HTTP:
                type= "HTTP";
                break;
            case RecapConstants.DATADUMP_TRANSMISSION_TYPE_FILESYSTEM:
                type= "Filesystem";
                break;
        }
        return type;
    }

    /**
     * Sets the request values to data dump request object.
     *
     * @param dataDumpRequest           the data dump request
     * @param fetchType                 the fetch type
     * @param institutionCodes          the institution codes
     * @param date                      the date
     * @param collectionGroupIds        the collection group ids
     * @param transmissionType          the transmission type
     * @param requestingInstitutionCode the requesting institution code
     * @param toEmailAddress            the to email address
     * @param outputFormat              the output format
     */
    public void setDataDumpRequest(DataDumpRequest dataDumpRequest, String fetchType, String institutionCodes, String date, String toDate, String collectionGroupIds,
                                   String transmissionType, String requestingInstitutionCode, String toEmailAddress, String outputFormat,String imsDepositoryCodes) {
        if (fetchType != null) {
            dataDumpRequest.setFetchType(fetchType);
        }
        if (institutionCodes != null) {
            List<String> institutionCodeList = splitStringAndGetList(institutionCodes);
            dataDumpRequest.setInstitutionCodes(institutionCodeList);
        }
        if (imsDepositoryCodes != null && !"".equals(imsDepositoryCodes)) {
            List<String> imsDepositoryCodesList = splitStringAndGetList(imsDepositoryCodes);
            dataDumpRequest.setImsDepositoryCodes(imsDepositoryCodesList);
        }
        else {
            ImsLocationEntity imsLocationEntity = imsLocationDetailsRepository.findByImsLocationCode(RecapConstants.IMS_DEPOSITORY_RECAP);
            dataDumpRequest.setImsDepositoryCodes(Arrays.asList(imsLocationEntity.getImsLocationCode()));
        }
        if (date != null && !"".equals(date)) {
            dataDumpRequest.setDate(date);
        }
        if (toDate != null && !"".equals(toDate)) {
            dataDumpRequest.setToDate(toDate);
        }
        if (collectionGroupIds != null && !"".equals(collectionGroupIds)) {
            List<Integer> collectionGroupIdList = splitStringAndGetIntegerList(collectionGroupIds);
            dataDumpRequest.setCollectionGroupIds(collectionGroupIdList);
        } else {
            List<Integer> collectionGroupIdList = new ArrayList<>();
            CollectionGroupEntity collectionGroupEntityShared = collectionGroupDetailsRepository.findByCollectionGroupCode(RecapConstants.COLLECTION_GROUP_SHARED);
            collectionGroupIdList.add(collectionGroupEntityShared.getId());
            CollectionGroupEntity collectionGroupEntityOpen = collectionGroupDetailsRepository.findByCollectionGroupCode(RecapConstants.COLLECTION_GROUP_OPEN);
            collectionGroupIdList.add(collectionGroupEntityOpen.getId());
            dataDumpRequest.setCollectionGroupIds(collectionGroupIdList);
        }
        if (transmissionType != null && !"".equals(transmissionType)) {
            dataDumpRequest.setTransmissionType(transmissionType);
        } else {
            dataDumpRequest.setTransmissionType(RecapConstants.DATADUMP_TRANSMISSION_TYPE_S3);
        }
        if (requestingInstitutionCode != null) {
            dataDumpRequest.setRequestingInstitutionCode(requestingInstitutionCode);
        }
        if (!StringUtils.isEmpty(toEmailAddress)) {
            dataDumpRequest.setToEmailAddress(toEmailAddress);
        }

        if (!StringUtils.isEmpty(outputFormat)) {
            dataDumpRequest.setOutputFileFormat(outputFormat);
        }

        dataDumpRequest.setDateTimeString(DateUtil.getDateTimeString());

        dataDumpRequest.setRequestId(new SimpleDateFormat(RecapCommonConstants.DATE_FORMAT_YYYYMMDDHHMM).format(new Date())+
                "-"+dataDumpRequest.getInstitutionCodes()+"-"+dataDumpRequest.getRequestingInstitutionCode()+"-"+dataDumpRequest.getFetchType());
    }

    /**
     * Splits the given string by comma and prepares a list.
     * @param inputString
     * @return
     */
    private List<String> splitStringAndGetList(String inputString) {
        String[] splittedString = inputString.split(",");
        return Arrays.asList(splittedString);
    }

    /**
     * Splits the string by comma and gets integer type list from string type list.
     * @param inputString
     * @return
     */
    private List<Integer> splitStringAndGetIntegerList(String inputString) {
        return getIntegerListFromStringList(splitStringAndGetList(inputString));
    }

    /**
     * Convert string type list to integer type list.
     * @param stringList
     * @return
     */
    private List<Integer> getIntegerListFromStringList(List<String> stringList) {
        List<Integer> integerList = new ArrayList<>();
        for (String stringValue : stringList) {
            integerList.add(Integer.parseInt(stringValue));
        }
        return integerList;
    }

    public ETLRequestLogEntity prepareRequestForAwaiting(DataDumpRequest dataDumpRequest, String status) {
        ETLRequestLogEntity etlRequestLogEntity =new ETLRequestLogEntity();
        ExportStatusEntity exportStatusEntity = exportStatusDetailsRepository.findByExportStatusCode(status);
        etlRequestLogEntity.setExportStatusId(exportStatusEntity.getId());
        etlRequestLogEntity.setExportStatusEntity(exportStatusEntity);
        String collectionGroupIds = dataDumpRequest.getCollectionGroupIds().stream().map(String::valueOf)
                .collect(Collectors.joining(","));
        String defaultCgds = Arrays.asList(RecapConstants.DATADUMP_CGD_SHARED, RecapConstants.DATADUMP_CGD_OPEN).stream().map(String::valueOf).collect(Collectors.joining(","));
        etlRequestLogEntity.setCollectionGroupIds(collectionGroupIds!=null?collectionGroupIds:defaultCgds );
        etlRequestLogEntity.setEmailIds(dataDumpRequest.getToEmailAddress());
        etlRequestLogEntity.setRequestedTime(new Date());
        etlRequestLogEntity.setFetchType(dataDumpRequest.getFetchType());
        etlRequestLogEntity.setOutputFormat(dataDumpRequest.getOutputFileFormat());
        etlRequestLogEntity.setRequestingInstCode(dataDumpRequest.getRequestingInstitutionCode());
        etlRequestLogEntity.setInstCodeToExport(String.join(",",dataDumpRequest.getInstitutionCodes()));
        etlRequestLogEntity.setTransmissionType(dataDumpRequest.getTransmissionType()!=null? dataDumpRequest.getTransmissionType() : "0");
        etlRequestLogEntity.setImsRepositoryCodes(dataDumpRequest.getImsDepositoryCodes()!=null?String.join(",",dataDumpRequest.getImsDepositoryCodes()): RecapConstants.IMS_DEPOSITORY_RECAP);
        etlRequestLogEntity.setUserName(dataDumpRequest.getUserName()!=null?dataDumpRequest.getUserName():RecapConstants.SWAGGER);
        etlRequestLogEntity.setProvidedDate(dataDumpRequest.getDate()!=null?DateUtil.getDateFromString(dataDumpRequest.getDate(),RecapCommonConstants.DATE_FORMAT_YYYYMMDDHHMM):null);
        return etlRequestLogEntity;
    }

    @Transactional
    public void updateStatusInETLRequestLog(DataDumpRequest dataDumpRequest, String outputString) {
        logger.info("ETL Request ID to update: {}",dataDumpRequest.getEtlRequestId());
        Optional<ETLRequestLogEntity> etlRequestLogEntity = etlRequestLogDetailsRepository.findById(dataDumpRequest.getEtlRequestId());
        etlRequestLogEntity.ifPresent(exportLog ->{
            if(outputString.contains(RecapConstants.DATADUMP_EXPORT_FAILURE) ){
                ExportStatusEntity exportStatusEntity = exportStatusDetailsRepository.findByExportStatusCode(RecapConstants.INVALID);
                exportLog.setExportStatusId(exportStatusEntity.getId());
                exportLog.setExportStatusEntity(exportStatusEntity);
                exportLog.setMessage(outputString);
            }
            else if(outputString.contains("100")){
                ExportStatusEntity exportStatusEntity = exportStatusDetailsRepository.findByExportStatusCode(RecapConstants.COMPLETED);
                exportLog.setExportStatusId(exportStatusEntity.getId());
                exportLog.setExportStatusEntity(exportStatusEntity);
                exportLog.setMessage("Diplayed the result in the response");
            }
            else{
                ExportStatusEntity exportStatusEntity = exportStatusDetailsRepository.findByExportStatusCode(outputString);
                exportLog.setExportStatusId(exportStatusEntity.getId());
                exportLog.setExportStatusEntity(exportStatusEntity);
            }
            exportLog.setCompleteTime(new Date());
            etlRequestLogDetailsRepository.saveAndFlush(exportLog);
        });
    }

    private DataDumpRequest prepareDataDumpReq(ETLRequestLogEntity etlRequestLogEntity) {
        DataDumpRequest dataDumpRequestForAwaiting=new DataDumpRequest();
        dataDumpRequestForAwaiting.setImsDepositoryCodes(Arrays.asList(etlRequestLogEntity.getImsRepositoryCodes()));
        dataDumpRequestForAwaiting.setFetchType(etlRequestLogEntity.getFetchType());
        dataDumpRequestForAwaiting.setOutputFileFormat(etlRequestLogEntity.getOutputFormat());
        dataDumpRequestForAwaiting.setTransmissionType(etlRequestLogEntity.getTransmissionType());
        List<Integer> collectionGroupIds = Arrays.stream(etlRequestLogEntity.getCollectionGroupIds()
                .split(",")).map(Integer::parseInt)
                .collect(Collectors.toList());
        dataDumpRequestForAwaiting.setCollectionGroupIds(collectionGroupIds);
        dataDumpRequestForAwaiting.setImsDepositoryCodes(Arrays.asList(etlRequestLogEntity.getImsRepositoryCodes()));
        dataDumpRequestForAwaiting.setRequestingInstitutionCode(etlRequestLogEntity.getRequestingInstCode());
        dataDumpRequestForAwaiting.setInstitutionCodes(Arrays.asList(etlRequestLogEntity.getInstCodeToExport()));
        dataDumpRequestForAwaiting.setDate(etlRequestLogEntity.getProvidedDate()!=null?String.valueOf(etlRequestLogEntity.getProvidedDate()):null);
        dataDumpRequestForAwaiting.setDateTimeString(DateUtil.getDateTimeString());
        dataDumpRequestForAwaiting.setRequestFromSwagger(true);
        dataDumpRequestForAwaiting.setEtlRequestId(etlRequestLogEntity.getId());
        dataDumpRequestForAwaiting.setToEmailAddress(etlRequestLogEntity.getEmailIds());
        dataDumpRequestForAwaiting.setUserName(etlRequestLogEntity.getUserName()!=null?etlRequestLogEntity.getUserName():RecapConstants.SWAGGER);
        return dataDumpRequestForAwaiting;
    }

    public DataDumpRequest checkAndPrepareAwaitingReqIfAny() {
        ExportStatusEntity awaitingStatusEntity = dataExportDBService.findByExportStatusCode(RecapConstants.AWAITING);
        List<ETLRequestLogEntity> etlRequestsAwaitingForExport = dataExportDBService.findAllStatusForS3OrderByRequestedTime(awaitingStatusEntity.getId(),RecapConstants.DATADUMP_TRANSMISSION_TYPE_S3);
        if(!etlRequestsAwaitingForExport.isEmpty()){
            return prepareRequestForExistingAwaiting();
        }
        return null;
    }

    public DataDumpRequest prepareRequestForExistingAwaiting() {
        ExportStatusEntity exportStatusEntity = dataExportDBService.findByExportStatusCode(RecapConstants.AWAITING);
        List<ETLRequestLogEntity> allByStatusOrderByRequestedTime = dataExportDBService.findAllStatusForS3OrderByRequestedTime(exportStatusEntity.getId(),RecapConstants.DATADUMP_TRANSMISSION_TYPE_S3);
        return prepareDataDumpReq(allByStatusOrderByRequestedTime.get(0));
    }



}