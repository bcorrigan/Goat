// Z Machine V3/V4/V5 Runtime 
//
// Copyright 2002, Brian J. Swetland <swetland@frotz.net>
// Available under a BSD-Style License.  Share and Enjoy.

package goat.util;

import java.io.PrintStream;

class ZFrame
{
	ZFrame prev;
	int pc;
	int sp;
	int bp;
	int res; 
	int locals[];
	int llen;
	
	ZFrame(int pc, int sp, int locals) {
		this.pc = pc;
		this.sp = sp;
		this.bp = sp;
		this.llen = locals + 1;
		if(ZMachine.CONSERVE_MEMORY){
			this.locals = new int[17];
		} else {
			this.locals = new int[locals + 1];
		}
	}
}

public class ZMachine implements Runnable
{
    //is this thread running or not?
    public boolean running = true;

	class ZObject {
		int Size;
		int PropMax;
		
		ZObject() {
			Size = kObjSize;
			PropMax = 31;
		}
		
		private static final int kObjParent = 4;
		private static final int kObjSibling = 5;
		private static final int kObjChild = 6;
		private static final int kObjProps = 7;
		private static final int kObjSize = 9;


		boolean attr_test(int obj, int attr) {
			return (U8(object_ptr + obj * kObjSize + (attr >> 3)) & (1 << (7 - (attr & 7)))) != 0;
		}
		void attr_set(int obj, int attr) {
			mem[object_ptr + obj * kObjSize + (attr >> 3)] |= (1 << (7 - (attr & 7)));
		} 
		void attr_clear(int obj, int attr){
			mem[object_ptr + obj * kObjSize + (attr >> 3)] &= ~(1 << (7 - (attr & 7)));
		}

		boolean inside(int a, int b) {
			return U8(object_ptr + a * kObjSize + kObjParent) == b;
		}

		int sibling(int obj) {
			return U8(object_ptr + obj * kObjSize + kObjSibling);
		}

		int parent(int obj) {
			return U8(object_ptr + obj * kObjSize + kObjParent);
		}

		int child(int obj) {
			return U8(object_ptr + obj * kObjSize + kObjChild);
		}

		void remove(int obj) {
			int ptr = object_ptr + obj * kObjSize;
			
			int x = U8(ptr + kObjParent);
			
			if(x == 0) return; /* no parent */
			
			int n = U8(ptr + kObjSibling);
			int c = U8(object_ptr + x * kObjSize + kObjChild);
			if(c == obj){
					/* immediate child -- simple case */
				W8(object_ptr + x * kObjSize + kObjChild, n);		   
			} else {
				while(c != 0) {
					c = object_ptr + c * kObjSize;
					x = U8(c + kObjSibling);
					if(x == obj) {
						W8(c + kObjSibling, n);
						break;
					} else {
						c = x;
					}
				}
			}
			W8(ptr + kObjSibling, 0);
			W8(ptr + kObjParent, 0);
		}
		
		void insert(int obj, int dest) {
			int objptr = object_ptr + obj * kObjSize;
			int dstptr = object_ptr + dest * kObjSize;
			
			if(U8(objptr + kObjParent) != 0) remove(obj);
			
				/* the old child (if any) becomes our sibling */
			W8(objptr + kObjSibling, U8(dstptr + kObjChild));
				/* we become the new eldest child */
			W8(dstptr + kObjChild, obj);
				/* and update our parent ptr */
			W8(objptr + kObjParent, dest);
		}
	
		void print(int obj) {
			obj = U16(object_ptr + obj * kObjSize + kObjProps);
			if(U8(obj) != 0){
				ZSCII(obj + 1);
				Print(zscii_buf,zscii_ptr);
			}
		}
		
		String name(int _obj) {
			int obj = U16(object_ptr + _obj * kObjSize + kObjProps);
			if(U8(obj) != 0){
				ZSCII(obj + 1);
				return new String(zscii_buf,0,zscii_ptr);
			} else {
				return new String("#"+_obj);
			}
		}		

		int status(){
			return U16(object_ptr + Load(16) * kObjSize + kObjProps);
		}
		


		int get_prop_len(int addr) {
			if(addr == 0){
				return 0;
			} else {
				return (U8(addr - 1) >> 5) + 1;
			}
		}
		
		int get_prop_addr(int obj, int prop_id) {
			int prop,sz;
		
			obj = object_ptr + kObjSize * obj;
			prop = U16(obj + kObjProps);
			prop += U8(prop) * 2 + 1; /* skip name */
			
			while((sz = U8(prop)) != 0){
				if((sz & 0x1f) == prop_id) {
					return prop + 1;
				} else {
					prop += (sz >> 5) + 2;
				}
			}
			
			return 0;
		}
	
		int get_prop_next(int obj, int prop_id) {
			int prop,sz;
			
			obj = object_ptr + kObjSize * obj;
			prop = U16(obj + kObjProps);
			prop += U8(prop) * 2 + 1; /* skip name */
			
			if(LOGGING) LOG("get prop next " + obj + " " + prop_id);
			
			if(prop_id == 0) {
					/* get first prop */
				return U8(prop) & 0x1f;
			} else {
				while((sz = U8(prop)) != 0){
					if((sz & 0x1f) == prop_id) {
						return U8(prop + (sz >> 5) + 2) & 0x1f;
					} else {
						prop += (sz >> 5) + 2;
					}
				}
			}
			
			return 0;
		}
	
		int get_prop(int obj, int prop_id) {
			int prop,sz;
			
			if(LOGGING) LOG("get_prop "+obj+" #"+prop_id);
			
			obj = object_ptr + kObjSize * obj;
			prop = U16(obj + kObjProps);
			prop += U8(prop) * 2 + 1; /* skip name */
			
			while((sz = U8(prop)) != 0){
				if((sz & 0x1f) == prop_id) {
					switch(sz >> 5){
					case 0:
						return U8(prop + 1);
					case 1:
						return U16(prop + 1);
					default:
						DIE("property not byte or word sized");
					}
				} else {
					prop += (sz >> 5) + 2;
				}
			}
			
			return U16(default_prop_ptr + prop_id * 2);
		}
		
		void put_prop(int obj, int prop_id, int val) {
			int prop,sz;
			
			if(LOGGING) LOG("put_prop "+obj+" #"+prop_id+" = 0x"+HEX(val));
			
			obj = object_ptr + kObjSize * obj;
			prop = U16(obj + kObjProps);
			prop += U8(prop) * 2 + 1; /* skip name */
			
			while((sz = U8(prop)) != 0){
				if((sz & 0x1f) == prop_id) {
					switch(sz >> 5){
					case 0:
						W8(prop + 1, val);
						return;
					case 1:
						W16(prop + 1, val);
						return;
					default:
						DIE("property not byte or word sized");
					}
				} else {
					prop += (sz >> 5) + 2;
				}
			}
			
			DIE("property not found");
		}
		
/*		void dump(int obj) {
			LOG("dumpobj("+obj+")");
			obj = object_ptr + kObjSize * obj;
			int props = U16(obj+kObjProps);
			int sz;
			String s;
			int i;
			
			LOG("@"+HEX(obj)+":"+
				" p:"+U8(obj+kObjParent)+" s:"+U8(obj+kObjSibling)+" c:"+U8(obj+kObjChild)+
				" pr:"+HEX(props));
			LOG("name: "+(U8(props) == 0 ? "*none*" : ZSTRING(props + 1))); 
			
			s = "attr:";
			for(i = 0; i < 32; i++){
				if(Attr(obj, i)) s = s + " A"+i;
			}
			LOG(s);
			props += U8(props) * 2 + 1;
			
			while((sz = U8(props)) != 0) {
				int id = sz & 0x1f;
				sz = (sz >> 5) + 1;
				s = "P"+id+":";
				for(i = 0; i < sz; i++) s = s + " " + HEX(U8(props + i + 1));
				LOG(s);
				props += sz + 1;
			}
		}
*/
	}
	
	class ZObjectWide extends ZObject {
		ZObjectWide() {
			Size = kObjSize;
			PropMax = 63;
		}
		
		private static final int kObjParent = 6;
		private static final int kObjSibling = 8;
		private static final int kObjChild = 10;
		private static final int kObjProps = 12;
		private static final int kObjSize = 14;

		boolean attr_test(int obj, int attr) {
			return (U8(object_ptr + obj * kObjSize + (attr >> 3)) & (1 << (7 - (attr & 7)))) != 0;
		}
		void attr_set(int obj, int attr) {
			mem[object_ptr + obj * kObjSize + (attr >> 3)] |= (1 << (7 - (attr & 7)));
		} 
		void attr_clear(int obj, int attr){
			mem[object_ptr + obj * kObjSize + (attr >> 3)] &= ~(1 << (7 - (attr & 7)));
		}

		boolean inside(int a, int b) {
			return U16(object_ptr + a * kObjSize + kObjParent) == b;
		}

		int sibling(int obj) {
			return U16(object_ptr + obj * kObjSize + kObjSibling);
		}

		int parent(int obj) {
			return U16(object_ptr + obj * kObjSize + kObjParent);
		}

		int child(int obj) {
			return U16(object_ptr + obj * kObjSize + kObjChild);
		}

		void remove(int obj) {
			int ptr = object_ptr + obj * kObjSize;
			
			int x = U16(ptr + kObjParent);
			
			if(x == 0) return; /* no parent */
			
			int n = U16(ptr + kObjSibling);
			int c = U16(object_ptr + x * kObjSize + kObjChild);
			if(c == obj){
					/* immediate child -- simple case */
				W16(object_ptr + x * kObjSize + kObjChild, n);		   
			} else {
				while(c != 0) {
					c = object_ptr + c * kObjSize;
					x = U16(c + kObjSibling);
					if(x == obj) {
						W16(c + kObjSibling, n);
						break;
					} else {
						c = x;
					}
				}
			}
			W16(ptr + kObjSibling, 0);
			W16(ptr + kObjParent, 0);
		}
		
		void insert(int obj, int dest) {
			int objptr = object_ptr + obj * kObjSize;
			int dstptr = object_ptr + dest * kObjSize;
			
			if(U16(objptr + kObjParent) != 0) remove(obj);
			
				/* the old child (if any) becomes our sibling */
			W16(objptr + kObjSibling, U16(dstptr + kObjChild));
				/* we become the new eldest child */
			W16(dstptr + kObjChild, obj);
				/* and update our parent ptr */
			W16(objptr + kObjParent, dest);
		}
	
		void print(int obj) {
			obj = U16(object_ptr + obj * kObjSize + kObjProps);
			if(U8(obj) != 0){
				ZSCII(obj + 1);
				Print(zscii_buf,zscii_ptr);
			}
		}
		
		String name(int _obj) {
			int obj = U16(object_ptr + _obj * kObjSize + kObjProps);
			if(U8(obj) != 0){
				ZSCII(obj + 1);
				return new String(zscii_buf,0,zscii_ptr);
			} else {
				return new String("#"+_obj);
			}
		}		

		int status(){
				//XXX ???
			return U16(object_ptr + Load(16) * kObjSize + kObjProps);
		}
		

			// 1xnnnnnn 1xssssss
			// 0snnnnnn
		int get_prop_len(int addr) {
			if(addr == 0){
				return 0;
			} else {
				int sz = U8(addr - 1);
				if((sz & 0x80) == 0){ // one-byte of size/number data
					if((sz & 0x40) == 0){
						return 1;
					} else {
						return 2;
					}
				} else { // two bytes of size/number data
					sz = sz & 63;
					if(sz == 0){
						return 64;
					} else {
						return sz;
					}
				}
			}
		}

		int last_sz;
		
		int get_prop_addr(int obj, int prop_id) {
			int id, prop,sz;
		
			obj = object_ptr + kObjSize * obj;
			prop = U16(obj + kObjProps);
			prop += U8(prop) * 2 + 1; /* skip name */
			
			for(;;){
				sz = U8(prop);
				id = sz & 0x3f;
				if(sz == 0) break;
				if(id == prop_id) {
					if((sz & 0x80) == 0){
						last_sz = sz;
						return prop + 1;
					} else {
						return prop + 2;
					}
				} else {
					if((sz & 0x80) == 0){
						if((sz & 0x40) == 0){
							prop += 2; /* 1 hdr, 1 data */
						} else {
							prop += 3; /* 1 hdr, 2 data */
						}
					} else {
						sz = U8(prop + 1) & 0x3f;
						if(sz == 0) {
							prop += (64 + 2);
						} else {
							prop += (sz + 2);
						}
					}
				}
			}
			
			return 0;
		}

		int get_prop(int obj, int prop_id) {
			if(LOGGING) LOG("get_prop "+obj+" #"+prop_id);

			int addr = get_prop_addr(obj, prop_id);
			int sz = last_sz;

			if(addr == 0){
				return U16(default_prop_ptr + prop_id * 2);
			}

			if((sz & 0x80) == 0){
				if((sz & 0x40) == 0){
					return U8(addr);
				} else {
					return U16(addr);
				}
			} else {
				DIE("property not byte or word sized");
				return 0;
			}

		}		
		
		void put_prop(int obj, int prop_id, int val) {
			if(LOGGING) LOG("put_prop "+obj+" #"+prop_id+" = 0x"+HEX(val));
			
			int addr = get_prop_addr(obj, prop_id);
			int sz = last_sz;
			
			if(addr == 0){
				DIE("property not byte or word sized");
			}
	
			if((sz & 0x80) == 0){
				if((sz & 0x40) == 0){
					W8(addr, val);
					return;
				} else {
					W16(addr, val);
					return;
				}
			} 
		}
	
		int get_prop_next(int obj, int prop_id) {
			if(LOGGING) LOG("get prop next " + obj + " #" + prop_id);

			if(prop_id == 0){
				obj = object_ptr + kObjSize * obj;
				int prop = U16(obj + kObjProps);
				prop += U8(prop) * 2 + 1; /* skip name */
				return U8(prop) & 0x3f;
			}
			
			int addr = get_prop_addr(obj, prop_id);
			int sz = last_sz;
			
			if(addr == 0) DIE("get_prop_next on nonexistant property");
			
			if((sz & 0x80) == 0){
				if((sz & 0x40) == 0){
					addr += 1;
				} else {
					addr += 2;
				}
			} else {
				sz = U8(addr-1) & 0x3f;
				if(sz == 0) {
					addr += 64;
				} else {
					addr += sz;
				}
			}
			return U8(addr) & 0x3f;
		}
	}	
	
	String HEX(int n) {
		return Integer.toHexString(n);
	}
	
	int U16(int addr) {
		return ((mem[addr] & 0xff) << 8) | (mem[addr + 1] & 0xff);
	}

	int U32(int addr) {
		return ((mem[addr] & 0xff) << 24) |
			((mem[addr + 1] & 0xff) << 16) |
			((mem[addr + 2] & 0xff) << 8) |
			(mem[addr + 3] & 0xff);		
	}
	
	void W16(int addr, int val) {
		mem[addr] = (byte) (val >> 8);
		mem[addr + 1] = (byte) val;
	}

	void W8(int addr, int val) {
		mem[addr] = (byte) val;
	}
	
	int U8(int addr) {
		return mem[addr] & 0xff;
	}
	
	int V8(int addr) {
		int x = mem[addr] & 0xff;
		
		switch(x){
		case 0x00: /* pop from stack */
			frame.sp--;
			if(frame.sp < frame.bp) DIE("sp underflow");
			return stack[frame.sp];
		case 0x01: case 0x02: case 0x03: case 0x04: case 0x05:
		case 0x06: case 0x07: case 0x08: case 0x09: case 0x0a:
		case 0x0b: case 0x0c: case 0x0d: case 0x0e: case 0x0f:
			return frame.locals[x];
			
		default:
			return U16(global_ptr + x * 2);
		}
	}
	
		/* load from stack (0), locals (1-15), or globals (16-255) */
	int Load(int id) {
		switch(id){
		case 0x00: /* pop from stack */
			frame.sp--;
			if(frame.sp < frame.bp) DIE("sp underflow");
			return stack[frame.sp];
		case 0x01: case 0x02: case 0x03: case 0x04: case 0x05:
		case 0x06: case 0x07: case 0x08: case 0x09: case 0x0a:
		case 0x0b: case 0x0c: case 0x0d: case 0x0e: case 0x0f:
			return frame.locals[id];
			
		default:
			return U16(global_ptr + id * 2);
		}
	}
	
	String Name(int id) {
		switch(id){
		case 0x00: 
			return "sp";
		case 0x01: case 0x02: case 0x03: case 0x04: case 0x05:
		case 0x06: case 0x07: case 0x08: case 0x09: case 0x0a:
		case 0x0b: case 0x0c: case 0x0d: case 0x0e: case 0x0f:
			return "l"+HEX(id-1);
		default:
			return "g"+HEX(id-16);
		}
		
	}
	
		/* store to stack (0), locals (1-15), or globals (16-255) */
	void Store(int id, int val) {
		if(LOGGING) LOG("store " + Name(id) + " (" + val + ")");
		if(false) if(TRACING && (id > 0)) {
			System.out.println("[> 0x" + HEX(id) + " = " + (val&0xffff));
		}
		
		switch(id){
		case 0x00: /* pop from stack */
			stack[frame.sp++] = val & 0xffff;
			return;
		case 0x01: case 0x02: case 0x03: case 0x04: case 0x05:
		case 0x06: case 0x07: case 0x08: case 0x09: case 0x0a:
		case 0x0b: case 0x0c: case 0x0d: case 0x0e: case 0x0f:
			frame.locals[id] = val  & 0xffff;
			return;
		default:
			if((id < 0) || (id > 255)) DIE("store out of range! " + id);
			id = global_ptr + id * 2;
			mem[id] = (byte) (val >> 8);
			mem[id + 1] = (byte) val;
		}
	}
	
	void Branch(boolean cond) {
		int x = U8(frame.pc++);

			/* branch on !cond bit */
		if((x & 0x80) == 0) cond = !cond;

		if(cond) {
				/* short branch bit */
			if((x & 0x40) != 0) {
				x = x & 0x3f;
			} else {
				if((x & 0x20) == 0) {
					x = x & 0x3f;
				} else {
					x = x | 0xffffffc0;
				}
				x = (x << 8) | U8(frame.pc++);
			}
			
			if(x == 0) {
				Return(0);
			} else if(x == 1) {
				Return(1);
			} else {
				if(LOGGING) LOG("branch to 0x" + HEX(frame.pc + x - 2) + " by " + x);
				frame.pc = frame.pc + x - 2;
			}
		} else {
				/* gotta bump the pc if the short branch bit is clear
				   and we didn't take the branch... */
			if((x & 0x40) == 0) frame.pc++;
		}
	}
	
	
	void LOG(String s) {
		System.out.println(s);
	}

	void DIE(String s) {
		int i;
		Println("*** oops ***");
		for(i = 1; i < frame.llen; i++){
			Println("L"+i+"="+frame.locals[i]+" (0x"+HEX(frame.locals[i])+")");
		}
		Println("Z: HALTED @"+HEX(frame.pc)+": "+s);
		scr.exit();
	}

	void BADOP(int op) {
		DIE("unknown opcode 0x"+HEX(op));
	}

	void BADOP(int op, int ex) {
		DIE("unknown opcode 0x"+HEX(op)+", 0x"+HEX(ex));
	}
	
	
	public int execute(ZFrame f) {


        if(!running)
            return 0;
        
		int op, num, t, x;
		int a,b,c,d;
		
		num = 0;
		t = 0;
		
		frame = f;
	
		a = 0;
		b = 0;
		c = 0;
		d = 0;
		
		for(;;){
			f = frame;

			int pc = f.pc;
			op = U8(pc++);				
		
				/* decode arguments */
			switch(op >> 4){
			case 0x00:
			case 0x01: /* 2OP, sconst, sconst */
				a = U8(pc++);
				b = U8(pc++);
				num = 2;
				break;
			case 0x02:
			case 0x03: /* 2OP, sconst, var */
				a = U8(pc++);
				b = V8(pc++);
				num = 2;
				break;
			case 0x04:
			case 0x05: /* 2OP, var, sconst */
				a = V8(pc++);
				b = U8(pc++);
				num = 2;
				break;
			case 0x06:
			case 0x07: /* 2OP, var, var */
				a = V8(pc++);
				b = V8(pc++);
				num = 2;
				break;
			case 0x08: /* 1OP, lconst */
				a = U16(pc);
				pc += 2;
				num = 1;
				break;
			case 0x09: /* 1OP, sconst */
				a = U8(pc++);
				num = 1;
				break;
			case 0x0a: /* 1OP, var */
				a = V8(pc++);
				num = 1;
				break;
			case 0x0b: /* 0OP */
				if(op != 0xbe) break;
				op = U8(pc++) + 0x100; 
					// read extended opcode and fall through
			case 0x0c:
			case 0x0d: /* 2OP, types-in-next */
			case 0x0e: /* VAR, types-in-next */
			case 0x0f:
				x = U8(pc++); /* arg descriptor */
				num = 0;
				while(num < 4) {
					switch((x >> (6 - num * 2)) & 3){
					case 0: /* lconst */
						t = U16(pc);
						pc += 2;
						break;
					case 1: /* sconst */
						t = U8(pc++);
						break;
					case 2: /* var */
						t = V8(pc++);
						break;
					case 3: /* none */
						t = -1;
						break;
					}
					if(t == -1) break;
					switch(num++){
					case 0:
						a = t;
						break;
					case 1:
						b = t;
						break;
					case 2:
						c = t;
						break;
					case 3:
						d = t;
						break;
					}
				}
					//XXX check argcounts...
			}

			if(TRACING) {			   
				String ss = "["+HEX(f.pc)+"] " + HEX(op);
				if(num > 0) ss = ss + " " + a;
				if(num > 1) ss = ss + " " + b;
				if(num > 2) ss = ss + " " + c;
				if(num > 3) ss = ss + " " + d;
				System.out.println(ss);
			}
			f.pc = pc;
		
			
				/* decode instruction */
			switch(op){
					/* 2OP instructions --------------- */
			case 0x01: case 0x21: case 0x41: case 0x61: case 0xc1: // je a b ?(label)
				switch(num){
				case 0:
				case 1:
					Branch(true);
					break;
				case 2:
					Branch(a == b);
					break;
				case 3:
					Branch((a == b) || (a == c));
					break;
				case 4:
					Branch((a == b) || (a == c) || (a == d));
					break;
				}
				break;
			case 0x02: case 0x22: case 0x42: case 0x62: case 0xc2: // jl a b ?(label)
				Branch(((short)a) < ((short)b));
				break;
			case 0x03: case 0x23: case 0x43: case 0x63: case 0xc3: // jg a b ?(label)
				Branch(((short)a) > ((short)b));
				break;
			case 0x04: case 0x24: case 0x44: case 0x64: case 0xc4: // dec_chk (var) val ?(label)
				t = (Load(a) - 1) & 0xffff;
				Store(a, t);
				Branch(((short)t) < ((short)b));
				break;
			case 0x05: case 0x25: case 0x45: case 0x65: case 0xc5: // inc_chk (var) val ?(label)
				t = (Load(a) + 1) & 0xffff;
				Store(a, t);
				Branch(((short)t) > ((short)b));
				break;
			case 0x06: case 0x26: case 0x46: case 0x66: case 0xc6: // jin obj1 obj2 ?(label)
				Branch(obj.inside(a,b));
				break;
			case 0x07: case 0x27: case 0x47: case 0x67: case 0xc7: // test bitmap flags ?(label)
				Branch((a & b) == b);
				break;
			case 0x08: case 0x28: case 0x48: case 0x68: case 0xc8: // or a b -> (result)
				Store(U8(f.pc++), a | b);
				break;
			case 0x09: case 0x29: case 0x49: case 0x69: case 0xc9: // and a b -> (result)
				Store(U8(f.pc++), a & b);
				break;
			case 0x0a: case 0x2a: case 0x4a: case 0x6a: case 0xca: // test_attr obj attr ?(label)
				Branch(obj.attr_test(a, b));
				break;
			case 0x0b: case 0x2b: case 0x4b: case 0x6b: case 0xcb: // set_attr obj attr
				obj.attr_set(a, b);
				break;
			case 0x0c: case 0x2c: case 0x4c: case 0x6c: case 0xcc: // clear_attr obj attr
				obj.attr_clear(a, b);
				break;
			case 0x0d: case 0x2d: case 0x4d: case 0x6d: case 0xcd: // store (var) value
				Store(a,b);
				break;
			case 0x0e: case 0x2e: case 0x4e: case 0x6e: case 0xce: // insert_obj object dest
				obj.insert(a, b);
				break;
			case 0x0f: case 0x2f: case 0x4f: case 0x6f: case 0xcf: // loadw array word-idx -> (result)
				Store(U8(f.pc++),U16(a + b * 2));
				break;
			case 0x10: case 0x30: case 0x50: case 0x70: case 0xd0: // loadb array byte-idx -> (result)
				Store(U8(f.pc++),U8(a + b));
				break;
			case 0x11: case 0x31: case 0x51: case 0x71: case 0xd1: // get_prop obj prop -> (result)
				Store(U8(f.pc++),obj.get_prop(a, b));
				break;
			case 0x12: case 0x32: case 0x52: case 0x72: case 0xd2: // get_prop_addr obj prop -> (result)
				Store(U8(f.pc++),obj.get_prop_addr(a, b));
				break;
			case 0x13: case 0x33: case 0x53: case 0x73: case 0xd3: // get_next_prop obj prop -> (result)
				Store(U8(f.pc++),obj.get_prop_next(a, b));
				break;
			case 0x14: case 0x34: case 0x54: case 0x74: case 0xd4: // add a b -> (result)
				Store(U8(f.pc++), ((short)a) + ((short)b));
				break;
			case 0x15: case 0x35: case 0x55: case 0x75: case 0xd5: // sub a b -> (result)
				Store(U8(f.pc++), ((short)a) - ((short)b));
				break;
			case 0x16: case 0x36: case 0x56: case 0x76: case 0xd6: // mul a b -> (result)
				Store(U8(f.pc++), ((short)a) * ((short)b));
				break;
			case 0x17: case 0x37: case 0x57: case 0x77: case 0xd7: // div a b -> (result)
				Store(U8(f.pc++), ((short)a) / ((short)b));
				break;
			case 0x18: case 0x38: case 0x58: case 0x78: case 0xd8: // mod a b -> (result)
				Store(U8(f.pc++), ((short)a) % ((short)b));
				break;
			case 0x19: case 0x39: case 0x59: case 0x79: case 0xd9: // call_2s a b -> (result) V4
				if(notV4) BADOP(op);
				op_call(num, a, b, 0, 0, U8(f.pc++));
				break;
			case 0x1a: case 0x3a: case 0x5a: case 0x7a: case 0xda: // call_2n a b V5
				if(notV5) BADOP(op);
				op_call(num, a, b, 0, 0, -1);
				break;
			case 0x1b: case 0x3b: case 0x5b: case 0x7b: case 0xdb: // set_color fg bg V5
				if(notV5) BADOP(op);
				break; //TODO
//			case 0x1c: case 0x3c: case 0x5c: case 0x7c: case 0xdc: // throw value frame V5/6
				
				 /* 1OP instructions --------------- */
			case 0x80: case 0x90: case 0xa0: // jz a ?(label)
				Branch(a == 0);
				break;
			case 0x81: case 0x91: case 0xa1: // get_sibling object -> (result) ?(label)
				Store(U8(f.pc++), t = obj.sibling(a));
				Branch(t != 0);
				break;
			case 0x82: case 0x92: case 0xa2: // get_child object -> (result) ?(label)
				Store(U8(f.pc++), t = obj.child(a));
				Branch(t != 0);
				break;
			case 0x83: case 0x93: case 0xa3: // get_parent object -> (result)
				Store(U8(f.pc++), obj.parent(a));
				break;
			case 0x84: case 0x94: case 0xa4: // get_prop_len prop-addr -> (result)
				Store(U8(f.pc++), obj.get_prop_len(a));
				break;
			case 0x85: case 0x95: case 0xa5: // inc (variable)
				Store(a, Load(a) + 1);
				break;
			case 0x86: case 0x96: case 0xa6: // dec (variable)
				Store(a, Load(a) - 1);
				break;
			case 0x87: case 0x97: case 0xa7: // print_addr byte-addr-of-str
				ZSCII(a);
				Print(zscii_buf,zscii_ptr);
				break;
			case 0x88: case 0x98: case 0xa8: // call_1s a -> (result) V4
				if(notV4) BADOP(op);
				op_call(1, a, 0, 0, 0, U8(f.pc++));
				break;
			case 0x89: case 0x99: case 0xa9: // remove_obj object
				obj.remove(a);
				break;
			case 0x8a: case 0x9a: case 0xaa: // print_obj object
				obj.print(a);
				break;
			case 0x8b: case 0x9b: case 0xab: // ret value
				Return(a);
				break;
			case 0x8c: case 0x9c: case 0xac: // jump ?(label)
				f.pc = f.pc - 2 + ((short)a);
				break;
			case 0x8d: case 0x9d: case 0xad: // print_paddr pack-addr-of-str
				ZSCII(a * PACK);
				Print(zscii_buf,zscii_ptr);
				break;
			case 0x8e: case 0x9e: case 0xae: // load (variable) -> (result)
				Store(U8(f.pc++), Load(a));
				break;
			case 0x8f: case 0x9f: case 0xaf: // not value -> (result)
				if(V5){
						// call func
					op_call(1, a, 0, 0, 0, -1);
					break;
				} else {
						// not value -> (result)
					Store(U8(f.pc++), ~a);
				}
				break;
					/* 0OP instructions --------------- */
			case 0xb0: // rtrue 
				Return(1);
				break;
			case 0xb1: // rfalse 
				Return(0);
				break;
			case 0xb2: // print (literal-string)
				frame.pc = ZSCII(frame.pc);
				Print(zscii_buf, zscii_ptr);
				break;
			case 0xb3: // print_ret (literal-string) 
				frame.pc = ZSCII(frame.pc);
				Print(zscii_buf, zscii_ptr);
				scr.NewLine();
				Return(1);
				break;
			case 0xb4: // nop 
				break;
			case 0xb5: 
				if(V5) BADOP(op); // illegal
				if(V4) BADOP(op); // save -> (result)
				Branch(scr.Save(save())); // save ?(label) 
				break;
			case 0xb6: 
				if(V5) BADOP(op); // illegal
				if(V4) BADOP(op); // restore -> (result)
				Branch(restore(scr.Restore())); // restore ?(label)
				break;
			case 0xb7: // restart 
				restart();
				break;
			case 0xb8: // ret_popped 
				Return(Load(0));
				break;
			case 0xb9: // pop 
				if(V5) BADOP(op); // catch -> (result)
				frame.sp--;
				if(frame.sp < frame.bp) DIE("stack underrun");
				break;
			case 0xba: // quit 
				restart();
				break;
			case 0xbb: // new_line 
				scr.NewLine();
				break;
			case 0xbc: // show_status
				if(V3) UpdateStatus();
					// illegal after 3, but zmachine spec says ignore it
				break;
			case 0xbd: // verify ?(label) 
					//XXX check checksum for real :-)
				Branch(true);
				break;
			case 0xbf: // piracy ?(label)
				if(notV5) BADOP(op);
				Branch(true);
				break;
					/* VAR instructions --------------- */
			case 0xe0: // call routine, arg0-3 -> (result)
				op_call(num, a, b, c, d, U8(f.pc++));
				break;
			case 0xe1: // storew array word-index value
				if(num != 3) DIE("storew needs 3 args");
				W16(a + b * 2, c);
				break;
			case 0xe2: // storeb array byte-index value
				if(num != 3) DIE("storeb needs 3 args");
				W8(a + b, c);
				break;
			case 0xe3: // put-prop object property value
				if(num != 3) DIE("putprop needs 3 args");
				obj.put_prop(a,b,c);
				break;
			case 0xe4:
				if(V3) {// sread text parse
					UpdateStatus();
					readline(a, b);
				} else {
					if(num < 4) d = 0;
					if(num < 3) c = 0;
//					if((c != 0) || (d != 0)) DIE("timed read unsupported");
					readline(a,b);
					Store(U8(f.pc++),10);
				}
				break;
			case 0xe5: // print_char out-char-code
				PrintChar(a);
				break;
			case 0xe6: // print_num value
				PrintNumber(a);
				break;
			case 0xe7: // random range -> (result)
				Store(U8(f.pc++), scr.Random(a));
				break; 			   
			case 0xe8: // push value
				Store(0, a);
				break;
			case 0xe9: // pull (variable)
				Store(a, Load(0));
				break;
			case 0xea: // split_window lines
				scr.SplitWindow(a);
				break;
			case 0xeb: // set_window window
				scr.SetWindow(a);
				break;
			case 0xec: // call_vs2 0..7 -> (result)
				DIE("call_vs2");
			case 0xed: // erase_window win V4
				scr.EraseWindow((short)a);
				break;
			case 0xee: //XXX erase line value V4
				System.err.println("ERASE LINE: "+a+","+b);
				break;
			case 0xef: // set_cursor line column V4
				scr.MoveCursor((short)b,(short)a);
				break;
			case 0xf0: //XXX get_cursor array V4/6
				BADOP(op);
				break;
			case 0xf1: //XXX set_text_style style V4
				break;
			case 0xf2: //XXX buffer_mode flag V4
//				System.err.println("BUFFER? "+(short)a);			
				break;
			case 0xf3: //XXX output_stream number
				a = (short) a;
				if(a == 1) str_display = true;
				if(a == -1) str_display = false;
				break;
			case 0xf4: //XXX input_stream number
				break;
			case 0xf5: //XXX soundfx V5
				break;
			case 0xf6: // read_char 1 time routine -> (result) V4
				if((b != 0) || (c != 0)) DIE("timed read not supported");
				int ch = scr.Read();
				Store(U8(f.pc++), ch);
				break;
			case 0xf7: //XXX scan_table x table len form -> (result) V4
				BADOP(op); break;
			case 0xf8: // not value -> (result) V5/6
				Store(U8(f.pc++), ~a);
				break;
			case 0xf9: // call_vn 0..3
				if(notV5) BADOP(op);
				op_call(num, a, b, c, d, -1);
				break;
			case 0xfa: // call_vn2 0..7 V5
				BADOP(op); break;
			case 0xfb: //XXX tokenize text parse dict flag V5
				BADOP(op); break;
			case 0xfc: //XXX encode_text zscii len from coded V5
				BADOP(op); break;
			case 0xfd: //XXX copy_table first second size V5
				BADOP(op); break;
			case 0xfe: //XXX print_table zscii width height skip V5
				BADOP(op); break;
			case 0xff: //XXX check_arg_count argument-number V5
				BADOP(op); break;
			case 0x100: //XXX save table bytes name -> (result) V5
				if(notV5) BADOP(op);
				if(num != 0) DIE("extended save unsupported");
				if(scr.Save(save())){
					Store(U8(f.pc++), 1);
				} else {
					Store(U8(f.pc++), 0);
				}
				break;
			case 0x101: //XXX restore table bytes name -> (result) V5
				if(notV5) BADOP(op);
				if(num != 0) DIE("extended restore unsupported");
				if(restore(scr.Restore())){
					f = frame;
					Store(U8(f.pc++), 2);
				} else {
					f = frame;
					Store(U8(f.pc++), 0);
				}
				break;
			case 0x102: //XXX log_shift number places -> (result) V5
				BADOP(op); break;
			case 0x103: //XXX arith_shift number places -> (result) V5
				BADOP(op); break;
			case 0x104: //XXX set_font font -> (result) V5
				BADOP(op); break;
			case 0x109: // save_undo -> (result) V5
				if(notV5) BADOP(op);
				Store(U8(f.pc++), -1);
				break; // unsupported
			case 0x10a: // restore_undo -> (result) V5
				if(notV5) BADOP(op);
				Store(U8(f.pc++), 0);
				break; // ignored as unsupported
			default:
				BADOP(op);
			}
		}
	}

	void op_call(int argc, int pc, int a1, int a2, int a3, int res) {
		int l, i;
		ZFrame f;
		
		if(argc < 1) DIE("call with no target");
		
		pc *= PACK; /* unpack addr */
		if(pc == 0) {
				/* weird special case for calling 0 */
			if(res != -1) Store(res,0);
			return;
		}
		
		l = U8(pc++);
		if((l < 0) || (l > 15)) DIE("bad local count " + l);

		if(V5){
			i = pc;
		} else {
				/* adjust for default arguments */
			i = pc + l * 2;
		}
		if(CONSERVE_MEMORY){
			if(freelist != null) {
				f = freelist;
				freelist = f.prev;
				f.pc = i;
				f.sp = frame.sp;
				f.bp = frame.sp;
				f.llen = l + 1;
			} else {
				f = new ZFrame(i, frame.sp, l);
			}
		} else {
			f = new ZFrame(i, frame.sp, l);
		}
		
		l++;
		if(V5){
				/* zero locals */
			for(i = 1; i < l; i++){
				f.locals[i] = 0;
			}
		} else {
				/* preload locals with default values */
			for(i = 1; i < l; i ++){
				f.locals[i] = U16(pc);
				pc += 2;
			}
		}

			/* load arguments into locals */
		if(argc > l) argc = l;
		if(argc > 1) {
			f.locals[1] = a1;
			if(argc > 2) {
				f.locals[2] = a2;
				if(argc > 3) {
					f.locals[3] = a3;
				}
			}
		}

		if(LOGGING) {
			String s = "locals #"+(l-1);
			for(i = 1; i < l; i++){
				s = s + ", "+f.locals[i];
			}
			LOG(s);
		}
		f.res = res;
		f.prev = frame;
		frame = f;
	}

	void Return(int value) {
		if(LOGGING) LOG("return " + value + " -> 0x" + HEX(frame.prev.pc));
		ZFrame f = frame;
		frame = f.prev;
		if(f.res != -1) Store(f.res,value);
		if(CONSERVE_MEMORY) {
			f.prev = freelist;
			freelist = f.prev;
		} else {
			f.prev = null;
		}
	}


	void UpdateStatus() {
		int a = obj.status();
		if(U8(a) != 0){
			ZSCII(a + 1);
		} else {
			zscii_ptr = 0;
		}
		scr.SetStatus(zscii_buf, zscii_ptr);
	}
	
	char line[] = new char[256];
	
		/* parsebuf: max, count, [ w:addr, len, off ] * count */
	void readline(int text, int parse) {		
		int len = scr.ReadLine(line);
		int max = U8(text);
		int pmax = U8(parse);
		int i,j,ptr,start;
		text++;

		if(V3) max -= 1;
		
			/* workaround for buggy early games per zspec1.0 */
		if(pmax > 59) pmax = 59;

		if(LOGGING) LOG("readline " + max + " " + U8(parse));
		parse += 2;
		
			/* copy linebuffer to zmemory, terminating with nul and
			   truncating if needed */
		if(len < max) max = len;
		if(V3){
			for(i = 0; i < max; i++){
				W8(text + i, line[i]);
			}
			W8(text + i, 0);
		} else {
			W8(text, max);
			for(i = 0; i < max; i++){
				W8(text + i + 1, line[i]);
			}
		}
			/* tokenize */
		ptr = 0;
		start = 0;
		i = 0;
		
		while(ptr < max) {
			if((ptr-start) == 0) {
				if(line[ptr] == ' ') {
					start = ptr = ptr + 1;
					continue;
				} 
			}
				/* check for term char special case */

			for(j = 0; j < special_count; j++) {
				if(line[ptr] == special_chars[j]){
					if((ptr - start) > 0){
						find_token(line, start, ptr - start, parse + i * 4);
						i++;
					}
					if(j != 0) {
						/* special #0 (space) does not get its own token */
						find_token(line, ptr, 1, parse + i * 4);
						i++;
					}
					start = ptr + 1;
					break;
				}
			}
			
			ptr++;
		}

		if((ptr - start) > 0){
			find_token(line, start, ptr - start, parse + i * 4);
			i++;
		}

		W8(parse - 1, i);
	}
	
	int token_workbuf[] = new int[16];
	
	void encode(char in[], int off, int len, int out[], int max) {
		int i;
		for(i = 0; i < max; i++) {
			if(len-- > 0) {
				int x = in[off++];
				if((x >= 'a') && (x <= 'z')){
					out[i] = x - ('a' - 6);
					continue;
				}
				if((x >= 'A') && (x <= 'Z')){
					out[i] = x - ('A' - 6);
					continue;
				}
				if((x >= '0') && (x <= '9')){
					out[i++] = 5;
					out[i] = x - ('0' - 8);
					continue;
				}
				switch(x) {
				case '.':  x = 18; break;
				case ',':  x = 19; break;
				case '!':  x = 20; break;
				case '?':  x = 21; break;
				case '_':  x = 22; break;
				case '#':  x = 23; break;
				case '\'': x = 24; break;
				case '"':  x = 25; break;
				case '/':  x = 26; break;
				case '\\': x = 27; break;
				case '-':  x = 28; break;
				case ':':  x = 29; break;
				case '(':  x = 30; break;
				case ')':  x = 31; break;
				default:
					LOG("unknown inchar " + x);
					x = 0;
				}
				out[i++] = 5;
				out[i] = x;
			} else {
				out[i] = 5;
			}
		}
	}
	
			
	void find_token(char data[], int off, int len, int parse) {
		int token = 0;
		int t, low, high, pos, i, n, KEYSZ;
		int buf[] = token_workbuf;
		if(LOGGING) LOG("W: '"+new String(data,off,len)+"' @"+off+","+len);
			
		if(V3) KEYSZ = 4;
		else KEYSZ = 6;

		encode(data, off, len, buf, KEYSZ*3/2);
		
		t = (buf[0] << 10) | (buf[1] << 5) | buf[2];
		buf[0] = (byte) (t >> 8);
		buf[1] = (byte) t;
		t = (buf[3] << 10) | (buf[4] << 5) | buf[5];
		buf[2] = (byte) (t >> 8);
		buf[3] = (byte) t;
		if(V3){
			buf[2] |= 0x80;
		} else {
			t = (buf[6] << 10) | (buf[7] << 5) | buf[8];
			buf[4] = (byte) ((t >> 8) | 0x80);
			buf[5] = (byte) t;			
		}

		low = -1;
		high = dent_count;
		token = 0;

		while((high - low) > 1){
			pos = (high + low) / 2;
			t = dict_ptr + dent_len * pos;
			
			for(n = 0, i = 0; i < KEYSZ; i++){
				n = (buf[i]&0xff) - (mem[t+i]&0xff);
				if(n != 0) break;
			}
			
			if(n == 0) {
				token = t;
				break;
			} else if(n > 0){
				low = pos;
			} else {
				high = pos;
			}
		}
		
		W16(parse, token);
		W8(parse + 2, len);
		W8(parse + 3, off + 1);
	}

	boolean Attr(int obj, int num) {
		if((U8(obj + (num >> 3)) & (1 << (7 - (num & 7)))) == 0){
			return false;
		} else {
			return true;
		}
	}


	String ZSTRING(int ptr) {
		ZSCII(ptr);
		return new String(zscii_buf,0,zscii_ptr);
	}

	int ZSCII(int ptr) {
		zscii_ptr = 0; // at the beginning
		zscii_mode = 0; // shift mode A0 
		return _ZSCII(ptr);
	}
	
	int _ZSCII(int ptr) {
		//boolean done;
		int a,b;
		
		do {
				/* XAAAAABB BBBCCCCC */
			a = mem[ptr++] & 0xff;
			b = mem[ptr++] & 0xff;
			ZCHAR((a >> 2) & 0x1f);
			ZCHAR(((a & 3) << 3) | (b >> 5));
			ZCHAR(b & 0x1f);
		} while((a & 0x80) == 0);
		return ptr;
	}
	
	void ZCHAR(int c) {
		switch(zscii_mode){
		case 0: // A0 mode
			switch(c) {
			case 1: case 2: case 3: // next char is an abbrev
				zscii_mode = c + 2;
				return;
			case 4: // shift to A1
				zscii_mode = 1;
				return;
			case 5: // shift to A2
				zscii_mode = 2;
				return;
			}
			zscii_buf[zscii_ptr++] = zscii_map[zscii_mode][c];
			return;
		case 1: // A1 mode
			switch(c) {
			case 1: case 2: case 3: // next char is an abbrev
				zscii_mode = c + 2;
				return;
			case 4: // shift to A1
				zscii_mode = 1;
				return;
			case 5: // shift to A2
				zscii_mode = 2;
				return;
			}
			zscii_buf[zscii_ptr++] = zscii_map[zscii_mode][c];
			zscii_mode = 0;
			return;
		case 2: // A2 mode
			switch(c) {
			case 1: case 2: case 3: // next char is an abbrev
				zscii_mode = c + 2;
				return;
			case 4: // shift to A1
				zscii_mode = 1;
				return;
			case 5: // shift to A2
				zscii_mode = 2;
				return;
			case 6:
				zscii_mode = 6;
				return;
			}
			zscii_buf[zscii_ptr++] = zscii_map[zscii_mode][c];
			zscii_mode = 0;
			return;
		case 3: // Abbrev modes
		case 4:
		case 5:
			c = ((zscii_mode - 3) * 32 + c);
			zscii_mode = 0;
			_ZSCII(U16(abbr_ptr + 2 * c) * 2);
			zscii_mode = 0;
			break;
		case 6:
			zscii_tmp = c;
			zscii_mode = 7;
			break;
		case 7:
			zscii_buf[zscii_ptr++] = (char) (c | ( zscii_tmp << 5 ));
			zscii_mode = 0;
			break;
			
		}

	}
	
	char zscii_map[][] = {
		{ ' ' ,0   ,0   ,0   ,0   ,0   ,'a' ,'b' ,'c' ,'d' ,'e' ,'f' ,'g' ,'h' ,'i' ,'j' ,
		  'k' ,'l' ,'m' ,'n' ,'o' ,'p' ,'q' ,'r' ,'s' ,'t' ,'u' ,'v' ,'w' ,'x' ,'y' ,'z' },
		{ 0   ,0   ,0   ,0   ,0   ,0   ,'A' ,'B' ,'C' ,'D' ,'E' ,'F' ,'G' ,'H' ,'I' ,'J' ,
		  'K' ,'L' ,'M' ,'N' ,'O' ,'P' ,'Q' ,'R' ,'S' ,'T' ,'U' ,'V' ,'W' ,'X' ,'Y' ,'Z' },
		{ 0   ,0   ,0   ,0   ,0   ,0   ,' ' ,'\n','0' ,'1' ,'2' ,'3' ,'4' ,'5' ,'6' ,'7',
		  '8' ,'9' ,'.' ,',' ,'!' ,'?' ,'_' ,'#' ,'\'','"' ,'/' ,'\\','-' ,':' ,'(' ,')' }
	};
	int zscii_ptr;
	int zscii_mode;
	int zscii_tmp;
	char zscii_buf[];
	
	public void run() {
		LOG("start");

			//for(i = 0; i < (32*3); i++) LOG("'"+ZSTRING(U16(abbr_ptr + i * 2) * 2)+"'");
		
			//for(i = 1; i < 251; i++) dumpobj(i);
		try {
			execute(new ZFrame(U16(6), 0, 0));
			System.err.println("*** DONE ***");
		} catch (Throwable t) {
			t.printStackTrace();
			DIE("jvm oops");
		}
	}
	
	void restart() {
		System.arraycopy(backup, 0, mem, 0, static_ptr);
		syncstate();
		frame = new ZFrame(U16(6), 0, 0);
		freelist = null;
	}
	
	void SW(byte[] data, int addr, int val) {
		data[addr] = (byte) ((val >> 24) & 0xff);
		data[addr + 1] = (byte) ((val >> 16) & 0xff);
		data[addr + 2] = (byte) ((val >> 8) & 0xff);
		data[addr + 3] = (byte) (val & 0xff);
	}

	int LW(byte[] mem, int addr) {
		return ((mem[addr] & 0xff) << 24) |
			((mem[addr + 1] & 0xff) << 16) |
			((mem[addr + 2] & 0xff) << 8) |
			(mem[addr + 3] & 0xff);		
	}
	
	byte[] save() {
		int size, p, i, c;
		ZFrame f;
		byte[] state;

			/* dynram, stack, stacksize, fcount */
		size = static_ptr + frame.sp * 4 + 8; 
		
		for(c = 0, f = frame; f != null; f = f.prev) {
			size += 16 + 4 * (f.locals.length - 1);
				/* pc, sp, bp, llen, local (x llen) */
			c ++;
		}

		state = new byte[size + 64];
		System.arraycopy(mem, 0, state, 64, static_ptr);
		p = static_ptr + 64;
		
			/* save stack */
		SW(state, p, frame.sp);
		p += 4;
		for(i = 0; i < frame.sp; i++) {
			SW(state, p, stack[i]);
			p += 4;
		}
			

			/* save framelist */
		SW(state, p, c);
		p += 4;
		for(f = frame ; f != null; f = f.prev) {
			SW(state, p, f.pc);
			SW(state, p + 4, f.sp);
			SW(state, p + 8, f.bp);
			SW(state, p + 12, f.locals.length - 1);
			p += 16;
			for(i = 0; i < f.locals.length - 1; i++){
				SW(state, p, f.locals[1 + i]);
				p += 4;
			}
		}
		
		return state;
	}

	boolean restore(byte[] state) {
		int p = static_ptr + 64;
		int c, i, j;
		ZFrame f = null;

		if((state == null) || (state.length < static_ptr)) return  false;
	
			/* sanity check that this is the same gamefile */
		for(i = 0; i < 64; i++){
			if(state[i+64] != mem[i]) return false;
		}
			/* restore dynamic ram */
		System.arraycopy(state, 64, mem, 0, static_ptr);

			/* restore stack */	
		c = LW(state, p);
		p += 4;
		for(i = 0; i < c; i++) {
			stack[i] = LW(state, p);
			p += 4;
		}

			/* restore framelist */
		c = LW(state, p);
		p += 4;
		frame = null;
		for(i = 0; i < c; i++) {
			int pc = LW(state, p + 0);
			int sp = LW(state, p + 4);
			int bp = LW(state, p + 8);
			int len = LW(state, p + 12);
			p += 16;
			if(f == null) {
				f = new ZFrame(pc, sp, len);
				frame = f;
			} else {
				ZFrame x = new ZFrame(pc, sp, len);
				f.prev = x;
				f = x;
			}
			f.bp = bp;
			for(j = 0; j < len; j++){
				f.locals[j + 1] = LW(state, p);
				p += 4;
			}
		}
		
		syncstate();
		return true;
	}
	
	int special_count;
	int special_chars[];
	
	public ZMachine(ZScreen screen, byte[] data) {
		int i;
		
		LOG("init");
		scr = screen;
		mem = data;
		stack = new int[4096];
		zscii_buf = new char[4096];
		int version = U8(0);
		
		LOG("game version    " + version);
		switch(version){
		case 1:
		case 2:
		case 3:
			PACK = 2;
			notV4 = true;
			notV5 = true;
			V3 = true;
			obj = new ZObject();
			break;
		case 4:
			PACK = 4;
			V4 = true;
			notV5 = true;
			obj = new ZObjectWide();
			break;
		case 5:
			PACK = 4;
			V5 = true;
			obj = new ZObjectWide();
			break;
		default:
			throw new RuntimeException("unsupported version");
		}
		LOG("high memory   @ 0x" + HEX(U16(4)));
		LOG("entry pc      @ 0x" + HEX(U16(6))); // XXX V6
		dict_ptr = U16(8);
			// load seps 
		special_count = U8(dict_ptr) + 1;
		special_chars = new int[special_count];
		special_chars[0] = ' ';
		for(i = 1; i < special_count; i++){
			special_chars[i] = U8(dict_ptr + i);
		}
		
		dict_ptr += U8(dict_ptr) + 1;
		dent_len = U8(dict_ptr++);
		dent_count = U16(dict_ptr);
		dict_ptr += 2;
		LOG("dictionary    @ 0x" + HEX(U16(8)) + ": " + dent_count + " of " + dent_len);
		
		LOG("object table  @ 0x" + HEX(U16(10)));
		LOG("global vars   @ 0x" + HEX(U16(12)));
		global_ptr = U16(12) - 32;
		LOG("static memory @ 0x" + HEX(U16(14)));
		static_ptr = U16(14);
		LOG("abbrev table  @ 0x" + HEX(U16(24)));
		abbr_ptr = U16(24);
		default_prop_ptr = U16(10);
			/* object table is after default props and biased for 1-based access */
		object_ptr = default_prop_ptr + obj.PropMax * 2 - obj.Size;
			/* bias default props for 1-based access */
		default_prop_ptr -= 2;

		backup = new byte[static_ptr];
		System.arraycopy(mem, 0, backup, 0, static_ptr);

		syncstate();		
	}

		/* let the interpreter know about our configuration */
	void syncstate() {
			/* screen size in lines by chars */
		mem[0x20] = (byte) scr.GetHeight();
		mem[0x21] = (byte) scr.GetWidth();
			/* screen size in 'units' w by h */
		mem[0x22] = (byte) scr.GetWidth();
		mem[0x24] = (byte) scr.GetHeight();
			/* font size in 'units' w by h */
		mem[0x26] = 1;
		mem[0x27] = 1;

		scr.EraseWindow(-1);

		if(V3){
			scr.SplitWindow(1);
		}
	}

	void Println(String s) {
		char data[] = new char[s.length()];
		s.getChars(0, s.length(), data, 0);
		scr.Print(data,data.length);
		scr.PrintChar('\n');
		System.err.println(s);
	}
	
	void Print(char data[], int len) {
		if(str_display) scr.Print(data,len);
	}
	
	void PrintChar(int ch) {
		if(str_display) scr.PrintChar(ch);
	}
	
	void PrintNumber(int n) {
		if(str_display) scr.PrintNumber(n);
	}
	
	boolean str_display = true;
	
	void SetLog(PrintStream p) {}
	
	ZObject obj;
	
	int static_ptr;
	int global_ptr; /* start of globals - 16 */
	int object_ptr; /* start of object table */
	int default_prop_ptr;
	int dict_ptr;
	int abbr_ptr;
	
	int dent_len;
	int dent_count;
	
	int stack[];
	ZFrame frame, freelist;
	
	byte[] mem;
	byte[] backup;
	ZScreen scr;

	int PACK;
	
	boolean V3, V4, V5, notV4, notV5;
	
	static final boolean TRACING = false;
	static final boolean LOGGING = false; 

		/* reuse zframes instead of allocating new ones, if we can */
	static final boolean CONSERVE_MEMORY = true;
}
