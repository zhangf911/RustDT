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
package melnorme.lang.ide.ui.text.completion;


import static melnorme.utilbox.core.Assert.AssertNamespace.assertNotNull;
import static melnorme.utilbox.core.Assert.AssertNamespace.assertTrue;

import java.text.MessageFormat;
import java.util.List;

import melnorme.lang.ide.ui.LangUIMessages;
import melnorme.lang.ide.ui.editor.EditorUtils;
import melnorme.utilbox.collections.ArrayList2;
import melnorme.utilbox.collections.Indexable;
import melnorme.utilbox.misc.ArrayUtil;

import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionListenerExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistantExtension2;
import org.eclipse.jface.text.contentassist.IContentAssistantExtension3;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

public class LangContentAssistProcessor extends ContenAssistProcessorExt {
	
	protected final ContentAssistant contentAssistant;
	protected final IEditorPart editor; // can be null
	protected final Indexable<CompletionProposalsGrouping> categories;
	
	public LangContentAssistProcessor(ContentAssistant contentAssistant, 
			IEditorPart fEditor, Indexable<CompletionProposalsGrouping> groupings) {
		this.contentAssistant = assertNotNull(contentAssistant);
		this.editor = fEditor;
		this.categories = groupings;
		assertTrue(categories != null && categories.size() > 0);
		
		contentAssistant.addCompletionListener(new CompletionSessionListener());
	}
	
	public static abstract class ContentAssistCategoriesBuilder {
		
		public ArrayList2<CompletionProposalsGrouping> getCategories() {
			ArrayList2<CompletionProposalsGrouping> categories = new ArrayList2<>();
			categories.addIfNotNull(createDefaultCategory());
			categories.addIfNotNull(createSnippetsCategory());
			return categories;
		}
		
		protected CompletionProposalsGrouping createDefaultCategory() {
			ArrayList2<ILangCompletionProposalComputer> computers = createDefaultCategoryComputers();
			return new CompletionProposalsGrouping("default", 
				LangUIMessages.ContentAssistProcessor_defaultProposalCategory, null, computers);
		}
		
		protected ArrayList2<ILangCompletionProposalComputer> createDefaultCategoryComputers() {
			ArrayList2<ILangCompletionProposalComputer> computers = new ArrayList2<>();
			computers.addIfNotNull(createDefaultSymbolsProposalComputer());
			computers.addIfNotNull(createSnippetsProposalComputer());
			return computers;
		}
		
		protected abstract ILangCompletionProposalComputer createDefaultSymbolsProposalComputer();
		
		protected CompletionProposalsGrouping createSnippetsCategory() {
			ArrayList2<ILangCompletionProposalComputer> computers = new ArrayList2<>();
			computers.addIfNotNull(createSnippetsProposalComputer());
			
			if(computers.isEmpty()) {
				return null;
			}
			
			return new CompletionProposalsGrouping("snippets", 
				LangUIMessages.ContentAssistProcessor_snippetsProposalCategory, null, computers);
		}
		
		protected abstract ILangCompletionProposalComputer createSnippetsProposalComputer();
		
	}
	
	/* -----------------  ----------------- */
	
	protected int invocationIteration = 0;
	
	protected class CompletionSessionListener implements ICompletionListener, ICompletionListenerExtension {
		
		public CompletionSessionListener() {
		}
		
		@Override
		public void assistSessionStarted(ContentAssistEvent event) {
			if(event.processor != LangContentAssistProcessor.this)
				return;
			
			invocationIteration = 0;
			
			if (event.assistant instanceof IContentAssistantExtension2) {
				IContentAssistantExtension2 extension = (IContentAssistantExtension2) event.assistant;
				
				KeySequence binding = getGroupingIterationBinding();
				boolean repeatedModeEnabled = categories.size() > 1;
				
				setRepeatedModeStatus(extension, repeatedModeEnabled, binding);
			}
			
			listener_assistSessionStarted();
		}
		
		protected void setRepeatedModeStatus(IContentAssistantExtension2 caExt2, boolean enabled, KeySequence binding) {
			caExt2.setShowEmptyList(enabled);
			
			caExt2.setRepeatedInvocationMode(enabled);
			caExt2.setStatusLineVisible(enabled);
			if(enabled) {
				caExt2.setStatusMessage(createIterationMessage());
			}
			if (caExt2 instanceof IContentAssistantExtension3) {
				IContentAssistantExtension3 ext3 = (IContentAssistantExtension3) caExt2;
				ext3.setRepeatedInvocationTrigger(binding);
			}
		}
		
		@Override
		public void assistSessionRestarted(ContentAssistEvent event) {
			invocationIteration = 0;
		}
		
		@Override
		public void assistSessionEnded(ContentAssistEvent event) {
			if(event.processor != LangContentAssistProcessor.this)
				return;
			
			invocationIteration = 0;
			
			listener_assistSessionEnded();
		}
		
		@Override
		public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
		}
		
	}
	
	protected CompletionProposalsGrouping getCurrentCategory() {
		return getCategory(invocationIteration);
	}
	
	protected CompletionProposalsGrouping getCategory(int categoryIndex) {
		assertTrue(categoryIndex >= 0);
		int cappedIndex = categoryIndex % categories.size();
		return categories.get(cappedIndex);
	}
	
	/* -----------------  ----------------- */
	
	protected void listener_assistSessionStarted() {
		for(CompletionProposalsGrouping cat : categories) {
			cat.sessionStarted();
		}
	}
	
	protected void listener_assistSessionEnded() {
		for(CompletionProposalsGrouping cat : categories) {
			cat.sessionEnded();
		}
	}
	
	@Override
	protected void resetComputeState() {
		super.resetComputeState();
		
		// These messages are iteration specific, so they need to be reset:
		contentAssistant.setStatusMessage(createIterationMessage());
		contentAssistant.setEmptyMessage(createEmptyMessage());
	}
	
	/* -----------------  ----------------- */
	
	protected LangContentAssistInvocationContext createContext(ITextViewer viewer, int offset) {
		return new LangContentAssistInvocationContext(viewer, offset, editor);
	}
	
	@Override
	protected ICompletionProposal[] doComputeCompletionProposals(ITextViewer viewer, int offset) {
		LangContentAssistInvocationContext context = createContext(viewer, offset);
		
		CompletionProposalsGrouping cat = getCurrentCategory();
		invocationIteration++;
		
		List<ICompletionProposal> proposals = cat.computeCompletionProposals(context);
		setAndDisplayErrorMessage(cat.getErrorMessage());
		
		return ArrayUtil.createFrom(proposals, ICompletionProposal.class);
	}
	
	@Override
	protected IContextInformation[] doComputeContextInformation(ITextViewer viewer, int offset) {
		LangContentAssistInvocationContext context = createContext(viewer, offset);
		
		CompletionProposalsGrouping cat = getCurrentCategory();
		invocationIteration++;
		
		List<IContextInformation> proposals = cat.computeContextInformation(context);
		setAndDisplayErrorMessage(cat.getErrorMessage());

		return ArrayUtil.createFrom(proposals, IContextInformation.class);
	}
	
	@Override
	public IContextInformationValidator getContextInformationValidator() {
		return null; // TODO: need to add proper support for this
	}
	
	protected void setAndDisplayErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
		if(editor instanceof AbstractTextEditor) {
			AbstractTextEditor abstractTextEditor = (AbstractTextEditor) editor;
			EditorUtils.setStatusLineErrorMessage(abstractTextEditor, errorMessage, null);
		}
	}
	
	/* ----------------- Messages ----------------- */
	
	
	protected String createEmptyMessage() {
		if(invocationIteration == 0) {
			return MessageFormat.format(LangUIMessages.ContentAssistProcessor_emptyDefaultProposals, 
				getCurrentCategory().getName());
		}
		
		return MessageFormat.format(LangUIMessages.ContentAssistProcessor_empty_message, 
			getCurrentCategory().getName());
	}
	
	protected String createIterationMessage() {
		TriggerSequence binding = getGroupingIterationBinding();
		String nextCategoryLabel = getCategory(invocationIteration + 1).getName();
		
		if(binding == null) {
			return MessageFormat.format(LangUIMessages.ContentAssistProcessor_toggle_affordance_click_gesture, 
				getCurrentCategory().getName(), nextCategoryLabel, null);
		} else {
			return MessageFormat.format(LangUIMessages.ContentAssistProcessor_toggle_affordance_press_gesture, 
				getCurrentCategory().getName(), nextCategoryLabel, binding.format());
		}
	}
	
	protected KeySequence getGroupingIterationBinding() {
		IBindingService bindingSvc = (IBindingService) PlatformUI.getWorkbench().getAdapter(IBindingService.class);
		TriggerSequence binding = bindingSvc.getBestActiveBindingFor(
			ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		if(binding instanceof KeySequence)
			return (KeySequence) binding;
		return null;
    }
	
}