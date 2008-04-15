/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.validation.ui.internal;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wst.validation.ValidationResult;
import org.eclipse.wst.validation.internal.ValOperation;
import org.eclipse.wst.validation.internal.ValType;
import org.eclipse.wst.validation.internal.ValidationRunner;
import org.eclipse.wst.validation.internal.operations.ValidatorManager;
import org.eclipse.wst.validation.ui.internal.dialog.ResultsDialog;

/**
 * Run a manual validation. 
 * @author karasiuk
 *
 */
public class ManualValidationRunner extends WorkspaceJob {
	
	/** If we checked the project already it gets placed in this set. */
	private static Set<IProject> _checked = new HashSet<IProject>(20);
	
	private Map<IProject, Set<IResource>> 	_projects;
	private ValType _valType;
	private boolean	_showResults;
	
	/**
	 * Validate the selected projects and/or resources.
	 * 
	 * @param projects
	 *            The selected projects. The key is an IProject and the value is
	 *            the Set of IResources that were selected. Often this will be
	 *            every resource in the project.
	 * 
	 * @param isManual
	 *            Is this a manual validation?
	 * 
	 * @param isBuild
	 *            Is this a build based validation?
	 * 
	 * @param showResults
	 *            When the validation is finished, show the results in a dialog
	 *            box.
	 */
	public static void validate(Map<IProject, Set<IResource>> projects, ValType valType, boolean showResults){
		ManualValidationRunner me = new ManualValidationRunner(projects, valType, showResults);
		me.schedule();
	}
	
	private ManualValidationRunner(Map<IProject, Set<IResource>> projects, ValType valType, boolean showResults){
		super(ValUIMessages.Validation);
		_projects = projects;
		_valType = valType;
		_showResults = showResults;
	}

	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		
		if (_valType == ValType.Manual)projectChecks();
		long start = System.currentTimeMillis();
		final ValOperation vo = ValidationRunner.validate(_projects, _valType, monitor);
		final long time = System.currentTimeMillis() - start;
		int resourceCount = 0;
		for (Set s : _projects.values())resourceCount += s.size();
		final int finalResourceCount = resourceCount;
		if (vo.getResult().isCanceled())return Status.CANCEL_STATUS;
		
		if (_showResults){
			Display display = Display.getDefault();
			Runnable run = new Runnable(){

				public void run() {
					ValidationResult vr = vo.getResult();
					ResultsDialog rd = new ResultsDialog(null, vr, time, finalResourceCount);
					rd.open();
				}
				
			};
			display.asyncExec(run);			
		}
		return Status.OK_STATUS;
	}

	/**
	 * Check to see if we have a validation builder on the projects, and if not add one.
	 */
	private void projectChecks() {
		for (IProject project : _projects.keySet()){
			if (_checked.contains(project))continue;
			_checked.add(project);
			if (!ValidatorManager.doesProjectSupportBuildValidation(project)){
				ValidatorManager.addProjectBuildValidationSupport(project);
			}
		}
		
	}
}