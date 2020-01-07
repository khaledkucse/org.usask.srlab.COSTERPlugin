package org.usask.srlab.coster.handlers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class InferTypes extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		SaveResultDialog dialog = new SaveResultDialog(window.getShell());
		if (dialog.open() == Window.OK) {}
		return null;
	}

}

class SaveResultDialog extends Dialog {
	private Text txtDataPath;
	private Text txtCode;
    private String dataPath = "";
    private String code = "";

	public SaveResultDialog(Shell parentShell) {
        super(parentShell);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        GridLayout layout = new GridLayout(2, false);
        layout.marginRight = 5;
        layout.marginLeft = 10;
        container.setLayout(layout);

        Label lblDataPath = new Label(container, SWT.NONE);
        lblDataPath.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true,1,1));
        lblDataPath.setText("Please either browse the code snippet file or paste the code in the following text box");

        Button btnDataPathBrowse = new Button(container, SWT.PUSH);
		btnDataPathBrowse.setText("Browse ...");
		btnDataPathBrowse.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, true,1,2));
		btnDataPathBrowse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell());
			    String path = dialog.open();
			    if (path != null) {
			    	txtDataPath.setText(path);
			    	txtDataPath.setVisible(true);
			    	txtDataPath.setEditable(false);
			    	String content = readFromFile(path);
			    	txtCode.setText(content);
			    }
			}

		});
        
        txtDataPath = new Text(container, SWT.BORDER);
        GridData dataPathTxt = new GridData(SWT.FILL, SWT.CENTER, true, true,2,2);
        dataPathTxt.widthHint = 500;  // You choose the width
        txtDataPath.setLayoutData(dataPathTxt);
        txtDataPath.setVisible(false);


		txtCode = new Text(container, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		GridData codeTxt = new GridData(GridData.FILL_BOTH);
		codeTxt.widthHint = 500;
		codeTxt.heightHint = 500;
		txtCode.setLayoutData(codeTxt);
		txtCode.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				code = txtCode.getText();
			}
			
		});
		
        return container;
    }
    
    protected String readFromFile(String dataPath) {
    	ArrayList<String> content = FixImports.getFileStringArray(dataPath);
    	StringBuffer sb = new StringBuffer();
    	for(String eachLine:content)
    		sb.append(eachLine+"\n");
    	
    	return sb.toString();
    }

    // override method to use "Login" as label for the OK button
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, "Infer", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void okPressed() {
        dataPath = txtDataPath.getText();
        if(txtCode.getText().trim()!= "")
        	code = txtCode.getText();
        if(code.equals("")) {
        	super.okPressed();

		}
		else {
			FixImports.writeToFile(Config.ROOT_PATH+"data/repo/input.java", code);
			inferTypes(Config.ROOT_PATH+"data/repo/input.java");
			String content = readFromFile(Config.ROOT_PATH+"data/repo/output.java");
			txtCode.setText(content);
			
		}
        
    }
    private void inferTypes(String dataPath) {
		ProcessBuilder processBuilder = new ProcessBuilder();
		
		String topk = Config.TOP_K+"";
		String costerJarFilePath = Config.ROOT_PATH+"COSTER.jar";
		String jarRepoPath = Config.ROOT_PATH+"data/jars/github/";
		String outputFilePath = Config.ROOT_PATH+"data/repo/output.java";
		String modelPath = Config.ROOT_PATH+"model/";
		
        processBuilder.command("java", "-jar", costerJarFilePath,
						        		"-f", "infer",
						        		"-i", dataPath,
						        		"-o", outputFilePath,
						        		"-j", jarRepoPath,
						        		"-m", modelPath,
						        		"-t", topk,
						        		"-c", Config.CONTEXT_SIMILARITY_FUNCTION,
						        		"-n", Config.NAME_SIMIALRITY_FUNCTION);
        try {

            Process process = processBuilder.start();

			// blocked :(
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            BufferedReader error =
                    new BufferedReader(new InputStreamReader(process.getErrorStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            System.out.println("\nExited with error code : " + exitCode);
            
            if(exitCode != 0) {
                while ((line = error.readLine()) != null) {
                    System.out.println(line);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
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

