package goat.core;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * <p>Loads and unloads Modules. Provides methods allowing you
 * to find out what Modules are loaded, to control the Modules, and
 * to return them.<p>
 * @version <p>Date: 17-Dec-2003</p>
 * @author <p><b>? Barry Corrigan</b> All Rights Reserved.</p>
 *
 */
public class ModuleController  {

	private ArrayList modules = new ArrayList();

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
		Module newmod = (Module) ClassLoader.getSystemClassLoader().loadClass("goat.module." + moduleName).newInstance();
		//Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
		//Module newmod = (Module) getClass().getClassLoader().loadClass("goat.module." + moduleName).newInstance();
		return load(newmod);  
	}

	private Module load(Module mod) {
		Iterator it = modules.listIterator();
		Module module;
		while (it.hasNext()) {
			module = (Module) it.next();
			if (module.getClass().getName().equals(mod.getClass().getName()))
				return null;    
		}
		modules.add(mod);
		return mod;
	}

	public boolean unload(String moduleName) {
		Iterator it = modules.listIterator();
		Module mod;
		while (it.hasNext()) {
			mod = (Module) it.next();
			if (mod.getClass().getName().toLowerCase().equals("goat.module." + moduleName.toLowerCase())) {
				modules.remove(mod);
				return true;
			}
		}
		return false;
	}

	public String[] lsmod() {
		Module mod;
		String[] modNames = new String[modules.size()];
		for(int i=0;i<modules.size();i++) {
			mod = (Module) modules.get(i);
			modNames[i] = mod.getClass().getName();
		}
		return modNames;
	}

	public Module get(int i) {
		return (Module) modules.get(i);
	}

	public Module get(String moduleName) {
		Iterator it = modules.listIterator();
		Module mod;
		while (it.hasNext()) {
			mod = (Module) it.next();
			if (mod.getClass().getName().toLowerCase().equals("goat.module." + moduleName.toLowerCase())) {
				return mod;
			}
		}
		return null;
	}

	public Iterator iterator() {
		return modules.iterator();
	}
}
