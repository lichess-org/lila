package lila.challenge
package ui

import play.api.libs.json.{ JsObject, Json }

import lila.challenge.Challenge.Status
import lila.core.LightUser
import lila.core.game.GameRule
import lila.core.user.WithPerf
import lila.core.relation.Relation
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class ChallengeUi(helpers: Helpers):
  import helpers.{ *, given }

  def page(c: Challenge, json: JsObject, owner: Boolean, color: Option[Color] = None)(using
      ctx: Context
  ) =
    val title = challengeTitle(c)
    Page(title)
      .graph(
        title = title,
        url = s"$netBaseUrl${routes.Round.watcher(c.gameId, Color.white).url}",
        description = "Join the challenge or watch the game here."
      )
      .js(Esm("bits.qrcode"))
      .js(
        PageModule(
          "bits.challengePage",
          Json.obj(
            "xhrUrl" -> routes.Challenge.show(c.id, color).url,
            "owner"  -> owner,
            "data"   -> json
          )
        )
      )
      .css("challenge.page")

  private def challengeTitle(c: Challenge)(using ctx: Context) =
    val speed = c.clock.map(_.config).fold(chess.Speed.Correspondence.name) { clock =>
      s"${chess.Speed(clock).name} (${clock.show})"
    }
    val variant    = c.variant.exotic.so(s" ${c.variant.name}")
    val challenger = c.challengerUser.fold(trans.site.anonymous.txt()): reg =>
      s"${titleNameOrId(reg.id)}${ctx.pref.showRatings.so(s" (${reg.rating.show})")}"
    val players =
      if c.isOpen then "Open challenge"
      else
        c.destUser.fold(s"Challenge from $challenger"): dest =>
          s"$challenger challenges ${titleNameOrId(dest.id)}${ctx.pref.showRatings.so(s" (${dest.rating.show})")}"
    s"$speed$variant ${c.rated.name} Chess • $players"

  private def details(c: Challenge, requestedColor: Option[Color])(using ctx: Context) =
    div(cls := "details-wrapper")(
      div(cls := "content")(
        div(
          cls      := "variant",
          dataIcon := (if c.initialFen.isDefined then Icon.Feather else c.perfType.icon)
        )(
          div(
            variantLink(c.variant, c.perfType, c.initialFen),
            br,
            span(cls := "clock"):
              c.daysPerTurn
                .fold(shortClockName(c.clock.map(_.config))): days =>
                  if days.value == 1 then trans.site.oneDay()
                  else trans.site.nbDays.pluralSame(days.value)
          )
        ),
        div(cls := "mode")(
          c.open.fold(c.colorChoice.some)(_.colorFor(requestedColor)).map { colorChoice =>
            frag(colorChoice.trans(), br)
          },
          ratedName(c.rated)
        )
      ),
      c.rules.nonEmpty.option(
        div(cls := "rules")(
          h2("Custom rules:"),
          div(fragList(c.rules.toList.map(showRule), "/"))
        )
      )
    )

  private def showRule(r: GameRule) =
    val (text, flair) = getRuleStyle(r);
    div(cls := "challenge-rule")(
      iconFlair(flair),
      text
    )

  private def getRuleStyle(r: GameRule): (String, Flair) =
    r match
      case GameRule.noAbort     => ("No abort", Flair("symbols.cross-mark"));
      case GameRule.noRematch   => ("No rematch", Flair("symbols.recycling-symbol"));
      case GameRule.noGiveTime  => ("No giving of time", Flair("objects.alarm-clock"));
      case GameRule.noClaimWin  => ("No claiming of win", Flair("objects.hourglass-done"));
      case GameRule.noEarlyDraw => ("No early draw", Flair("people.handshake-light-skin-tone"));

  def mine(
      c: Challenge,
      json: JsObject,
      friends: Seq[LightUser],
      error: Option[String],
      color: Option[Color]
  )(using ctx: Context) =

    val cancelForm =
      postForm(action := routes.Challenge.cancel(c.id), cls := "cancel xhr"):
        submitButton(cls := "button button-red text", dataIcon := Icon.X)(trans.site.cancel())

    page(c, json, owner = true):
      val challengeLink = s"$netBaseUrl${routes.Round.watcher(c.gameId, Color.white)}"
      main(cls := s"page-small challenge-page box box-pad challenge--${c.status.name}")(
        c.status match
          case Status.Created | Status.Offline =>
            div(id := "ping-challenge")(
              h1(cls := "box__top")(
                if c.isOpen then c.name | "Open challenge" else trans.challenge.challengeToPlay.txt()
              ),
              details(c, color),
              c.destUserId
                .map { destId =>
                  div(cls := "waiting")(
                    userIdLink(destId.some, cssClass = "target".some),
                    if c.clock.isEmpty then
                      div(cls := "correspondence-waiting text", dataIcon := Icon.Checkmark):
                        "Challenge sent"
                    else spinner,
                    p(trans.site.waitingForOpponent())
                  )
                }
                .getOrElse {
                  if c.isOpen then
                    div(cls := "waiting")(
                      spinner,
                      p(trans.site.waitingForOpponent())
                    )
                  else
                    div(cls := "invite")(
                      button(cls := "mobile-instructions button none")("Tap here to share"),
                      div(cls := "invite__url")(
                        h2(cls := "ninja-title", trans.site.toInviteSomeoneToPlayGiveThisUrl()),
                        br,
                        copyMeInput(challengeLink),
                        br,
                        p(trans.site.theFirstPersonToComeOnThisUrlWillPlayWithYou())
                      ),
                      ctx.isAuth.option(
                        div(cls := "invite__user")(
                          h2(cls := "ninja-title", trans.challenge.inviteLichessUser()),
                          friends.nonEmpty.option(
                            div(cls := "invite__user__recent")(
                              friends.map: user =>
                                button(cls := "button", dataUser := user.name):
                                  lightUserSpan(user, withOnline = true)
                            )
                          ),
                          br,
                          postForm(
                            cls    := "user-invite complete-parent",
                            action := routes.Challenge.toFriend(c.id)
                          )(
                            input(
                              name        := "username",
                              cls         := "friend-autocomplete",
                              placeholder := trans.search.search.txt()
                            ),
                            error.map { p(cls := "error")(_) }
                          )
                        )
                      ),
                      div(cls := "invite__qrcode")(
                        h2(cls := "ninja-title", trans.site.orLetYourOpponentScanQrCode()),
                        qrcode(challengeLink, width = 150)
                      )
                    )
                },
              c.notableInitialFen.map: fen =>
                frag(
                  br,
                  div(cls := "board-preview", chessgroundMini(fen.board, c.finalColor)(div))
                ),
              (!c.isOpen).option(cancelForm)
            )
          case Status.Declined =>
            div(cls := "follow-up")(
              h1(cls := "box__top")(trans.challenge.challengeDeclined()),
              blockquote(cls := "challenge-reason pull-quote")(
                p(c.anyDeclineReason.trans()),
                footer(userIdLink(c.destUserId))
              ),
              details(c, color),
              a(cls := "button button-fat", href := routes.Lobby.home)(trans.site.newOpponent())
            )
          case Status.Accepted =>
            div(cls := "follow-up")(
              h1(cls := "box__top")(trans.challenge.challengeAccepted()),
              details(c, color),
              a(
                id   := "challenge-redirect",
                href := routes.Round.watcher(c.gameId, Color.white),
                cls  := "button button-fat"
              ):
                trans.site.joinTheGame()
            )
          case Status.Canceled =>
            div(cls := "follow-up")(
              h1(cls := "box__top")(trans.challenge.challengeCanceled()),
              details(c, color),
              a(cls := "button button-fat", href := routes.Lobby.home)(trans.site.newOpponent())
            )
      )

  def theirs(
      c: Challenge,
      json: JsObject,
      user: Option[WithPerf],
      color: Option[Color],
      relation: Option[Relation]
  )(using ctx: Context) =
    page(c, json, owner = false, color):
      main(cls := "page-small challenge-page challenge-theirs box box-pad"):
        c.status match
          case Status.Created | Status.Offline =>
            frag(
              boxTop:
                h1:
                  if c.isOpen then c.name | "Open challenge"
                  else
                    user.fold[Frag]("Anonymous"): u =>
                      frag(
                        userLink(u.user),
                        ctx.pref.showRatings.option(frag(" (", u.perf.glicko.display, ")"))
                      )
              ,
              details(c, color),
              c.notableInitialFen.map: fen =>
                div(cls := "board-preview", chessgroundMini(fen.board, !c.finalColor)(div)),
              if relation.has(Relation.Block) then badTag("You have blocked this player.")
              else if c.open.exists(!_.canJoin) then
                div(
                  "Waiting for ",
                  fragList((~c.open.flatMap(_.userIdList)).map(uid => userIdLink(uid.some)), " and "),
                  " to start the game."
                )
              else if color.map(Challenge.ColorChoice.apply).has(c.colorChoice) then
                badTag(
                  // very rare message, don't translate
                  s"You have the wrong color link for this open challenge. The ${color.so(_.name)} player has already joined."
                )
              else if c.rated.no || ctx.isAuth then
                frag(
                  (c.rated.yes && c.unlimited)
                    .option(badTag(trans.site.bewareTheGameIsRatedButHasNoClock())),
                  postForm(cls := "accept", action := routes.Challenge.accept(c.id, color))(
                    submitButton(cls := "text button button-fat", dataIcon := Icon.PlayTriangle)(
                      trans.site.joinTheGame()
                    )
                  )
                )
              else
                frag(
                  hr,
                  badTag(
                    p(trans.site.thisGameIsRated()),
                    a(
                      cls  := "button",
                      href := s"${routes.Auth.login}?referrer=${routes.Round.watcher(c.gameId, Color.white)}"
                    )(trans.site.signIn())
                  )
                )
            )
          case Status.Declined =>
            div(cls := "follow-up")(
              h1(cls := "box__top")(trans.challenge.challengeDeclined()),
              details(c, color),
              a(cls := "button button-fat", href := routes.Lobby.home)(trans.site.newOpponent())
            )
          case Status.Accepted =>
            div(cls := "follow-up")(
              h1(cls := "box__top")(trans.challenge.challengeAccepted()),
              details(c, color),
              a(
                id   := "challenge-redirect",
                href := routes.Round.watcher(c.gameId, Color.white),
                cls  := "button button-fat"
              )(
                trans.site.joinTheGame()
              )
            )
          case Status.Canceled =>
            div(cls := "follow-up")(
              h1(cls := "box__top")(trans.challenge.challengeCanceled()),
              details(c, color),
              a(cls := "button button-fat", href := routes.Lobby.home)(trans.site.newOpponent())
            )
