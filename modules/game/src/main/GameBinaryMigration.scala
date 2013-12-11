package lila.game

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{ Try, Success, Failure }

import chess.Pos.posAt
import chess.{ Pos, Castles, History, Piece, AllPieces, Color, White, Black, Clock, PausedClock, RunningClock }
import org.joda.time.DateTime
import play.api.libs.iteratee._
import reactivemongo.bson._

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.ByteArray
import lila.db.Implicits._
import lila.game.Game.{ BSONFields ⇒ F }

object GameBinaryMigration {

  type Doc = BSONDocument
  type Docs = Iterator[Doc]

  val debug = lila.db.BSON.debug _
  val writer = lila.db.BSON.writer

  def apply(db: lila.db.Env, system: akka.actor.ActorSystem) = {

    val oldGameColl = db("game4")
    val oldPgnColl = db("pgn2")
    val repo = GameRepo
    // val limit = 1000
    // val limit = 600 * 1000
    val limit = 20 * 1000 * 1000
    val batchSize = math.min(limit, 50)

    def toMap(o: BSONDocument) = (o.stream collect { case Success(e) ⇒ e }).toMap
    def getAs[T](map: Map[String, BSONValue], key: String)(implicit reader: BSONReader[_ <: BSONValue, T]): Option[T] = {
      map.get(key) flatMap { e ⇒ Try(reader.asInstanceOf[BSONReader[BSONValue, T]] read e).toOption }
    }
    def getAsGet[T](map: Map[String, BSONValue], key: String)(implicit reader: BSONReader[_ <: BSONValue, T]): T = {
      Try(reader.asInstanceOf[BSONReader[BSONValue, T]] read map(key)).get
    }

    def parseLastMove(lastMove: String): Option[(Pos, Pos)] = lastMove match {
      case History.MoveString(a, b) ⇒ for (o ← posAt(a); d ← posAt(b)) yield (o, d)
      case _                        ⇒ None
    }

    def writeDocument(doc: Doc): Doc = writeDoc(doc.elements, Set.empty)
    def writeDoc(els: Stream[BSONElement], drops: Set[String]): Doc = BSONDocument(els collect {
      case (k, BSONDouble(x)) if !drops(k) ⇒ k -> BSONInteger(x.toInt)
      case (k, v) if !drops(k)             ⇒ k -> v
    })

    val gameDrop = Set("next", "c", "cc", "cs", "lm", "lmt", "p", "me", "ph", "uids", "tk", "ck")
    val playerDrop = Set("id", "u", "ps", "mts", "uid", "elo", "bs", "ed", "isOfferingDraw", "isOfferingRematch", "isProposingTakeback", "lastDrawOffer")

    def convertGame(o: Doc): (String, Doc) = {
      val gameId = o.getAs[String]("_id").get
      try {
        val d1 = toMap(o)
        val cl = CastleLastMoveTime(
          castles = getAs[String](d1, "cs").fold(Castles.all)(Castles.apply),
          lastMove = getAs[String](d1, "lm") flatMap parseLastMove,
          lastMoveTime = None,
          check = getAs[String](d1, "ck").flatMap(Pos.posAt))
        val binCL = BinaryFormat.castleLastMoveTime write cl
        val bsonCL = BSONBinary(binCL.value, Subtype.UserDefinedSubtype)
        def player(x: Int) = getAs[BSONArray](d1, "p").get.getAsTry[BSONDocument](x).get
        val p0 = player(0)
        val p1 = player(1)
        val playerIds = p0.getAsTry[String]("id").get + p1.getAsTry[String]("id").get
        val bsonPs = BinaryFormat.piece write getPieces(p0, p1)
        val bsonClock = getAs[BSONDocument](d1, "c") map getClock map BinaryFormat.clock.write
        val mts = MTS.getMovetimes(~p0.getAs[String]("mts"), ~p1.getAs[String]("mts"))
        val bsonMts = mts map BinaryFormat.moveTime.write
        val meta = getAs[BSONDocument](d1, "me")
        val playerUids = List(~p0.getAs[String]("uid"), ~p1.getAs[String]("uid"))
        gameId -> {
          writeDoc(o.elements, gameDrop) ++ BSONDocument(
            F.next -> getAs[BSONString](d1, "next"),
            F.clock -> bsonClock,
            F.castleLastMoveTime -> bsonCL,
            F.binaryPieces -> bsonPs,
            F.whitePlayer -> writer.docO(convertPlayer(p0)),
            F.blackPlayer -> writer.docO(convertPlayer(p1)),
            F.playerIds -> playerIds,
            F.playerUids -> writer.listO(playerUids),
            F.moveTimes -> bsonMts,
            F.source -> (meta flatMap (x ⇒ x.getAs[BSONNumberLike]("so")) map (_.toInt) map writer.int),
            F.pgnImport -> (meta flatMap (x ⇒ x.getAs[BSONDocument]("pgni")) map writeDocument),
            F.tournamentId -> (meta flatMap (x ⇒ x.getAs[String]("tid")) flatMap writer.strO),
            F.tvAt -> (meta flatMap (x ⇒ x.getAs[DateTime]("tv")) map writer.date)
          )
        }
      }
      catch {
        case e: Exception ⇒ {
          val msg = s"Game $gameId ${debug(o)} $e.getMessage"
          println(msg)
          throw new Exception(msg)
        }
      }
    }

    object MTS {
      private val chars: List[Char] =
        "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toList
      private val decodeHash: Map[Char, Int] = chars.zipWithIndex.toMap
      private val lastInt: Int = chars.size - 1
      private def decode(str: String): Vector[Int] = str.toVector map { mt ⇒ decodeHash get mt getOrElse lastInt }
      def getMovetimes(m0: String, m1: String): Option[Vector[Int]] = (m0.nonEmpty || m1.nonEmpty) option {
        (decode(m0) zip decode(m1) flatMap {
          case (w, b) ⇒ Vector(w * 10, b * 10)
        })
      }
    }

    def getClock(doc: Doc): Clock = try {
      val d = toMap(doc)
      val l = getAsGet[BSONNumberLike](d, "l").toInt
      val i = getAsGet[BSONNumberLike](d, "i").toInt
      val w = getAsGet[BSONNumberLike](d, "w").toFloat
      val b = getAsGet[BSONNumberLike](d, "b").toFloat
      val timerOption = getAs[BSONNumberLike](d, "t") map (_.toDouble)
      timerOption.fold(
        PausedClock(
          color = White,
          increment = i,
          limit = l,
          whiteTime = w,
          blackTime = b): Clock) { timer ⇒
          RunningClock(
            color = White,
            increment = i,
            limit = l,
            whiteTime = w,
            blackTime = b,
            timer = timer)
        }
    }
    catch {
      case e: Exception ⇒ throw new Exception(s"Clock ${debug(doc)} $e.getMessage")
    }

    def getPieces(p0: Doc, p1: Doc): AllPieces = try {

      val p0S = p0.getAsTry[String]("ps").get
      val p1S = p1.getAsTry[String]("ps").get

      import chess.Pos.piotr, chess.Role.forsyth
      def posPiece(posCode: Char, roleCode: Char, color: Color): Option[(Pos, Piece)] = for {
        pos ← piotr(posCode)
        role ← forsyth(roleCode)
      } yield (pos, Piece(color, role))

      {
        for {
          player ← List(White -> p0S, Black -> p1S)
          color = player._1
          piece ← player._2 grouped 2
        } yield (color, piece(0), piece(1))
      }.foldLeft((Map[Pos, Piece](), List[Piece]())) {
        case ((ps, ds), (color, pos, role)) ⇒ {
          if (role.isUpper) posPiece(pos, role.toLower, color) map { p ⇒ (ps, p._2 :: ds) }
          else posPiece(pos, role, color) map { p ⇒ (ps + p, ds) }
        } | (ps, ds)
      }
    }
    catch {
      case e: Exception ⇒ throw new Exception(s"Pieces ${debug(p0)} ${debug(p1)} $e.getMessage")
    }

    def convertPlayer(o: Doc): Doc = {
      val d1 = (o.stream collect { case Success(e) ⇒ e }).toMap
      val filtered = o.elements filter {
        case ("id", _) if (d1 contains "ai") ⇒ false
        case _                               ⇒ true
      }
      writeDoc(filtered, playerDrop) ++ BSONDocument(
        Player.BSONFields.elo -> getAs[BSONNumberLike](d1, "elo").map(x ⇒ writer.int(x.toInt)),
        Player.BSONFields.eloDiff -> getAs[BSONNumberLike](d1, "ed").map(x ⇒ writer.int(x.toInt)),
        Player.BSONFields.blurs -> getAs[BSONNumberLike](d1, "bs").map(x ⇒ writer.int(x.toInt))
      )
    }

    def getPgns(games: Seq[Doc]): Future[Map[String, Option[BSONBinary]]] = {
      val ids = games.map(g ⇒ g.getAsTry[String]("_id").get).toList
      oldPgnColl.find(
        BSONDocument("_id" -> BSONDocument("$in" -> ids))
      ).cursor[BSONDocument].collect[List]() map { pgns ⇒
          pgns.map(g ⇒ g.getAsTry[String]("_id").get -> g.getAs[BSONBinary]("p")).toMap
        }
    }

    def convertPrll(docs: Seq[Doc]): Future[Seq[(String, Doc)]] = Future.traverse(docs) { game ⇒
      Future { convertGame(game) } addFailureEffect {
        case e: Exception ⇒ {
          println(e)
          e.printStackTrace()
        }
      }
    }

    def convert(docs: Seq[Doc]): Future[Iterator[(String, Doc)]] = Future {
      (docs.par map convertGame).toIterator
    }

    def migrate: Funit = {

      println("---------------------------- MIGRATION")

      val docPool: Enumerator[Docs] = oldGameColl
        // .find(BSONDocument("_id" -> "dgg04imk"))
        .find(BSONDocument())
        // .sort(BSONDocument("ca" -> -1))
        // .skip(6996800)
        .batch(batchSize)
        .cursor[BSONDocument].enumerateBulks(limit)

      val docTransformer: Enumeratee[Docs, Doc] = Enumeratee mapConcat { docs ⇒
        val docSeq = docs.toSeq
        val games = getPgns(docSeq) zip convertPrll(docSeq) map {
          case (pgns, docs) ⇒ docs map {
            case (id, doc) ⇒ doc ++ BSONDocument(F.binaryPgn -> (pgns get id))
          }
        }
        (games await 5.seconds).toSeq 
      }
      // val docTransformer: Enumeratee[Docs, Doc] = Enumeratee mapConcat { docs ⇒
      //   getPgns(docs) zip convertPrll(docs) map {
      //     case (pgns, docs) ⇒ docs map {
      //       case (id, doc) ⇒ doc ++ BSONDocument(F.binaryPgn -> (pgns get id))
      //     }
      //   }
      // }

      val docSink: Iteratee[Doc, Int] = tube.gameTube.coll.bulkInsertIteratee(
        bulkSize = batchSize,
        bulkByteSize = 1024 * 1024 * 8 /* 8MB */ )

      ((docPool &> docTransformer) |>>> docSink).void
    }

    (tube.gameTube.coll.drop recover {
      case e ⇒ println(e)
    }) >> migrate
  }
}
