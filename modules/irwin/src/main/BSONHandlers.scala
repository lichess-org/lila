package lila.irwin

import reactivemongo.api.bson.*

import lila.db.BSON
import lila.db.dsl.{ *, given }

object BSONHandlers:

  import IrwinReport.*

  private given BSON[MoveReport] with

    private val activation = "a"
    private val rank       = "r"
    private val ambiguity  = "m"
    private val odds       = "o"
    private val loss       = "l"

    def reads(r: BSON.Reader) =
      MoveReport(
        activation = r.intD(activation),
        rank = r.intO(rank),
        ambiguity = r.intD(ambiguity),
        odds = r.intD(odds),
        loss = r.intD(loss)
      )

    def writes(w: BSON.Writer, o: MoveReport) =
      BSONDocument(
        activation -> w.intO(o.activation),
        rank       -> o.rank.flatMap(w.intO),
        ambiguity  -> w.intO(o.ambiguity),
        odds       -> w.intO(o.odds),
        loss       -> w.intO(o.loss)
      )

  private given BSONDocumentHandler[GameReport] = Macros.handler
  given BSONDocumentHandler[IrwinReport]        = Macros.handler

  import KaladinUser.{ Pred, Requester, Response }
  private given BSONHandler[Requester] = quickHandler[Requester](
    {
      case BSONString("TournamentLeader") => Requester.TournamentLeader
      case BSONString("TopOnline")        => Requester.TopOnline
      case BSONString("Report")           => Requester.Report
      case BSONString(modId)              => Requester.Mod(UserId(modId))
    },
    {
      case Requester.Mod(modId) => BSONString(modId.value)
      case other                => BSONString(other.name)
    }
  )
  given BSONDocumentHandler[Pred]        = Macros.handler
  given BSONDocumentHandler[Response]    = Macros.handler
  given BSONDocumentHandler[KaladinUser] = Macros.handler
