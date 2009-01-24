package goat.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.HashMap;

/**
 * <p>Loads and unloads Modules. Provides methods allowing you
 * to find out what Modules are loaded, to control the Modules, and
 * to return them.<p>
 * @version <p>Date: 17-Dec-2003</p>
 * @author <p><b>? Barry Corrigan</b> All Rights Reserved.</p>
 *
 */
public class ModuleController  {

	private ExecutorService pool = Executors.newCachedThreadPool();
	
	public ExecutorService getPool() {
		return pool;
	}

	private ArrayList<Module> loadedModules = new ArrayList<Module>();
	
	private ArrayList<Class<?>> allModules = new ArrayList<Class<?>>() ;
	private ArrayList<String> allCommands = new ArrayList<String>() ;
	
	public ModuleController() {
		buildAllModulesList() ;
		buildAllCommandsList() ;
		
		BotStats.modules = getAllModules();
		BotStats.commands = getAllCommands();
	}	
	
    /**
	 * Loads a module.
	 *
	 * @param moduleName
	 * @return
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws ClassNotFoundException
	 * @throws NoClassDefFoundError
	 * @throws ClassCastException
	 */
	public Module load(String moduleName) 
	throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		if(! moduleName.startsWith("goat.module."))
			moduleName = "goat.module." + moduleName;
		return load(Class.forName(moduleName));
	}

	public Module loadInAllChannels(String moduleName) 
	throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		Module ret = load(moduleName);
		if (null == ret) // wasn't loaded
			ret = getLoaded(moduleName);
		ret.inAllChannels = true;
		return ret;
	}
	
	public Module load(Class<?> modClass) throws IllegalAccessException, InstantiationException {
		if (null == modClass)
			return null;
		System.out.print("Loading Module " + modClass.getName() + " ... ");
		
		Module module = null;
		// return null if module is already loaded
		if(null != getLoaded(modClass))
			return null;

		module = (Module) modClass.newInstance();

		if(null == module)
			return null;
		if(module.isThreadSafe()) {
			System.out.print("is threaded ... ");
 			pool.execute(module);
 			while(! module.isRunning()) // wait to make sure module is running before adding it to loaded module list
 				try {
 					Thread.sleep(5);  // short wait, it shouldn't take too long for the thread pool to start the module
 				} catch (InterruptedException ie) {}
 			System.out.print("running ... ");
		}
		loadedModules.add(module);
		System.out.println("loaded.");
		return module;
	}
	
	public Module loadInAllChannels(Class<?> modClass)
	throws IllegalAccessException, InstantiationException {
		Module ret = load(modClass);
		if(null != ret)
			ret.inAllChannels = true;
		return ret;
	}
	
	public boolean unload(String moduleName) {
		Module mod = getLoaded(moduleName);
		if (null == mod)
			return false ;
		else {
			loadedModules.remove(mod) ;
			if(mod.isThreadSafe())
				mod.stopThread();
		}
		return true ;
	}

	public String[] lsmod() {
		Module mod;
		String[] modNames = new String[loadedModules.size()];
		for(int i=0;i<loadedModules.size();i++) {
			mod = loadedModules.get(i);
			modNames[i] = mod.getClass().getName();
		}
		return modNames;
	}

	public Module getLoaded(int i) {
		return loadedModules.get(i);
	}
	
	public Module getLoaded(Class<?> modClass) {
		Iterator<Module> it = loadedModules.listIterator();
		Module ret = null;
		if(!modClass.equals(goat.core.Module.class) && !modClass.equals(Object.class))
			while(it.hasNext()) {
				Module mod = it.next();
				if(modClass.isInstance(mod))
					ret = mod;
			}
		return ret;
	}
	
	public Module getLoaded(String className) {
		if(!className.startsWith("goat.module."))
			className = "goat.module." + className;
		Class<?> modClass = null;
		try {
			modClass = Class.forName(className);
		} catch (ClassNotFoundException cnfe) {}
		return getLoaded(modClass) ;
	}

	public Iterator<Module> iterator() {
		return loadedModules.iterator();
	}
	
	/**
	 * Build the list of all public classes in package goat.module.
	 *
	 * This gets sort of ugly.  It sure would be nice if the java reflection API
	 * could give us a list of all classes in a package, wouldn't it?
	 */
	private void buildAllModulesList() {
		if(BotStats.testing)
			return;
        JarFile jf = null;
        try {
        	jf = new JarFile("goat.jar") ;
        } catch (IOException e) {
        	e.printStackTrace() ;
        	System.err.println("Error while trying to read goat modules from goat.jar, available module list not built.") ;
        	return ;
        }
        Enumeration<JarEntry> jes = jf.entries() ;
        while(jes.hasMoreElements()) {
        	JarEntry je = jes.nextElement() ;
        	if(je.getName().matches(".*goat/module/[^\\$]*\\.class")) {
        		// System.out.println(je.toString()) ;
        		try {
        			Class<?> modClass = Class.forName(je.getName().replace(".class", "").replaceAll("/", ".").replaceAll("$.*", "")) ;
        			if(modClass.getSuperclass().getName().equals("goat.core.Module"))
        				allModules.add(modClass) ;
        		} catch (ClassNotFoundException e) {
        			System.err.println("Error while building goat modules list, jar entry: \"" + je.getName() + "\", skipping") ;
        		}
        	}
        }

        ArrayList<String> tempList = new ArrayList<String>();
        for (Class<?> modClass : allModules) {
        	tempList.add(modClass.getName());
        }
        Collections.sort(tempList);
        System.out.println("Available Modules: ") ;
        for (String modName : tempList) {
        	System.out.println("   " + modName);
        }
        System.out.println() ;
	}
	
	/**
	 * Build the list of all possible goat commands.
	 * 
	 * This goes through all the available goat modules and builds a list of all the commands
	 * they ask to respond to (via goat.core.Module.getCommands()).  It whines on stderr
	 * if it finds multiple modules responding to the same command.
	 *
	 */
	private void buildAllCommandsList() {
		//this has to be called after the allModules list it built.  With buildAllModulesList().
		HashMap<String, String> commands = new HashMap<String, String>() ;
		boolean collisions = false ;
        for (Class<?> modClass : allModules) {
            String[] modCommands = Module.getCommands(modClass);
            for (String modCommand : modCommands) {
                if (commands.containsKey(modCommand)) {
                    commands.put(modCommand, commands.get(modCommand) + ", " + modClass.getName());
                    collisions = true;
                } else {
                    commands.put(modCommand, modClass.getName());
                }
            }
        }
        allCommands.addAll(commands.keySet()) ;
		Collections.sort(allCommands) ;
		if (collisions) {
			System.out.println("WARNING: multiple modules with same command detected: ") ;
            for (String allCommand : allCommands)
                if (commands.get(allCommand).contains(","))
                    System.out.println("   \"" + allCommand + "\" :  " + commands.get(allCommand));
            System.out.println() ;
		}
	}
	
	public Class<?> [] getAllModules() {
		return allModules.toArray(new Class<?>[0]) ;
	}
	
	public String [] getAllCommands() {
		return allCommands.toArray(new String[0]) ;
	}
	
	/**
	 *
	 * @return may contain duplicates
	 */
	public String [] getLoadedCommands() {
		ArrayList<String> lcommands = new ArrayList<String>() ;
		for(int i=0; i<loadedModules.size(); i++) {
			lcommands.addAll(Arrays.asList(Module.getCommands(getLoaded(i).getClass()))) ;
		}
		Collections.sort(lcommands) ;
		return lcommands.toArray(new String[0]) ;
	}
	
	public boolean stringInArray(String s, String [] array) {
		boolean found = false ;
        for (String anArray : array) {
            if (s.equalsIgnoreCase(anArray)) {
                found = true;
                break;
            }
        }
        return found ;
	}
	
	public boolean isCommand(String s) {
		return stringInArray(s, getAllCommands()) ;
	}
	
	public boolean isLoadedCommand(String s) {
		return stringInArray(s, getLoadedCommands()) ;
	}
}
