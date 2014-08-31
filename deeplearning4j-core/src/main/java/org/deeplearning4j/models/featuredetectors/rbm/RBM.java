package org.deeplearning4j.models.featuredetectors.rbm;


import static org.deeplearning4j.linalg.ops.transforms.Transforms.*;


import org.deeplearning4j.berkeley.Pair;
import org.deeplearning4j.linalg.api.activation.Activations;
import org.deeplearning4j.linalg.api.ndarray.INDArray;
import org.deeplearning4j.linalg.factory.NDArrays;
import org.deeplearning4j.linalg.ops.transforms.Transforms;
import org.deeplearning4j.linalg.sampling.Sampling;
import org.deeplearning4j.nn.BaseNeuralNetwork;
import org.deeplearning4j.nn.api.NeuralNetwork;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.gradient.NeuralNetworkGradient;
import org.deeplearning4j.optimize.optimizers.NeuralNetworkOptimizer;
import org.deeplearning4j.plot.NeuralNetPlotter;
import org.deeplearning4j.optimize.optimizers.rbm.RBMOptimizer;
import org.deeplearning4j.util.RBMUtil;




/**
 * Restricted Boltzmann Machine.
 *
 * Markov chain with gibbs sampling.
 *
 * Supports the following visible units:
 *
 *     binary
 *     gaussian
 *     softmax
 *     linear
 *
 * Supports the following hidden units:
 *     rectified
 *     binary
 *     gaussian
 *     softmax
 *     linear
 *
 * Based on Hinton et al.'s work
 *
 * Great reference:
 * http://www.iro.umontreal.ca/~lisa/publications2/index.php/publications/show/239
 *
 *
 * @author Adam Gibson
 *
 */
@SuppressWarnings("unused")
public  class RBM extends BaseNeuralNetwork {


    public  static enum VisibleUnit {
        BINARY,GAUSSIAN,SOFTMAX,LINEAR


    }

    public  static enum HiddenUnit {
        RECTIFIED,BINARY,GAUSSIAN,SOFTMAX
    }
    /**
     *
     */
    private static final long serialVersionUID = 6189188205731511957L;
    protected NeuralNetworkOptimizer optimizer;
    protected VisibleUnit visibleType = VisibleUnit.BINARY;
    protected HiddenUnit  hiddenType = HiddenUnit.BINARY;
    protected INDArray sigma,hiddenSigma;



    protected RBM() {}

    public RBM(INDArray input, INDArray W, INDArray hbias, INDArray vbias,NeuralNetConfiguration conf) {
        super(input, W, hbias, vbias,conf);
    }



    /**
     * Contrastive divergence revolves around the idea
     * of approximating the log likelihood around x1(input) with repeated sampling.
     * Given is an energy based model: the higher k is (the more we sample the model)
     * the more we lower the energy (increase the likelihood of the model)
     *
     * and lower the likelihood (increase the energy) of the hidden samples.
     *
     * Other insights:
     *    CD - k involves keeping the first k samples of a gibbs sampling of the model.
     *
     * @param learningRate the learning rate to scale by
     * @param k the number of iterations to do
     * @param input the input to sample from
     */
    public void contrastiveDivergence(double learningRate,int k,INDArray input) {
        if(input != null)
            this.input = input;
        this.lastMiniBatchSize = input.rows();
        NeuralNetworkGradient gradient = getGradient(new Object[]{k,learningRate,-1});
        float norm = gradient.getwGradient().norm2(Integer.MAX_VALUE).get(0);
        getW().addi(gradient.getwGradient());
        gethBias().addi(gradient.gethBiasGradient());
        getvBias().addi(gradient.getvBiasGradient());

    }

    /**
     * Contrastive divergence revolves around the idea
     * of approximating the log likelihood around x1(input) with repeated sampling.
     * Given is an energy based model: the higher k is (the more we sample the model)
     * the more we lower the energy (increase the likelihood of the model)
     *
     * and lower the likelihood (increase the energy) of the hidden samples.
     *
     * Other insights:
     *    CD - k involves keeping the first k samples of a gibbs sampling of the model.
     *
     * @param learningRate the learning rate to scale by
     * @param k the number of iterations to do
     * @param input the input to sample from
     * @param iteration  the iteration to use
     */
    public void contrastiveDivergence(double learningRate,int k,INDArray input,int iteration) {
        if(input != null)
            this.input = input;
        this.lastMiniBatchSize = input.rows();
        NeuralNetworkGradient gradient = getGradient(new Object[]{k,learningRate,iteration});
        getW().addi(gradient.getwGradient());
        gethBias().addi(gradient.gethBiasGradient());
        getvBias().addi(gradient.getvBiasGradient());

    }


    @Override
    public NeuralNetworkGradient getGradient(Object[] params) {



        int k = conf.getK();
        float learningRate = conf.getLr();
        int iteration = params[params.length - 1] == null ? 0 : (int) params[params.length - 1];

        if(wAdaGrad != null)
            wAdaGrad.setMasterStepSize(learningRate);
        if(hBiasAdaGrad != null )
            hBiasAdaGrad.setMasterStepSize(learningRate);
        if(vBiasAdaGrad != null)
            vBiasAdaGrad.setMasterStepSize(learningRate);

		/*
		 * Cost and updates dictionary.
		 * This is the update rules for weights and biases
		 */

        //POSITIVE PHASE
        Pair<INDArray,INDArray> probHidden = sampleHiddenGivenVisible(input);

		/*
		 * Start the gibbs sampling.
		 */
        INDArray chainStart = probHidden.getSecond();

		/*
		 * Note that at a later date, we can explore alternative methods of 
		 * storing the chain transitions for different kinds of sampling
		 * and exploring the search space.
		 */
        Pair<Pair<INDArray,INDArray>,Pair<INDArray,INDArray>> matrices;
        //negative visible means or expected values
        INDArray nvMeans = null;
        //negative value samples
        INDArray nvSamples = null;
        //negative hidden means or expected values
        INDArray nhMeans = null;
        //negative hidden samples
        INDArray nhSamples = null;

		/*
		 * K steps of gibbs sampling. This is the positive phase of contrastive divergence.
		 * 
		 * There are 4 matrices being computed for each gibbs sampling.
		 * The samples from both the positive and negative phases and their expected values 
		 * or averages.
		 * 
		 */

        for(int i = 0; i < k; i++) {

            //NEGATIVE PHASE
            if(i == 0)
                matrices = gibbhVh(chainStart);
            else
                matrices = gibbhVh(nhSamples);

            //getFromOrigin the cost updates for sampling in the chain after k iterations
            nvMeans = matrices.getFirst().getFirst();
            nvSamples = matrices.getFirst().getSecond();
            nhMeans = matrices.getSecond().getFirst();
            nhSamples = matrices.getSecond().getSecond();
        }

		/*
		 * Update gradient parameters
		 */
        INDArray transpose = input.transpose();
        INDArray inputTimesProbHidden = transpose.mmul(probHidden.getSecond());
        INDArray samplesTimesNhMeans = nvSamples.transpose().mmul(nhMeans);
        INDArray sub = inputTimesProbHidden.sub(samplesTimesNhMeans);
        INDArray wGradient = input.transpose().mmul(probHidden.getSecond()).sub(
                nvSamples.transpose().mmul(nhMeans)
        );



        INDArray hBiasGradient;

        if(conf.getSparsity() != 0)
            //all hidden units must stay around this number
            hBiasGradient = probHidden.getSecond().rsubi(conf.getSparsity()).mean(0);
        else
            //update rule: the expected values of the hidden input - the negative hidden  means adjusted by the learning rate
            hBiasGradient = probHidden.getSecond().sub(nhMeans).mean(0);




        //update rule: the expected values of the input - the negative samples adjusted by the learning rate
        INDArray  vBiasGradient = input.sub(nvSamples).mean(0);
        NeuralNetworkGradient ret = new NeuralNetworkGradient(wGradient, vBiasGradient, hBiasGradient);

        updateGradientAccordingToParams(ret, iteration,learningRate);
        return ret;
    }

    /**
     * Fit the model to the given data
     *
     * @param data the data to fit the model to
     */
    @Override
    public void fit(INDArray data) {
        fit(data,null);
    }

    @Override
    public NeuralNetwork transpose() {
        RBM r = (RBM) super.transpose();
        HiddenUnit h = RBMUtil.inverse(visibleType);
        VisibleUnit v = RBMUtil.inverse(hiddenType);
        if(h == null)
            h = hiddenType;
        if(v == null)
            v = visibleType;

        r.setHiddenType(h);
        r.setVisibleType(v);
        r.sigma = sigma;
        r.hiddenSigma = hiddenSigma;
        return r;
    }

    @Override
    public NeuralNetwork clone() {
        RBM r = (RBM) super.clone();
        r.setHiddenType(hiddenType);
        r.setVisibleType(visibleType);
        r.sigma = sigma;
        r.hiddenSigma = hiddenSigma;
        return r;
    }

    /**
     * Free energy for an RBM
     * Lower energy models have higher probability
     * of activations
     * @param visibleSample the sample to test on
     * @return the free energy for this sample
     */
    public double freeEnergy(INDArray visibleSample) {
        INDArray wxB = visibleSample.mmul(W).addiRowVector(hBias);
        double vBiasTerm = NDArrays.getBlasWrapper().dot(visibleSample, vBias);
        double hBiasTerm = (double) log(exp(wxB).add(1)).sum(Integer.MAX_VALUE).element();
        return -hBiasTerm - vBiasTerm;
    }


    /**
     * Binomial sampling of the hidden values given visible
     * @param v the visible values
     * @return a binomial distribution containing the expected values and the samples
     */
    @Override
    public Pair<INDArray,INDArray> sampleHiddenGivenVisible(INDArray v) {
        if(hiddenType == HiddenUnit.RECTIFIED) {
            INDArray h1Mean = propUp(v);
            INDArray sigH1Mean = sigmoid(h1Mean);
		/*
		 * Rectified linear part
		 */
            INDArray sqrtSigH1Mean = sqrt(sigH1Mean);
            //NANs here with Word2Vec
            INDArray h1Sample = h1Mean.addi(Sampling.normal(conf.getRng(), h1Mean,1).mul(sqrtSigH1Mean));
            h1Sample = Transforms.max(h1Sample);
            //apply dropout
            applyDropOutIfNecessary(h1Sample);


            return new Pair<>(h1Mean,h1Sample);

        }

        else if(hiddenType == HiddenUnit.GAUSSIAN) {
            INDArray h1Mean = propUp(v);
            this.hiddenSigma = h1Mean.var(1);

            INDArray h1Sample =  h1Mean.addi(Sampling.normal(conf.getRng(),h1Mean,this.hiddenSigma));

            //apply dropout
            applyDropOutIfNecessary(h1Sample);
            return new Pair<>(h1Mean,h1Sample);
        }

        else if(hiddenType == HiddenUnit.SOFTMAX) {
            INDArray h1Mean = propUp(v);
            INDArray h1Sample = Activations.softMaxRows().apply(h1Mean);
            applyDropOutIfNecessary(h1Sample);
            return new Pair<>(h1Mean,h1Sample);
        }



        else if(hiddenType == HiddenUnit.BINARY) {
            INDArray h1Mean = propUp(v);
            INDArray h1Sample = Sampling.binomial(h1Mean, 1, conf.getRng());
            applyDropOutIfNecessary(h1Sample);
            return new Pair<>(h1Mean,h1Sample);
        }



        throw new IllegalStateException("Hidden unit type must either be rectified linear or binary");

    }

    /**
     * Gibbs sampling step: hidden ---> visible ---> hidden
     * @param h the hidden input
     * @return the expected values and samples of both the visible samples given the hidden
     * and the new hidden input and expected values
     */
    public Pair<Pair<INDArray,INDArray>,Pair<INDArray,INDArray>> gibbhVh(INDArray h) {

        Pair<INDArray,INDArray> v1MeanAndSample = sampleVisibleGivenHidden(h);
        INDArray vSample = v1MeanAndSample.getSecond();

        Pair<INDArray,INDArray> h1MeanAndSample = sampleHiddenGivenVisible(vSample);
        return new Pair<>(v1MeanAndSample,h1MeanAndSample);
    }


    /**
     * Guess the visible values given the hidden
     * @param h the hidden units
     * @return a visible mean and sample relative to the hidden states
     * passed in
     */
    @Override
    public Pair<INDArray,INDArray> sampleVisibleGivenHidden(INDArray h) {
        INDArray v1Mean = propDown(h);

        if(visibleType == VisibleUnit.GAUSSIAN) {
            INDArray v1Sample = v1Mean.add(NDArrays.randn(v1Mean.rows(),v1Mean.columns(),conf.getRng()));
            return new Pair<>(v1Mean,v1Sample);

        }

        else if(visibleType == VisibleUnit.LINEAR) {
            INDArray v1Sample = Sampling.normal(conf.getRng(),v1Mean,1);
            return new Pair<>(v1Mean,v1Sample);
        }

        else if(visibleType == VisibleUnit.SOFTMAX) {
            INDArray v1Sample = Activations.softMaxRows().apply(v1Mean);
            return new Pair<>(v1Mean,v1Sample);
        }

        else if(visibleType == VisibleUnit.BINARY) {
            INDArray v1Sample = Sampling.binomial(v1Mean, 1, conf.getRng());
            return new Pair<>(v1Mean,v1Sample);
        }


        throw new IllegalStateException("Visible type must either be binary,gaussian, softmax, or linear");

    }

    /**
     * Calculates the activation of the visible :
     * sigmoid(v * W + hbias)
     * @param v the visible layer
     * @return the approximated activations of the visible layer
     */
    public INDArray propUp(INDArray v) {
        if(visibleType == VisibleUnit.GAUSSIAN)
            this.sigma = v.var(1).divi(input.rows());

        INDArray preSig = v.mmul(W);
        if(conf.isConcatBiases())
            preSig = NDArrays.concatHorizontally(preSig,hBias);
        else
            preSig.addiRowVector(hBias);


        if(hiddenType == HiddenUnit.RECTIFIED) {
            preSig = Transforms.max(preSig);
            return preSig;
        }

        else if(hiddenType == HiddenUnit.GAUSSIAN) {
            INDArray add =  preSig.add(NDArrays.randn(preSig.rows(), preSig.columns(),conf.getRng()));
            preSig.addi(add);
            return preSig;
        }

        else if(hiddenType == HiddenUnit.BINARY) {
            return sigmoid(preSig);
        }

        else if(hiddenType == HiddenUnit.SOFTMAX)
            return Activations.softMaxRows().apply(preSig);

        throw new IllegalStateException("Hidden unit type should either be binary, gaussian, or rectified linear");

    }


    @Override
    public void iterationDone(int epoch) {
        int plotEpochs = conf.getRenderWeightsEveryNumEpochs();
        if(plotEpochs <= 0)
            return;
        if(epoch % plotEpochs == 0 || epoch == 0) {
            NeuralNetPlotter plotter = new NeuralNetPlotter();
            plotter.plotNetworkGradient(this,this.getGradient(new Object[]{1,0.001,1000}),getInput().rows());
        }
    }

    /**
     * Calculates the activation of the hidden:
     * sigmoid(h * W + vbias)
     * @param h the hidden layer
     * @return the approximated output of the hidden layer
     */
    public INDArray propDown(INDArray h) {
        INDArray vMean = h.mmul(W.transpose());
        if(conf.isConcatBiases())
            vMean = NDArrays.concatHorizontally(vMean, vBias);
        else
            vMean.addiRowVector(vBias);

        if(visibleType  == VisibleUnit.GAUSSIAN) {
            vMean.addi(Sampling.normal(conf.getRng(), vMean, 1.0));
            return vMean;
        }
        else if(visibleType == VisibleUnit.LINEAR) {
            vMean = Sampling.normal(conf.getRng(),vMean,1);
            return vMean;
        }

        else if(visibleType == VisibleUnit.BINARY) {
            return sigmoid(vMean);
        }

        else if(visibleType == VisibleUnit.SOFTMAX) {
            return Activations.softMaxRows().apply(vMean);
        }


        throw new IllegalStateException("Visible unit type should either be binary or gaussian");

    }

    /**
     * Reconstructs the visible input.
     * A reconstruction is a propdown of the reconstructed hidden input.
     * @param v the visible input
     * @return the reconstruction of the visible input
     */
    @Override
    public INDArray transform(INDArray v) {
        //reconstructed: propUp ----> hidden propDown to transform
        return propDown(propUp(v));
    }



    /**
     * Note: k is the first input in params.
     */
    @Override
    public void fit(INDArray input,
                    Object[] params) {
        if(input != null)
            this.input = Transforms.stabilize(input, 1);
        this.lastMiniBatchSize = input.rows();

        if(visibleType == VisibleUnit.GAUSSIAN) {
            this.sigma = input.var(1);
            this.sigma.divi(input.rows());
        }

        optimizer = new RBMOptimizer(this, conf.getLr(), params, conf.getOptimizationAlgo(), conf.getLossFunction());
        optimizer.train(input);
    }


    @Override
    public String toString() {
        return "RBM{" +
                "optimizer=" + optimizer +
                ", visibleType=" + visibleType +
                ", hiddenType=" + hiddenType +
                ", sigma=" + sigma +
                ", hiddenSigma=" + hiddenSigma +
                "} " + super.toString();
    }

    public VisibleUnit getVisibleType() {
        return visibleType;
    }

    public void setVisibleType(VisibleUnit visibleType) {
        this.visibleType = visibleType;
    }

    public HiddenUnit getHiddenType() {
        return hiddenType;
    }

    public void setHiddenType(HiddenUnit hiddenType) {
        this.hiddenType = hiddenType;
    }

    @Override
    public float lossFunction(Object[] params) {
        return getReConstructionCrossEntropy();
    }

    @Override
    public void iterate(INDArray input, Object[] params) {
        if(visibleType == VisibleUnit.GAUSSIAN)
            this.sigma = input.var(1).divi(input.rows());


        int k = (int) params[0];
        contrastiveDivergence(conf.getLr(), k, input);
    }

    public static class Builder extends BaseNeuralNetwork.Builder<RBM> {

        public Builder() {
            clazz =  RBM.class;
        }





        @Override
        public RBM buildEmpty() {
            return super.buildEmpty();
        }

        @Override
        public Builder withClazz(Class<? extends BaseNeuralNetwork> clazz) {
            super.withClazz(clazz);
            return this;
        }



        @Override
        public Builder withInput(INDArray input) {
            super.withInput(input);
            return this;
        }

        @Override
        public Builder asType(Class<RBM> clazz) {
            super.asType(clazz);
            return this;
        }

        @Override
        public Builder withWeights(INDArray W) {
            super.withWeights(W);
            return this;
        }

        @Override
        public Builder withVisibleBias(INDArray vBias) {
            super.withVisibleBias(vBias);
            return this;
        }

        @Override
        public Builder withHBias(INDArray hBias) {
            super.withHBias(hBias);
            return this;
        }




        public RBM build() {
            RBM ret = super.build();

            return ret;
        }

    }






}