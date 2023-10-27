package views.html

import controllers.routes
import scala.util.chaining.*

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.oauth.AccessToken

object dgt:

  import trans.dgt.*

  private val liveChessVersion = "2.2.5+"

  def index(using PageContext) =
    layout("index")(
      h1(cls := "box__top")(lichessAndDgt()),
      p(thisPageAllowsConnectingDgtBoard()),
      br,
      br,
      st.section(
        h2(dgtBoardRequirements()),
        br,
        p(toConnectTheDgtBoard(s"LiveChess $liveChessVersion")),
        p(
          downloadHere(
            a(href := "https://www.livechesscloud.com/software/")(s"LiveChess $liveChessVersion")
          )
        ),
        p(
          ifLiveChessRunningOnThisComputer(
            "LiveChess",
            a(href := "http://localhost:1982/doc/index.html")(openingThisLink())
          )
        )
      ),
      p(
        ifLiveChessRunningElsewhere(
          "LiveChess",
          a(href := routes.DgtCtrl.config)(configurationSection())
        )
      ),
      st.section(
        h2(dgtBoardLimitations()),
        br,
        p(keepPlayPageOpen()),
        p(boardWillAutoConnect()),
        p(
          timeControlsForCasualGames(),
          br,
          timeControlsForRatedGames()
        )
      ),
      p(
        whenReadySetupBoard(
          a(href := routes.DgtCtrl.play)(trans.play())
        )
      )
    )

  def play(token: AccessToken)(using PageContext) =
    layout("play", s"'${token.plain.value}'".some)(
      div(id := "dgt-play-zone")(pre(id := "dgt-play-zone-log")),
      div(cls := "dgt__play__help")(
        h2(iconTag(licon.InfoCircle, ifMoveNotDetected())),
        p(checkYouHaveMadeOpponentsMove()),
        p(
          asALastResort(
            a(href := routes.DgtCtrl.play)(reloadThisPage())
          )
        )
      )
    )

  def config(token: Option[lila.oauth.AccessToken])(using PageContext) =
    layout("config")(
      div(cls := "account")(
        h1(cls := "box__top")(dgtConfigure()),
        form(action := routes.DgtCtrl.generateToken, method := "post")(
          st.section(
            h2(lichessConnectivity()),
            if token.isDefined then
              p(cls := "text", dataIcon := licon.Checkmark)(
                validDgtOauthToken(),
                br,
                br,
                dgtPlayMenuEntryAdded(
                  strong(dgtBoard())
                )
              )
            else
              frag(
                p(noSuitableOauthToken()),
                form3.submit(clickToGenerateOne())
              )
          )
        ),
        form(cls := "form3", id := "dgt-config")(
          st.section(
            h2(dgtBoardConnectivity()),
            "dgt-livechess-url" pipe { name =>
              div(cls := "form-group")(
                st.label(`for` := name, cls := "form-label")(
                  webSocketUrl(s"LiveChess $liveChessVersion")
                ),
                st.input(id := name, st.name := name, cls := "form-control", required := true),
                st.small(cls := "form-help")(
                  useWebSocketUrl(
                    "ws://localhost:1982/api/v1.0",
                    "LiveChess"
                  )
                )
              )
            }
          ),
          st.section(
            h2(textToSpeech()),
            div(cls := "form-group")(
              p(configureVoiceNarration())
            ),
            div(cls := "form-group")(
              st.label(cls := "form-label")(enableSpeechSynthesis()),
              radios(
                "dgt-speech-synthesis",
                List((false, trans.no.txt()), (true, trans.yes.txt()))
              )
            ),
            "dgt-speech-voice" pipe { name =>
              div(cls := "form-group")(
                st.label(`for` := name, cls := "form-label")(
                  speechSynthesisVoice()
                ),
                st.select(id := name, st.name := name, cls := "form-control")
              )
            },
            div(cls := "form-group")(
              st.label(cls := "form-label")(announceAllMoves()),
              radios(
                "dgt-speech-announce-all-moves",
                List((false, trans.no.txt()), (true, trans.yes.txt()))
              ),
              st.small(cls := "form-help")(
                selectAnnouncePreference()
              )
            ),
            div(cls := "form-group")(
              st.label(cls := "form-label")(announceMoveFormat()),
              radios(
                "dgt-speech-announce-move-format",
                List(("san", "SAN (Nf6)"), ("uci", "UCI (g8f6)"))
              ),
              st.small(cls := "form-help")(
                moveFormatDescription()
              )
            ),
            "dgt-speech-keywords" pipe { name =>
              div(cls := "form-group")(
                st.label(`for` := name, cls := "form-label")(keywords()),
                st.textarea(
                  id        := name,
                  st.name   := name,
                  cls       := "form-control",
                  maxlength := 600,
                  rows      := 10
                ),
                st.small(cls := "form-help")(
                  keywordFormatDescription()
                )
              )
            }
          ),
          st.section(
            h2(debug()),
            div(cls := "form-group")(
              st.label(cls := "form-label")(verboseLogging()),
              radios(
                "dgt-verbose",
                List((false, trans.no.txt()), (true, trans.yes.txt()))
              ),
              st.small(cls := "form-help")(
                toSeeConsoleMessage()
              )
            )
          ),
          form3.submit(trans.save())
        )
      )
    )

  private def radios(name: String, options: Iterable[(Any, String)]) =
    st.group(cls := "radio")(
      options.map { v =>
        val id = s"${name}_${v._1}"
        div(
          input(
            st.id   := id,
            tpe     := "radio",
            value   := v._1.toString,
            st.name := name
          ),
          label(`for` := id)(v._2)
        )
      }.toList
    )

  private def layout(path: String, token: Option[String] = None)(body: Modifier*)(using PageContext) =
    views.html.base.layout(
      moreCss = cssTag("dgt"),
      moreJs = token.fold(jsModuleInit("dgt"))(jsModuleInit("dgt", _)),
      title = playWithDgtBoard.txt(),
      csp = defaultCsp.withAnyWs.some
    )(
      main(cls := "page-menu dgt")(
        views.html.site.bits.pageMenuSubnav(
          a(cls := path.active("index"), href := routes.DgtCtrl.index)(
            dgtBoard()
          ),
          a(cls := path.active("play"), href := routes.DgtCtrl.play)(
            trans.play()
          ),
          a(cls := path.active("config"), href := routes.DgtCtrl.config)(
            configure()
          )
        ),
        div(cls := s"page-menu__content box box-pad dgt__$path")(body)
      )
    )
