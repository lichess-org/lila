package chess

case class MoveMetrics(
    clientLag: Option[Centis] = None,
    clientMoveTime: Option[Centis] = None
) {

  // Calculate client reported lag given the server's duration for the move.
  def reportedLag(elapsed: Centis) =
    clientMoveTime.fold(clientLag)(mt => Some(elapsed - mt))
}
