package controllers

import views._

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import cats.data.Validated
import scalatags.Text.all.stringFrag
import lila.app._
import lila.api.Context
import lila.oauth.{ AccessTokenRequest, AuthorizationRequest }

final class OAuth(env: Env) extends LilaController(env) {

  private def reqToAuthorizationRequest(req: RequestHeader) =
    AuthorizationRequest.Raw(
      clientId = get("client_id", req),
      responseType = get("response_type", req),
      redirectUri = get("redirect_uri", req),
      state = get("state", req),
      codeChallenge = get("code_challenge", req),
      codeChallengeMethod = get("code_challenge_method", req),
      scope = get("scope", req)
    )

  private def withPrompt(f: AuthorizationRequest.Prompt => Fu[Result])(implicit ctx: Context) =
    reqToAuthorizationRequest(ctx.req).prompt match {
      case Validated.Valid(prompt) => f(prompt)
      case Validated.Invalid(error) =>
        BadRequest(html.site.message("Bad authorization request")(stringFrag(error.description))).fuccess
    }

  def authorize =
    Open { implicit ctx =>
      withPrompt { prompt =>
        fuccess(ctx.me.fold(Redirect(routes.Auth.login.url, Map("referrer" -> List(ctx.req.uri)))) { me =>
          Ok(html.oAuth.app.authorize(prompt))
        })
      }
    }

  def authorizeApply =
    Auth { implicit ctx => me =>
      withPrompt { prompt =>
        prompt.authorize(me) match {
          case Validated.Valid(authorized) =>
            env.oAuth.authorizationApi.create(authorized) map { code =>
              Redirect(authorized.redirectUrl(code))
            }
          case Validated.Invalid(error) => Redirect(error.redirectUrl(prompt.redirectUri)).fuccess
        }
      }
    }

  val accessTokenRequestForm = Form(
    mapping(
      "grant_type"   -> text,
      "code"         -> text,
      "redirect_uri" -> text,
      "client_id"    -> text,
      "xxx"          -> text
    )(AccessTokenRequest.Raw.apply)(AccessTokenRequest.Raw.unapply)
  )

  def tokenApply =
    Action.async(parse.form(
      accessTokenRequestForm,
      onErrors = (form: Form[AccessTokenRequest.Raw]) =>
            BadRequest(
              Json
                .obj(
                  "error" -> "invalid_request"
                )
                .add("error_description" -> form.errors.headOption.map(_.message))
            )
    )) { implicit req =>
      req.body.pp
      ???
    }
}
