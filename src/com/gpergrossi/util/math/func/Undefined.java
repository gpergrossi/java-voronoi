package com.gpergrossi.util.math.func;

public class Undefined extends Function {

	public Undefined() {}
	
	@Override
	public double getValue(double x) {
		return Double.NaN;
	}

	@Override
	public Function tryAdd(Function f) {
		return this;
	}

	@Override
	public Function negate() {
		return this;
	}

	@Override
	public Function tryMultiply(Function f) {
		return this;
	}

	@Override
	public Function inverse() {
		return this;
	}

	@Override
	public Function derivative() {
		return this;
	}

	@Override
	public double[] zeros() {
		return new double[0];
	}
	
	public String toString() {
		return "Undefined";
	}

}
