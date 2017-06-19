package dev.mortus.util.math.func1d;

public class Linear extends Function {
	
	public final double m, b;
	
	Function create(double m, double b) {
		if (m != 0) return new Linear(m, b);
		return new Constant(b);
	}
	
	Linear (double m, double b) {
		this.m = m;
		this.b = b;
	}
	
	@Override
	public double[] zeros() {
		if (m == 0) return new double[0];
		return new double[] { -b/m };
	}
	
	@Override
	public double getValue(double x) {
		return m*x+b;
	}

	@Override
	public Function tryAdd(Function f) {
		if (f instanceof Constant) return this.add((Constant) f);
		if (f instanceof Linear) return this.add((Linear) f);
		return null;
	}

	public Function add(Constant other) {
		return create(this.m, this.b + other.c);
	}
	
	public Function add(Linear other) {
		return create(this.m + other.m, this.b + other.b);
	}
	
	@Override
	public Function negate() {
		return create(-this.m, -this.b);
	}

	@Override
	public Function tryMultiply(Function f) {
		if (f instanceof Constant) return this.multiply((Constant) f);
		if (f instanceof Linear) return this.multiply((Linear) f);
		return null;
	}

	public Function multiply(Constant other) {
		return create(this.m * other.c, this.b * other.c);
	}
	
	public Function multiply(Linear other) {
		return Quadratic.create(this.m * other.m, this.m * other.b + this.b * other.m, this.b * other.b);
	}

	@Override
	public Function inverse() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Function derivative() {
		return new Constant(this.m);
	}
	
}
