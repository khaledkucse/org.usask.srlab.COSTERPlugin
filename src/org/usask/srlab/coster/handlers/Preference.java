package org.usask.srlab.coster.handlers;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class Preference extends PreferencePage implements
IWorkbenchPreferencePage{
	Combo comboNumberOfSuggestion, comboContextSimilarity, comboNameSimilarity, combotrainThreshold;
	Text txtDataPath;
	

	@Override
	public void init(IWorkbench workbench) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected Control createContents(Composite container) {
		
		
		Label lblDataPath = new Label(container, SWT.NONE);
        lblDataPath.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true,1,1));
        lblDataPath.setText("Directory Path to supporting files for plugin");

        Button btnDataPathBrowse = new Button(container, SWT.PUSH);
		btnDataPathBrowse.setText("Browse ...");
		btnDataPathBrowse.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, true,1,2));
		btnDataPathBrowse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(Display.getCurrent().getActiveShell());
			    String path = dialog.open();
			    if (path != null) {
			    	txtDataPath.setText(path);
			    	txtDataPath.setVisible(true);
			    	txtDataPath.setEditable(false);
			    	Config.ROOT_PATH = path+"/";
			    }
			}

		});
        
        txtDataPath = new Text(container, SWT.BORDER);
        GridData dataPathTxt = new GridData(SWT.FILL, SWT.CENTER, true, true,2,2);
        dataPathTxt.widthHint = 500;  // You choose the width
        txtDataPath.setLayoutData(dataPathTxt);
        txtDataPath.setVisible(false);
		
		
		
		
		
		
		
		
		Label lblNumberOfSuggestion = new Label(container, SWT.LEFT);
		lblNumberOfSuggestion.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
		lblNumberOfSuggestion.setText("Number of Suggestions");
		
		comboNumberOfSuggestion = new Combo(container, SWT.NONE);
		comboNumberOfSuggestion.setItems(new String[] {"1", "2", "3", "4", "5"});
		comboNumberOfSuggestion.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		comboNumberOfSuggestion.select(0);
		
		Label lblContextSimilarity = new Label(container, SWT.LEFT);
		lblContextSimilarity.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
		lblContextSimilarity.setText("Method to use in Context Similarity");
		
		comboContextSimilarity = new Combo(container, SWT.NONE);
		comboContextSimilarity.setItems(new String[] {"Cosine", "Jaccard Index", "Longest Common Subsequence"});
		comboContextSimilarity.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		comboContextSimilarity.select(0);
		new Label(container, SWT.LEFT);
		
		Label lblNameSimilarity = new Label(container, SWT.LEFT);
		lblNameSimilarity.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
		lblNameSimilarity.setText("Method to use in Name Similarity");
		
		comboNameSimilarity = new Combo(container, SWT.NONE);
		comboNameSimilarity.setItems(new String[] {"Levenshtein Distance", "Hamming Distance", "Longest Common Subsequence"});
		comboNameSimilarity.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		comboNameSimilarity.select(0);
		new Label(container, SWT.LEFT);
		
		Label lbltrianThreshold = new Label(container, SWT.LEFT);
		lbltrianThreshold.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
		lbltrianThreshold.setText("Minimum number of contexts for an FQN to be trained");
		
		combotrainThreshold = new Combo(container, SWT.NONE);
		combotrainThreshold.setItems(new String[] {"10", "25", "50","100","200","500"});
		combotrainThreshold.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		combotrainThreshold.select(2);
		new Label(container, SWT.LEFT);
		
		return container;
	}
	
	@Override
	protected void performDefaults() {
		comboNumberOfSuggestion.select(0);
		comboContextSimilarity.select(0);
		comboNameSimilarity.select(0);
		combotrainThreshold.select(2);	
		performApply();
	}
	
	@Override
	protected void performApply() {
		Config.TOP_K = Integer.parseInt(comboNumberOfSuggestion.getText());
		Config.FQN_THRESHOLD = Integer.parseInt(combotrainThreshold.getText());
		String conSim = comboContextSimilarity.getText();
		if(conSim.equals("Jaccard Index"))
			Config.CONTEXT_SIMILARITY_FUNCTION = "jaccard";
		else if(conSim.equals("Longest Common Subsequence"))
			Config.CONTEXT_SIMILARITY_FUNCTION = "lcs";
		else
			Config.CONTEXT_SIMILARITY_FUNCTION = "cosine";
		
		String nameSim = comboNameSimilarity.getText();
		if(nameSim.equals("Hamming Distance"))
			Config.NAME_SIMIALRITY_FUNCTION = "hamming";
		else if(nameSim.equals("Longest Common Subsequence"))
			Config.NAME_SIMIALRITY_FUNCTION = "lcs";
		else
			Config.NAME_SIMIALRITY_FUNCTION = "levenshtein";
		
		System.out.println("The Pulgin is Configured!!!");
		System.out.println("Root Path: "+ Config.ROOT_PATH);
		System.out.println("Top-K selected: "+ Config.TOP_K);
		System.out.println("Function for Context Similarity: "+ Config.CONTEXT_SIMILARITY_FUNCTION);
		System.out.println("Function for Name Similarity: "+ Config.NAME_SIMIALRITY_FUNCTION);
	}
	@Override
	public boolean performCancel() {
		performDefaults();
		return true;
	}
	@Override
	public boolean performOk() {
		performApply();
		return true;
	}

}
