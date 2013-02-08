package goat.util.zipcode;

public class Combined extends ZipCode {

	private Zip1990 zip1990 = new Zip1990();
	private Zip1999 zip1999 = new Zip1999();
	private Zcta2000 zcta2000 = new Zcta2000();
	private CfdZip zipCFD = new CfdZip(); 

	public Combined(){}
	
	@Override
	String dataFileURL() {
		return "";
	}

	@Override
	ZipCode getByCode(String code) {
		return get(code);
	}
	
	@Override
	boolean importData() {
		boolean ret = true;
		if(! zipCFD.hasDataCached())
			if(! zipCFD.importData())
				ret = false;
		if(! zip1990.hasDataCached())
			if(! zip1990.importData())
				ret = false;
		if(! zip1999.hasDataCached())
			if(! zip1999.importData())
				ret = false;
		if(! zcta2000.hasDataCached())
			if(! zcta2000.importData())
				ret = false;
		return ret;
	}
	
	public static Combined get(String code) {
		ZipCode[] list = new ZipCode[]{ CfdZip.get(code), Zip1990.get(code), Zip1999.get(code), Zcta2000.get(code)} ;
		Combined ret = null;
		for(int i=0;i<list.length;i++)
			if(ret != null) {
				if (list[i] != null) 
					ret.overwriteWith(list[i]);
			} else if(list[i] != null){
				ret = new Combined();
				ret.overwriteWith(list[i]);
			}
		return ret;
	}
}
