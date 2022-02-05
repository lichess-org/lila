package lila.round

import shogi.Centis
import shogi.format.Glyphs
import shogi.format.forsyth.Sfen
import shogi.format.usi.{ Usi, UsiCharPair }
import shogi.opening._
import shogi.variant.Variant
import JsonView.WithFlags
import lila.analyse.{ Advice, Analysis, Info }
import lila.tree._

object TreeBuilder {

  private type Ply       = Int
  private type OpeningOf = Sfen => Option[FullOpening]

  private def makeEval(info: Info) =
    Eval(
      cp = info.cp,
      mate = info.mate,
      best = info.best
    )

  def apply(
      game: lila.game.Game,
      analysis: Option[Analysis],
      initialSfen: Sfen,
      withFlags: WithFlags
  ): Root =
    apply(
      id = game.id,
      usiMoves = game.usiMoves,
      variant = game.variant,
      analysis = analysis,
      initialSfen = initialSfen,
      withFlags = withFlags,
      clocks = withFlags.clocks ?? game.bothClockStates
    )

  def apply(
      id: String,
      usiMoves: Vector[Usi],
      variant: Variant,
      analysis: Option[Analysis],
      initialSfen: Sfen,
      withFlags: WithFlags,
      clocks: Option[Vector[Centis]]
  ): Root = {
    val withClocks: Option[Vector[Centis]] = withFlags.clocks ?? clocks
    shogi.Replay.gamesWhileValid(usiMoves, initialSfen.some, variant) match {
      case (gamesWithInit, error) =>
        error foreach logShogiError(id)
        val init  = gamesWithInit.head
        val games = gamesWithInit.tail
        val openingOf: OpeningOf =
          if (withFlags.opening && Variant.openingSensibleVariants(variant)) FullOpeningDB.findBySfen
          else _ => None
        val sfen                = init.toSfen
        val infos: Vector[Info] = analysis.??(_.infos.toVector)
        val advices: Map[Ply, Advice] = analysis.??(_.advices.view.map { a =>
          a.ply -> a
        }.toMap)
        val root = Root(
          ply = init.plies,
          sfen = sfen,
          check = init.situation.check,
          opening = openingOf(sfen),
          clock = withClocks.flatMap(_.headOption),
          eval = infos lift 0 map makeEval
        )
        def makeBranch(index: Int, g: shogi.Game, usi: Usi) = {
          val sfen   = g.toSfen
          val info   = infos lift (index - 1)
          val advice = advices get g.plies
          val branch = Branch(
            id = UsiCharPair(usi, g.variant),
            ply = g.plies,
            usi = usi,
            sfen = sfen,
            check = g.situation.check,
            opening = openingOf(sfen),
            clock = withClocks flatMap (_ lift (g.plies - init.plies - 1)),
            eval = info map makeEval,
            glyphs = Glyphs.fromList(advice.map(_.judgment.glyph).toList),
            comments = Node.Comments {
              advice.map(_.makeComment(false, true)).toList.map { text =>
                Node.Comment(
                  Node.Comment.Id.make,
                  Node.Comment.Text(text),
                  Node.Comment.Author.Lishogi
                )
              }
            }
          )
          advices.get(g.plies + 1).flatMap { adv =>
            games.lift(index - 1).map { fromGame =>
              withAnalysisChild(id, branch, variant, fromGame.toSfen, openingOf)(adv.info)
            }
          } getOrElse branch
        }
        games.zip(usiMoves).zipWithIndex.reverse match {
          case Nil => root
          case ((g, m), i) :: rest =>
            root prependChild rest.foldLeft(makeBranch(i + 1, g, m)) { case (node, ((g, m), i)) =>
              makeBranch(i + 1, g, m) prependChild node
            }
        }
    }
  }

  private def withAnalysisChild(
      id: String,
      root: Branch,
      variant: Variant,
      fromSfen: Sfen,
      openingOf: OpeningOf
  )(info: Info): Branch = {
    def makeBranch(g: shogi.Game, usi: Usi) = {
      val sfen = g.toSfen
      Branch(
        id = UsiCharPair(usi, g.variant),
        ply = g.plies,
        usi = usi,
        sfen = sfen,
        check = g.situation.check,
        opening = openingOf(sfen),
        eval = none
      )
    }
    val usis = ~Usi.readList(info.variation take 20)
    shogi.Replay.gamesWhileValid(usis, fromSfen.some, variant) match {
      case (games, error) =>
        error foreach logShogiError(id)
        games.tail.zip(usis).reverse match {
          case Nil => root
          case (g, m) :: rest =>
            root addChild rest
              .foldLeft(makeBranch(g, m)) { case (node, (g, m)) =>
                makeBranch(g, m) addChild node
              }
              .setComp
        }
    }
  }

  private val logShogiError = (id: String) =>
    (err: String) =>
      logger.warn(s"round.TreeBuilder https://lishogi.org/$id ${err.linesIterator.toList.headOption}")
}
