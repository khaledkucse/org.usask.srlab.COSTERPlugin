package org.usask.srlab.coster.handlers;

import java.io.File;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class About extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		MessageDialog.openInformation(
		window.getShell(),
		"COSTER",
		"Context Sensitive Type Solver\n"
		+ "Developed By C M Khaled Saifullah, Muhammad Assaduzzaman, Chanchal K. Roy\n"
		+ "SRLab, University of Saskatchewan, Canada");
		
		String path = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().getAbsolutePath()+File.separator+"model";
		
		System.out.println(path);
		return null;
	}

}
