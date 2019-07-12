/*  MicroJava Parser (HM 06-12-28)
    ================
*/
package MJ;

import java.util.*;
//import MJ.SymTab.*;
//import MJ.CodeGen.*;

public class Parser {
	private static final int  // token codes
		none      = 0,
		ident     = 1,
		number    = 2,
		charCon   = 3,
		plus      = 4,
		minus     = 5,
		times     = 6,
		slash     = 7,
		rem       = 8,
		eql       = 9,
		neq       = 10,
		lss       = 11,
		leq       = 12,
		gtr       = 13,
		geq       = 14,
		assign    = 15,
		semicolon = 16,
		comma     = 17,
		period    = 18,
		lpar      = 19,
		rpar      = 20,
		lbrack    = 21,
		rbrack    = 22,
		lbrace    = 23,
		rbrace    = 24,
		class_    = 25,
		else_     = 26,
		final_    = 27,
		if_       = 28,
		new_      = 29,
		print_    = 30,
		program_  = 31,
		read_     = 32,
		return_   = 33,
		void_     = 34,
		while_    = 35,
		eof       = 36;
	private static final String[] name = { // token names for error messages
		"none", "identifier", "number", "char constant", "+", "-", "*", "/", "%",
		"==", "!=", "<", "<=", ">", ">=", "=", ";", ",", ".", "(", ")",
		"[", "]", "{", "}", "class", "else", "final", "if", "new", "print",
		"program", "read", "return", "void", "while", "eof"
		};

	private static Token t;			// current token (recently recognized)
	private static Token la;		// lookahead token
	private static int sym;			// always contains la.kind
	public  static int errors;  // error counter
	private static int errDist;	// no. of correctly recognized tokens since last error

	private static BitSet exprStart, statStart, statSeqFollow, declStart, declFollow;

	//------------------- auxiliary methods ----------------------
	private static void scan() {
		t = la;
		la = Scanner.next();
		sym = la.kind;
		errDist++;
		///*
		System.out.print("line " + la.line + ", col " + la.col + ": " + name[sym]);
		if (sym == ident) System.out.print(" (" + la.string + ")");
		if (sym == number || sym == charCon) System.out.print(" (" + la.val + ")");
		System.out.println();//*/
	}

	private static void check(int expected) {
		if (sym == expected) scan();
		else error(name[expected] + " expected");
	}

	public static void error(String msg) { // syntactic error at token la
		if (errDist >= 3) {
			System.out.println("-- line " + la.line + " col " + la.col + ": " + msg);
			errors++;
		}
		errDist = 0;
	}

	//-------------- parsing methods (in alphabetical order) -----------------


	// ActPars = "(" [ Expr { "," Expr } ] ")"
	private static void ActPars() {
		check(lpar);
		if (sym == minus || sym == ident) {
			Expr();
			while (sym == comma) {
				check(comma);
				Expr();
			}
		}
		check(rpar);
	}
	// Block = "{" {Statement} "}"
	private static void Block() {
		check(lbrace);
		while (true) {
			// first(Statement) = first(Designator) = ident
			if (sym == ident) Statement();
			else break;
		}
		check(rbrace);
	}

	// ClassDecl = "class" ident "{" {VarDecl} "}"
	private static void ClassDecl() {
		check(class_);
		check(ident);
		check(lbrace);
		// firstVarDecl = firstType = ident
		while (sym == ident) VarDecl();
		check(rbrace);
	}

	// Condition = Expr Relop Expr
	private static void Condition() {
		Expr();
		Relop();
		Expr();
	}

	// ConstDecl = "final" Type ident "=" (number | charCon) ";"
	private static void ConstDecl() {
		check(final_);
		Type();
		check(ident);
		check(assign);
		if (sym == number) {
			check(number);
		}
		else if (sym == charCon) {
			check(charCon);
		}
		else error("Invalid ConstDecl");
		check(semicolon);
	}

	// Designator = ident {"." ident | "[" Expr "]"}
	private static void Designator() {
		check(ident);
		if (sym == period) {
			check(period);
			check(ident);
		}
		else if (sym == lbrack) {
			check(lbrack);
			Expr();
			check(rbrack);
		}
		else error("Designator");
	}

	// Expr = ["-"] Term {Addop Term}
	private static void Expr() {
		if (sym == minus) check(minus);
		Term();
		if (sym == plus | sym == minus) {
			check(sym);
			Term();
		}
	}

	// Factor = Designator [ActPars] | number | charCon | "new" ident ["[" Expr "]" | "(" Expr ")"
	private static void Factor() {
		if (sym == ident) {
			Designator();
			if (sym == lpar) ActPars();
		}
		else if (sym == number || sym == charCon) check(sym);
		else if (sym == new_) {
			check(new_);
			check(ident);
			if (sym == lbrack) {
				check(lbrack);
				Expr();
				check(rbrack);
			}
		}
		else if (sym == lpar) {
			check(lpar);
			Expr();
			check(rpar);
		}
		else error("Factor");
	}

	// FormPars = Type ident {"," Type Ident}
	private static void FormPars() {
		Type();
		check(ident);
		if (sym == comma) FormPars();
	}

	// MethodDecl = (Type | void) ident "(" [FormPars] ")" {VarDecl} Block
	private static void MethodDecl() {
		if (sym == void_) check(void_);
		else if (sym == ident) Type();
		else error("MethodDecl");
		check(ident);
		check(lpar);
		if (sym == ident) FormPars();
		check(rpar);
		if (sym == ident) VarDecl();
		Block();
	}

	// Program = "program" ident {ConstDecl | ClassDecl | VarDecl} '{' {MethodDecl} '}'.
	private static void Program() {
		check(program_);
		check(ident);
		while (true) {
			// first(ConstDecl) = final_
			if (sym == final_) ConstDecl();
			// first(ClassDecl) = class_
			else if (sym == class_) ClassDecl();
			// first(VarDecl)   = first(Type) = ident
			else if (sym == ident) VarDecl();
			else break;
		}
		if (sym == lbrace) {
			check(lbrace);
			while (true) {
				// first(MethodDecl) = Type | void
				if (sym == ident || sym == void_) {
					MethodDecl();
				}
				else break;
			}
			check(rbrace);
		}
	}

	// Relop = relational operators
	private static void Relop() {
		if (sym == eql || sym == neq || sym == gtr || sym == geq || sym == lss || sym == leq) {
			check(sym);
		}
		else error("relational");
	}

	// Statement = Designator ("=" Expr | ActPars) ";" | <if> | <while> | <return> | <read> | <print> | Block | ";"
	private static void Statement() {
		Designator();
		if (sym == assign) {
			check(assign);
			Expr();
		}
		check(semicolon);
	}

	// Term = Factor { Mulop Factor }
	private static void Term() {
		Factor();
		while (true) {
			// Mulop
			if (sym == times || sym == slash || sym == rem) {
				check(sym);
				Factor();
			}
		}
	}

	// Type = ident ["[" "]"]
	private static void Type() {
		check(ident);
		if (sym == lbrack) {
			check(lbrack);
			check(rbrack);
		}
	}

	// VarDecl = Type ident {"," ident }";"
	private static void VarDecl() {
		Type();
		check(ident);
		while (sym == comma) {
			check(comma);
			check(ident);
		}
		check(semicolon);
	}

	//TODO  // add parsing methods for all productions

	public static void parse() {
		// initialize symbol sets
		BitSet s;
		s = new BitSet(64); exprStart = s;
		s.set(ident); s.set(number); s.set(charCon); s.set(new_); s.set(lpar); s.set(minus);

		s = new BitSet(64); statStart = s;
		s.set(ident); s.set(if_); s.set(while_); s.set(read_);
		s.set(return_); s.set(print_); s.set(lbrace); s.set(semicolon);

		s = new BitSet(64); statSeqFollow = s;
		s.set(rbrace); s.set(eof);

		s = new BitSet(64); declStart = s;
		s.set(final_); s.set(ident); s.set(class_);

		s = new BitSet(64); declFollow = s;
		s.set(lbrace); s.set(void_); s.set(eof);

		// start parsing
		errors = 0; errDist = 3;
		scan();
		Program();
		if (sym != eof) error("end of file found before end of program");
	}

}
