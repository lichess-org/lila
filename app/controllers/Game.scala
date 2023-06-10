package controllers

import java.time.format.DateTimeFormatter
import play.api.mvc.*
import scala.util.chaining.*

import lila.api.GameApiV2
import lila.api.context.*
import lila.app.{ given, * }
import lila.common.config.MaxPerSecond
import lila.common.HTTPRequest
import lila.game.{ Game as GameModel }

final class Game(env: Env, apiC: => Api) extends LilaController(env):

  def bookmark(gameId: GameId) = Auth { _ ?=> me =>
    env.bookmark.api.toggle(gameId, me.id)
  }

  def delete(gameId: GameId) = Auth { _ ?=> me =>
    OptionFuResult(env.game.gameRepo game gameId): game =>
      if game.pgnImport.flatMap(_.user).has(me.id)
      then
        env.hub.bookmark ! lila.hub.actorApi.bookmark.Remove(game.id)
        (env.game.gameRepo remove game.id) >>
          (env.analyse.analysisRepo remove game.id) >>
          env.game.cached.clearNbImportedByCache(me.id) inject
          Redirect(routes.User.show(me.username))
      else
        fuccess:
          Redirect(routes.Round.watcher(game.id, game.naturalOrientation.name))
  }

  def exportOne(id: GameAnyId) = Anon:
    exportGame(GameModel anyToId id, req)

  private[controllers] def exportGame(gameId: GameId, req: RequestHeader): Fu[Result] =
    env.round.proxyRepo.gameIfPresent(gameId) orElse env.game.gameRepo.game(gameId) flatMap {
      case None => NotFound.toFuccess
      case Some(game) =>
        val config = GameApiV2.OneConfig(
          format = if (HTTPRequest acceptsJson req) GameApiV2.Format.JSON else GameApiV2.Format.PGN,
          imported = getBool("imported", req),
          flags = requestPgnFlags(req, extended = true),
          playerFile = get("players", req)
        )
        env.api.gameApiV2.exportOne(game, config) flatMap { content =>
          env.api.gameApiV2.filename(game, config.format) map { filename =>
            Ok(content)
              .pipe(asAttachment(filename))
              .withHeaders(
                lila.app.http.ResponseHeaders.headersForApiOrApp(req)*
              ) as gameContentType(config)
          }
        }
    }

  def exportByUser(username: UserStr) = OpenOrScoped()(
    open = ctx ?=> handleExport(username, ctx.me, oauth = false),
    scoped = _ ?=> me => handleExport(username, me.some, oauth = true)
  )

  def apiExportByUser(username: UserStr) = AnonOrScoped() { _ ?=> me =>
    handleExport(username, me, oauth = me.isDefined)
  }

  private def handleExport(
      username: UserStr,
      me: Option[lila.user.User],
      oauth: Boolean
  )(using req: RequestHeader) =
    env.user.repo byId username flatMap {
      _.filter(u => u.enabled.yes || me.exists(_ is u) || me.??(isGranted(_.GamesModView, _))) ?? { user =>
        val format = GameApiV2.Format byRequest req
        import lila.rating.{ Perf, PerfType }
        WithVs(req) { vs =>
          val finished = getBoolOpt("finished", req) | true
          val config = GameApiV2.ByUserConfig(
            user = user,
            format = format,
            vs = vs,
            since = getTimestamp("since", req),
            until = getTimestamp("until", req),
            max = getInt("max", req).map(_ atLeast 1),
            rated = getBoolOpt("rated", req),
            perfType = (~get("perfType", req) split "," map { Perf.Key(_) } flatMap PerfType.apply).toSet,
            color = get("color", req) flatMap chess.Color.fromName,
            analysed = getBoolOpt("analysed", req),
            flags = requestPgnFlags(req, extended = false),
            sort =
              if (get("sort", req) has "dateAsc") GameApiV2.GameSort.DateAsc else GameApiV2.GameSort.DateDesc,
            perSecond = MaxPerSecond(me match {
              case Some(m) if m is lila.user.User.explorerId => env.apiExplorerGamesPerSecond.get()
              case Some(m) if m is user.id                   => 60
              case Some(_) if oauth => 30 // bonus for oauth logged in only (not for CSRF)
              case _                => 20
            }),
            playerFile = get("players", req),
            ongoing = getBool("ongoing", req) || !finished,
            finished = finished
          )
          if (me.exists(_ is lila.user.User.explorerId))
            Ok.chunked(env.api.gameApiV2.exportByUser(config))
              .pipe(noProxyBuffer)
              .as(gameContentType(config))
              .toFuccess
          else
            apiC
              .GlobalConcurrencyLimitPerIpAndUserOption(req, me, user.some)(
                env.api.gameApiV2.exportByUser(config)
              ) { source =>
                Ok.chunked(source)
                  .pipe(
                    asAttachmentStream(
                      s"lichess_${user.username}_${fileDate}.${format.toString.toLowerCase}"
                    )
                  )
                  .as(gameContentType(config))
              }
              .toFuccess

        }
      }
    }

  private def fileDate = DateTimeFormatter ofPattern "yyyy-MM-dd" print nowInstant

  def apiExportByUserImportedGames(username: UserStr) =
    def doExport(username: UserStr)(me: lila.user.User)(using ctx: AnyContext) = fuccess:
      if !me.is(username) then Forbidden("Imported games of other players cannot be downloaded")
      else
        apiC
          .GlobalConcurrencyLimitPerIpAndUserOption(ctx.req, me.some, me.some)(
            env.api.gameApiV2.exportUserImportedGames(me)
          ): source =>
            Ok.chunked(source)
              .pipe(asAttachmentStream(s"lichess_${me.username}_$fileDate.imported.pgn"))
              .as(pgnContentType)
    AuthOrScoped()(doExport(username), doExport(username))

  def exportByIds = AnonBodyOf(parse.tolerantText): body =>
    val config = GameApiV2.ByIdsConfig(
      ids = GameId from body.split(',').view.take(300).toSeq,
      format = GameApiV2.Format byRequest req,
      flags = requestPgnFlags(req, extended = false),
      perSecond = MaxPerSecond(30),
      playerFile = get("players", req)
    )
    apiC.GlobalConcurrencyLimitPerIP
      .download(req.ipAddress)(env.api.gameApiV2.exportByIds(config)) { source =>
        noProxyBuffer(Ok.chunked(source)).as(gameContentType(config))
      }
      .toFuccess

  private def WithVs(req: RequestHeader)(f: Option[lila.user.User] => Fu[Result]): Fu[Result] =
    getUserStr("vs", req) match
      case None => f(none)
      case Some(name) =>
        env.user.repo byId name flatMap {
          case None       => notFoundJson(s"No such opponent: $name")
          case Some(user) => f(user.some)
        }

  private[controllers] def requestPgnFlags(req: RequestHeader, extended: Boolean) =
    lila.game.PgnDump.WithFlags(
      moves = getBoolOpt("moves", req) | true,
      tags = getBoolOpt("tags", req) | true,
      clocks = getBoolOpt("clocks", req) | extended,
      evals = getBoolOpt("evals", req) | extended,
      opening = getBoolOpt("opening", req) | extended,
      literate = getBool("literate", req),
      pgnInJson = getBool("pgnInJson", req),
      delayMoves = delayMovesFromReq(req),
      lastFen = getBool("lastFen", req),
      accuracy = getBool("accuracy", req)
    )

  private[controllers] def delayMovesFromReq(req: RequestHeader) =
    !get("key", req).exists(env.noDelaySecretSetting.get().value.contains)

  private[controllers] def gameContentType(config: GameApiV2.Config) =
    config.format match
      case GameApiV2.Format.PGN => pgnContentType
      case GameApiV2.Format.JSON =>
        config match
          case _: GameApiV2.OneConfig => JSON
          case _                      => ndJsonContentType

  private[controllers] def preloadUsers(game: GameModel): Funit =
    env.user.lightUserApi preloadMany game.userIds
