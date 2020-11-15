package goat.module;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import goat.Goat;
import goat.core.IrcMessage;
import goat.core.Module;
import goat.jcalc.Calculator;
import goat.jcalc.CalculatorException;

/**
 * A piece of glue code to tie goat to the calculator code stolen (JCalc?)
 *
 * @author bc
 */
public class Calc extends Module {

	Calculator calc = new Calculator();
	
	public Calc() {
		
		// set up some prevention of obvious qpting
		//  ... less necessary now that we've added a couple of interruptability knobs to jcalc 
		//      in addition to these admittedly lame computation limits
		
		//calc.setLimitFactorial(15342L);
		//calc.setLimitUpperPower(100);
		//calc.setLimitLowerPower(-100);
	}
	
	public void processPrivateMessage(Message m) {
    	processChannelMessage(m);
	}

	public void processChannelMessage(Message m) {
		
		ExecutorService pool = Goat.modController.getPool();
		Cogitator brain = new Cogitator(m);
		Future<String> future = pool.submit(brain);
		String reply;
		try {
			reply = future.get(2000, TimeUnit.MILLISECONDS);
		} catch (CancellationException ce) {
			reply = "I've gone ahead and cancelled that computation for you.  Asshat.";
		} catch (TimeoutException te) {
			
			// important:  when you get a timeout exception via Future.get(), the underlying
			//   calculation has not stopped.  This means you might be able to DOS the goat
			//   with this, so don't tell qpt.
			
			future.cancel(true);	// this doesn't actually stop the underlying calculation, either,
									// unless you've taken pains to make sure it's interruptable.
									// which we have.
			
			reply = "I'm not thinking that hard, wanker.";
		} catch (ExecutionException ee) {
			reply = ee.getCause().getLocalizedMessage();
		} catch (InterruptedException ie) {
			reply = "I'm sorry, where were we before we were so rudely interrupted?";
		} 
		if(reply.length() > 256)
			reply = reply.length() + " digits:  " + reply;
		m.pagedReply(reply);
	}

	/**
	 * The thinkin' thread
	 */
	private class Cogitator implements Callable<String> {
		
        private IrcMessage target;
        
        public Cogitator(Message m) {
        	target = m;
        }

		public String call() throws CalculatorException, InterruptedException {
			return calc.evaluate_equation(target.getModTrailing());
		}
	}

	public String[] getCommands() {
		return new String[]{"calc"};
	}
}
