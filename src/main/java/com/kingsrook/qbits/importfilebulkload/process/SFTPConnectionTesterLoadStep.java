/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qbits.importfilebulkload.process;


import java.util.ArrayList;
import com.kingsrook.qbits.importfilebulkload.model.SFTPConnection;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractLoadStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ProcessSummaryProviderInterface;
import com.kingsrook.qqq.backend.module.filesystem.sftp.actions.SFTPTestConnectionAction;
import org.apache.commons.lang3.BooleanUtils;


/*******************************************************************************
 ** Test SFTP Connections
 *******************************************************************************/
public class SFTPConnectionTesterLoadStep extends AbstractLoadStep implements ProcessSummaryProviderInterface
{
   private static final QLogger LOG = QLogger.getLogger(SFTPConnectionTesterLoadStep.class);

   private ArrayList<ProcessSummaryLineInterface> summaryLines = new ArrayList<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void runOnePage(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      for(SFTPConnection sftpConnection : runBackendStepInput.getRecordsAsEntities(SFTPConnection.class))
      {
         SFTPTestConnectionAction.SFTPTestConnectionTestInput input = new SFTPTestConnectionAction.SFTPTestConnectionTestInput()
            .withUsername(sftpConnection.getUsername())
            .withPassword(sftpConnection.getPassword())
            .withHostName(sftpConnection.getHostname())
            .withPort(sftpConnection.getPort())
            .withBasePath(sftpConnection.getBasePath());
         SFTPTestConnectionAction.SFTPTestConnectionTestOutput output = new SFTPTestConnectionAction().testConnection(input);

         String recordLabel = sftpConnection.getName() + " (Id " + sftpConnection.getId() + ")";
         if(output.getIsConnectionSuccess())
         {
            if(BooleanUtils.isFalse(output.getIsListBasePathSuccess()))
            {
               summaryLines.add(new ProcessSummaryLine(Status.WARNING, null, recordLabel + " connected, but failed to read the base path: " + output.getListBasePathErrorMessage()));
            }
            else
            {
               summaryLines.add(new ProcessSummaryLine(Status.OK, null, recordLabel + " connected successfully"));
            }
         }
         else
         {
            summaryLines.add(new ProcessSummaryLine(Status.ERROR, null, recordLabel + " failed to connect: " + output.getConnectionErrorMessage()));
         }
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput runBackendStepOutput, boolean isForResultScreen)
   {
      return (summaryLines);
   }
}
