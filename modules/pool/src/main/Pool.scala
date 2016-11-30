package lila.pool

final class Pool(
    seconds: Int,
    increment: Int) {

  val clock = chess.Clock(seconds, increment)
}
