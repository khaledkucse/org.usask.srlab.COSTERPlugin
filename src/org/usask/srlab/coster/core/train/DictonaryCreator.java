package org.usask.srlab.coster.core.train;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Type;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;
import org.usask.srlab.coster.core.config.Config;
import org.usask.srlab.coster.core.utils.FileUtil;
import org.usask.srlab.coster.core.utils.NotifyingBlockingThreadPoolExecutor;
import org.usask.srlab.coster.core.utils.ParseUtil;


/**
 * This class creates the dictonary of class/classes, methods and field from the jar file in the library and source files.
 *
 * There are couple of class variables.
 * THREAD_POOL_SIZE: Integer variable represent how many threds will be run when the program will run.
 * blockingTimeoutCallback: Call back function that says what will happen if any blocking will happen in the threads
 * pool: The thread pooler that implements the threds
 * numOfJars: Atomic Integer represents number of Jars
 * numOfProjects: Atomic Integer represents number of Projects
 * numOfClasses: Atomic Integer represents number of Classes/Types
 * numOfMethods: Atomic Integer represents number of Methods
 * numOfFields: Atomic Integer represents number of Fields
 * classes: Hashset that contains FQN of all Classes
 * methods: Hashset that contains FQN of all Methods
 * fields: Hashset that contains FQN of all Fields
 * logFilePath: Path of the log file
 */

public class DictonaryCreator {

	private static final int THREAD_POOL_SIZE = 8;

	private static final Callable<Boolean> blockingTimeoutCallback = new Callable<Boolean>() {
		@Override
		public Boolean call() throws Exception {
			return true; // keep waiting
		}
	};
	private static NotifyingBlockingThreadPoolExecutor pool = new NotifyingBlockingThreadPoolExecutor(THREAD_POOL_SIZE, THREAD_POOL_SIZE, 15, TimeUnit.SECONDS, 200, TimeUnit.MILLISECONDS, blockingTimeoutCallback);
	
	private static final AtomicInteger numOfJars = new AtomicInteger(0), numOfProjects = new AtomicInteger(0), numOfClasses = new AtomicInteger(0), numOfMethods = new AtomicInteger(0), numOfFields = new AtomicInteger(0);
	
	private final HashSet<String> classes = new HashSet<>(), methods = new HashSet<>(), fields = new HashSet<>();

	private static String logFilePath = Config.LOG_PATH+"coster_dictonary.log";

    private static void print(Object s){System.out.println(s.toString());}

    /**
     * The only public method of the class that creates dictonary.
     * @param libDirPath: path of the library/jar files
	 * @param libDictOutPath: Path to write the dictonary of the jar files
	 * @param repPath: path of the repository/subject systems
     * @param repoDictPath: Path to write the dictonary of the subject systems files
     //* @param repositoryPath: path of the repository where all the subject systems are reside.
     */
	//public static void createDictonary(String libDirPath, String dictonaryOutpath, String repositoryPath) {
    private static void createDictonary(String libDirPath,String libDictOutPath,String repPath, String repoDictPath) {
        //extractFromJars(libDirPath, libDictOutPath);
		extractFromSource(new File(repPath),repoDictPath);

        System.out.println("Total Jars: " + numOfJars);
		System.out.println("Total Projects: " + numOfProjects);
		System.out.println("Total Classes/Types: " + numOfClasses);
		System.out.println("Total Methods: " + numOfMethods);
		System.out.println("Total Fields: " + numOfFields);
	}

    /**
     * The method that collects dictonary firstly from jar file and then from jre
     * @param libraryDirPath: path of the library/jar files
     * @param dictonaryOutpath: path of the dictonary directory where class/classes, methods, fields will be stored
     */
	private static void extractFromJars(String libraryDirPath, String dictonaryOutpath) {
		extractFromJar(new File(libraryDirPath), dictonaryOutpath);
		String jrePath = System.getProperty("java.home") + "/lib";
		File dir = new File(jrePath);
		extractFromJar(dir, dictonaryOutpath);
	}

    /**
     * For each jar file it decompiles the jar file and put the values in classes, methods, fields hashsets
     * @param jarLocation: location of the jar file
     * @param dictonaryOutpath: Path where it will store the value
     */
	private static void extractFromJar(final File jarLocation, String dictonaryOutpath) {
		if (jarLocation.isDirectory()) {
			for (File sub : jarLocation.listFiles())
				extractFromJar(sub, dictonaryOutpath);
		}
		else if (jarLocation.getName().endsWith(".jar")) {
			numOfJars.incrementAndGet();
			final File out = new File(dictonaryOutpath);
			//if (new File(out, jarLocation.getName() + "-classes").exists())
			//	return;
			final String jarFilePath = jarLocation.getAbsolutePath();
			FileUtil.getSingleTonFileUtilInst().appendLineToFile(logFilePath,"Parsing jar file " + jarFilePath);
            pool.execute(new Runnable() {
				@Override
				public void run() {
					try {
						DictonaryCreator dictonaryCreator = new DictonaryCreator();
						dictonaryCreator.extractFromJarFile(jarFilePath);

                        FileUtil.getSingleTonFileUtilInst().appendLineToFile(logFilePath,"Jars: " + numOfJars);
                        FileUtil.getSingleTonFileUtilInst().appendLineToFile(logFilePath,"Projects: " + numOfProjects);
                        FileUtil.getSingleTonFileUtilInst().appendLineToFile(logFilePath,"Classes: " + numOfClasses);
                        FileUtil.getSingleTonFileUtilInst().appendLineToFile(logFilePath,"Methods: " + numOfMethods);
                        FileUtil.getSingleTonFileUtilInst().appendLineToFile(logFilePath,"Fields: " + numOfFields);

						FileUtil.getSingleTonFileUtilInst().writeToFile(new File(out, jarLocation.getName() + "-classes").getAbsolutePath(), dictonaryCreator.classes);
						FileUtil.getSingleTonFileUtilInst().writeToFile(new File(out, jarLocation.getName() + "-methods").getAbsolutePath(), dictonaryCreator.methods);
						FileUtil.getSingleTonFileUtilInst().writeToFile(new File(out, jarLocation.getName() + "-fields").getAbsolutePath(), dictonaryCreator.fields);
					} catch (Throwable t) {
					    FileUtil.getSingleTonFileUtilInst().appendLineToFile(logFilePath,"Error in parsing jar file " + jarFilePath);
                        FileUtil.getSingleTonFileUtilInst().appendLineToFile(logFilePath,t.getMessage());
					}
				}
			});
		}
	}

    /**
     * Supporting method of extractFromJar that decompile the jar and bind classes, methods and fields
     * @param jarFilePath: Path of the jar file
     * @throws IOException: If file read is intrupted
     */
	private void extractFromJarFile(final String jarFilePath) throws IOException {
		JarFile jarFile = new JarFile(jarFilePath);
		Enumeration<JarEntry> entries = jarFile.entries();
		while(entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			if(entry.getName().endsWith(".class")) {
				try {
					ClassParser parser = new ClassParser(jarFilePath, entry.getName());
					JavaClass jc = parser.parse();
					String pn = jc.getPackageName();
					if ((jc.isPublic() || jc.isProtected()) && !jc.isAnonymous() && pn != null && !pn.isEmpty()) {
						String className = jc.getClassName();
						className = className.replace('$', '.');
						classes.add(className);
						numOfClasses.incrementAndGet();
						for (Field field : jc.getFields())
							if (field.isPublic() || field.isProtected())
								extractFieldDeclerationFromJar(field, className);
						for (Method method : jc.getMethods())
							if (method.isPublic() || method.isProtected())
								extractMethodDeclerationFromJar(method, className);
					}
				} catch (IOException | ClassFormatException e) {
//							System.err.println("Error in parsing class file: " + entry.getName());
//							System.err.println(e.getMessage());
				}
			}
		}
		jarFile.close();
	}

    /**
     * method that extract source code from the project.
     * @param repository: Path of the repository
     * @param dictonaryOutpath: Path where dictonary will be written
     */
	private static void extractFromSource(final File repository, String dictonaryOutpath) {
	    if (repository.isDirectory()) {
            if (new File(repository, ".git").exists() || new File(repository, ".gitignore").exists()) {
                numOfProjects.incrementAndGet();
                FileUtil.getSingleTonFileUtilInst().appendLineToFile(logFilePath,"Projects: " + numOfProjects);
                final File out = new File(dictonaryOutpath);
                if (new File(out, repository.getParentFile().getName() + "___" + repository.getName() + "-classes").exists())
                    return;
                FileUtil.getSingleTonFileUtilInst().appendLineToFile(logFilePath,"Parsing project " + repository.getAbsolutePath());
                pool.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            DictonaryCreator dictonaryCreator = new DictonaryCreator();
							dictonaryCreator.extractFromSourceProject(repository);

                            FileUtil.getSingleTonFileUtilInst().appendLineToFile(logFilePath,"Jars: " + numOfJars);
                            FileUtil.getSingleTonFileUtilInst().appendLineToFile(logFilePath,"Projects: " + numOfProjects);
                            FileUtil.getSingleTonFileUtilInst().appendLineToFile(logFilePath,"Classes: " + numOfClasses);
                            FileUtil.getSingleTonFileUtilInst().appendLineToFile(logFilePath,"Methods: " + numOfMethods);
                            FileUtil.getSingleTonFileUtilInst().appendLineToFile(logFilePath,"Fields: " + numOfFields);


                            FileUtil.getSingleTonFileUtilInst().writeToFile(new File(out, repository.getParentFile().getName() + "___" + repository.getName() + "-classes").getAbsolutePath(), dictonaryCreator.classes);
                            FileUtil.getSingleTonFileUtilInst().writeToFile(new File(out, repository.getParentFile().getName() + "___" + repository.getName() + "-methods").getAbsolutePath(), dictonaryCreator.methods);
                            FileUtil.getSingleTonFileUtilInst().writeToFile(new File(out, repository.getParentFile().getName() + "___" + repository.getName() + "-fields").getAbsolutePath(), dictonaryCreator.fields);
                        } catch (Throwable t) {
                            FileUtil.getSingleTonFileUtilInst().appendLineToFile(logFilePath,"Error in parsing project " + repository.getAbsolutePath());
                            FileUtil.getSingleTonFileUtilInst().appendLineToFile(logFilePath,t.getMessage());
                        }
                    }
                });
            }
            else
                for (File sub : repository.listFiles())
                    extractFromSource(sub,dictonaryOutpath);
        }
	}

    /**
     * Method that bind from a single project file
     * @param projectFile: Path of the project file
     */
	private void extractFromSourceProject(File projectFile) {
		ArrayList<File> files = ParseUtil.collectJavaFiles(projectFile);
		String[] paths = new String[files.size()];
		for (int i = 0; i < files.size(); i++) {
			paths[i] = files.get(i).getAbsolutePath();
		}

		final HashSet<CompilationUnit> cus = new HashSet<>();
		FileASTRequestor r = new FileASTRequestor() {
			@Override
			public void acceptAST(String sourceFilePath, CompilationUnit cu) {
				cus.add(cu);
			}
		};

		@SuppressWarnings("rawtypes")
		Map options = JavaCore.getOptions();
		options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
		options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setCompilerOptions(options);
		parser.setEnvironment(new String[0], new String[]{}, new String[]{}, true);
		parser.setIgnoreMethodBodies(true);
		parser.setResolveBindings(true);
		parser.createASTs(paths, null, new String[0], r, null);

		for (CompilationUnit cu : cus)
			for (int i = 0 ; i < cu.types().size(); i++)
				extractTypeDecleration((AbstractTypeDeclaration) cu.types().get(i), cu.getPackage() == null ? "" : cu.getPackage().getName().getFullyQualifiedName() + ".");
	}

    /**
     * Method the extract the type decleration from a .java file
     * @param type: The class name
     * @param prefix: String having previous value while recursing
     */
	private void extractTypeDecleration(AbstractTypeDeclaration type, String prefix) {
		if (!Modifier.isPublic(type.getModifiers()) && !Modifier.isProtected(type.getModifiers()))
			return;
		ITypeBinding tb = type.resolveBinding();
		if (tb == null)
			return;
		tb = tb.getTypeDeclaration();
		if (tb == null)
			return;
		if (tb.isAnonymous() || tb.isLocal())
			return;
		String fqn = getQualifiedName(tb);
		if (tb.isNested())
			fqn = getQualifiedName(tb.getDeclaringClass().getTypeDeclaration()) + "$" + tb.getName();
		classes.add(fqn);
		numOfClasses.incrementAndGet();
		for (int i = 0; i < type.bodyDeclarations().size(); i++) {
			BodyDeclaration bd = (BodyDeclaration) type.bodyDeclarations().get(i);
			if (!Modifier.isPublic(bd.getModifiers()) && !Modifier.isProtected(bd.getModifiers()))
				return;
			if (bd instanceof FieldDeclaration)
				extractFieldDecleration((FieldDeclaration) bd);
			else if (bd instanceof MethodDeclaration)
				extractMethodDecleration((MethodDeclaration) bd);
			else if (bd instanceof AbstractTypeDeclaration)
				extractTypeDecleration((AbstractTypeDeclaration) bd, prefix);
		}
	}

    /**
     * Method the extract the field decleration from a .java file
     * @param fieldDeclerration: Field Decleration Node
     */
	private void extractFieldDecleration(FieldDeclaration fieldDeclerration) {
		for (int j = 0; j < fieldDeclerration.fragments().size(); j++) {
			VariableDeclarationFragment vdf = (VariableDeclarationFragment) fieldDeclerration.fragments().get(j);
			IVariableBinding vb = vdf.resolveBinding();
			if (vb == null) 
				continue;
			vb = vb.getVariableDeclaration();
			fields.add(getFieldSignature(vb));
			numOfFields.incrementAndGet();
		}
	}

    /**
     * Create the field deceleration for the dictonary
     * @param vb: Type binded Field
     * @return String represents the FQN of the field
     */
	private static String getFieldSignature(IVariableBinding vb) {
		return getQualifiedName(vb.getDeclaringClass().getTypeDeclaration()) + "." + vb.getName() + " " + getQualifiedName(vb.getType().getTypeDeclaration());
	}

    /**
     * Method the extract the method decleration from a .java file
     * @param method: Method Decleration node
     */
	private void extractMethodDecleration(MethodDeclaration method) {
		IMethodBinding mb = method.resolveBinding();
		if (mb == null)
			return;
		mb = mb.getMethodDeclaration();
		methods.add(getMethodSignature(mb));
		numOfMethods.incrementAndGet();
	}
    /**
     * Create the method decleration for the dictonary
     * @param mb: Type binded method
     * @return String represents the FQN of the method decleration
     */
	private static String getMethodSignature(IMethodBinding mb) {
		StringBuilder sb = new StringBuilder();
		sb.append(getQualifiedName(mb.getDeclaringClass().getTypeDeclaration()) + "." + mb.getName() + " (");
		for (ITypeBinding tb : mb.getParameterTypes())
			sb.append(getQualifiedName(tb.getTypeDeclaration()) + ",");
		sb.append(") " + getQualifiedName(mb.getReturnType().getTypeDeclaration()));
		return sb.toString();
	}

	private static String getQualifiedName(ITypeBinding tb) {
		if (tb.isArray()) {
			return tb.getElementType().getTypeDeclaration().getQualifiedName() + "[" + tb.getDimensions() + "]";
		} else 
			return tb.getQualifiedName();
	}
    /**
     * Method the extract the field decleration from a jar file
     * @param field: Field Decleration node
     * @param fqn: Fully Qualified Name of the Class
     */
	private void extractFieldDeclerationFromJar(Field field, String fqn) {
		fields.add(getFieldSignatureFromJar(field, fqn));
		numOfFields.incrementAndGet();
	}
    /**
     * Create the field decleration for the dictonary
     * @param field: Type binded Field
     * @param fqn: Fully Qualified Name of the Class
     * @return String represents the FQN of the field
     */
	private static String getFieldSignatureFromJar(Field field, String fqn) {
		return fqn + "." + field.getName() + " " + field.getType().toString();
	}
    /**
     * Method the extract the method decleration from a jar file
     * @param method: Method Decleration node
     * @param fqn: Fully Qualified Name of the Class
     */
	private void extractMethodDeclerationFromJar(Method method, String fqn) {
		methods.add(getMethodSignatureFromJar(method, fqn));
		numOfMethods.incrementAndGet();
	}
    /**
     * Create the method decleration for the dictonary
     * @param method: Type binded Method
     * @param fqn: Fully Qualified Name of the Class
     * @return String represents the FQN of the method
     */
	private static String getMethodSignatureFromJar(Method method, String fqn) {
		StringBuilder sb = new StringBuilder();
		sb.append(fqn + "." + method.getName() + " (");
		for (Type type : method.getArgumentTypes())
			sb.append(type.toString() + ",");
		sb.append(") " + method.getReturnType().toString());
		return sb.toString();
	}

    public static void main(String[] args) {
        DictonaryCreator.createDictonary(Config.FULL_JAR_PATH,Config.JAR_DICTONARY_PATH,Config.GITHUB_SUBJECT_SYSTEM_PATH,Config.SUBJECT_SYSTEM_DICTONARY_PATH);
    }

}
