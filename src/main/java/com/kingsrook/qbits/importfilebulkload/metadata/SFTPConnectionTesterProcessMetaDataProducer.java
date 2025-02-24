/*
 * Copyright © 2022-2025. ColdTrack <contact@coldtrack.com>.  All Rights Reserved.
 */

package com.kingsrook.qbits.importfilebulkload.metadata;


import java.util.List;
import com.kingsrook.qbits.importfilebulkload.model.SFTPConnection;
import com.kingsrook.qbits.importfilebulkload.process.SFTPConnectionTesterLoadStep;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ExtractViaQueryStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.NoopTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;


/*******************************************************************************
 ** Meta Data Producer for SFTPConnectionTester
 *******************************************************************************/
public class SFTPConnectionTesterProcessMetaDataProducer extends MetaDataProducer<QProcessMetaData>
{
   public static final String NAME = "SFTPConnectionTester";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      QProcessMetaData processMetaData = StreamedETLWithFrontendProcess.processMetaDataBuilder()
         .withName(NAME)
         .withLabel("SFTP Connection Tester")
         .withIcon(new QIcon().withName("flaky"))
         .withTableName(SFTPConnection.TABLE_NAME)
         .withSourceTable(SFTPConnection.TABLE_NAME)
         .withDestinationTable(SFTPConnection.TABLE_NAME)
         .withExtractStepClass(ExtractViaQueryStep.class)
         .withTransformStepClass(NoopTransformStep.class)
         .withLoadStepClass(SFTPConnectionTesterLoadStep.class)
         .withReviewStepRecordFields(List.of(
            new QFieldMetaData("id", QFieldType.INTEGER),
            new QFieldMetaData("name", QFieldType.STRING)
         ))
         .withTransactionLevelAutoCommit()
         .withPreviewMessage("This is a preview of the connections that will be tested")
         .withSupportsFullValidation(false)
         .getProcessMetaData();

      return processMetaData;
   }

}
