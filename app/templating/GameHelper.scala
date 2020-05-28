package lidraughts.app
package templating

import draughts.format.Forsyth
import draughts.{ Status => S, Color, Clock, Mode }
import controllers.routes

import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.game.{ Game, Player, Namer, Pov }
import lidraughts.i18n.{ I18nKeys => trans, enLang }
import lidraughts.user.{ User, UserContext, Title }

trait GameHelper { self: I18nHelper with UserHelper with AiHelper with StringHelper with DraughtsgroundHelper =>

  private val dataLive = attr("data-live")
  private val dataLastmove = attr("data-lastmove")
  private val dataResult = attr("data-result")

  def netBaseUrl: String
  def cdnUrl(path: String): String

  def povOpenGraph(pov: Pov) = lidraughts.app.ui.OpenGraph(
    image = cdnUrl(routes.Export.png(pov.gameId).url).some,
    title = titleGame(pov.game),
    url = s"$netBaseUrl${routes.Round.watcher(pov.gameId, pov.color.name).url}",
    description = describePov(pov)
  )

  def titleGame(g: Game) = {
    val speed = draughts.Speed(g.clock.map(_.config)).name
    val variant = g.variant.exotic ?? s" ${g.variant.name}"
    s"$speed$variant Draughts • ${playerText(g.whitePlayer)} vs ${playerText(g.blackPlayer)}"
  }

  def describePov(pov: Pov) = {
    import pov._
    val p1 = playerText(player, withRating = true)
    val p2 = playerText(opponent, withRating = true)
    val speedAndClock =
      if (game.imported) "imported"
      else game.clock.fold(draughts.Speed.Correspondence.name) { c =>
        s"${draughts.Speed(c.config).name} (${c.config.show})"
      }
    val mode = game.mode.name
    val variant = if (game.variant == draughts.variant.FromPosition) "position setup draughts"
    else if (game.variant.exotic) game.variant.name else "draughts"
    import draughts.Status._
    val result = (game.winner, game.loser, game.status) match {
      case (Some(w), _, Mate) => s"${playerText(w)} won"
      case (_, Some(l), Resign | Timeout | Cheat | NoStart) => s"${playerText(l)} resigned"
      case (_, Some(l), Outoftime) => s"${playerText(l)} forfeits by time"
      case (Some(w), _, UnknownFinish) => s"${playerText(w)} won"
      case (_, _, Draw | Stalemate | UnknownFinish) => "Game is a draw"
      case (_, _, Aborted) => "Game has been aborted"
      case (_, _, VariantEnd) => game.variant match {
        case draughts.variant.Frisian => "Capture horizontally and vertically"
        case draughts.variant.Frysk => "Frisian draughts starting with 5 pieces"
        case draughts.variant.Antidraughts => "Lose all your pieces or run out of moves"
        case draughts.variant.Breakthrough => "Promote to a king to win"
        case _ => "Variant ending"
      }
      case _ => "Game is still being played"
    }
    val moves = s"${game.draughts.fullMoveNumber} moves"
    s"$p1 plays $p2 in a $mode $speedAndClock game of $variant. $result after $moves. Click to replay, analyse, and discuss the game!"
  }

  def variantName(variant: draughts.variant.Variant)(implicit ctx: UserContext) = variant match {
    case draughts.variant.Standard => trans.standard.txt()
    case draughts.variant.FromPosition => trans.fromPosition.txt()
    case v => v.name
  }

  def variantNameNoCtx(variant: draughts.variant.Variant) = variant match {
    case draughts.variant.Standard => trans.standard.literalTxtTo(enLang)
    case draughts.variant.FromPosition => trans.fromPosition.literalTxtTo(enLang)
    case v => v.name
  }

  def shortClockName(clock: Option[Clock.Config])(implicit ctx: UserContext): Frag =
    clock.fold[Frag](trans.unlimited())(shortClockName)

  def shortClockName(clock: Clock.Config): Frag = raw(clock.show)

  def modeName(mode: Mode)(implicit ctx: UserContext): String = mode match {
    case Mode.Casual => trans.casual.txt()
    case Mode.Rated => trans.rated.txt()
  }

  def modeNameNoCtx(mode: Mode): String = mode match {
    case Mode.Casual => trans.casual.literalTxtTo(enLang)
    case Mode.Rated => trans.rated.literalTxtTo(enLang)
  }

  def playerUsername(player: Player, withRating: Boolean = true, withTitle: Boolean = true): Frag =
    player.aiLevel.fold[Frag](
      player.userId.flatMap(lightUser).fold[Frag](lidraughts.user.User.anonymous) { user =>
        val title = user.title ifTrue withTitle map { t =>
          frag(
            span(
              cls := "title",
              (Title(t) == Title.BOT) option dataBotAttr,
              st.title := Title titleName Title(t)
            )(t),
            " "
          )
        }
        if (withRating) frag(title, user.name, " ", "(", lidraughts.game.Namer ratingString player, ")")
        else frag(title, user.name)
      }
    ) { level => raw(s"A.I. level $level") }

  def playerText(player: Player, withRating: Boolean = false) =
    Namer.playerText(player, withRating)(lightUser)

  def gameVsText(game: Game, withRatings: Boolean = false): String =
    Namer.gameVsText(game, withRatings)(lightUser)

  val berserkIconSpan = iconTag("`")
  val statusIconSpan = i(cls := "status")

  def playerLink(
    player: Player,
    cssClass: Option[String] = None,
    withOnline: Boolean = true,
    withRating: Boolean = true,
    withDiff: Boolean = true,
    engine: Boolean = false,
    withStatus: Boolean = false,
    withBerserk: Boolean = false,
    mod: Boolean = false,
    link: Boolean = true
  )(implicit ctx: UserContext): Frag = {
    val statusIcon =
      if (withStatus) statusIconSpan.some
      else if (withBerserk && player.berserk) berserkIconSpan.some
      else none
    player.userId.flatMap(lightUser) match {
      case None =>
        val klass = cssClass.??(" " + _)
        span(cls := s"user-link$klass")(
          (player.aiLevel, player.name) match {
            case (Some(level), _) => aiNameFrag(level, withRating)
            case (_, Some(name)) => name
            case _ => User.anonymous
          },
          statusIcon
        )
      case Some(user) => frag(
        (if (link) a else span)(
          cls := userClass(user.id, cssClass, withOnline),
          href := s"${routes.User show user.name}${if (mod) "?mod" else ""}"
        )(
          withOnline option frag(lineIcon(user), " "),
          playerUsername(player, withRating),
          (player.ratingDiff ifTrue withDiff) map { d => frag(" ", showRatingDiff(d)) },
          engine option span(cls := "engine_mark", title := trans.thisPlayerUsesDraughtsComputerAssistance.txt())
        ),
        statusIcon
      )
    }
  }

  def gameEndStatus(game: Game)(implicit ctx: UserContext): String = game.status match {
    case S.Aborted => trans.gameAborted.txt()
    case S.Mate => ""
    case S.Resign => game.loser match {
      case Some(p) if p.color.white => trans.whiteResigned.txt()
      case _ => trans.blackResigned.txt()
    }
    case S.UnknownFinish => trans.finished.txt()
    case S.Stalemate => trans.stalemate.txt()
    case S.Timeout => game.loser match {
      case Some(p) if p.color.white => trans.whiteLeftTheGame.txt()
      case Some(_) => trans.blackLeftTheGame.txt()
      case None => trans.draw.txt()
    }
    case S.Draw => trans.draw.txt()
    case S.Outoftime => trans.timeOut.txt()
    case S.NoStart => {
      val color = game.loser.fold(Color.white)(_.color).name.capitalize
      s"$color didn't move"
    }
    case S.Cheat => "Cheat detected"
    case S.VariantEnd => game.variant match {
      case draughts.variant.Breakthrough => trans.promotion.txt()
      case _ => trans.variantEnding.txt()
    }
    case _ => ""
  }

  private def gameTitle(game: Game, color: Color): String = {
    val u1 = playerText(game player color, withRating = true)
    val u2 = playerText(game opponent color, withRating = true)
    val clock = game.clock ?? { c => " • " + c.config.show }
    val variant = game.variant.exotic ?? s" • ${game.variant.name}"
    s"$u1 vs $u2$clock$variant"
  }

  def gameResult(game: Game) =
    if (game.finished) draughts.Color.showResult(game.winnerColor, lidraughts.pref.Pref.default.draughtsResult)
    else "*"

  def gameLink(
    game: Game,
    color: Color,
    ownerLink: Boolean = false,
    tv: Boolean = false
  )(implicit ctx: UserContext): String = {
    val owner = ownerLink ?? ctx.me.flatMap(game.player)
    if (tv) routes.Tv.index else owner.fold(routes.Round.watcher(game.id, color.name)) { o =>
      routes.Round.player(game fullIdOf o.color)
    }
  }.toString

  def gameLink(pov: Pov)(implicit ctx: UserContext): String = gameLink(pov.game, pov.color)

  def gameFen(
    pov: Pov,
    ownerLink: Boolean = false,
    tv: Boolean = false,
    withTitle: Boolean = true,
    withLink: Boolean = true,
    withLive: Boolean = true,
    withResult: Boolean = true
  )(implicit ctx: UserContext): Frag = {
    val game = pov.game
    val isLive = withLive && game.isBeingPlayed
    val cssClass = isLive ?? "live"
    val variant = game.variant.key
    val tag = if (withLink) a else span
    val boardClass = s"is${pov.game.variant.boardSize.key}"
    val boardSize = s"${pov.game.variant.boardSize.width}x${pov.game.variant.boardSize.height}"
    val classes = s"mini-board mini-board-${game.id} cg-wrap parse-fen is2d $boardClass $cssClass $variant"
    tag(
      href := withLink.option(gameLink(game, pov.color, ownerLink, tv)),
      title := withTitle.option(gameTitle(game, pov.color)),
      cls := classes,
      dataLive := isLive.option(game.id),
      dataColor := pov.color.name,
      dataFen := Forsyth.exportBoard(game.board),
      dataBoard := boardSize,
      dataLastmove := ~game.lastMoveKeys,
      dataResult := withResult.option(game.resultChar)
    )(cgWrapContent)
  }

  def gameFenNoCtx(pov: Pov, tv: Boolean = false, blank: Boolean = false): Frag = {
    val isLive = pov.game.isBeingPlayed
    val variant = pov.game.variant.key
    val boardClass = s"is${pov.game.variant.boardSize.key}"
    val boardSize = s"${pov.game.variant.boardSize.width}x${pov.game.variant.boardSize.height}"
    a(
      href := (if (tv) routes.Tv.index() else routes.Round.watcher(pov.gameId, pov.color.name)),
      title := gameTitle(pov.game, pov.color),
      cls := List(
        s"mini-board mini-board-${pov.gameId} cg-wrap parse-fen is2d $boardClass $variant" -> true,
        s"live mini-board-${pov.gameId}" -> isLive
      ),
      dataLive := isLive.option(pov.gameId),
      dataColor := pov.color.name,
      dataFen := Forsyth.exportBoard(pov.game.board),
      dataBoard := boardSize,
      dataLastmove := ~pov.game.lastMoveKeys,
      target := blank.option("_blank")
    )(cgWrapContent)
  }

  def challengeTitle(c: lidraughts.challenge.Challenge)(implicit ctx: UserContext) = {
    val speed = c.clock.map(_.config).fold(draughts.Speed.Correspondence.name) { clock =>
      s"${draughts.Speed(clock).name} (${clock.show})"
    }
    val variant = c.variant.exotic ?? s" ${c.variant.name}"
    val challenger = c.challenger.fold(
      _ => User.anonymous,
      reg => s"${usernameOrId(reg.id)} (${reg.rating.show})"
    )
    val players = c.destUser.fold(s"Challenge from $challenger") { dest =>
      s"$challenger challenges ${usernameOrId(dest.id)} (${dest.rating.show})"
    }
    s"$speed$variant ${c.mode.name} Draughts • $players"
  }

  def challengeOpenGraph(c: lidraughts.challenge.Challenge)(implicit ctx: UserContext) =
    lidraughts.app.ui.OpenGraph(
      title = challengeTitle(c),
      url = s"$netBaseUrl${routes.Round.watcher(c.id, draughts.White.name).url}",
      description = "Join the challenge or watch the game here."
    )
}
