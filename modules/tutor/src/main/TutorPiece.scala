package lila.tutor

import chess.Role

import lila.analyse.AccuracyPercent
import lila.insight.{ InsightApi, InsightDimension, InsightMetric, Question }

case class TutorPiece(
    role: Role,
    accuracy: TutorBothOption[AccuracyPercent],
    awareness: TutorBothOption[GoodPercent]
):

  def mix: TutorBothOption[GoodPercent] =
    TutorBothValues.mix(accuracy.map(_.map(_.into(GoodPercent))), awareness)

case class TutorPieces(list: List[TutorPiece]):

  lazy val accuracyCompare = TutorCompare[Role, AccuracyPercent](
    InsightDimension.PieceRole,
    TutorMetric.Accuracy,
    list.map { piece => (piece.role, piece.accuracy) }
  )

  lazy val awarenessCompare = TutorCompare[Role, GoodPercent](
    InsightDimension.PieceRole,
    TutorMetric.Awareness,
    list.map { piece => (piece.role, piece.awareness) }
  )

  def compares = List(accuracyCompare, awarenessCompare)

  val highlights = TutorCompare.mixedBag(compares.flatMap(_.peerComparisons))

  def apply(role: Role) = list.find(_.role == role)

  def frequency(role: Role) =
    val total = list.flatMap(_.accuracy).map(_.mine.count).sum
    val count = ~apply(role).flatMap(_.accuracy).map(_.mine.count)
    GoodPercent(count, total)

private object TutorPieces:

  import TutorBuilder.*

  private val accuracyQuestion = Question(InsightDimension.PieceRole, InsightMetric.MeanAccuracy)
  private val awarenessQuestion = Question(InsightDimension.PieceRole, InsightMetric.Awareness)
  private val roles = InsightDimension.valuesOf(InsightDimension.PieceRole).toList

  private type RoleGet = Role => Option[Double]

  def compute(user: TutorPlayer)(using InsightApi, Executor): Fu[TutorPieces] =

    def cachedOrComputedPeerRoleGet[V](
        question: Question[Role],
        cacheGet: TutorPiece => Option[Double]
    ): Fu[RoleGet] =
      user.peerMatch
        .map:
          _.pieces.list.flatMap(p => cacheGet(p).map(p.role -> _)).toMap
        .filter(_.size == roles.size)
        .map(_.get)
        .match
          case Some(cache) => fuccess(cache)
          case None => answerPeer(question, user, peerNbGames).map(_.getValue)

    for
      myAccuracy <- answerMine(accuracyQuestion, user)
      peerAccuracyGet <- cachedOrComputedPeerRoleGet(accuracyQuestion, _.accuracy.map(_.peer.value))
      myAwareness <- answerMine(awarenessQuestion, user)
      peerAwarenessGet <- cachedOrComputedPeerRoleGet(awarenessQuestion, _.awareness.map(_.peer.value))
    yield TutorPieces:
      roles.map: role =>
        TutorPiece(
          role,
          accuracy = for
            mine <- myAccuracy.get(role)
            peer <- peerAccuracyGet(role)
          yield AccuracyPercent.from(TutorBothValues(mine, peer)),
          awareness = for
            mine <- myAwareness.get(role)
            peer <- peerAwarenessGet(role)
          yield GoodPercent.from(TutorBothValues(mine, peer))
        )
