package lila.ui

import chess.{ Clock, Color, Rated, Outcome }

import lila.core.LightUser
import lila.core.game.{ Game, LightPlayer, Namer, Player }
import lila.ui.ScalatagsTemplate.{ *, given }
import chess.Ply

trait GameHelper:
  self: I18nHelper & StringHelper & AssetHelper & UserHelper =>

  protected val namer: Namer

  def titleGame(g: Game) =
    val speed = chess.Speed(g.clock.map(_.config)).name
    val variant = g.variant.exotic.so(s" ${g.variant.name}")
    s"$speed$variant Chess • ${playerText(g.whitePlayer)} vs ${playerText(g.blackPlayer)}"

  def shortClockName(clock: Option[Clock.Config])(using t: Translate): Frag =
    clock.fold[Frag](trans.site.unlimited())(shortClockName)

  def shortClockName(clock: Clock.Config): Frag = raw(clock.show)

  def shortClockName(game: Game)(using Translate): Frag =
    game.correspondenceClock
      .map(c => trans.site.nbDays(c.daysPerTurn))
      .orElse(game.clock.map(_.config).map(shortClockName))
      .getOrElse(trans.site.unlimited())

  def ratedName(rated: Rated)(using Translate): String =
    if rated.yes
    then trans.site.rated.txt()
    else trans.site.casual.txt()

  def playerUsername(
      player: LightPlayer,
      user: Option[LightUser],
      withRating: Boolean = true,
      withTitle: Boolean = true
  )(using Translate): Frag =
    player.aiLevel.fold[Frag](
      user
        .fold[Frag](trans.site.anonymous.txt()): user =>
          frag(
            titleTag(withTitle.so(user.title)),
            user.name,
            user.flair.map(userFlair),
            withRating.option(
              span(cls := "rating")(
                " (",
                player.rating.fold(frag("?")): rating =>
                  if player.provisional.yes then
                    abbr(title := trans.perfStat.notEnoughRatedGames.txt())(rating, "?")
                  else rating,
                ")"
              )
            )
          )
    ): level =>
      frag(aiName(level))

  def playerText(player: Player, withRating: Boolean = false): String =
    namer.playerTextBlocking(player, withRating)(using lightUserSync)

  def gameVsText(game: Game, withRatings: Boolean = false): String =
    namer.gameVsTextBlocking(game, withRatings)(using lightUserSync)

  val berserkIconSpan = iconTag(lila.ui.Icon.Berserk)

  def playerLink(
      player: Player,
      cssClass: Option[String] = None,
      withOnline: Boolean = true,
      withRating: Boolean = true,
      withDiff: Boolean = true,
      engine: Boolean = false,
      withBerserk: Boolean = false,
      mod: Boolean = false,
      link: Boolean = true
  )(using ctx: Context): Frag =
    val statusIcon = (withBerserk && player.berserk).option(berserkIconSpan)
    player.userId.flatMap(lightUserSync) match
      case None =>
        val klass = cssClass.so(" " + _)
        span(cls := s"user-link$klass")(
          (player.aiLevel, player.name) match
            case (Some(level), _) => aiNameFrag(level)
            case (_, Some(name)) => name
            case _ => trans.site.anonymous()
          ,
          player.rating.ifTrue(withRating && ctx.pref.showRatings).map { rating => s" ($rating)" },
          statusIcon
        )
      case Some(user) =>
        frag(
          (if link then a else span) (
            cls := userClass(user.id, cssClass, withOnline),
            (if link then href
             else dataHref) := s"${routes.User.show(user.name)}${if mod then "?mod" else ""}"
          )(
            withOnline.option(frag(lineIcon(user), " ")),
            playerUsername(
              player.light,
              user.some,
              withRating = withRating && ctx.pref.showRatings
            ),
            (player.ratingDiff.ifTrue(withDiff && ctx.pref.showRatings)).map { d =>
              frag(" ", showRatingDiff(d))
            },
            tosMark(engine)
          ),
          statusIcon
        )

  def lightPlayerLink(
      player: LightPlayer,
      cssClass: Option[String] = None,
      withOnline: Boolean = true,
      withRating: Boolean = true,
      withDiff: Boolean = true,
      engine: Boolean = false,
      withBerserk: Boolean = false,
      mod: Boolean = false,
      link: Boolean = true
  )(using ctx: Context): Frag =
    val statusIcon = (withBerserk && player.berserk).option(berserkIconSpan)
    player.userId.flatMap(lightUserSync) match
      case None =>
        val klass = cssClass.so(" " + _)
        span(cls := s"user-link$klass")(
          player.aiLevel.fold(trans.site.anonymous())(aiNameFrag),
          player.rating.ifTrue(withRating && ctx.pref.showRatings).map { rating => s" ($rating)" },
          statusIcon
        )
      case Some(user) =>
        frag(
          (if link then a else span) (
            cls := userClass(user.id, cssClass, withOnline),
            (if link then href
             else dataHref) := s"${routes.User.show(user.name)}${if mod then "?mod" else ""}"
          )(
            withOnline.option(frag(lineIcon(user), " ")),
            playerUsername(
              player,
              user.some,
              withRating = withRating && ctx.pref.showRatings
            ),
            (player.ratingDiff.ifTrue(withDiff && ctx.pref.showRatings)).map { d =>
              frag(" ", showRatingDiff(d))
            },
            tosMark(engine)
          ),
          statusIcon
        )

  private def tosMark(mark: Boolean)(using Translate): Option[Tag] =
    mark.option(span(cls := "tos_violation", title := trans.site.thisAccountViolatedTos.txt()))

  def gameResult(game: Game) =
    Outcome.showResult(game.finished.option(Outcome(game.winnerColor)))

  def gameLink(
      game: Game,
      color: Color,
      ownerLink: Boolean = false,
      tv: Boolean = false,
      ply: Option[Ply] = Option.empty[Ply]
  )(using ctx: Context): String =
    val url = {
      val owner = ownerLink.so(ctx.me.flatMap(game.player))
      if tv then routes.Tv.index
      else
        owner.fold(routes.Round.watcher(game.id, color)): o =>
          routes.Round.player(game.fullIdOf(o.color))
    }.toString
    ply.map((ply) => s"$url#${ply}").getOrElse(url)

  def gameLink(pov: Pov)(using Context): String = gameLink(pov.game, pov.color)

  def aiName(level: Int)(using Translate): String =
    trans.site.aiNameLevelAiLevel.txt("Stockfish", level)

  def aiNameFrag(level: Int)(using Translate) =
    raw(aiName(level).replace(" ", "&nbsp;"))

  def variantLink(
      variant: chess.variant.Variant,
      pk: PerfKey,
      initialFen: Option[chess.format.Fen.Full] = None,
      shortName: Boolean = false
  )(using Translate): Frag =

    def link(href: String, title: String, name: String) = a(
      cls := "variant-link",
      st.href := href,
      targetBlank,
      st.title := title
    )(name)

    if variant.exotic then
      link(
        href = variant match
          case chess.variant.FromPosition =>
            s"""${routes.Editor.index}?fen=${initialFen.so(_.value.replace(' ', '_'))}"""
          case v => routes.Cms.variant(v.key).url
        ,
        title = variant.variantTitleTrans.txt(),
        name = (if shortName && variant == chess.variant.KingOfTheHill then variant.shortName
                else variant.variantTrans.txt()).toUpperCase
      )
    else if pk == PerfKey.correspondence then
      link(
        href = s"${routes.Main.faq}#correspondence",
        title = PerfKey.correspondence.perfDesc.txt(),
        name = PerfKey.correspondence.perfTrans
      )
    else span(title := pk.perfDesc.txt())(pk.perfTrans)
