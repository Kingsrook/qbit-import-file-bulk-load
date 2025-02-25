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
* A process that syncs files from the file-source table to a file staging table, along with rows in the `ImportFile` table
to track their status.
* A process that executes the bulk load process, using the selected bulk load profile, against records in the `ImportFile`
table, accessing file contents from the staging file-table. 

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
* To enable public-key authentication to SFTP servers, you can load a private key into your application
server's sftp-backend, loading it through an environment variable, whose name you can set in the config
field: `sftpPrivateKeyEnvVarName`.
    * By convention, you may wish to set this config value to: `SFTP_PRIVATE_KEY_PEM`.
    * If this field is not set, then all SFTP connections will require password authentication.
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
         // any customizations you may want on all tables
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
* `sftpConnection` - Defines a connection to an SFTP server.
* `sftpImportConfig` - Defines the usage of an SFTP Server and a Bulk Load Config for loading files into records.
* `importFile` - Records which track files imported from an SFTP server, as they are bulk loaded. 
* `SFTPImportSourceFileTable` - _If so configured_ - SFTP-backed file-table, where files to be sync'ed and bulk-loaded come from.
* `SFTPImportStagingFileTable` - _If so configured_ - Filesystem table, where files imported from an SFTP Import source are stored.

### Processes
* `SFTPConnectionTester` - Used to validate the setup of SFTP Connection records.
* `syncSFTPImportConfigScheduledJob` - Table Sync process that manages Scheduled Job records for SFTPImportConfig records with a cron schedule.
* `SFTPImportFileSyncProcess` - Sync files from an SFTP server source file table (as defined in an SFTP Import Config) to the staging table.
* `ImportFileBulkLoadProcess` - Run the Bulk Load Process against ImportFile records and their corresponding file data.

## Dependencies
* `qqq-backend-module-filesystem` >= 0.24.0 (for SFTP support)
* An active Quartz scheduler in your QQQ instance, for running scheduled cron jobs, along with Scheduled Jobs table.
* 
