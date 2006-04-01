package goat.module;

import goat.core.Module;
import goat.core.Message;
import goat.jcalc.Calculator;
import goat.jcalc.CalculatorException;

/**
 * A piece of glue code to tie goat to the calculator code stolen (JCalc?)
 *
 * @author bc
 */
public class Calc extends Module {

	Calculator calc = new Calculator();

	public void processPrivateMessage(Message m) {
    	processChannelMessage(m);
	}

	public void processChannelMessage(Message m) {
    	Observer ob = new Observer();
		ob.target = m;
		ob.start();
	}

	public void run() {

	}
	/**
	 * Keeps tabs on the Cogitate thread. When it is taking too long, it stops it.
	 */
	private class Observer extends Thread {
		private Message target;

		public void run() {
			Cogitate cog = new Cogitate();
        	cog.target = target;
			cog.start();
            try {
				sleep(100);     // 1/10th of a second
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			cog.tooLong = true;
			if(cog.answer==null && cog.error==false) {
                //TODO we should peacefully inform the other thread that it would be a good idea for it to stop, rather than this brutality.
				cog.stop();
				target.createReply("Don't be a wanker. I'm not thinking that hard.").send();
			}

		}
	}

	/**
	 * The thinkin' thread
	 */
	private class Cogitate extends Thread {
        private Message target;
        private String answer;
        private boolean tooLong = false;
		private boolean error = false;

		public void run() {
			try {

				answer = calc.evaluate_equation(target.modTrailing);
				if(!tooLong)
					target.createPagedReply(answer).send();
			} catch (CalculatorException e) {
				error = true;
				target.createReply(e.getLocalizedMessage()).send();
			}
		}
	}

	public static String[] getCommands() {
		return new String[]{"calc"};
	}
}
