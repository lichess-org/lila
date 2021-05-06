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
            { case (enable, redirect) =>
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
      env.hub.captcher.actor ? ValidCaptcha(id, ~get("solution")) map { case valid: Boolean =>
        Ok(if (valid) 1 else 0)
      }
    }

  def webmasters =
    Open { implicit ctx =>
      pageHit
      fuccess {
        html.site.page.webmasters
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
      OptionOk(prismicC getBookmark "mobile-apk") { case (doc, resolver) =>
        html.mobile(doc, resolver)
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
        ip = HTTPRequest ipAddress ctx.req,
        fullId = lila.game.Game.FullId(id),
        name = get("n", ctx.req) | "?"
      )
      NoContent.fuccess
    }

  /** Event monitoring endpoint
    */
  def jsmon(event: String) =
    Action {
      lila.mon.http.jsmon(event).increment()
      NoContent
    }

  def glyphs =
    Open { implicit ctx =>
      import chess.format.pgn.Glyph
      import lila.tree.Node.glyphWriter
      import lila.i18n.{ I18nKeys => trans }
      JsonOk(
        Json.obj(
          "move" -> List(
            Glyph.MoveAssessment.good.copy(name = trans.glyphs.goodMove.txt()),
            Glyph.MoveAssessment.mistake.copy(name = trans.glyphs.mistake.txt()),
            Glyph.MoveAssessment.brillant.copy(name = trans.glyphs.brilliantMove.txt()),
            Glyph.MoveAssessment.blunder.copy(name = trans.glyphs.blunder.txt()),
            Glyph.MoveAssessment.interesting.copy(name = trans.glyphs.interestingMove.txt()),
            Glyph.MoveAssessment.dubious.copy(name = trans.glyphs.dubiousMove.txt()),
            Glyph.MoveAssessment.only.copy(name = trans.glyphs.onlyMove.txt()),
            Glyph.MoveAssessment.zugzwang.copy(name = trans.glyphs.zugzwang.txt())
          ),
          "position" -> List(
            Glyph.PositionAssessment.equal.copy(name = trans.glyphs.equalPosition.txt()),
            Glyph.PositionAssessment.unclear.copy(name = trans.glyphs.unclearPosition.txt()),
            Glyph.PositionAssessment.whiteSlightlyBetter.copy(name = trans.glyphs.whiteIsSlightlyBetter.txt()),
            Glyph.PositionAssessment.blackSlightlyBetter.copy(name = trans.glyphs.blackIsSlightlyBetter.txt()),
            Glyph.PositionAssessment.whiteQuiteBetter.copy(name = trans.glyphs.whiteIsBetter.txt()),
            Glyph.PositionAssessment.blackQuiteBetter.copy(name = trans.glyphs.blackIsBetter.txt()),
            Glyph.PositionAssessment.whiteMuchBetter.copy(name = trans.glyphs.whiteIsWinning.txt()),
            Glyph.PositionAssessment.blackMuchBetter.copy(name = trans.glyphs.blackIsWinning.txt())
          ),
          "observation" -> List(
            Glyph.Observation.novelty.copy(name = trans.glyphs.novelty.txt()),
            Glyph.Observation.development.copy(name = trans.glyphs.development.txt()),
            Glyph.Observation.initiative.copy(name = trans.glyphs.initiative.txt()),
            Glyph.Observation.attack.copy(name = trans.glyphs.attack.txt()),
            Glyph.Observation.counterplay.copy(name = trans.glyphs.counterplay.txt()),
            Glyph.Observation.timeTrouble.copy(name = trans.glyphs.timeTrouble.txt()),
            Glyph.Observation.compensation.copy(name = trans.glyphs.withCompensation.txt()),
            Glyph.Observation.withIdea.copy(name = trans.glyphs.withTheIdea.txt())
          )
        )
      ).fuccess
    }

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
      JsonOk {
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
              "sizes" -> s"${size}x$size",
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
      } withHeaders (CACHE_CONTROL -> "max-age=1209600")
    }

  def getFishnet =
    Open { implicit ctx =>
      pageHit
      Ok(html.site.bits.getFishnet()).fuccess
    }

  def costs =
    Action { req =>
      pageHit(req)
      Redirect("https://docs.google.com/spreadsheets/d/1Si3PMUJGR9KrpE5lngSkHLJKJkb0ZuI4/preview")
    }

  def verifyTitle =
    Action { req =>
      pageHit(req)
      Redirect(
        "https://docs.google.com/forms/d/e/1FAIpQLSelXSHdiFw_PmZetxY8AaIJSM-Ahb5QnJcfQMDaiPJSf24lDQ/viewform"
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
      if (ctx.isAuth) fuccess(Redirect(routes.Lobby.home))
      else
        fuccess {
          Redirect(s"${routes.Lobby.home}#pool/10+0").withCookies(
            env.lilaCookie.withSession { s =>
              s + ("theme" -> "ic") + ("pieceSet" -> "icpieces")
            }
          )
        }
    }

  def legacyQaQuestion(id: Int, @nowarn("cat=unused") slug: String) =
    Open { _ =>
      MovedPermanently {
        val faq = routes.Main.faq.url
        id match {
          case 103  => s"$faq#acpl"
          case 258  => s"$faq#marks"
          case 13   => s"$faq#titles"
          case 87   => routes.Stat.ratingDistribution("blitz").url
          case 110  => s"$faq#name"
          case 29   => s"$faq#titles"
          case 4811 => s"$faq#lm"
          case 216  => routes.Main.mobile.url
          case 340  => s"$faq#trophies"
          case 6    => s"$faq#ratings"
          case 207  => s"$faq#hide-ratings"
          case 547  => s"$faq#leaving"
          case 259  => s"$faq#trophies"
          case 342  => s"$faq#provisional"
          case 50   => routes.Page.help.url
          case 46   => s"$faq#name"
          case 122  => s"$faq#marks"
          case _    => faq
        }
      }.fuccess
    }

  def devAsset(@nowarn("cat=unused") v: String, path: String, file: String) = assetsC.at(path, file)
}
