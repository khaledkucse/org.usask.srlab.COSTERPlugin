package org.usask.srlab.coster.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.usask.srlab.coster.core.COSTER;
import org.usask.srlab.coster.core.infer.ImportStmtComplete;
import org.usask.srlab.coster.core.utils.FileUtil;

public class FixImports extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editorPart = HandlerUtil.getActiveEditor(event);
		IEditorInput input = editorPart.getEditorInput();
		if (input instanceof FileEditorInput) {
		    IFile file = ((FileEditorInput) input).getFile();
		    try {
				InputStream inputStream = file.getContents();
			    InputStreamReader isReader = new InputStreamReader(inputStream);
			    BufferedReader reader = new BufferedReader(isReader);
			    StringBuffer sb = new StringBuffer();
			    String str;
			    while((str = reader.readLine())!= null){
			    	sb.append(str);
			    	sb.append("\n");
			    }
			    
			    String inputFilePath = Config.ROOT_PATH+"data"+File.separator+"repo"+File.separator+"input.java";
			    File inputFile = new File(inputFilePath); 		
	    			    		
	    		inputFilePath = inputFile.getAbsolutePath();
	    		String inputDirectory = inputFilePath.
	    		    substring(0,inputFilePath.lastIndexOf(File.separator))+File.separator;
			    
	    		FileUtil.getSingleTonFileUtilInst().writeToFile(inputFilePath,sb.toString());
			    
			    ArrayList<String> returnedFQNs = (ArrayList<String>) completeImportStatements(inputDirectory);
			    
			    StringBuffer importStmt = new StringBuffer();
			    for(String eachFQN:returnedFQNs)
			    	if(eachFQN.contains(".")) 
			    		importStmt.append("import "+eachFQN+";\n");
			    insertText(editorPart,importStmt.toString());
			    
			    IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
			    ImportLibraryDialog dialog = new ImportLibraryDialog(window.getShell(),returnedFQNs);
				if (dialog.open() == Window.OK) {}
			    
			} catch (Exception e) {
				e.printStackTrace();
			}
		    
		}
		return null;
	}
	
	private static List<String> completeImportStatements(String inputRepo) {
		String jarRepoPath = Config.ROOT_PATH+"data/jars/so/";
		String modelPath = Config.ROOT_PATH+"model/";
		String dictPath = Config.ROOT_PATH+"data/dictonary/so/";
		COSTER.init();
		COSTER.setJarRepoPath(jarRepoPath);
		COSTER.setRepositoryPath(inputRepo);
		COSTER.setModelPath(modelPath);
		COSTER.setContextSimilarity(Config.CONTEXT_SIMILARITY_FUNCTION);
		COSTER.setNameSimilarity(Config.NAME_SIMIALRITY_FUNCTION);
		COSTER.setReccs(Config.TOP_K);
		COSTER.setFqnThreshold(Config.FQN_THRESHOLD);
		
		return ImportStmtComplete.complete(dictPath);
		
	}
	
	private void insertText(IEditorPart editorPart, String importStmt) {
		ITextEditor editor = (ITextEditor)editorPart;
	    IDocumentProvider dp = editor.getDocumentProvider();
	    IDocument doc = dp.getDocument(editor.getEditorInput());
	    FindReplaceDocumentAdapter searchdoc = new FindReplaceDocumentAdapter(doc);
		try {
			String searchTOken = "package .*;";
			IRegion firstLine = searchdoc.find(0, searchTOken, true, false, false,true);
			if(firstLine == null)
				doc.replace(0, 0, importStmt+"\n");
			else {
				int offset = firstLine.getOffset()+firstLine.getLength()+1;
				doc.replace(offset, 0, "\n"+importStmt+"\n");
			}
				
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	    
	}
	
	
}

class ImportLibraryDialog extends Dialog {
	private Text txtDataPath;
	private Text txtFqn;
	private Text txtPotenLib;
    private String dataPath = "";
    private String code = "";
    ArrayList<String> fqns;

	public ImportLibraryDialog(Shell parentShell,ArrayList<String> fqns) {
		super(parentShell);
        this.fqns = fqns;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        GridLayout layout = new GridLayout(2, false);
        layout.numColumns = 2;
        layout.marginRight = 5;
        layout.marginLeft = 10;
        container.setLayout(layout);
        
        
        Label lblFQN = new Label(container, SWT.NONE);
        lblFQN.setLayoutData(new GridData(GridData.CENTER, GridData.CENTER, true, true));
        lblFQN.setText("Fully Qualified Names");
        
        Label lblLibrary = new Label(container, SWT.NONE);
        lblLibrary.setLayoutData(new GridData(GridData.CENTER, GridData.CENTER, true, true));
        lblLibrary.setText("Libraries");

		txtFqn = new Text(container, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		GridData fqnGrid = new GridData(GridData.CENTER, GridData.CENTER, true, true);
		fqnGrid.widthHint = 500;
		fqnGrid.heightHint = 250;
		txtFqn.setLayoutData(fqnGrid);
		
		StringBuilder strFqns = new StringBuilder();
		for(String eachFqn: fqns)
			strFqns.append(eachFqn+"\n");
		txtFqn.setText(strFqns.toString());	
				
		txtFqn.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				code = txtFqn.getText();
			}
			
		});
		
		txtPotenLib = new Text(container, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		GridData potenLibTxt = new GridData(GridData.CENTER, GridData.CENTER, true, true);
		potenLibTxt.widthHint = 500;
		potenLibTxt.heightHint = 250;
		txtPotenLib.setLayoutData(potenLibTxt);
		txtPotenLib.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				code = txtPotenLib.getText();
			}
			
		});
        
		
        return container;
    }

    // override method to use "Login" as label for the OK button
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, "Import", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void okPressed() {
        dataPath = txtDataPath.getText();
        if(txtFqn.getText().trim()!= "")
        	code = txtFqn.getText();
        if(code.equals("")) {
        	super.okPressed();

		}
		else {	
		}
        
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }
    
    public String getCode() {
		return code;
	}

	public void setCode(String jarPath) {
		this.code = jarPath;
	}

}
