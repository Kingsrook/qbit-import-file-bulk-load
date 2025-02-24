# QBit:  SFTP Import Bulk Load

## Overview
*Note:  This is one of the original QBit implementations - so, some of the mechanics of how
it is loaded and used by an application are not exactly fully defined at the time of its
creation... Please excuse any dust or not-quite-round wheels you find here!*

This QBit provides tables to implement an SFTP-based file-integration, using QQQ's Bulk Load 
mechanism to allow users to define file mappings.  

The architecture of such an integration looks like:
* User-defined records in an `SFTPConnection` table which define connection credentials
* User-defined records in an `SFTPImportConfig` table, which reference an `SFTPConnection` as the source of files to import
and a `BulkLoadProfile` as the instructions on how and where to load file contents.
* A table built on the QQQ SFTP-backend module, using the `SFTPImportConfig` table as its variant-options table, to serve
as the source for files to be imported.
* 

## Usage

### Pom dependency
```xml
<dependency>
    <groupId>com.kingsrook.qbits</groupId>
    <artifactId>qbit-import-file-bulk-load</artifactId>
    <version>${TODO}</version>
</dependency>
```

### Setup
Using this QBit follows the (evolving, but as of this time, standard) QBit Config + Produce pattern.

Required configs here are:
* For your SFTP source file table:
  * Whether you want this QBit to define this table and its SFTP Backend, in which case, you would call this method on the Config object:
    * `.withSourceFileTableConfig(ProvidedOrSuppliedTableConfig.provideTableUsingBackendNamed(SFTPImportSourceFileBackendMetaDataProducer.NAME))`
  * Or if you want to define that table yourself and supply it to this QBit:
      * `.withSourceFileTableConfig(ProvidedOrSuppliedTableConfig.useSuppliedTaleNamed("mySftpSourceFileTableName"))`
* Similarly, for Staging file table:
    * If you want this QBit to define this table - note that you will need to always supply your own backend name:
        * `.withStagingFileTableConfig(ProvidedOrSuppliedTableConfig.provideTableUsingBackendNamed("myStagingFileBackendName"))`
    * Or if you want to define that table yourself and supply it to this QBit:
        * `.withStagingFileTableConfig(ProvidedOrSuppliedTableConfig.useSuppliedTaleNamed("myStagingFileTableName"))`
* The name of your instance's scheduler:
  * `.withSchedulerName("myScheduler")`

Optional configs include:
* A `MetaDataCustomizerInterface<QTableMetaData>` can be run against all tables produced by this QBit:
    `.withTableMetaDataCustomizer(tableMetaDataCustomizer)`
* A ProcessTracerCodeReference, which will be used on scheduled jobs ran by this QBit, as in:
  * `.withProcessTracerCodeReference(new QCodeReference(StandardProcessTracer.class))`
* If you add additional fields to the `SFTPImportConfig` and `ImportFile` tables (e.g., for record security locks), you need to
tell the QBit to copy values from `SFTPImportConfig` records `ImportFile` records as they are built:
  * `.withAdditionalFieldsToCopyFromSftpImportConfigToImportFile(Map.of("clientId", "clientId"))`

A full Config & Produce flow then, in a `MetaDataProducer<MetaDataProducerMultiOutput>`, may look like:

```java
public class ImportFileBulkLoadQBitMetaDataLoader extends MetaDataProducer<MetaDataProducerMultiOutput>
{
   @Override
   public MetaDataProducerMultiOutput produce(QInstance qInstance) throws QException
   {
      MetaDataProducerMultiOutput rs = new MetaDataProducerMultiOutput();

      MetaDataCustomizerInterface<QTableMetaData> tableMetaDataCustomizer = (instance, table) ->
      {
         // whatever customizations you want on all tables
         return (table);
      };

      ImportFileBulkLoadQBitConfig importFileBulkLoadQBitConfig = new ImportFileBulkLoadQBitConfig()
         .withSourceFileTableConfig(ProvidedOrSuppliedTableConfig.provideTableUsingBackendNamed(SFTPImportSourceFileBackendMetaDataProducer.NAME))
         .withStagingFileTableConfig(ProvidedOrSuppliedTableConfig.provideTableUsingBackendNamed(SFTPImportStagingFileBackendMetaDataProducer.NAME))
         .withSchedulerName(QuartzSchedulerMetaDataProducer.NAME)
         .withTableMetaDataCustomizer(tableMetaDataCustomizer)
         .withProcessTracerCodeReference(new QCodeReference(StandardProcessTracer.class))
         .withAdditionalFieldsToCopyFromSftpImportConfigToImportFile(Map.of("clientId", "clientId"));

      new ImportFileBulkLoadQBitProducer()
         .withImportFileBulkLoadQBitConfig(importFileBulkLoadQBitConfig)
         .produce(qInstance);

      // any additional customizations you may want

      return (rs);
   }
}
```

## Provides
### Tables
TODO

### Classes
TODO

## Dependencies
TODO

