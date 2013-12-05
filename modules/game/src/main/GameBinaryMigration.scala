package lila.game

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{ Try, Success, Failure }

import chess.Pos.posAt
import chess.{ Pos, Castles, History, Piece, AllPieces, Color, White, Black, Clock, PausedClock, RunningClock }
import org.joda.time.DateTime
import play.api.libs.iteratee._
import reactivemongo.bson._

import lila.db.Implicits._
import lila.game.Game.{ BSONFields ⇒ F }
import lila.db.BSON.BSONJodaDateTimeHandler

object GameBinaryMigration {

  type Doc = BSONDocument
  type Docs = Iterator[Doc]

  val debug = lila.db.BSON.debug _
  val writer = lila.db.BSON.writer

  def apply(db: lila.db.Env, system: akka.actor.ActorSystem) = {

    val oldGameColl = db("game4")
    val repo = GameRepo
    // val limit = 1
    val limit = 600 * 1000
    // val limit = 20 * 1000 * 1000
    val batchSize = math.min(limit, 100)

    def toMap(o: BSONDocument) = (o.stream collect { case Success(e) ⇒ e }).toMap
    def getAs[T](map: Map[String, BSONValue], key: String)(implicit reader: BSONReader[_ <: BSONValue, T]): Option[T] = {
      map.get(key) flatMap { e ⇒ Try(reader.asInstanceOf[BSONReader[BSONValue, T]] read e).toOption }
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

    val gameDrop = Set("c", "cc", "cs", "lm", "lmt", "p", "me", "ph")
    val playerDrop = Set("ps", "mts")

    def convertGame(o: Doc): Doc = {
      val gameId = o.getAs[String]("_id").get
      try {
        val d1 = toMap(o)
        val cl = CastleLastMoveTime(
          castles = getAs[String](d1, "cs").fold(Castles.all)(Castles.apply),
          lastMove = getAs[String](d1, "lm") flatMap parseLastMove,
          lastMoveTime = None)
        val binCL = BinaryFormat.castleLastMoveTime write cl
        val bsonCL = BSONBinary(binCL.value, Subtype.UserDefinedSubtype)
        def player(x: Int) = getAs[BSONArray](d1, "p").get.getAsTry[BSONDocument](x).get
        val p0 = player(0)
        val p1 = player(1)
        val bsonPs = BinaryFormat.piece write getPieces(p0, p1)
        val bsonClock = getAs[BSONDocument](d1, "c") map getClock map BinaryFormat.clock.write
        val mts = MTS.getMovetimes(~p0.getAs[String]("mts"), ~p1.getAs[String]("mts"))
        val bsonMts = mts map BinaryFormat.moveTime.write
        val meta = getAs[BSONDocument](d1, "me")
        writeDoc(o.elements, gameDrop) ++ BSONDocument(
          F.clock -> bsonClock,
          F.castleLastMoveTime -> bsonCL,
          F.binaryPieces -> bsonPs,
          F.whitePlayer -> convertPlayer(p0),
          F.blackPlayer -> convertPlayer(p1),
          F.moveTimes -> bsonMts,
          F.source -> (meta flatMap (x ⇒ x.getAs[Int]("so")) map writer.int),
          F.pgnImport -> (meta flatMap (x ⇒ x.getAs[BSONDocument]("pgni")) map writeDocument),
          F.tournamentId -> (meta flatMap (x ⇒ x.getAs[String]("tid")) flatMap writer.strO),
          F.tvAt -> (meta flatMap (x ⇒ x.getAs[DateTime]("tv")) map writer.date)
        )
      }
      catch {
        case e: Exception ⇒ throw new Exception(s"Game $gameId ${debug(o)} $e.getMessage")
      }
    }

    def checkGame(o: Doc) {
      // println(debug(o))
      // (Game.gameBSONHandler read o).pp
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
      val l = getAs[Double](d, "l").get.toInt
      val i = getAs[Double](d, "i").get.toInt
      val w = getAs[Double](d, "w").get.toFloat
      val b = getAs[Double](d, "b").get.toFloat
      val timerOption = getAs[Double](d, "t")
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
      writeDoc(filtered, playerDrop)
    }

    def convertPrll(docs: Docs): Docs = {
      Future.traverse(docs) { o ⇒
        Future { convertGame(o) ~ checkGame } addFailureEffect {
          case e: Exception ⇒ println(e)
        }
      }
    }.await(5 seconds)

    def migrate: Funit = {

      println("---------------------------- MIGRATION")

      val docsEnumerator: Enumerator[Docs] = oldGameColl
        .find(BSONDocument())
        .sort(BSONDocument("ca" -> -1))
        .batch(batchSize)
        .cursor[BSONDocument].enumerateBulks(limit)

      val docIteratee: Iteratee[Doc, Int] = tube.gameTube.coll.bulkInsertIteratee(
        bulkSize = batchSize,
        bulkByteSize = 1024 * 1024 * 8 /* 8MB */ )

      docsEnumerator |>>> Iteratee.foreach[Docs] { docs ⇒
        {
          Enumerator.enumerate(convertPrll(docs)) |>>> docIteratee
        }.await(10 seconds)
      }

    }

    (tube.gameTube.coll.drop recover {
      case e ⇒ println(e)
    }) >> migrate
  }
}
