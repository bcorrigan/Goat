/*
 * Created on 25-Feb-2006
 */
package goat.countdown;

import java.util.*;

/** Solve the CountDown problem */
public class Solver {
    private static final int N = 6;
    private static final int expN = 1 << N;
    private static final int HSIZE = 20000;
    
    private static int          // Operator symbols
        CONST = 0, PLUS = 1, MINUS = 2, TIMES = 3, DIVIDE = 4;

    /* Each expression formed is recorded as a `blob'.  These blobs are
    linked together in three ways: a blob that's labelled with a binary
    operator is linked to its left and right operands in a binary tree
    structure.  Also, each blob is put in a linked list with all others
    that use the same inputs.  Finally, there is a chained hash table
    that allows us to find all blobs with a certain value. */

    private static class Blob {
        int op;                 // Operator
        Blob left, right;       // Left and right operands
        int val;                // Value of expression
        int used;               // Bitmap of inputs used
        Blob next;              // Next blob with same inputs
        Blob hlink;             // Next blob in same hash chain
    }

    /* The binary tree structure of blobs is used by |Grind|, which
    converts an expression to printed form.  The output is simplified by
    omitting brackets where they are unnecessary because of the
    priority and associativity of operators.  Thus both of the
    expressions \verb|(1+2)+3| and \verb|1+(2+3)| will be shown without
    brackets. */

    private static final char sym[] = { '?', '+', '-', '*', '/' };
    private static final int pri[] = { 0, 1, 1, 2, 2 };
    private static final int rpri[] = { 0, 1, 2, 2, 3 };

    /** Pretty-print an expression into a StringBuffer */
    private static void Walk(Blob e, int p, StringBuffer buf) {
        if (e.op == CONST)
            buf.append(e.val);
        else {
            int xp = pri[e.op], rp = rpri[e.op];
            if (xp < p) buf.append('(');
            Walk(e.left, xp, buf);
            buf.append(" " + sym[e.op] + " ");
            Walk(e.right, rp, buf);
            if (xp < p) buf.append(')');
        }
    }
    
    /** Convert an expression to a string for display */
    private static String Grind(Blob e) {
        StringBuffer buf = new StringBuffer();
        Walk(e, 1, buf);
        return buf.toString();
    }
    
    /* Sets of input numbers are represented by bitmaps, i.e. integers in
    the range [0..2^N) in which the one bits indicate which
    numbers are present.  The array entry |pool[s]| shows all the expressions
    that have been created using the set of inputs |s|. */

    private static Blob pool[] = new Blob[expN];

    /* For each possible value |val|, we keep track of all the expressions
    with value |val| that have been created: they are kept in a linked
    list starting at |htable[val % HSIZE]|, together (of course) with
    others that hash to the same bucket.  We've chosen |HSIZE| large
    enough that such collisions will rarely happen. 
    The purpose of this hash table is to make it easy to avoid creating
    a `useless' expression if another with the same value already
    exists and uses no inputs that the new one would not use.  This
    speeds up the search immensely. */

    private static Blob htable[] = new Blob[HSIZE];

    /* As we generate expressions, we keep track of the best answer seen
    so far: an expression that comes closest to the target, and of the 
    expressions that are that close, the one that is the shortest when 
    printed. We don't guarantee to produce the shortest of all, because 
    some expressions are discarded as useless. */
    
    private static int target;
    private static String best;
    private static int bestval, bestdist;

    /** Try to create a new blob with specified operator and arguments */
    private static void Add(int op, Blob p, Blob q, int val, int used) {
        /* Return immediately if the expression is useless */
        for (Blob r = htable[val % HSIZE]; r != null; r = r.hlink) {
            if (r.val == val && (r.used & ~used) == 0)
                return;
        }
        
        /* Create the expression and add it to |pool| and |htable| */
        Blob t = new Blob();
        t.op = op; t.left = p; t.right = q; t.val = val; t.used = used;
        t.next = pool[used]; pool[used] = t;
        t.hlink = htable[val % HSIZE]; htable[val % HSIZE] = t;
        
        /* See if the new expression comes near the target */
        int dist = Math.abs(val - target);
        if (dist <= bestdist) {
            String temp = Grind(t);
            if (dist < bestdist || temp.length() < best.length()) {
                bestval = val; bestdist = dist; best = temp;
            }
        }
    }                                           
        
    /* The |Combine| procedure combines the contents of |pool[r]| with the
    contents of |pool[s]| using every possible operator.  The results are
    entered into the pool for the set union of |r| and |s|.
    To speed the search, we do not allow expressions of the form $E_1+E_2$
    where the value of $E_1$ is smaller than the value of $E_2$; the
    equivalent expression $E_2+E_1$ renders this one useless anyway */

    private static void Combine(int r, int s) {
        int used = r | s;
        for (Blob p = pool[r]; p != null; p = p.next) {
            for (Blob q = pool[s]; q != null; q = q.next) {
                if (p.val >= q.val) {
                    Add(PLUS, p, q, p.val+q.val, used);
                    if (p.val > q.val) Add(MINUS, p, q, p.val-q.val, used);
                    Add(TIMES, p, q, p.val*q.val, used);
                    if (q.val > 0 && p.val%q.val == 0)
                        Add(DIVIDE, p, q, p.val/q.val, used);
                }
            }
        }
    }

    /* The search algorithm works by starting with just the input numbers,
    and successively forming all expressions using 2, 3,~\dots input numbers.
    Each expression with $i$ inputs can be obtained by combining two
    expressions that use $j$ and $k$ inputs, where $j+k=i$, and the two
    expressions use disjoint sets of inputs.  Since the expressions are
    divided into pools according to the set of inputs they use, at the |i|'th
    stage we must combine each pool |r| with each pool |s| such that
    |ones[r]+ones[s]=i| and |r| and |s| are disjoint. */
    
    /* Set up a table of bitcounts in |ones| */
    private static int ones[] = new int[expN];
    
    static {
        // This uses the recurrence ones[i+2^n] = ones[i] + 1
        ones[0] = 0;
        for (int i = 0; i < N; i++) {
            int t = 1 << i;
            for (int r = 0; r < t; r++) ones[t+r] = ones[r]+1;
        }
    }

    private static void solve(int draw[], int target) {
        Solver.target = target;
        bestdist = 1000000;

        /* Empty the hash table and pools */
        for (int i = 0; i < HSIZE; i++) htable[i] = null;
        for (int r = 0; r < expN; r++) pool[r] = null;
        
        /* Plant the draw numbers as seeds */
        for (int i = 0; i < N; i++)
            Add(CONST, null, null, draw[i], 1 << i);
        
        /* Combine using up to N-1 operations */
        for (int i = 2; i <= N; i++) {
            /* Combine disjoint pools that together use |i| inputs */
            for (int r = 1; r < expN; r++) {
                for (int s = 1; s < expN; s++) {
                    if (ones[r] + ones[s] == i && (r & s) == 0)
                        Combine(r, s);
                }
            }
        }
    }
    
    public static String Solve(int draw[], int target) {
        solve( draw, target);

        if (bestdist == 0)
            return target + " = " + best;
        else
            return bestval + " = " + best + " (off by " + bestdist + ")";
    }
    
    /**
     * Simply gets the best possible answer for the arguments.
     * @param draw
     * @param target
     * @return
     */
    public static int getBestVal( int draw[], int target ) {
        solve( draw, target);
        return bestval;
    }
}
