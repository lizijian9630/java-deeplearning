package org.deeplearning4j.nn.conf;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.deeplearning4j.linalg.api.activation.ActivationFunction;
import org.deeplearning4j.linalg.lossfunctions.LossFunctions;
import org.deeplearning4j.models.featuredetectors.rbm.RBM;
import org.deeplearning4j.nn.WeightInit;
import org.deeplearning4j.nn.api.NeuralNetwork;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A Serializable configuration
 * for neural nets that covers per layer parameters
 *
 * @author Adam Gibson
 */
public class NeuralNetConfiguration implements Serializable {

    private float sparsity;
    private boolean useAdaGrad = true;
    private float lr = 1e-1f;
    /* momentum for learning */
    protected float momentum = 0.5f;
    /* L2 Regularization constant */
    protected float l2 = 0.1f;
    protected boolean useRegularization = false;
    //momentum after n iterations
    protected Map<Integer,Float> momentumAfter = new HashMap<>();
    //reset adagrad historical gradient after n iterations
    protected int resetAdaGradIterations = -1;
    protected float dropOut = 0;
    //use only when binary hidden neuralNets are active
    protected boolean applySparsity = false;
    //weight init scheme, this can either be a distribution or a applyTransformToDestination scheme
    protected WeightInit weightInit;
    protected NeuralNetwork.OptimizationAlgorithm optimizationAlgo = NeuralNetwork.OptimizationAlgorithm.CONJUGATE_GRADIENT;
    protected LossFunctions.LossFunction lossFunction = LossFunctions.LossFunction.RECONSTRUCTION_CROSSENTROPY;
    protected int renderWeightsEveryNumEpochs = -1;
    //whether to concat hidden bias or add it
    protected  boolean concatBiases = false;
    //whether to constrain the gradient to unit norm or not
    protected boolean constrainGradientToUnitNorm = false;
    /* RNG for sampling. */
    protected transient RandomGenerator rng;
    protected transient RealDistribution dist;
    protected long seed = 123;
    protected int nIn,nOut;
    protected ActivationFunction activationFunction;
    protected boolean useHiddenActivationsForwardProp = true;
    private RBM.VisibleUnit visibleUnit = RBM.VisibleUnit.BINARY;
    private RBM.HiddenUnit hiddenUnit = RBM.HiddenUnit.BINARY;

    public NeuralNetConfiguration() {

    }



    public NeuralNetConfiguration(float sparsity, boolean useAdaGrad, float lr, float momentum, float l2, boolean useRegularization, Map<Integer, Float> momentumAfter, int resetAdaGradIterations, float dropOut, boolean applySparsity, WeightInit weightInit, NeuralNetwork.OptimizationAlgorithm optimizationAlgo, LossFunctions.LossFunction lossFunction, int renderWeightsEveryNumEpochs, boolean concatBiases, boolean constrainGradientToUnitNorm, RandomGenerator rng, RealDistribution dist, long seed, int nIn, int nOut, ActivationFunction activationFunction, boolean useHiddenActivationsForwardProp, RBM.VisibleUnit visibleUnit, RBM.HiddenUnit hiddenUnit) {
        this.sparsity = sparsity;
        this.useAdaGrad = useAdaGrad;
        this.lr = lr;
        this.momentum = momentum;
        this.l2 = l2;
        this.useRegularization = useRegularization;
        this.momentumAfter = momentumAfter;
        this.resetAdaGradIterations = resetAdaGradIterations;
        this.dropOut = dropOut;
        this.applySparsity = applySparsity;
        this.weightInit = weightInit;
        this.optimizationAlgo = optimizationAlgo;
        this.lossFunction = lossFunction;
        this.renderWeightsEveryNumEpochs = renderWeightsEveryNumEpochs;
        this.concatBiases = concatBiases;
        this.constrainGradientToUnitNorm = constrainGradientToUnitNorm;
        this.rng = rng;
        this.dist = dist;
        this.seed = seed;
        this.nIn = nIn;
        this.nOut = nOut;
        this.activationFunction = activationFunction;
        this.useHiddenActivationsForwardProp = useHiddenActivationsForwardProp;
        this.visibleUnit = visibleUnit;
        if(dist == null)
            this.dist = new NormalDistribution(rng,0,.01,NormalDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY);

        this.hiddenUnit = hiddenUnit;
    }

    public RBM.HiddenUnit getHiddenUnit() {
        return hiddenUnit;
    }

    public void setHiddenUnit(RBM.HiddenUnit hiddenUnit) {
        this.hiddenUnit = hiddenUnit;
    }

    public RBM.VisibleUnit getVisibleUnit() {
        return visibleUnit;
    }

    public void setVisibleUnit(RBM.VisibleUnit visibleUnit) {
        this.visibleUnit = visibleUnit;
    }

    public boolean isUseHiddenActivationsForwardProp() {
        return useHiddenActivationsForwardProp;
    }

    public void setUseHiddenActivationsForwardProp(boolean useHiddenActivationsForwardProp) {
        this.useHiddenActivationsForwardProp = useHiddenActivationsForwardProp;
    }

    public LossFunctions.LossFunction getLossFunction() {
        return lossFunction;
    }

    public void setLossFunction(LossFunctions.LossFunction lossFunction) {
        this.lossFunction = lossFunction;
    }

    public ActivationFunction getActivationFunction() {
        return activationFunction;
    }

    public void setActivationFunction(ActivationFunction activationFunction) {
        this.activationFunction = activationFunction;
    }

    public int getnIn() {
        return nIn;
    }

    public void setnIn(int nIn) {
        this.nIn = nIn;
    }

    public int getnOut() {
        return nOut;
    }

    public void setnOut(int nOut) {
        this.nOut = nOut;
    }

    public float getSparsity() {
        return sparsity;
    }

    public void setSparsity(float sparsity) {
        this.sparsity = sparsity;
    }

    public boolean isUseAdaGrad() {
        return useAdaGrad;
    }

    public void setUseAdaGrad(boolean useAdaGrad) {
        this.useAdaGrad = useAdaGrad;
    }

    public float getLr() {
        return lr;
    }

    public void setLr(float lr) {
        this.lr = lr;
    }

    public float getMomentum() {
        return momentum;
    }

    public void setMomentum(float momentum) {
        this.momentum = momentum;
    }

    public float getL2() {
        return l2;
    }

    public void setL2(float l2) {
        this.l2 = l2;
    }

    public boolean isUseRegularization() {
        return useRegularization;
    }

    public void setUseRegularization(boolean useRegularization) {
        this.useRegularization = useRegularization;
    }

    public Map<Integer, Float> getMomentumAfter() {
        return momentumAfter;
    }

    public void setMomentumAfter(Map<Integer, Float> momentumAfter) {
        this.momentumAfter = momentumAfter;
    }

    public int getResetAdaGradIterations() {
        return resetAdaGradIterations;
    }

    public void setResetAdaGradIterations(int resetAdaGradIterations) {
        this.resetAdaGradIterations = resetAdaGradIterations;
    }

    public float getDropOut() {
        return dropOut;
    }

    public void setDropOut(float dropOut) {
        this.dropOut = dropOut;
    }

    public boolean isApplySparsity() {
        return applySparsity;
    }

    public void setApplySparsity(boolean applySparsity) {
        this.applySparsity = applySparsity;
    }

    public WeightInit getWeightInit() {
        return weightInit;
    }

    public void setWeightInit(WeightInit weightInit) {
        this.weightInit = weightInit;
    }

    public NeuralNetwork.OptimizationAlgorithm getOptimizationAlgo() {
        return optimizationAlgo;
    }

    public void setOptimizationAlgo(NeuralNetwork.OptimizationAlgorithm optimizationAlgo) {
        this.optimizationAlgo = optimizationAlgo;
    }

    public int getRenderWeightsEveryNumEpochs() {
        return renderWeightsEveryNumEpochs;
    }

    public void setRenderWeightsEveryNumEpochs(int renderWeightsEveryNumEpochs) {
        this.renderWeightsEveryNumEpochs = renderWeightsEveryNumEpochs;
    }

    public boolean isConcatBiases() {
        return concatBiases;
    }

    public void setConcatBiases(boolean concatBiases) {
        this.concatBiases = concatBiases;
    }

    public boolean isConstrainGradientToUnitNorm() {
        return constrainGradientToUnitNorm;
    }

    public void setConstrainGradientToUnitNorm(boolean constrainGradientToUnitNorm) {
        this.constrainGradientToUnitNorm = constrainGradientToUnitNorm;
    }

    public RandomGenerator getRng() {
        return rng;
    }

    public void setRng(RandomGenerator rng) {
        this.rng = rng;
    }

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public RealDistribution getDist() {
        return dist;
    }

    public void setDist(RealDistribution dist) {
        this.dist = dist;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NeuralNetConfiguration)) return false;

        NeuralNetConfiguration that = (NeuralNetConfiguration) o;

        if (applySparsity != that.applySparsity) return false;
        if (concatBiases != that.concatBiases) return false;
        if (constrainGradientToUnitNorm != that.constrainGradientToUnitNorm) return false;
        if (Float.compare(that.dropOut, dropOut) != 0) return false;
        if (Float.compare(that.l2, l2) != 0) return false;
        if (Float.compare(that.lr, lr) != 0) return false;
        if (Float.compare(that.momentum, momentum) != 0) return false;
        if (nIn != that.nIn) return false;
        if (nOut != that.nOut) return false;
        if (renderWeightsEveryNumEpochs != that.renderWeightsEveryNumEpochs) return false;
        if (resetAdaGradIterations != that.resetAdaGradIterations) return false;
        if (seed != that.seed) return false;
        if (Float.compare(that.sparsity, sparsity) != 0) return false;
        if (useAdaGrad != that.useAdaGrad) return false;
        if (useRegularization != that.useRegularization) return false;
        if (!activationFunction.equals(that.activationFunction)) return false;
        if (dist != null ? !dist.equals(that.dist) : that.dist != null) return false;
        if (momentumAfter != null ? !momentumAfter.equals(that.momentumAfter) : that.momentumAfter != null)
            return false;
        if (optimizationAlgo != that.optimizationAlgo) return false;
        if (rng != null ? !rng.equals(that.rng) : that.rng != null) return false;
        if (weightInit != that.weightInit) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (sparsity != +0.0f ? Float.floatToIntBits(sparsity) : 0);
        result = 31 * result + (useAdaGrad ? 1 : 0);
        result = 31 * result + (lr != +0.0f ? Float.floatToIntBits(lr) : 0);
        result = 31 * result + (momentum != +0.0f ? Float.floatToIntBits(momentum) : 0);
        result = 31 * result + (l2 != +0.0f ? Float.floatToIntBits(l2) : 0);
        result = 31 * result + (useRegularization ? 1 : 0);
        result = 31 * result + (momentumAfter != null ? momentumAfter.hashCode() : 0);
        result = 31 * result + resetAdaGradIterations;
        result = 31 * result + (dropOut != +0.0f ? Float.floatToIntBits(dropOut) : 0);
        result = 31 * result + (applySparsity ? 1 : 0);
        result = 31 * result + weightInit.hashCode();
        result = 31 * result + optimizationAlgo.hashCode();
        result = 31 * result + renderWeightsEveryNumEpochs;
        result = 31 * result + (concatBiases ? 1 : 0);
        result = 31 * result + (constrainGradientToUnitNorm ? 1 : 0);
        result = 31 * result + (rng != null ? rng.hashCode() : 0);
        result = 31 * result + (dist != null ? dist.hashCode() : 0);
        result = 31 * result + (int) (seed ^ (seed >>> 32));
        result = 31 * result + nIn;
        result = 31 * result + nOut;
        result = 31 * result + activationFunction.hashCode();
        return result;
    }
}