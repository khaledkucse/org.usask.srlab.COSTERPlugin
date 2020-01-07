package org.usask.srlab.coster.handlers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

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
			    
			    String inputFilePath = Config.ROOT_PATH+"data/repo/input.java";
			    String outputFilePath = Config.ROOT_PATH+"data/repo/output.java";
			    File inputFile = new File(inputFilePath); 		
	    			    		
	    		inputFilePath = inputFile.getAbsolutePath();
	    		String inputDirectory = inputFilePath.
	    		    substring(0,inputFilePath.lastIndexOf(File.separator))+File.separator;
			    
	    		writeToFile(inputFilePath,sb.toString());
			    	
			    
			    File outputFile = new File(outputFilePath); 
			    outputFilePath = outputFile.getAbsolutePath();
			    
			    completeImportStatement(inputDirectory, outputFile.getAbsolutePath());
			    
			    ArrayList<String> returnedFQNs = getFileStringArray(outputFilePath);
			    
			    StringBuffer importStmt = new StringBuffer();
			    for(String eachFQN:returnedFQNs)
			    	if(eachFQN.contains(".")) 
			    		importStmt.append("import "+eachFQN+";\n");
			    insertText(editorPart,importStmt.toString());
			    
			} catch (Exception e) {
				e.printStackTrace();
			}
		    
		}
		return null;
	}
	
	public static void writeToFile(String path, String content) {
		BufferedWriter bf = null;
		try {
			bf = new BufferedWriter(new FileWriter(new File(path), false));
			bf.write(content);

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			try {
				if (bf != null)
					bf.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	private void completeImportStatement(String inputRepo, String outputFilePath) {
		ProcessBuilder processBuilder = new ProcessBuilder();
		
		String costerJarFilePath = Config.ROOT_PATH+"COSTER.jar";
		String jarRepoPath = Config.ROOT_PATH+"data/jars/github/";

        processBuilder.command("java", "-jar", costerJarFilePath,
        								"-f","complete",
        								"-i",inputRepo,
        								"-o",outputFilePath,
        								"-j",jarRepoPath);
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
	
	public static ArrayList<String> getFileStringArray(String fp) {
		ArrayList<String> lstResults = new ArrayList<String>();
		try {
			try (BufferedReader br = new BufferedReader(new FileReader(fp))) {
				String line;
				while ((line = br.readLine()) != null) {
					// process the line.
					// strResult+=line+"\n";
					if (!line.trim().isEmpty()) {
						lstResults.add(line.trim());
					}
				}
			}
		} catch (Exception ex) {
			// ex.printStackTrace();
		}
		return lstResults;

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
