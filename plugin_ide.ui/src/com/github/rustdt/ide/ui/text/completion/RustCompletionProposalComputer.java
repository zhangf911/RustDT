/*******************************************************************************
 * Copyright (c) 2015, 2015 Bruno Medeiros and other Contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bruno Medeiros - initial API and implementation
 *******************************************************************************/
package com.github.rustdt.ide.ui.text.completion;

import melnorme.lang.ide.core.operations.DaemonEnginePreferences;
import melnorme.lang.ide.core.operations.TimeoutProgressMonitor;
import melnorme.lang.ide.ui.text.completion.LangCompletionProposalComputer;
import melnorme.lang.ide.ui.text.completion.LangContentAssistInvocationContext;
import melnorme.lang.tooling.completion.LangCompletionResult;
import melnorme.utilbox.concurrency.OperationCancellation;
import melnorme.utilbox.core.CommonException;
import melnorme.utilbox.misc.Location;

import org.eclipse.core.runtime.CoreException;

import com.github.rustdt.ide.core.operations.RustSDKPreferences;
import com.github.rustdt.tooling.ops.RacerCompletionOperation;

public class RustCompletionProposalComputer extends LangCompletionProposalComputer {
	
	@Override
	protected LangCompletionResult doComputeProposals(LangContentAssistInvocationContext context, int offset,
			TimeoutProgressMonitor pm) throws CoreException, CommonException, OperationCancellation {
		
		context.getEditor_nonNull().doSave(pm);
		
		String racerPath = DaemonEnginePreferences.DAEMON_PATH.get();
		String sdkSrcPath = RustSDKPreferences.SDK_SRC_PATH.get();
		
		int line_0 = context.getInvocationLine_0();
		int col_0 = context.getInvocationColumn_0();
		Location fileLocation = context.getEditorInputLocation();
		
		RacerCompletionOperation racerCompletionOp = new RacerCompletionOperation(new ToolProcessRunner(pm), 
			racerPath, sdkSrcPath, offset, line_0, col_0, fileLocation);
		return racerCompletionOp.executeAndProcessOutput();
	}
	
}