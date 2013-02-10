package goat.eliza;


/**
 *  Eliza memory class
 */

public class Mem {

    /** The memory size */
    final int memMax = 20;
    /** The memory */
    String memory[] = new String[memMax];
    /** The memory top */
    int memTop = 0;

    public void save(String str) {
        if (memTop < memMax) {
            memory[memTop++] = new String(str);
        }
    }

    public String get() {
        if (memTop == 0) return null;
        String m = memory[0];
        System.arraycopy(memory, 1, memory, 0, memTop - 1);
        memTop--;
        return m;
    }
}


