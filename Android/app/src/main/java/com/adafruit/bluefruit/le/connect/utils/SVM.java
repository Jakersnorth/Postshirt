package com.adafruit.bluefruit.le.connect.utils;

/*
 * General SVM class for initializing and using SVM classifiers,
 * initialize an SVM object with the vectors, coefficients, intercepts, and weights of a trained model and then use to predict new data
 *
 * Auto-generated using sklearn-porter
 */
public class SVM {

    private enum Kernel { LINEAR, POLY, RBF, SIGMOID }

    private int nClasses;
    private int nRows;
    private int[] classes;
    private double[][] vectors;
    private double[][] coefficients;
    private double[] intercepts;
    private int[] weights;
    private Kernel kernel;
    private double gamma;
    private double coef0;
    private double degree;

    public SVM(int nClasses, int nRows, double[][] vectors, double[][] coefficients, double[] intercepts, int[] weights, String kernel, double gamma, double coef0, double degree) {
        this.nClasses = nClasses;
        this.classes = new int[nClasses];
        for (int i = 0; i < nClasses; i++) {
            this.classes[i] = i;
        }
        this.nRows = nRows;

        this.vectors = vectors;
        this.coefficients = coefficients;
        this.intercepts = intercepts;
        this.weights = weights;

        this.kernel = Kernel.valueOf(kernel.toUpperCase());
        this.gamma = gamma;
        this.coef0 = coef0;
        this.degree = degree;
    }

    public int predict(double[] features) {

        double[] kernels = new double[vectors.length];
        double kernel;
        switch (this.kernel) {
            case LINEAR:
                // <x,x'>
                for (int i = 0; i < this.vectors.length; i++) {
                    kernel = 0.;
                    for (int j = 0; j < this.vectors[i].length; j++) {
                        kernel += this.vectors[i][j] * features[j];
                    }
                    kernels[i] = kernel;
                }
                break;
            case POLY:
                // (y<x,x'>+r)^d
                for (int i = 0; i < this.vectors.length; i++) {
                    kernel = 0.;
                    for (int j = 0; j < this.vectors[i].length; j++) {
                        kernel += this.vectors[i][j] * features[j];
                    }
                    kernels[i] = Math.pow((this.gamma * kernel) + this.coef0, this.degree);
                }
                break;
            case RBF:
                // exp(-y|x-x'|^2)
                for (int i = 0; i < this.vectors.length; i++) {
                    kernel = 0.;
                    for (int j = 0; j < this.vectors[i].length; j++) {
                        kernel += Math.pow(this.vectors[i][j] - features[j], 2);
                    }
                    kernels[i] = Math.exp(-this.gamma * kernel);
                }
                break;
            case SIGMOID:
                // tanh(y<x,x'>+r)
                for (int i = 0; i < this.vectors.length; i++) {
                    kernel = 0.;
                    for (int j = 0; j < this.vectors[i].length; j++) {
                        kernel += this.vectors[i][j] * features[j];
                    }
                    kernels[i] = Math.tanh((this.gamma * kernel) + this.coef0);
                }
                break;
        }

        int[] starts = new int[this.nRows];
        for (int i = 0; i < this.nRows; i++) {
            if (i != 0) {
                int start = 0;
                for (int j = 0; j < i; j++) {
                    start += this.weights[j];
                }
                starts[i] = start;
            } else {
                starts[0] = 0;
            }
        }

        int[] ends = new int[this.nRows];
        for (int i = 0; i < this.nRows; i++) {
            ends[i] = this.weights[i] + starts[i];
        }

        if (this.nClasses == 2) {

            for (int i = 0; i < kernels.length; i++) {
                kernels[i] = -kernels[i];
            }

            double decision = 0.;
            for (int k = starts[1]; k < ends[1]; k++) {
                decision += kernels[k] * this.coefficients[0][k];
            }
            for (int k = starts[0]; k < ends[0]; k++) {
                decision += kernels[k] * this.coefficients[0][k];
            }
            decision += this.intercepts[0];

            if (decision > 0) {
                return 0;
            }
            return 1;

        }

        double[] decisions = new double[this.intercepts.length];
        for (int i = 0, d = 0, l = this.nRows; i < l; i++) {
            for (int j = i + 1; j < l; j++) {
                double tmp = 0.;
                for (int k = starts[j]; k < ends[j]; k++) {
                    tmp += this.coefficients[i][k] * kernels[k];
                }
                for (int k = starts[i]; k < ends[i]; k++) {
                    tmp += this.coefficients[j - 1][k] * kernels[k];
                }
                decisions[d] = tmp + this.intercepts[d];
                d++;
            }
        }

        int[] votes = new int[this.intercepts.length];
        for (int i = 0, d = 0, l = this.nRows; i < l; i++) {
            for (int j = i + 1; j < l; j++) {
                votes[d] = decisions[d] > 0 ? i : j;
                d++;
            }
        }

        int[] amounts = new int[this.nClasses];
        for (int i = 0, l = votes.length; i < l; i++) {
            amounts[votes[i]] += 1;
        }

        int classVal = -1, classIdx = -1;
        for (int i = 0, l = amounts.length; i < l; i++) {
            if (amounts[i] > classVal) {
                classVal = amounts[i];
                classIdx= i;
            }
        }
        return this.classes[classIdx];

    }
    /*
    public static void main(String[] args) {
        if (args.length == 6) {

            // Features:
            double[] features = new double[args.length];
            for (int i = 0, l = args.length; i < l; i++) {
                features[i] = Double.parseDouble(args[i]);
            }

            // Parameters:
            double[][] vectors = {{474.0, 405.0, 501.0, 568.0, 409.0, 501.0}, {474.0, 405.0, 500.0, 568.0, 410.0, 500.0}, {453.0, 423.0, 492.0, 520.0, 395.0, 493.0}};
            double[][] coefficients = {{-1.5676316581642036e-14, -0.0005870267096996199, 0.0005870267097152961}};
            double[] intercepts = {-24.581449955982507};
            int[] weights = {2, 1};

            // Prediction:
            SVM clf = new SVM(2, 2, vectors, coefficients, intercepts, weights, "linear", 0.5, 0.0, 3);
            int estimation = clf.predict(features);
            System.out.println(estimation);

        }
    }
    */
}

