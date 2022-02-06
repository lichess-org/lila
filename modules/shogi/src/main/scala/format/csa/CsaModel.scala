package shogi
package format
package csa

import cats.syntax.option._

import shogi.variant.Standard
import shogi.format.usi.Usi

case class Csa(
    tags: Tags,
    moves: List[NotationMove],
    initial: Initial = Initial.empty
) extends Notation {

  def withMoves(moves: List[NotationMove]) =
    copy(moves = moves)

  def withTags(tags: Tags) =
    copy(tags = tags)

  private def renderMainline(moveline: List[NotationMove], turn: Color): String =
    moveline
      .foldLeft[(List[String], Color)]((Nil, turn)) { case ((acc, curTurn), cur) =>
        ((Csa.renderNotationMove(cur, curTurn.some) :: acc), !curTurn)
      }
      ._1
      .reverse mkString "\n"

  def render: String = {
    val initStr =
      if (initial.comments.nonEmpty)
        initial.comments.map(Csa.fixComment _).mkString("")
      else ""
    val header = Csa renderHeader tags
    val setup =
      tags.sfen.getOrElse(Standard.initialSfen).toSituation(Standard).fold("")(Csa renderSituation _)
    val startColor: Color = tags.sfen.flatMap(_.color) | Sente
    val movesStr          = renderMainline(moves, startColor)
    List(
      header,
      setup,
      initStr,
      movesStr
    ).filter(_.nonEmpty).mkString("\n")
  }.trim

  override def toString = render
}

object Csa {

  def renderNotationMove(cur: NotationMove, turn: Option[Color]) = {
    val csaMove     = renderCsaMove(cur.usiWithRole, turn)
    val timeStr     = clockString(cur) | ""
    val commentsStr = cur.comments.map { text => s"\n'${fixComment(text)}" }.mkString("")
    val resultStr   = cur.result.fold("")(t => s"\n$t")
    s"$csaMove$timeStr$commentsStr$resultStr"
  }

  def renderCsaMove(usiWithRole: Usi.WithRole, turn: Option[Color]) =
    usiWithRole.usi match {
      case Usi.Drop(role, pos) =>
        s"${turn.fold("")(_.fold("+", "-"))}00${pos.numberKey}${role.csa}"
      case Usi.Move(orig, dest, prom) => {
        val finalRole = Standard.promote(usiWithRole.role).filter(_ => prom) | usiWithRole.role
        s"${turn.fold("")(_.fold("+", "-"))}${orig.numberKey}${dest.numberKey}${finalRole.csa}"
      }
    }

  def renderHeader(tags: Tags): String =
    csaHeaderTags
      .map { ct =>
        if (ct == Tag.Sente || ct == Tag.Gote) {
          tags(ct.name).fold("")(tagValue =>
            if (isValidTagValue(tagValue))
              s"N${ct.csaName}${tagValue.replace(",", ";")}"
            else
              ""
          )
        } else {
          tags(ct.name).fold("")(tagValue => {
            if (isValidTagValue(tagValue)) s"$$${ct.csaName}:${tagValue.replace(",", ";")}"
            else ""
          })
        }
      }
      .filter(_.nonEmpty)
      .mkString("\n")

  // we want only ascii tags
  private def isValidTagValue(str: String): Boolean =
    str.nonEmpty && str != "?" && str.forall(c => c >= 32 && c < 127)

  def renderSituation(sit: Situation): String = {
    val csaBoard = new scala.collection.mutable.StringBuilder(256)
    for (y <- 0 to 8) {
      csaBoard append ("P" + (y + 1))
      for (x <- 8 to 0 by -1) {
        sit.board(x, y) match {
          case None => csaBoard append " * "
          case Some(piece) =>
            csaBoard append s"${piece.csa}"
        }
      }
      if (y < 8) csaBoard append '\n'
    }
    List(
      csaBoard.toString,
      renderHand(sit.hands(Sente), "P+"),
      renderHand(sit.hands(Gote), "P-"),
      if (sit.color.gote) "-" else "+"
    ).filter(_.nonEmpty).mkString("\n")
  }

  private def renderHand(hand: Hand, prefix: String): String = {
    if (hand.isEmpty) ""
    else
      Standard.handRoles
        .map { r =>
          val cnt = hand(r)
          s"00${r.csa}".repeat(math.min(cnt, 81))
        }
        .filter(_.nonEmpty)
        .mkString(prefix, "", "")
  }

  def createTerminationMove(
      status: Status,
      winnerTurn: Boolean,
      winnerColor: Option[Color]
  ): Option[String] = {
    import Status._
    status match {
      case Aborted | NoStart                                                         => "%CHUDAN".some
      case Timeout | Outoftime                                                       => "%TIME_UP".some
      case Resign if !winnerTurn                                                     => "%TORYO".some
      case PerpetualCheck if winnerColor.contains(Sente)                             => "%-ILLEGAL_ACTION".some
      case PerpetualCheck                                                            => "%+ILLEGAL_ACTION".some
      case Mate if winnerTurn && winnerColor.contains(Sente)                         => "%-ILLEGAL_ACTION".some // pawn checkmate
      case Mate if winnerTurn                                                        => "%+ILLEGAL_ACTION".some // pawn checkmate
      case Mate | Stalemate                                                          => "%TSUMI".some
      case Draw                                                                      => "%SENNICHITE".some
      case Impasse27                                                                 => "%KACHI".some
      case Created | Started | UnknownFinish | VariantEnd | TryRule | Cheat | Resign => None
      case _                                                                         => None
    }
  }

  // tags we render in header
  private val csaHeaderTags = List(
    Tag.Sente,
    Tag.Gote,
    Tag.Event,
    Tag.Site,
    Tag.Start,
    Tag.End,
    Tag.TimeControl,
    Tag.SenteTeam,
    Tag.GoteTeam,
    Tag.Opening
  )

  private def clockString(cur: NotationMove): Option[String] =
    cur.secondsSpent.map(spent => s",T$spent")

  private val noDoubleLineBreakRegex = "(\r?\n){2,}".r

  private def fixComment(txt: String) =
    noDoubleLineBreakRegex.replaceAllIn(txt, "\n").replace("\n", "\n'")

}
