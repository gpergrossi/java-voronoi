package dev.mortus.util.math.func1d;

import dev.mortus.util.math.vectors.Double2D;

public final class Quadratic extends Function {

	public static Function fromPointAndLine(double x, double y, double lineY) {
		double den = (y - lineY) * 2;
		if (Math.abs(den) < 0.0001) { //TODO: EPSILON?
			return new Vertical(x);
		}

		double a = 1 / den;
		double b = -(2 * x) / den;
		double c = (x*x + y*y - lineY*lineY) / den;
		return new Quadratic(a, b, c);
	}

	public final double a, b, c;

	public static Function create(double a, double b, double c) {
		if (a == 0) {
			if (b == 0)
				return new Constant(c);
			return new Linear(b, c);
		}
		return new Quadratic(a, b, c);
	}

	Quadratic(double a, double b, double c) {
		this.a = a;
		this.b = b;
		this.c = c;
	}

	@Override
	public double[] zeros() {
		double range = b * b - 4 * a * c;

		// No real zeros
		if (range < 0)
			return new double[0];

		// Normal quadratic
		if (range == 0) {
			// Parabola touches zero exactly once
			return new double[] { -b / (2 * a) };
		} else {
			range = Math.sqrt(range);

			return new double[] { (-b - range) / (2 * a), (-b + range) / (2 * a) };
		}
	}

	@Override
	public double getValue(double x) {
		return a * x * x + b * x + c;
	}

	public static Double2D getIntersect(Function leftGreater, Function rightGreater) {
		Function difference = rightGreater.subtract(leftGreater);

		double[] zeros = difference.zeros();
		if (zeros.length == 0) return null;

		Function derivative = difference.derivative();
		if (derivative instanceof Undefined) {
			if (rightGreater instanceof Undefined) {
				double x = zeros[0];
				double y = leftGreater.getValue(x);
				return new Double2D(x, y);
			} else {
				double x = zeros[0];
				double y = rightGreater.getValue(x);
				return new Double2D(x, y);
			}
		}

		// Assuming the functions given are quadratic, we only
		// care about a single intersect with a positive derivative
		for (double zero : zeros) {
			if (derivative.getValue(zero) > 0) {
				double x = zero;
				double y = rightGreater.getValue(zero);
				return new Double2D(x, y);
			}
		}

		return null;
	}

	public String toString() {
		return "Quadratic[equation=" + a + "x^2 + " + b + "x + " + c + "]";
	}

	@Override
	public Function tryAdd(Function f) {
		if (f instanceof Constant)
			return this.add((Constant) f);
		if (f instanceof Linear)
			return this.add((Linear) f);
		if (f instanceof Quadratic)
			return this.add((Quadratic) f);
		return null;
	}

	public Function add(Constant other) {
		return create(a, b, c + other.c);
	}

	public Function add(Linear other) {
		return create(a, b + other.m, c + other.b);
	}

	public Function add(Quadratic other) {
		return create(a + other.a, b + other.b, c + other.c);
	}

	@Override
	public Function negate() {
		return new Quadratic(-a, -b, -c);
	}

	@Override
	public Function tryMultiply(Function f) {
		if (f instanceof Constant)
			return this.multiply((Constant) f);
		return null;
	}

	public Function multiply(Constant other) {
		return create(a * other.c, b * other.c, c * other.c);
	}

	@Override
	public Function inverse() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Function derivative() {
		return new Linear(2 * this.a, this.b);
	}
}
