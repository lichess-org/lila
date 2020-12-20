package lila.evalCache

private object Validator {

  case class Error(message: String) extends AnyVal

  def apply(in: EvalCacheEntry.Input): Option[Error] =
    in.eval.pvs.toList.foldLeft(none[Error]) {
      case (None, pv) =>
        chess.Replay
          .boardsFromUci(
            pv.moves.value.toList,
            in.fen.some,
            in.id.variant
          )
          .fold(err => Error(err).some, _ => none)
      case (error, _) => error
    }
}
