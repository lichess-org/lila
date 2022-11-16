package org.goochjs.glicko2;

interface Result {

	public double getScore(Rating player);

	public Rating getOpponent(Rating player);

	public boolean participated(Rating player);

	public java.util.List<Rating> getPlayers();
}
