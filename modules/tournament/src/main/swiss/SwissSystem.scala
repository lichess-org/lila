package lila.tournament
package swiss

import org.joda.time.DateTime
import org.joda.time.Duration

import lila.tournament.{ Score => AbstractScore }
import lila.game.{ Game, GameRepo }

import scala.concurrent.Future
import scala.util.Try

import scalaz.NonEmptyList

object SwissSystem extends PairingSystem with ScoringSystem {
  private val MinTimeBetweenRounds = Duration.standardSeconds(10L)

  sealed abstract class Score(val value: Int, val repr: String) extends AbstractScore
  case object Win     extends Score(2, "1")
  case object Loss    extends Score(0, "0")
  case object Draw    extends Score(1, "½")
  case object Byed    extends Score(2, "B")
  case object Absent  extends Score(0, "—")
  case object Ongoing extends Score(0, "*")

  case class Sheet(scores: List[Score], total: Int, neustadtl: Int) extends ScoreSheet {
    private def f(d: Int): String = d match {
      case 0 => ""
      case 1 => "¼"
      case 2 => "½"
      case 3 => "¾"
    }
    lazy val totalRepr: String = (total/2) + f(2*(total%2))
    lazy val neustadtlRepr: String = (neustadtl/4) + f(neustadtl%4)

    def compare(other: Sheet): Int = {
      if(total > other.total) 1
      else if(total < other.total) -1
      else if(neustadtl > other.neustadtl) 1
      else if(neustadtl < other.neustadtl) -1
      else 0
    }
  }
  private val BlankSheet = Sheet(Nil, 0, 0)

  private type STour = swisssystem.Tournament[String]
  private type Sheets = Map[String,Sheet]

  private object SheetOrdering extends Ordering[Sheet] {
    def compare(s1: Sheet, s2: Sheet): Int = s1 compare s2
  }

  // I feel like this must exist in the stdlib somewhere...
  // Maybe in scalaz? It's like String.split, but for lists...
  private def split[T](l: List[T], p: T=>Boolean): List[List[T]] = {
    def s(l: List[T], p: T=>Boolean): NonEmptyList[List[T]] = l match {
      case Nil => NonEmptyList(Nil, Nil)
      case x :: xs if p(x) => NonEmptyList(Nil, s(xs, p).list: _*)
      case x :: xs =>
        val rec = s(xs, p)
        val (r, rs) = (rec.head, rec.tail)
        NonEmptyList(x :: r, rs: _*)
    }
    s(l,p).list.filterNot(_.isEmpty)
  }

  override def scoreSheets(tour: Tournament): Map[String,Sheet] = {
    fromHistory(tour)._2
  }

  override def rank(tour: Tournament, players: Players): RankedPlayers = {
    val ss = scoreSheets(tour)

    def r(ps: Players) = {
      val withSheets = ps map { p =>
        (p, ss.getOrElse(p.id, BlankSheet))
      }
      val sorted = withSheets.sortBy(_._2)(SheetOrdering).reverse

      // yeurk.
      val ranked = sorted.foldLeft[(RankedPlayers,Sheet)]((Nil,BlankSheet)) {
        case ((Nil,_),(p,s))                               => ((1, p) :: Nil, s)
        case ((l@(r0::_),s0), (p,s)) if s0.compare(s) == 0 => ((r0._1, p) :: l, s0)
        case ((l,_),(p, s))                                => ((l.size + 1, p) :: l, s)
      }
      ranked._1.reverse
    }

    val (active,inactive) = players.partition(_.active)
    r(active) ::: r(inactive)
  }

  override def createPairings(tour: Tournament, users: List[String]): Future[(Pairings,Events)] = {
    val failed       = (Nil,Nil)
    val failedFuture = Future.successful(failed) // Oh the irony.

    // Notice how this doesn't use users: we get to pair players who haven't returned to the lobby yet.
    val toPair = tour.activePlayers.map(_.id).toSet

    if(toPair.size < 2) {
      failedFuture
    } else if(tour.pairings.exists(_.playing)) {
      // Can't pair if games are still going on.
      failedFuture
    } else {
      val now = DateTime.now

      val pairingTimes: Option[NonEmptyList[DateTime]] = tour.pairings.flatMap(_.pairedAt).toNel

      val lastRoundGames: Future[List[Game]] = pairingTimes.fold(Future.successful(Nil:List[Game])) { pts =>
        val mostRecentPairingTime: DateTime = pts.maxBy(_.getMillis) // safe !
        val lastRoundGameIds: List[String] = tour.pairings.collect {
          case p if p.pairedAt == Some(mostRecentPairingTime) => p.gameId
        }
        GameRepo.games(lastRoundGameIds)
      }

      lastRoundGames map { games =>
        val updateTimes: List[DateTime] = games.flatMap(_.updatedAt)
        if(updateTimes.exists(t => t.plus(MinTimeBetweenRounds).isAfter(now))) {
          // Too soon!
          failed
        } else {
          val (tt, _) = fromHistory(tour)

          tt flatMap { t =>
            t.pairings(toPair) map { p =>
              val ps = p.pairs.map {
                case (p1,p2) => Pairing(p1,p2,now)
              }
              val roundEnd = RoundEnd(now.plusMillis(1))
              val events = p.unpaired map { u =>
                Bye(u, now) :: roundEnd :: Nil
              } getOrElse {
                roundEnd :: Nil
              }
              (ps,events)
            }
          } getOrElse {
            failed
          }
        }
      }
    }
  }

  // Make sure you don't run that too often. For example, use scoreSheets rather than scoreSheet...
  private def fromHistory(tour: Tournament): (Try[STour],Sheets) = {
    val players: Map[String,Int] = tour.players.map(p => (p.id -> p.rating)).toMap

    val history = tour.pairingsAndEvents

    val historyByRound = split[Either[Pairing,Event]](history, _ match {
      case Right(RoundEnd(_)) => true
      case _ => false
    })

    val listsByRound: List[(Pairings,Events)] = historyByRound.map { r =>
      val (ls,rs) = r.partition(_.isLeft)
      (ls.map(_.left.get), rs.map(_.right.get))
    }

    val pairingsByRound: List[List[Pairing]] = listsByRound.map(_._1)
    val eventsByRound: List[List[Event]] = listsByRound.map(_._2)

    // Creating the swisssystem instance...
    val t = Try(swisssystem.Tournament.create(players))
    val t2 = pairingsByRound.flatten.filter(_.finished).foldLeft(t) { (tt, p) =>
      tt flatMap { t =>
        val p1 = p.user1
        val p2 = p.user2
        val (v1,v2) = if(p.draw) (1,1) else if(p.wonBy(p1)) (2,0) else (0,2)
        t.withResult(p1, v1, p2, v2)
      }
    }
    val t3 = eventsByRound.flatten.collect { case Bye(u, _) => u }.foldLeft(t2) { (tt, p) =>
      tt flatMap { t => t.withBye(p, 2) }
    }

    val ss: Map[String,Sheet] = players.keySet.toList map { player =>
      val scores = listsByRound.map { r =>
        r._1 find(_.contains(player)) map { p =>
          if(p.playing) Ongoing
          else if(p.wonBy(player)) Win
          else if(p.draw) Draw else Loss
        } getOrElse {
          if(r._2.exists(e => e match {
            case Bye(u, _) if u == player => true
            case _ => false
          })) Byed else Absent
        }
      } reverse // reverse is for compatibility... the view reverses it again.

      val total     = scores.map(_.value).sum
      val neustadtl = t3.map(_.performances.getOrElse(player, 0)).getOrElse(0)
      val sheet     = Sheet(scores, total, neustadtl)

      (player -> sheet)
    } toMap

    (t3, ss)
  }
}

