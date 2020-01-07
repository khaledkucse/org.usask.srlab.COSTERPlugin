package org.usask.srlab.coster.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class RetrainModel extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		RetrainDialog dialog = new RetrainDialog(window.getShell());
		
		if (dialog.open() == Window.OK) {
			String dataPath = dialog.getDataPath();
			if(dataPath.trim().equals("")) {
				MessageDialog.openError(window.getShell(),
						"Data Path Not Inserted",
						"You dont select any subject system that will be used for training. Please select the directory where the subject systems need to be trained are stored.");
			}
			else {
				File repoDir = new File(dataPath);
				if(!repoDir.exists()) {
					MessageDialog.openError(window.getShell(),
							"Repository Not Found",
							"No Repository is found. Please try again with valid repository path");

				}
				else if(!repoDir.isDirectory()) {
					MessageDialog.openError(window.getShell(),
							"Directory Required",
							"You have to give the path of the repository directory not any file. Try again");
				}
				else {
					if(dialog.getJarPath().trim().equals("")) {
						retrainModel(repoDir.getAbsolutePath());
					}
					else {
						String jarPath = dialog.getJarPath();
						File jarDir = new File(jarPath);
						if(!jarDir.exists()) {
							MessageDialog.openError(window.getShell(),
									"Jar Repository Not Found",
									"No Jar Repository is found. Please try again with valid repository path");

						}
						else if(!jarDir.isDirectory()) {
							MessageDialog.openError(window.getShell(),
									"Directory Required",
									"You have to give the path of the repository directory not any file. Try again");
						}
						else {
							try {
								List<File> jarFiles = collectJarFiles(jarDir);
								for(File eachJarFile:jarFiles) {
									String name = eachJarFile.getName();
									Path destination = Paths.get(Config.ROOT_PATH+"data/jars/github/"+name);
									Files.copy(eachJarFile.toPath(), destination);
								}
							} catch (IOException e) {
							e.printStackTrace();
							}
							retrainModel(repoDir.getAbsolutePath());
						}
					}
				}	
			}
		}
		
		return null;
	}
	
    public static List<File> collectJarFiles(File jarFiles){
        String[] extensions = { ".jar"};
        HashMap<String, List<File>> allJarFiles = getFilteredRecursiveFiles(jarFiles, extensions);
        List<File> arrJars = allJarFiles.get(".jar");
        return arrJars;
    }
    
    public static HashMap<String,List<File>> getFilteredRecursiveFiles(File parentDir, String [] sourceFileExt){
        HashMap<String,List<File>> recursiveFiles = new HashMap<>();

        File[] childFiles = parentDir.listFiles();
        if (childFiles==null)
            return recursiveFiles;
        for (File file:childFiles){
            if (file.isFile()){
                String ext=isPassFileReturn(file, sourceFileExt);
                if (!ext.isEmpty()) {
                    if(!recursiveFiles.containsKey(ext)){
                        List<File> lstFiles=new ArrayList<>();
                        lstFiles.add(file);
                        recursiveFiles.put(ext,lstFiles);
                    }else{
                        recursiveFiles.get(ext).add(file);
                    }

                }
            }
            else{
                HashMap<String,List<File>> subList = getFilteredRecursiveFiles(file, sourceFileExt);
                for(String strKey:subList.keySet()){
                    List<File> lstSubListFile=subList.get(strKey);
                    if(lstSubListFile.size()>0){
                        if(!recursiveFiles.containsKey(strKey)){
                            recursiveFiles.put(strKey, lstSubListFile);
                        } else{
                            recursiveFiles.get(strKey).addAll(lstSubListFile);
                        }
                    }

                }

            }
        }
        return recursiveFiles;
    }

    private static String isPassFileReturn(File file, String [] sourceFileExt)
    {
        String name = file.getName();
        for (String fileExt:sourceFileExt){
            if (name.endsWith(fileExt)){
                return fileExt;
            }
        }
        return "";
    }
	
	
	private void retrainModel(String dataPath) {
		ProcessBuilder processBuilder = new ProcessBuilder();
		
		String fqnT= Config.FQN_THRESHOLD+"";
		String costerJarFilePath = Config.ROOT_PATH+"COSTER.jar";
		String jarRepoPath = Config.ROOT_PATH+"data/jars/github/";
		String datasetPath = Config.ROOT_PATH+"data/GitHubDataset/dataset/";
		String modelPath = Config.ROOT_PATH+"model/";
        processBuilder.command("java", "-Xmx8000m",
        								"-jar", costerJarFilePath,
        								"-f", "retrain",
        								"-r", dataPath,
        								"-j", jarRepoPath,
        								"-d", datasetPath,
        								"-m", modelPath,
        								"-q", fqnT);
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
	
	

}

class RetrainDialog extends Dialog {
	private Text txtDataPath;
	private Text txtJarPath;
    private String dataPath = "";
    private String jarPath = "";

	public RetrainDialog(Shell parentShell) {
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
        lblDataPath.setText("Path to repository need to be retrained");

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
			    }
			}

		});
        
        txtDataPath = new Text(container, SWT.BORDER);
        GridData dataPathTxt = new GridData(SWT.FILL, SWT.CENTER, true, true,2,2);
        dataPathTxt.widthHint = 500;  // You choose the width
        txtDataPath.setLayoutData(dataPathTxt);
        txtDataPath.setVisible(false);
        
        
        
		
		Label lblJarPath = new Label(container, SWT.NONE);
		lblJarPath.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true,1,3));
		lblJarPath.setText("Path to Jar repository need to be retrained");
		
		Button btnJarPathBrowse = new Button(container, SWT.PUSH);
        btnJarPathBrowse.setText("Browse ...");
        btnJarPathBrowse.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, true,1,4));
        btnJarPathBrowse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(Display.getCurrent().getActiveShell());
			    String path = dialog.open();
			    if (path != null) {
			    	txtJarPath.setText(path);
			    	txtJarPath.setVisible(true);
			    	txtJarPath.setEditable(false);
			    }
			}

		});

		txtJarPath = new Text(container, SWT.BORDER);
		GridData jarPathTxt = new GridData(SWT.FILL, SWT.CENTER, true, true,2,4);
		jarPathTxt.widthHint = 500;  // You choose the width
		txtJarPath.setLayoutData(jarPathTxt);
		txtJarPath.setVisible(false);
		
        return container;
    }

    // override method to use "Login" as label for the OK button
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, "Train", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void okPressed() {
        dataPath = txtDataPath.getText();
        if(txtJarPath.getText().trim()!= "")
        	jarPath = txtJarPath.getText();
        super.okPressed();
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }
    public String getJarPath() {
		return jarPath;
	}

	public void setJarPath(String jarPath) {
		this.jarPath = jarPath;
	}

}
