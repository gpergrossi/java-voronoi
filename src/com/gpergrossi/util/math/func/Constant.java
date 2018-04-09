package com.gpergrossi.util.math.func;

public class Constant extends Function {

	public static final Constant ZERO = new Constant(0);
	
	public final double c;
	
	public static Function create(double c) {
		if (c == 0) return ZERO;
		return new Constant(c);
	}
	
	Constant (double c) {
		this.c = c;
	}
	
	@Override
	public double[] zeros() {
		return new double[0];
	}
	
	@Override
	public double getValue(double x) {
		return c;
	}

	@Override
	public Function tryAdd(Function f) {
		if (f instanceof Constant) return this.add((Constant) f);
		return null;
	}
	
	public Constant add(Constant other) {
		return new Constant(this.c + other.c);
	}

	@Override
	public Function negate() {
		return new Constant(-c);
	}

	@Override
	public Function tryMultiply(Function f) {
		if (f instanceof Constant) return this.multiply((Constant) f);
		return null;
	}

	public Constant multiply(Constant other) {
		return new Constant(this.c * other.c);
	}
	
	@Override
	public Function inverse() {
		return new Constant(1.0 / c);
	}

	@Override
	public Function derivative() {
		return ZERO;
	}
	
}
