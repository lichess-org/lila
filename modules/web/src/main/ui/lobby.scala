package lila.web
package ui

import lila.core.i18n.Translate
import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.core.i18n.I18nKey as trans

object lobby:

  def page(siteName: String)(using Translate) =
    Page("")
      .copy(fullTitle = s"$siteName • ${trans.site.freeOnlineChess.txt()}".some)
      .i18n(_.variant)
      .css("lobby")
      .hrefLangs(lila.ui.LangPath("/"))

  def about(using ctx: Context)(using Translate) =
    div(cls := "about-side")(
      ctx.blind.option(h2(trans.site.about())),
      trans.site.xIsAFreeYLibreOpenSourceChessServer(
        "Lichess",
        a(cls := "blue", href := routes.Plan.features)(trans.site.really.txt())
      ),
      " ",
      a(href := "/about")(trans.site.aboutX("Lichess"), "...")
    )

  def aboutLinks(using ctx: Context)(using Translate) =
    div(cls := "lobby__about")(
      ctx.blind.option(h2(trans.site.about())),
      a(href := "/about")(trans.site.aboutX("Lichess")),
      a(href := "/faq")(trans.faq.faqAbbreviation()),
      a(href := "/contact")(trans.contact.contact()),
      a(href := "/app")(trans.site.mobileApp()),
      a(href := "/terms-of-service")(trans.site.termsOfService()),
      a(href := "/privacy")(trans.site.privacy()),
      a(href := "/source")(trans.site.sourceCode()),
      a(href := "/ads")(trans.site.blockAds()),
      lila.web.ui.bits.connectLinks
    )

  val app = div(cls := "lobby__app")(
    div(cls := "tabs-horiz")(span(nbsp)),
    div(cls := "lobby__app__content lpools")
  )

  def showUnreadLichessMessage(using Translate) =
    nopeInfo(
      cls := "unread-lichess-message",
      p(trans.site.showUnreadLichessMessage()),
      p:
        a(cls := "button button-fat", href := routes.Msg.convo(UserId.lichess)):
          trans.site.readTheMessage()
    )

  def nopeInfo(content: Modifier*) =
    frag(
      div(cls := "lobby__app"),
      div(cls := "lobby__nope"):
        st.section(cls := "lobby__app__content")(content)
    )

  def currentGameInfo(pov: Pov, opponent: String)(using Translate) =
    lobby.nopeInfo(
      h1(trans.site.hangOn()),
      p(trans.site.gameInProgress(strong(opponent))),
      br,
      br,
      a(
        cls := "text button button-fat",
        dataIcon := Icon.PlayTriangle,
        href := routes.Round.player(pov.fullId)
      )(
        trans.site.joinTheGame()
      ),
      br,
      br,
      "or",
      br,
      br,
      form(method := "post", action := routes.Round.resign(pov.fullId))(
        button(cls := "text button button-red", dataIcon := Icon.X):
          if pov.game.abortableByUser then trans.site.abortTheGame() else trans.site.resignTheGame()
      ),
      br,
      p(trans.site.youCantStartNewGame())
    )

  def playbanInfo(endsAt: Frag)(using Translate) =
    lobby.nopeInfo(
      h1(trans.site.sorry()),
      p(trans.site.weHadToTimeYouOutForAWhile()),
      p(strong(endsAt)),
      h2(trans.site.why()),
      p(
        trans.site.pleasantChessExperience(),
        br,
        trans.site.goodPractice(),
        br,
        trans.site.potentialProblem()
      ),
      h2(trans.site.howToAvoidThis()),
      ul(
        li(trans.site.playEveryGame()),
        li(trans.site.tryToWin()),
        li(trans.site.resignLostGames())
      ),
      p(
        trans.site.temporaryInconvenience(),
        br,
        trans.site.wishYouGreatGames(),
        br,
        trans.site.thankYouForReading()
      )
    )

  def table(using Translate) =
    div(cls := "lobby__table")(
      div(cls := "lobby__start")(
        button(cls := "button button-metal lobby__start__button lobby__start__button--hook")(
          trans.site.createLobbyGame()
        ),
        button(cls := "button button-metal lobby__start__button lobby__start__button--friend")(
          trans.site.challengeAFriend()
        ),
        button(cls := "button button-metal lobby__start__button lobby__start__button--ai")(
          trans.site.playAgainstComputer()
        )
      )
    )

  def toursAndSimuls(tours: Frag, simuls: Option[Frag])(using ctx: Context)(using Translate) =
    ctx.noBot.option:
      div(cls := "lobby__tournaments-simuls")(
        div(cls := "lobby__tournaments lobby__box")(
          a(cls := "lobby__box__top", href := routes.Tournament.home)(
            h2(cls := "title text", dataIcon := Icon.Trophy)(trans.site.openTournaments()),
            span(cls := "more")(trans.site.more(), " »")
          ),
          div(cls := "lobby__box__content")(tours)
        ),
        simuls.map: simuls =>
          div(cls := "lobby__simuls lobby__box")(
            a(cls := "lobby__box__top", href := routes.Simul.home)(
              h2(cls := "title text", dataIcon := Icon.Group)(trans.site.simultaneousExhibitions()),
              span(cls := "more")(trans.site.more(), " »")
            ),
            div(cls := "lobby__box__content")(simuls)
          )
      )

  def donateLink(using me: Option[Me])(using Translate) =
    a(cls := "lobby__support-link", href := routes.Plan.index())(
      iconTag(Icon.Wings),
      span(cls := "lobby__support-link__text")(
        strong:
          if me.exists(_.isPatron) then trans.patron.lichessPatron()
          else trans.patron.donate()
        ,
        span(
          if me.exists(_.isPatron) then trans.patron.thankYou()
          else trans.patron.becomePatron()
        )
      )
    )

  def swagLink(using Translate) =
    a(cls := "lobby__support-link", href := "/swag")(
      iconTag(Icon.Tshirt),
      span(cls := "lobby__support-link__text")(
        strong("Swag Store"),
        span(trans.site.playChessInStyle())
      )
    )
