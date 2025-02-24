/*
 * Copyright © 2022-2025. ColdTrack <contact@coldtrack.com>.  All Rights Reserved.
 */

package com.kingsrook.qbits.importfilebulkload.metadata;


import java.util.List;
import com.kingsrook.qbits.importfilebulkload.ImportFileBulkLoadQBitConfig;
import com.kingsrook.qbits.importfilebulkload.model.ImportFile;
import com.kingsrook.qbits.importfilebulkload.model.SFTPImportConfig;
import com.kingsrook.qbits.importfilebulkload.process.SFTPImportFileSyncExtractStep;
import com.kingsrook.qbits.importfilebulkload.process.SFTPImportFileSyncLoadStep;
import com.kingsrook.qbits.importfilebulkload.process.SFTPImportFileSyncTransformStep;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.VariantRunStrategy;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitComponentMetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;


/*******************************************************************************
 ** Meta Data Producer for SFTPImportFileSync
 **
 ** This process exists on the SFTP Import Config table.
 **
 ** This process gets scheduled to run every-so-often, looking for new files in
 ** the SFTP source table associated with the connection, and then sync'ing them
 ** to the staging filesystem table and the ImportFile database table.
 *******************************************************************************/
public class SFTPImportFileSyncProcessMetaDataProducer extends QBitComponentMetaDataProducer<QProcessMetaData, ImportFileBulkLoadQBitConfig>
{
   public static final String NAME = "SFTPImportFileSyncProcess";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      ImportFileBulkLoadQBitConfig qBitConfig = getQBitConfig();
      String sourceFileTableName = qBitConfig.getEffectiveSourceFileTableName();

      QProcessMetaData processMetaData = StreamedETLWithFrontendProcess.processMetaDataBuilder()
         .withName(NAME)
         .withLabel("SFTP Import File Sync")
         .withIcon(new QIcon().withName("cloud_sync"))
         .withTableName(SFTPImportConfig.TABLE_NAME)
         .withSourceTable(sourceFileTableName)
         .withDestinationTable(ImportFile.TABLE_NAME)
         .withExtractStepClass(SFTPImportFileSyncExtractStep.class)
         .withTransformStepClass(SFTPImportFileSyncTransformStep.class)
         .withLoadStepClass(SFTPImportFileSyncLoadStep.class)
         .withReviewStepRecordFields(List.of(
            new QFieldMetaData("sourcePath", QFieldType.STRING),
            new QFieldMetaData("stagedPath", QFieldType.STRING)
         ))
         .withTransactionLevelAutoCommit()
         .getProcessMetaData();

      //////////////////////////////////////////////////////////////////////////////////
      // if the source-file table uses variants, set that variant data on the process //
      //////////////////////////////////////////////////////////////////////////////////
      QTableMetaData   sourceFileTable   = qInstance.getTable(sourceFileTableName);
      QBackendMetaData sourceFileBackend = qInstance.getBackend(sourceFileTable.getBackendName());
      if(sourceFileBackend.getUsesVariants())
      {
         processMetaData.withVariantBackend(sourceFileBackend.getName());
         processMetaData.withVariantRunStrategy(VariantRunStrategy.SERIAL);
      }

      processMetaData.setProcessTracerCodeReference(getQBitConfig().getProcessTracerCodeReference());

      return (processMetaData);
   }

}
