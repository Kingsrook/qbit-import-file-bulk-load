/*
 * Copyright © 2022-2025. ColdTrack <contact@coldtrack.com>.  All Rights Reserved.
 */

package com.kingsrook.qbits.importfilebulkload.metadata;


import java.io.Serializable;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qbits.importfilebulkload.ImportFileBulkLoadQBitConfig;
import com.kingsrook.qbits.importfilebulkload.model.SFTPConnection;
import com.kingsrook.qbits.importfilebulkload.model.SFTPImportConfig;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitComponentMetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.variants.BackendVariantsConfig;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeFunction;
import com.kingsrook.qqq.backend.module.filesystem.sftp.model.metadata.SFTPBackendMetaData;
import com.kingsrook.qqq.backend.module.filesystem.sftp.model.metadata.SFTPBackendVariantSetting;


/*******************************************************************************
 ** Meta Data Producer for SFTPImportSourceFile
 *******************************************************************************/
public class SFTPImportSourceFileBackendMetaDataProducer extends QBitComponentMetaDataProducer<QBackendMetaData, ImportFileBulkLoadQBitConfig>
{
   public static final String NAME = "SFTPImportSourceFileBackend";

   public static final String VARIANT_TYPE_KEY = SFTPImportConfig.TABLE_NAME;



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public boolean isEnabled()
   {
      boolean doProvideTable             = getQBitConfig().getSourceFileTableConfig().getDoProvideTable();
      boolean isSourceBackendThisBackend = NAME.equals(getQBitConfig().getSourceFileTableConfig().getBackendName());

      return (doProvideTable && isSourceBackendThisBackend);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QBackendMetaData produce(QInstance qInstance) throws QException
   {
      SFTPBackendMetaData backendMetaData = new SFTPBackendMetaData()
         .withName(NAME);

      backendMetaData.setUsesVariants(true);
      backendMetaData.setBackendVariantsConfig(new BackendVariantsConfig()
         .withOptionsTableName(SFTPImportConfig.TABLE_NAME)
         .withOptionsFilter(new QQueryFilter(new QFilterCriteria("isActive", QCriteriaOperator.EQUALS, true)))
         .withVariantTypeKey(VARIANT_TYPE_KEY)
         .withVariantRecordLookupFunction(new QCodeReference(VariantRecordSupplier.class))
         .withBackendSettingSourceFieldNameMap(Map.of(
            SFTPBackendVariantSetting.USERNAME, SFTPConnection.TABLE_NAME + ".username",
            SFTPBackendVariantSetting.PASSWORD, SFTPConnection.TABLE_NAME + ".password",
            SFTPBackendVariantSetting.HOSTNAME, SFTPConnection.TABLE_NAME + ".hostname",
            SFTPBackendVariantSetting.PORT, SFTPConnection.TABLE_NAME + ".port",
            SFTPBackendVariantSetting.BASE_PATH, "fullPath"
         ))
      );

      /////////////////////////////////////////////////////////////////////
      // if the config has an env-var name with a private-key PEM        //
      // env-var name, then look up that value and add it to the backend //
      /////////////////////////////////////////////////////////////////////
      if(StringUtils.hasContent(getQBitConfig().getSftpPrivateKeyEnvVarName()))
      {
         String pem = new QMetaDataVariableInterpreter().interpret("${env." + getQBitConfig().getSftpPrivateKeyEnvVarName() + "}");
         if(StringUtils.hasContent(pem))
         {
            backendMetaData.setPrivateKey(Base64.getDecoder().decode(pem.replaceAll("\\s", "")));
         }
      }

      return (backendMetaData);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static class VariantRecordSupplier implements UnsafeFunction<Serializable, QRecord, QException>
   {
      /***************************************************************************
       **
       ***************************************************************************/
      public QRecord apply(Serializable id) throws QException
      {
         QueryOutput queryOutput = new QueryAction().execute(new QueryInput(SFTPImportConfig.TABLE_NAME)
            .withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, id)))
            .withQueryJoin(new QueryJoin(SFTPConnection.TABLE_NAME).withSelect(true)));

         if(queryOutput.getRecords().isEmpty())
         {
            return null;
         }

         QRecord record   = queryOutput.getRecords().get(0);
         String  fullPath = Objects.requireNonNullElse(record.getValueString(SFTPConnection.TABLE_NAME + ".basePath"), "") + "/" + Objects.requireNonNullElse(record.getValueString("subPath"), "");
         record.setValue("fullPath", fullPath);
         return record;
      }
   }

}
