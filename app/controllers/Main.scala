package controllers

import akka.pattern.ask
import play.api.data._, Forms._
import play.api.libs.json._
import play.api.mvc._
import scala.annotation.nowarn

import lila.app._
import lila.common.HTTPRequest
import lila.hub.actorApi.captcha.ValidCaptcha
import makeTimeout.large
import views._

final class Main(
    env: Env,
    prismicC: Prismic,
    assetsC: ExternalAssets
) extends LilaController(env) {

  private lazy val blindForm = Form(
    tuple(
      "enable"   -> nonEmptyText,
      "redirect" -> nonEmptyText
    )
  )

  def toggleBlindMode =
    OpenBody { implicit ctx =>
      implicit val req = ctx.body
      fuccess {
        blindForm
          .bindFromRequest()
          .fold(
            _ => BadRequest,
            {
              case (enable, redirect) =>
                Redirect(redirect) withCookies env.lilaCookie.cookie(
                  env.api.config.accessibility.blindCookieName,
                  if (enable == "0") "" else env.api.config.accessibility.hash,
                  maxAge = env.api.config.accessibility.blindCookieMaxAge.toSeconds.toInt.some,
                  httpOnly = true.some
                )
            }
          )
      }
    }

  def handlerNotFound(req: RequestHeader) = reqToCtx(req) map renderNotFound

  def captchaCheck(id: String) =
    Open { implicit ctx =>
      env.hub.captcher.actor ? ValidCaptcha(id, ~get("solution")) map {
        case valid: Boolean => Ok(if (valid) 1 else 0)
      }
    }

  def webmasters =
    Open { implicit ctx =>
      pageHit
      fuccess {
        html.site.help.webmasters()
      }
    }

  def lag =
    Open { implicit ctx =>
      pageHit
      fuccess {
        html.site.lag()
      }
    }

  def mobile =
    Open { implicit ctx =>
      pageHit
      OptionOk(prismicC getBookmark "mobile-apk") {
        case (doc, resolver) => html.mobile(doc, resolver)
      }
    }

  def dailyPuzzleSlackApp =
    Open { implicit ctx =>
      pageHit
      fuccess {
        html.site.dailyPuzzleSlackApp()
      }
    }

  def jslog(id: String) =
    Open { ctx =>
      env.round.selfReport(
        userId = ctx.userId,
        ip = HTTPRequest lastRemoteAddress ctx.req,
        fullId = lila.game.Game.FullId(id),
        name = get("n", ctx.req) | "?"
      )
      NoContent.fuccess
    }

  /**
    * Event monitoring endpoint
    */
  def jsmon(event: String) =
    Action {
      lila.mon.http.jsmon(event).increment()
      NoContent
    }

  private lazy val glyphsResult: Result = {
    import chess.format.pgn.Glyph
    import lila.tree.Node.glyphWriter
    Ok(
      Json.obj(
        "move"        -> (Glyph.MoveAssessment.display: List[Glyph]),
        "position"    -> (Glyph.PositionAssessment.display: List[Glyph]),
        "observation" -> (Glyph.Observation.display: List[Glyph])
      )
    ) as JSON
  }
  val glyphs = Action(glyphsResult)

  def image(id: String, @nowarn("cat=unused") hash: String, @nowarn("cat=unused") name: String) =
    Action.async {
      env.imageRepo
        .fetch(id)
        .map {
          case None => NotFound
          case Some(image) =>
            lila.mon.http.imageBytes.record(image.size.toLong)
            Ok(image.data).withHeaders(
              CONTENT_DISPOSITION -> image.name
            ) as image.contentType.getOrElse("image/jpeg")
        }
    }

  val robots = Action { req =>
    Ok {
      if (env.net.crawlable && req.domain == env.net.domain.value) """User-agent: *
Allow: /
Disallow: /game/export/
Disallow: /games/export/
Allow: /game/export/gif/thumbnail/

User-agent: Twitterbot
Allow: /
"""
      else "User-agent: *\nDisallow: /"
    }
  }

  def manifest =
    Action {
      Ok {
        Json.obj(
          "name"             -> env.net.domain.value,
          "short_name"       -> "Lichess",
          "start_url"        -> "/",
          "display"          -> "standalone",
          "background_color" -> "#161512",
          "theme_color"      -> "#161512",
          "description"      -> "The (really) free, no-ads, open source chess server.",
          "icons" -> List(32, 64, 128, 192, 256, 512, 1024).map { size =>
            Json.obj(
              "src"   -> s"//${env.net.assetDomain.value}/assets/logo/lichess-favicon-$size.png",
              "sizes" -> s"${size}x${size}",
              "type"  -> "image/png"
            )
          },
          "related_applications" -> Json.arr(
            Json.obj(
              "platform" -> "play",
              "url"      -> "https://play.google.com/store/apps/details?id=org.lichess.mobileapp"
            ),
            Json.obj(
              "platform" -> "itunes",
              "url"      -> "https://itunes.apple.com/us/app/lichess-free-online-chess/id968371784"
            )
          )
        )
      } as JSON withHeaders (CACHE_CONTROL -> "max-age=1209600")
    }

  def getFishnet =
    Open { implicit ctx =>
      pageHit
      Ok(html.site.bits.getFishnet()).fuccess
    }

  def costs =
    Action { req =>
      pageHit(req)
      Redirect("https://docs.google.com/spreadsheets/d/1CGgu-7aNxlZkjLl9l-OlL00fch06xp0Q7eCVDDakYEE/preview")
    }

  def verifyTitle =
    Action { req =>
      pageHit(req)
      Redirect(
        "https://docs.google.com/forms/d/e/1FAIpQLSd64rDqXOihJzPlBsQba75di5ioL-WMFhkInS2_vhVTvDtBag/viewform"
      )
    }

  def contact =
    Open { implicit ctx =>
      pageHit
      Ok(html.site.contact()).fuccess
    }

  def faq =
    Open { implicit ctx =>
      pageHit
      Ok(html.site.faq()).fuccess
    }

  def movedPermanently(to: String) =
    Action {
      MovedPermanently(to)
    }

  def instantChess =
    Open { implicit ctx =>
      pageHit
      if (ctx.isAuth) fuccess(Redirect(routes.Lobby.home()))
      else
        fuccess {
          Redirect(s"${routes.Lobby.home()}#pool/10+0").withCookies(
            env.lilaCookie.withSession { s =>
              s + ("theme" -> "ic") + ("pieceSet" -> "icpieces")
            }
          )
        }
    }

  def legacyQaQuestion(id: Int, @nowarn("cat=unused") slug: String) =
    Open { _ =>
      MovedPermanently {
        val faq = routes.Main.faq().url
        id match {
          case 103  => s"$faq#acpl"
          case 258  => s"$faq#marks"
          case 13   => s"$faq#titles"
          case 87   => routes.Stat.ratingDistribution("blitz").url
          case 110  => s"$faq#name"
          case 29   => s"$faq#titles"
          case 4811 => s"$faq#lm"
          case 216  => routes.Main.mobile().url
          case 340  => s"$faq#trophies"
          case 6    => s"$faq#ratings"
          case 207  => s"$faq#hide-ratings"
          case 547  => s"$faq#leaving"
          case 259  => s"$faq#trophies"
          case 342  => s"$faq#provisional"
          case 50   => routes.Page.help().url
          case 46   => s"$faq#name"
          case 122  => s"$faq#marks"
          case _    => faq
        }
      }.fuccess
    }

  def devAsset(@nowarn("cat=unused") v: String, path: String, file: String) = assetsC.at(path, file)
}
