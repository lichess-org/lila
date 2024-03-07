package lila.study

import shogi.format.usi.{ Usi, UsiCharPair }

import lila.game.Game

object GameToRoot {

  def apply(
      game: Game,
      withClocks: Boolean
  ): Node.Root = {
    val clocks = withClocks ?? (
      for {
        initTime <- game.clock.map(_.config.initTime)
        times    <- game.bothClockStates
      } yield (initTime +: times)
    )

    apply(
      Node.GameMainline(
        id = game.id,
        part = 0,
        variant = game.variant,
        usis = game.usis,
        initialSfen = game.initialSfen,
        clocks = clocks
      )
    )
  }

  def apply(
      gm: Node.GameMainline
  ): Node.Root = {
    val usis = gm.usis.take(Node.MAX_PLIES)
    shogi.Replay.gamesWhileValid(usis, gm.initialSfen, gm.variant) match {
      case (gamesWithInit, error) =>
        error foreach logShogiError(gm.id)
        val init  = gamesWithInit.head
        val games = gamesWithInit.tail
        val root = Node.Root(
          ply = init.plies,
          sfen = init.toSfen,
          check = init.situation.check,
          clock = gm.clocks.flatMap(_.headOption),
          gameMainline = gm.some,
          children = Node.emptyChildren
        )
        def makeNode(g: shogi.Game, usi: Usi) =
          Node(
            id = UsiCharPair(usi, g.variant),
            ply = g.plies,
            usi = usi,
            sfen = g.toSfen,
            check = g.situation.check,
            clock = gm.clocks flatMap (_ lift (g.plies - init.plies)),
            forceVariation = false,
            children = Node.emptyChildren
          )

        games.zip(usis).reverse match {
          case Nil => root
          case (g, m) :: rest =>
            root addChild rest.foldLeft(makeNode(g, m)) { case (node, (g, m)) =>
              makeNode(g, m) addChild node
            }
        }
    }
  }

  private val logShogiError = (id: String) =>
    (err: String) =>
      logger.warn(s"study.GameToRoot https://lishogi.org/$id ${err.linesIterator.toList.headOption}")

}
