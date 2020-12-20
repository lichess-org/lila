/*
 * Copyright (C) 2013 Jeremy Gooch <http://www.linkedin.com/in/jeremygooch/>
 *
 * The licence covering the contents of this file is described in the file LICENCE.txt,
 * which should have been included as part of the distribution containing this file.
 */
package org.goochjs.glicko2;

import org.joda.time.DateTime;

/**
 * Holds an individual's Glicko-2 rating.
 *
 * <p>Glicko-2 ratings are an average skill value, a standard deviation and a volatility (how consistent the player is).
 * Prof Glickman's paper on the algorithm allows scaling of these values to be more directly comparable with existing rating
 * systems such as Elo or USCF's derivation thereof. This implementation outputs ratings at this larger scale.</p>
 *
 * @author Jeremy Gooch
 */
public class Rating {

	private double rating;
	private double ratingDeviation;
	private double volatility;
	private int numberOfResults; // the number of results from which the rating has been calculated
	private final DateTime lastRatingPeriodEndDate;

	 // the following variables are used to hold values temporarily whilst running calculations
	private double workingRating;
	private double workingRatingDeviation;
	private double workingVolatility;

	public Rating(double initRating, double initRatingDeviation, double initVolatility, int nbResults) {
		this(initRating, initRatingDeviation, initVolatility, nbResults, null);
	}

	public Rating(double initRating, double initRatingDeviation, double initVolatility, int nbResults, DateTime lastRatingPeriodEndDate) {
		this.rating = initRating;
		this.ratingDeviation = initRatingDeviation;
		this.volatility = initVolatility;
		this.numberOfResults = nbResults;
		this.lastRatingPeriodEndDate = lastRatingPeriodEndDate;
	}

	/**
	 * Return the average skill value of the player.
	 *
	 * @return double
	 */
	public double getRating() {
		return this.rating;
	}

	public void setRating(double rating) {
		this.rating = rating;
	}

	/**
	 * Return the average skill value of the player scaled down
	 * to the scale used by the algorithm's internal workings.
	 *
	 * @return double
	 */
	public double getGlicko2Rating() {
		return RatingCalculator.convertRatingToGlicko2Scale(this.rating);
	}

	/**
	 * Set the average skill value, taking in a value in Glicko2 scale.
	 *
	 * @param double
	 */
	public void setGlicko2Rating(double rating) {
		this.rating = RatingCalculator.convertRatingToOriginalGlickoScale(rating);
	}

	public double getVolatility() {
		return volatility;
	}

	public void setVolatility(double volatility) {
		this.volatility = volatility;
	}

	public double getRatingDeviation() {
		return ratingDeviation;
	}

	public void setRatingDeviation(double ratingDeviation) {
		this.ratingDeviation = ratingDeviation;
	}

	public DateTime getLastRatingPeriodEndDate() {
		return lastRatingPeriodEndDate;
	}

	/**
	 * Return the rating deviation of the player scaled down
	 * to the scale used by the algorithm's internal workings.
	 *
	 * @return double
	 */
	public double getGlicko2RatingDeviation() {
		return RatingCalculator.convertRatingDeviationToGlicko2Scale( ratingDeviation );
	}

	/**
	 * Set the rating deviation, taking in a value in Glicko2 scale.
	 *
	 * @param double
	 */
	public void setGlicko2RatingDeviation(double ratingDeviation) {
		this.ratingDeviation = RatingCalculator.convertRatingDeviationToOriginalGlickoScale( ratingDeviation );
	}

	/**
	 * Used by the calculation engine, to move interim calculations into their "proper" places.
	 *
	 */
	public void finaliseRating() {
		this.setGlicko2Rating(workingRating);
		this.setGlicko2RatingDeviation(workingRatingDeviation);
		this.setVolatility(workingVolatility);

		this.setWorkingRatingDeviation(0);
		this.setWorkingRating(0);
		this.setWorkingVolatility(0);
	}

	/**
	 * Returns a formatted rating for inspection
	 *
	 * @return {ratingDeviation} / {volatility} / {numberOfResults}
	 */
	@Override
	public String toString() {
		return rating + " / " +
				ratingDeviation + " / " +
				volatility + " / " +
				numberOfResults;
	}

	public int getNumberOfResults() {
		return numberOfResults;
	}

	public void incrementNumberOfResults(int increment) {
		this.numberOfResults = numberOfResults + increment;
	}

	public void setWorkingVolatility(double workingVolatility) {
		this.workingVolatility = workingVolatility;
	}

	public void setWorkingRating(double workingRating) {
		this.workingRating = workingRating;
	}

	public void setWorkingRatingDeviation(double workingRatingDeviation) {
		this.workingRatingDeviation = workingRatingDeviation;
	}
}
