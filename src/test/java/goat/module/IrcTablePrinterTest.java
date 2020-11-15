package goat.module;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import goat.core.Constants;
import goat.core.Module;
import goat.util.IrcTablePrinter;
import goat.util.IrcTablePrinter.Decorator;

public class IrcTablePrinterTest extends Module {

	@Override
	public void processChannelMessage(Message m) {
		// TODO Auto-generated method stub
		String[][] testTable = {{"rs", "1000"}, {"bc", "4"}, {"cd", "23"}, {"ef", "34"}, {"fg", "45"},
				{"gh", "56"}, {"hi there", "67",}, {"ij", "78"}, {"jk", "89"}, {"kl", "90"}};

		IrcTablePrinter ip = new IrcTablePrinter();
		List<String> lines = ip.printArrays(testTable);
		m.createReply("default:").send();
 		for(String line: lines)
 			m.createReply(line).send();

 		try {
 			Thread.sleep(1500);
 		} catch (InterruptedException ie) {}

 		m.createReply("limited to 40 chars:").send();
 		ip = new IrcTablePrinter();
 		ip.setDisplayWidth(40);
 		lines = ip.printArrays(testTable);
 		for(String line: lines)
 			m.createReply(line).send();

 		try {
 			Thread.sleep(1500);
 		} catch (InterruptedException ie) {}

 		m.createReply("limited to 3 columns, add headers, left justify first column, enumerate:").send();
 		ip = new IrcTablePrinter();
 		ip.setFieldDecorator(0, EnumSet.of(Decorator.LEFT_JUSTIFY));
 		ip.setMaxColumns(3);
 		ip.setHeaders(new String[]{"name","score"});
 		ip.setEnumerate(true);
 		lines = ip.printArrays(testTable);
 		for(String line: lines)
 			m.createReply(line).send();

 		try {
 			Thread.sleep(1500);
 		} catch (InterruptedException ie) {}

 		m.createReply("again, after fucking up the data :").send();
 		String[][] jackedTable = {{"rs", null}, {"bc", "4"}, null, {"ef", "34"}, {"fg", "45"},
				//{"gh", "56"},
				{"hi there", "6" + Constants.BOLD + "7" + Constants.NORMAL,},
				{"ij", "7" + Constants.COLCODE + ",228"}, {"jk", "89"}, {"kl", "90"},
				{"lm", Constants.COLCODE + "44,228"}};
 		lines = ip.printArrays(jackedTable);
 		for(String line: lines)
 			m.createReply(line).send();

 		try {
 			Thread.sleep(1500);
 		} catch (InterruptedException ie) {}

 		m.createReply("again, with differently fucked data :").send();
 		List<List<String>> crapTable = new ArrayList<List<String>>();
 		crapTable.add(null);
 		ArrayList<String> record = new ArrayList<String>();
 		record.add("bob");
 		record.add("88");
 		crapTable.add(record);
 		record = new ArrayList<String>();
 		record.add("sue");
 		record.add("99");
 		crapTable.add(record);
 		record = new ArrayList<String>();
 		record.add("dan");
 		record.add("00");
 		crapTable.add(record);
 		record = new ArrayList<String>();
 		record.add("eugene");
 		record.add("33");
 		record.add("ha ha ha");
 		crapTable.add(record);
 		lines = ip.printArrays(crapTable);
 		for(String line: lines)
 			m.createReply(line).send();
	}

	@Override
	public void processPrivateMessage(Message m) {
		// TODO Auto-generated method stub

	}

	public String[] getCommands() {
		return new String[]{"tableTest"};
	}
}
