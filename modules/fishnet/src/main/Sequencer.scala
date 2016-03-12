package lila.fishnet

import lila.hub.FutureSequencer

private final class Sequencer(
  val move: FutureSequencer,
  val analysis: FutureSequencer)
