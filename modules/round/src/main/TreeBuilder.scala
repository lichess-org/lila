package lila.round

import shogi.Centis
import shogi.format.Glyphs
import shogi.format.forsyth.Sfen
import shogi.format.usi.{ UciToUsi, Usi, UsiCharPair }
import shogi.variant.Variant
import JsonView.WithFlags
import lila.analyse.{ Advice, Analysis, Info }
import lila.tree._

object TreeBuilder {

  private type Ply = Int

  private def makeEval(info: Info) =
    Eval(
      cp = info.cp,
      mate = info.mate,
      best = info.best
    )

  def apply(
      game: lila.game.Game,
      analysis: Option[Analysis],
      withFlags: WithFlags
  ): Root = {
    val withClocks: Option[Vector[Centis]] = withFlags.clocks ?? game.bothClockStates
    shogi.Replay.gamesWhileValid(game.usis, game.initialSfen, game.variant) match {
      case (gamesWithInit, error) =>
        error foreach logShogiError(game.id)
        val init                = gamesWithInit.head
        val games               = gamesWithInit.tail
        val sfen                = init.toSfen
        val infos: Vector[Info] = analysis.??(_.infos.toVector)
        val advices: Map[Ply, Advice] = analysis.??(
          _.advices.view
            .map { a =>
              a.ply -> a
            }
            .toMap
        )
        val root = Root(
          ply = init.plies,
          sfen = sfen,
          check = init.situation.check,
          clock = withFlags.clocks ?? game.clock.map { c =>
            Centis.ofSeconds(c.limitSeconds)
          },
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
              withAnalysisChild(game.id, branch, game.variant, fromGame.toSfen)(adv.info)
            }
          } getOrElse branch
        }
        games.zip(game.usis).zipWithIndex.reverse match {
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
      fromSfen: Sfen
  )(info: Info): Branch = {
    def makeBranch(g: shogi.Game, usi: Usi) = {
      val sfen = g.toSfen
      Branch(
        id = UsiCharPair(usi, g.variant),
        ply = g.plies,
        usi = usi,
        sfen = sfen,
        check = g.situation.check,
        eval = none
      )
    }
    val variation = info.variation take 20
    val usis      = ~(Usi.readList(variation).orElse(UciToUsi.readList(variation)))
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
