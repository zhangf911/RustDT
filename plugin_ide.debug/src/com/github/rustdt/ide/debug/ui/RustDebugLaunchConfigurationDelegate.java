package com.github.rustdt.ide.debug.ui;


import melnorme.lang.ide.debug.core.AbstractLangDebugLaunchConfigurationDelegate;

import org.eclipse.cdt.dsf.gdb.launching.GdbLaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ISourceLocator;

public class RustDebugLaunchConfigurationDelegate extends AbstractLangDebugLaunchConfigurationDelegate {
	
	@Override
	protected GdbLaunch doCreateGdbLaunch(ILaunchConfiguration configuration, String mode, ISourceLocator locator) {
		return new RustGdbLaunch(configuration, mode, locator);
	}
	
}