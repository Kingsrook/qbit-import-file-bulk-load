/*
 * Copyright © 2022-2025. ColdTrack <contact@coldtrack.com>.  All Rights Reserved.
 */

package com.kingsrook.qbits.importfilebulkload.metadata;


import java.util.List;
import com.kingsrook.qbits.importfilebulkload.model.SFTPImportConfig;
import com.kingsrook.qbits.importfilebulkload.process.SyncSFTPImportConfigScheduledJobTransformStep;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.tablesync.TableSyncProcess;


/*******************************************************************************
 ** Meta Data Producer for SyncSFTPImportConfigScheduledJob
 *******************************************************************************/
public class SyncSFTPImportConfigScheduledJobMetaDataProducer extends MetaDataProducer<QProcessMetaData>
{
   public static final String NAME = "syncSFTPImportConfigScheduledJob";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      QProcessMetaData processMetaData = TableSyncProcess.processMetaDataBuilder(false)
         .withName(NAME)
         .withSyncTransformStepClass(SyncSFTPImportConfigScheduledJobTransformStep.class)
         .withReviewStepRecordFields(List.of(
            new QFieldMetaData("sftpImportConfigId", QFieldType.INTEGER).withPossibleValueSourceName(SFTPImportConfig.TABLE_NAME),
            new QFieldMetaData("cronExpression", QFieldType.STRING)
         ))
         .getProcessMetaData()
         .withIsHidden(true);

      return (processMetaData);
   }

}
