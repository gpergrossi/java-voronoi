package com.gpergrossi.util.math.func;

public class Vertical extends Undefined {

	double verticalX;
	
	public Vertical(double x) {
		this.verticalX = x;
	}

	@Override
	public Function tryAdd(Function f) {
		if (f instanceof Vertical) return add((Vertical) f);
		return super.tryAdd(f);
	}
	
	public Function add(Vertical f) {
		if (f.verticalX == verticalX) return this;
		return new Undefined();
	}

	@Override
	public double[] zeros() {
		return new double[] { verticalX };
	}

	public String toString() {
		return "Vertical[x="+verticalX+"]";
	}

	public double getX() {
		return verticalX;
	}
	
}
