package lila.web
package ui

import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class DgtUi(helpers: Helpers):
  import helpers.{ *, given }
  import trans.dgt as trd

  private val liveChessVersion = "2.2.5+"

  def index(using Context) =
    layout("index"):
      frag(
        h1(cls := "box__top")(trd.lichessAndDgt()),
        p(trd.thisPageAllowsConnectingDgtBoard()),
        br,
        br,
        h2(trd.dgtBoardRequirements()),
        br,
        p(trd.toConnectTheDgtBoard(s"LiveChess $liveChessVersion")),
        p(
          trd.downloadHere(
            a(href := "https://www.livechesscloud.com/software/")(s"LiveChess $liveChessVersion")
          )
        ),
        p(
          trd.ifLiveChessRunningOnThisComputer(
            "LiveChess",
            a(href := "http://localhost:1982/doc/index.html")(trd.openingThisLink())
          )
        ),
        p(
          trd.ifLiveChessRunningElsewhere(
            "LiveChess",
            a(href := routes.DgtCtrl.config)(trd.configurationSection())
          )
        ),
        st.section(
          h2(trd.dgtBoardLimitations()),
          br,
          p(trd.keepPlayPageOpen()),
          p(trd.boardWillAutoConnect()),
          p(
            trd.timeControlsForCasualGames(),
            br,
            trd.timeControlsForRatedGames()
          )
        ),
        p(
          trd.whenReadySetupBoard(
            a(href := routes.DgtCtrl.play)(trans.site.play())
          )
        )
      )

  def play(token: String)(using Context) =
    layout("play", s"$token".some):
      frag(
        div(id := "dgt-play-zone")(pre(id := "dgt-play-zone-log")),
        div(cls := "dgt__play__help")(
          h2(iconTag(Icon.InfoCircle, trd.ifMoveNotDetected())),
          p(trd.checkYouHaveMadeOpponentsMove()),
          p(
            trd.asALastResort(
              a(href := routes.DgtCtrl.play)(trd.reloadThisPage())
            )
          )
        )
      )

  def config(token: Option[String])(using Context) =
    layout("config"):
      frag(
        h1(cls := "box__top")(trd.dgtConfigure()),
        form(action := routes.DgtCtrl.generateToken, method := "post")(
          st.section(
            h2(trd.lichessConnectivity()),
            if token.isDefined then
              p(cls := "text", dataIcon := Icon.Checkmark)(
                trd.validDgtOauthToken(),
                br,
                br,
                trd.dgtPlayMenuEntryAdded(
                  strong(trd.dgtBoard())
                )
              )
            else
              frag(
                p(trd.noSuitableOauthToken()),
                form3.submit(trd.clickToGenerateOne())
              )
          )
        ),
        form(cls := "form3", id := "dgt-config")(
          st.section(
            h2(trd.dgtBoardConnectivity()),
            "dgt-livechess-url".pipe { name =>
              div(cls := "form-group")(
                st.label(`for` := name, cls := "form-label")(
                  trd.webSocketUrl(s"LiveChess $liveChessVersion")
                ),
                st.input(id := name, st.name := name, cls := "form-control", required := true),
                st.small(cls := "form-help")(
                  trd.useWebSocketUrl(
                    "ws://localhost:1982/api/v1.0",
                    "LiveChess"
                  )
                )
              )
            }
          ),
          st.section(
            h2(trd.textToSpeech()),
            div(cls := "form-group")(
              p(trd.configureVoiceNarration())
            ),
            div(cls := "form-group")(
              st.label(cls := "form-label")(trd.enableSpeechSynthesis()),
              radios(
                "dgt-speech-synthesis",
                List((false, trans.site.no.txt()), (true, trans.site.yes.txt()))
              )
            ),
            "dgt-speech-voice".pipe { name =>
              div(cls := "form-group")(
                st.label(`for` := name, cls := "form-label")(
                  trd.speechSynthesisVoice()
                ),
                st.select(id := name, st.name := name, cls := "form-control")
              )
            },
            div(cls := "form-group")(
              st.label(cls := "form-label")(trd.announceAllMoves()),
              radios(
                "dgt-speech-announce-all-moves",
                List((false, trans.site.no.txt()), (true, trans.site.yes.txt()))
              ),
              st.small(cls := "form-help")(
                trd.selectAnnouncePreference()
              )
            ),
            div(cls := "form-group")(
              st.label(cls := "form-label")(trd.announceMoveFormat()),
              radios(
                "dgt-speech-announce-move-format",
                List(("san", "SAN (Nf6)"), ("uci", "UCI (g8f6)"))
              ),
              st.small(cls := "form-help")(
                trd.moveFormatDescription()
              )
            ),
            "dgt-speech-keywords".pipe { name =>
              div(cls := "form-group")(
                st.label(`for` := name, cls := "form-label")(trd.keywords()),
                st.textarea(
                  id := name,
                  st.name := name,
                  cls := "form-control",
                  maxlength := 600,
                  rows := 10
                ),
                st.small(cls := "form-help")(
                  trd.keywordFormatDescription()
                )
              )
            }
          ),
          st.section(
            h2(trd.debug()),
            div(cls := "form-group")(
              st.label(cls := "form-label")(trd.verboseLogging()),
              radios(
                "dgt-verbose",
                List((false, trans.site.no.txt()), (true, trans.site.yes.txt()))
              ),
              st.small(cls := "form-help")(
                trd.toSeeConsoleMessage()
              )
            )
          ),
          form3.submit(trans.site.save())
        )
      )

  private def radios(name: String, options: Iterable[(Any, String)]) =
    st.group(cls := "radio")(
      options.map { v =>
        val id = s"${name}_${v._1}"
        div(
          input(
            st.id := id,
            tpe := "radio",
            value := v._1.toString,
            st.name := name
          ),
          label(`for` := id)(v._2)
        )
      }.toList
    )

  private def layout(path: String, token: Option[String] = None)(using Context) =
    Page(trd.playWithDgtBoard.txt())
      .css("dgt")
      .js(token.fold(esmInit("dgt"))(esmInit("dgt", _)))
      .csp(_.withAnyWs)
      .wrap: body =>
        main(cls := "account page-menu dgt")(
          lila.ui.bits.pageMenuSubnav(
            a(cls := path.active("index"), href := routes.DgtCtrl.index)(
              trd.dgtBoard()
            ),
            a(cls := path.active("play"), href := routes.DgtCtrl.play)(
              trans.site.play()
            ),
            a(cls := path.active("config"), href := routes.DgtCtrl.config)(
              trd.configure()
            )
          ),
          div(cls := s"page-menu__content box box-pad dgt__$path")(body)
        )
