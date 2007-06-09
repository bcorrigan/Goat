package goat.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
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

	private ArrayList<Module> loadedModules = new ArrayList<Module>();
	
	private ArrayList<String> allModules = new ArrayList<String>() ;
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
	public Module load(String moduleName) throws IllegalAccessException, InstantiationException, ClassNotFoundException,
													NoClassDefFoundError, ClassCastException {
		if( null != get(moduleName))
			return null ; // return null if module is already loaded
		Module newmod = (Module) ClassLoader.getSystemClassLoader().loadClass("goat.module." + moduleName).newInstance();
		//Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
		//Module newmod = (Module) getClass().getClassLoader().loadClass("goat.module." + moduleName).newInstance();
		loadedModules.add(newmod) ;
		return newmod;  
	}

	public Module load(Module mod) {
		if( null != get(mod))
			return null;    // return null if module is already loaded
		loadedModules.add(mod);
		return mod;
	}

	public boolean unload(String moduleName) {
		Module mod = get(moduleName);
		if (null == mod)
			return false ;
		else 
			loadedModules.remove(mod) ;
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

	public Module get(int i) {
		return loadedModules.get(i);
	}

	public Module get(String moduleName) {
		Iterator<Module> it = loadedModules.listIterator();
		Module mod;
		while (it.hasNext()) {
			mod = it.next();
			if (mod.getClass().getName().toLowerCase().equals("goat.module." + moduleName.toLowerCase())) {
				return mod;
			}
		}
		return null;
	}
	
	public Module get(Module mod) {
		if( null == mod )
			return null ;
		return get(mod.getClass().getName().replace("goat.module.", "")) ;
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
        			Class modClass = Class.forName(je.getName().replace(".class", "").replaceAll("/", ".").replaceAll("$.*", "")) ;
        			if(modClass.getSuperclass().getName().equals("goat.core.Module"))
        				allModules.add(modClass.getCanonicalName()) ;
        		} catch (ClassNotFoundException e) {
        			System.err.println("Error while building goat modules list, jar entry: \"" + je.getName() + "\", skipping") ;
        		}
        	}
        }
        Collections.sort(allModules) ;
        System.out.println("Available Modules: ") ;
        for (String allModule : allModules) {
            System.out.println("   " + allModule);
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
        for (String allModule : allModules) {
            Class modClass;
            try {
                modClass = Class.forName(allModule);
            } catch (ClassNotFoundException e) {
                System.err.println("Couldn't find module class: " + allModule);
                e.printStackTrace();
                continue;
            }
            String[] modCommands = Module.getCommands(modClass);
            for (String modCommand : modCommands) {
                if (commands.containsKey(modCommand)) {
                    commands.put(modCommand, commands.get(modCommand) + ", " + allModule);
                    collisions = true;
                } else {
                    commands.put(modCommand, allModule);
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
	
	public String [] getAllModules() {
		return allModules.toArray(new String[0]) ;
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
			lcommands.addAll(Arrays.asList(Module.getCommands(get(i).getClass()))) ;
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
