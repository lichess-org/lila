package lila.swiss

case class SwissPairing(
    id: GameId,
    swissId: SwissId,
    round: SwissRoundNumber,
    white: UserId,
    black: UserId,
    status: SwissPairing.Status,
    isForfeit: Boolean = false
):
  def apply(c: Color)            = c.fold(white, black)
  def gameId                     = id
  def players                    = List(white, black)
  def has(userId: UserId)        = white == userId || black == userId
  def colorOf(userId: UserId)    = Color.fromWhite(white == userId)
  def opponentOf(userId: UserId) = if white == userId then black else white
  def winner: Option[UserId]     = (~status.toOption).map(apply)
  def isOngoing                  = status.isLeft
  def resultFor(userId: UserId)  = winner.map(userId.==)
  def whiteWins                  = status == Right(Some(Color.White))
  def blackWins                  = status == Right(Some(Color.Black))
  def isDraw                     = status == Right(None)
  def strResultOf(color: Color)  = status.fold(_ => "*", _.fold("1/2")(c => if c == color then "1" else "0"))
  def forfeit(userId: UserId)    = copy(status = Right(Some(!colorOf(userId))), isForfeit = true)

object SwissPairing:

  sealed trait Ongoing
  case object Ongoing extends Ongoing
  type Status = Either[Ongoing, Option[Color]]

  val ongoing: Status = Left(Ongoing)

  case class Pending(white: UserId, black: UserId)
  case class Bye(player: UserId)

  type ByeOrPending = Either[Bye, Pending]

  type PairingMap = Map[UserId, Map[SwissRoundNumber, SwissPairing]]

  case class View(pairing: SwissPairing, player: SwissPlayer.WithUser)

  object Fields:
    val id        = "_id"
    val swissId   = "s"
    val round     = "r"
    val gameId    = "g"
    val players   = "p"
    val status    = "t"
    val isForfeit = "f"
  def fields[A](f: Fields.type => A): A = f(Fields)

  def toMap(pairings: List[SwissPairing]): PairingMap =
    pairings.foldLeft[PairingMap](Map.empty): (acc, pairing) =>
      pairing.players.foldLeft(acc): (acc, player) =>
        acc.updatedWith(player): playerPairings =>
          (~playerPairings).updated(pairing.round, pairing).some
