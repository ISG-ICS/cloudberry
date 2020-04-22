package util;

public class Randomizer {

    static double randomizationSeed = 0;

    public static double customRandom() {
        double x = Math.sin(randomizationSeed * Math.random()) * 10000;
        return x - Math.floor(x);
    }

    /**
     * a random number with normal distribution
     */
    public static double randomNorm(double mean, double stdDev) {
        return mean + (((customRandom() + customRandom() +
                customRandom() + customRandom() +
                customRandom() + customRandom()) - 3.0) / 3.0) * stdDev;
    }

    /**
     * randomize a pin coordinate for a tweet according to the bounding box (normally distributed within the bounding box) when the actual coordinate is not availalble.
     *     // by using the tweet id as the seed, the same tweet will always be randomized to the same coordinate.
     */
    public static double rangeRandom(double seed, double min, double max) {
        randomizationSeed = seed;
        return randomNorm((min + max) / 2.0, (max - min) / 16.0);
    };
}
