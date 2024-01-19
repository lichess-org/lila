package controllers

import java.time.format.DateTimeFormatter
import play.api.mvc.*
import scala.util.chaining.*

import lila.api.GameApiV2

import lila.app.{ given, * }
import lila.common.config.MaxPerSecond
import lila.common.HTTPRequest

final class Game(env: Env, apiC: => Api) extends LilaController(env):

  def bookmark(gameId: GameId) = Auth { _ ?=> me ?=>
    env.bookmark.api.toggle(gameId, me) inject NoContent
  }

  def delete(gameId: GameId) = Auth { _ ?=> me ?=>
    Found(env.game.gameRepo game gameId): game =>
      if game.pgnImport.flatMap(_.user).exists(me.is(_)) then
        for
          _ <- env.bookmark.api.removeByGameId(game.id)
          _ <- env.game.gameRepo remove game.id
          _ <- env.analyse.analysisRepo remove game.id
          _ <- env.game.cached.clearNbImportedByCache(me)
        yield Redirect(routes.User.show(me.username))
      else Redirect(routes.Round.watcher(game.id, game.naturalOrientation.name))
  }

  def exportOne(id: GameAnyId) = AnonOrScoped():
    exportGame(id.gameId)

  private[controllers] def exportGame(gameId: GameId)(using Context): Fu[Result] =
    env.round.proxyRepo.gameIfPresent(gameId) orElse env.game.gameRepo.game(gameId) flatMap {
      case None => NotFound
      case Some(game) =>
        val config = GameApiV2.OneConfig(
          format = if HTTPRequest acceptsJson req then GameApiV2.Format.JSON else GameApiV2.Format.PGN,
          imported = getBool("imported"),
          flags = requestPgnFlags(extended = true),
          playerFile = get("players")
        )
        env.api.gameApiV2.exportOne(game, config) flatMap: content =>
          env.api.gameApiV2.filename(game, config.format) map: filename =>
            Ok(content)
              .pipe(asAttachment(filename))
              .withHeaders(headersForApiOrApp*)
              .as(gameContentType(config))
    }

  def exportByUser(username: UserStr)    = OpenOrScoped()(handleExport(username))
  def apiExportByUser(username: UserStr) = AnonOrScoped()(handleExport(username))

  private def handleExport(username: UserStr)(using ctx: Context) =
    env.user.repo byId username flatMap {
      _.filter(u => u.enabled.yes || ctx.is(u) || isGrantedOpt(_.GamesModView)) so { user =>
        val format = GameApiV2.Format byRequest req
        import lila.rating.{ Perf, PerfType }
        WithVs: vs =>
          env.security.ipTrust
            .throttle(MaxPerSecond:
              if ctx is lila.user.User.explorerId then env.apiExplorerGamesPerSecond.get()
              else if ctx is user then 60
              else if ctx.isOAuth then 30 // bonus for oauth logged in only (not for CSRF)
              else 25
            )
            .flatMap: perSecond =>
              val finished = getBoolOpt("finished") | true
              val config = GameApiV2.ByUserConfig(
                user = user,
                format = format,
                vs = vs,
                since = getTimestamp("since"),
                until = getTimestamp("until"),
                max = getInt("max").map(_ atLeast 1),
                rated = getBoolOpt("rated"),
                perfType = (~get("perfType") split "," map { Perf.Key(_) } flatMap PerfType.apply).toSet,
                color = get("color") flatMap chess.Color.fromName,
                analysed = getBoolOpt("analysed"),
                flags = requestPgnFlags(extended = false),
                sort =
                  if get("sort") has "dateAsc" then GameApiV2.GameSort.DateAsc
                  else GameApiV2.GameSort.DateDesc,
                perSecond = perSecond,
                playerFile = get("players"),
                ongoing = getBool("ongoing") || !finished,
                finished = finished
              )
              if ctx.is(lila.user.User.explorerId) then
                Ok.chunked(env.api.gameApiV2.exportByUser(config))
                  .pipe(noProxyBuffer)
                  .as(gameContentType(config))
              else
                apiC
                  .GlobalConcurrencyLimitPerIpAndUserOption(user.some)(
                    env.api.gameApiV2.exportByUser(config)
                  ): source =>
                    Ok.chunked(source)
                      .pipe:
                        asAttachmentStream:
                          s"lichess_${user.username}_${fileDate}.${format.toString.toLowerCase}"
                      .as(gameContentType(config))

      }
    }

  private def fileDate = DateTimeFormatter ofPattern "yyyy-MM-dd" print nowInstant

  def apiExportByUserImportedGames() = AuthOrScoped() { ctx ?=> me ?=>
    apiC.GlobalConcurrencyLimitPerIpAndUserOption(me.some)(
      env.api.gameApiV2.exportUserImportedGames(me)
    ): source =>
      Ok.chunked(source)
        .pipe(asAttachmentStream(s"lichess_${me.username}_$fileDate.imported.pgn"))
        .as(pgnContentType)
  }

  def exportByIds = AnonBodyOf(parse.tolerantText): body =>
    val config = GameApiV2.ByIdsConfig(
      ids = GameId from body.split(',').view.take(300).toSeq,
      format = GameApiV2.Format byRequest req,
      flags = requestPgnFlags(extended = false),
      perSecond = MaxPerSecond(30),
      playerFile = get("players")
    )
    apiC.GlobalConcurrencyLimitPerIP
      .download(req.ipAddress)(env.api.gameApiV2.exportByIds(config)): source =>
        noProxyBuffer(Ok.chunked(source)).as(gameContentType(config))

  private def WithVs(f: Option[lila.user.User] => Fu[Result])(using RequestHeader): Fu[Result] =
    getUserStr("vs") match
      case None => f(none)
      case Some(name) =>
        env.user.repo byId name flatMap {
          case None       => notFoundJson(s"No such opponent: $name")
          case Some(user) => f(user.some)
        }

  private[controllers] def requestPgnFlags(extended: Boolean)(using RequestHeader) =
    lila.game.PgnDump.WithFlags(
      moves = getBoolOpt("moves") | true,
      tags = getBoolOpt("tags") | true,
      clocks = getBoolOpt("clocks") | extended,
      evals = getBoolOpt("evals") | extended,
      opening = getBoolOpt("opening") | extended,
      literate = getBool("literate"),
      pgnInJson = getBool("pgnInJson"),
      delayMoves = delayMovesFromReq,
      lastFen = getBool("lastFen"),
      accuracy = getBool("accuracy")
    )

  private[controllers] def delayMovesFromReq(using RequestHeader) =
    !get("key").exists(env.noDelaySecretSetting.get().value.contains)

  private[controllers] def gameContentType(config: GameApiV2.Config) =
    config.format match
      case GameApiV2.Format.PGN => pgnContentType
      case GameApiV2.Format.JSON =>
        config match
          case _: GameApiV2.OneConfig => JSON
          case _                      => ndJsonContentType

  private[controllers] def preloadUsers(game: lila.game.Game): Funit =
    env.user.lightUserApi preloadMany game.userIds
  private[controllers] def preloadUsers(users: lila.user.GameUsers): Unit =
    env.user.lightUserApi preloadUsers users.all.collect:
      case Some(lila.user.User.WithPerf(u, _)) => u
