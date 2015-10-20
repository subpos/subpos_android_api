package iocomms.subpos;

import org.apache.commons.math3.fitting.leastsquares.MultivariateJacobianFunction;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Pair;

/**
 * Models the Trilateration problem. This is a formulation for a nonlinear least
 * squares optimizer.
 * 
 * The MIT License (MIT)
 * Copyright (c) 2014 Scott Wiedemann
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
public class TrilaterationFunction implements MultivariateJacobianFunction {

	private static final double epsilon = 1E-7;
	
	/**
	 * Known positions of static nodes
	 */
	private final double positions[][];

	/**
	 * Euclidean distances from static nodes to mobile node
	 */
	private final double distances[];

	public TrilaterationFunction(double positions[][], double distances[]) {
		
		if(positions.length < 2) {
			throw new IllegalArgumentException("Need at least two positions.");
		}
		
		if(positions.length != distances.length) {
			throw new IllegalArgumentException("The number of positions you provided, " + positions.length + ", does not match the number of distances, " + distances.length + ".");
		}
		
		// bound distances to strictly positive domain
		for (int i = 0; i < distances.length; i++) {
			distances[i] = Math.max(distances[i], epsilon);
		}
		
		int positionDimension = positions[0].length;
		for (int i = 1; i < positions.length; i++) {
			if(positionDimension != positions[i].length) {
				throw new IllegalArgumentException("The dimension of all positions should be the same.");
			}
		}
		
		this.positions = positions;
		this.distances = distances;
	}

	public final double[] getDistances() {
		return distances;
	}

	public final double[][] getPositions() {
		return positions;
	}

	/**
	 * Calculate and return Jacobian function Actually return initialized function
	 * 
	 * Jacobian matrix, [i][j] at 
	 * J[i][0] = delta_[(x0-xi)^2 + (y0-yi)^2 - ri^2]/delta_[x0] at 
	 * J[i][1] = delta_[(x0-xi)^2 + (y0-yi)^2 - ri^2]/delta_[y0] partial derivative with respect to the parameters passed to value() method
	 * 
	 */
	public RealMatrix jacobian(RealVector point) {
		double[] pointArray = point.toArray();

		double[][] jacobian = new double[distances.length][pointArray.length];
		for (int i = 0; i < jacobian.length; i++) {
			for (int j = 0; j < pointArray.length; j++) {
				jacobian[i][j] = 2 * pointArray[j] - 2 * positions[i][j];
			}
		}

		return new Array2DRowRealMatrix(jacobian);
	}

	@Override
	public Pair<RealVector, RealMatrix> value(RealVector point) {

		// input
		double[] pointArray = point.toArray();

		// output
		double[] resultPoint = new double[this.distances.length];
		
		// compute least squares
		for (int i = 0; i < resultPoint.length; i++) {
			resultPoint[i] = 0.0;
			// calculate sum, add to overall
			for (int j = 0; j < pointArray.length; j++) {
				resultPoint[i] += (pointArray[j] - this.getPositions()[i][j]) * (pointArray[j] - this.getPositions()[i][j]);
			}
			resultPoint[i] -= (this.getDistances()[i]) * (this.getDistances()[i]);
		}

		RealMatrix jacobian = jacobian(point);
		return new Pair<RealVector, RealMatrix>(new ArrayRealVector(resultPoint), jacobian);
	}
}
