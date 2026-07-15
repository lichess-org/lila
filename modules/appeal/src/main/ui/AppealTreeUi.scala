package lila.appeal
package ui

import lila.core.id.CmsPageKey
import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }

final class AppealTreeUi(helpers: Helpers, ui: AppealUi)(
    newAppeal: AppealTopic => String => Context ?=> Frag,
    inactiveAppeals: List[Appeal] => Context ?=> Frag
):
  import helpers.{ *, given }

  import trans.appeal as tap
  import lila.ui.navTree.*
  import lila.ui.navTree.Node.*

  private def cmsPageUrl(key: String) = routes.Cms.lonePage(CmsPageKey(key))

  private def noTopicMenu(status: UserStatus, appeals: Appeal.ByTopic)(using Context): Branch =
    Branch(
      "root",
      if status.isClean then tap.cleanAllGood() else "No active appeals",
      List(
        Leaf(
          "clean-other-account",
          "I want to appeal for another account",
          frag(
            p(
              "Sorry we don't take appeals from other accounts. The appeal should come from nowhere else, but the concerned account."
            )
          )
        ).some,
        Option.when(status.warning && appeals.get(AppealTopic.warning).forall(_.isOpen)):
          Leaf(
            "clean-warning",
            "I want to discuss a warning I received",
            frag(
              p(
                "Please note that warnings are only warnings, and that your account has not been restricted currently.",
                br,
                "If you still want to file an appeal, use the following form:"
              ),
              newAppeal(AppealTopic.warning)("")
            )
          )
        ,
        Leaf(
          "clean-other-issue",
          "I have another issue to discuss",
          p(
            "This channel of communication is for appealing moderation related issues.",
            br,
            "Please use ",
            a(href := routes.Main.contact)("the contact page"),
            " or ",
            a(href := "https://discord.gg/lichess")("our Discord server"),
            " to contact us about other issues.",
            br,
            "You can also ",
            a(href := cmsPageUrl("appeal"))("find here more information about appeals.")
          )
        ).some
      ).flatten
    )

  private def newAppealFieldset(form: Frag) =
    form3.fieldset("I have read the above, and want to create an appeal", toggle = false.some)(
      cls := "form-toggle"
    )(form)

  private def engineMenu(using Context): Branch =
    val accept =
      "I accept that I used external assistance in my games."
    val deny =
      "I deny having used external assistance in my games."
    Branch(
      "root",
      tap.engineMarked(),
      List(
        Leaf(
          "engine-accept",
          accept,
          frag(
            sendUsAnAppeal,
            newAppeal(AppealTopic.cheat)(s"$accept I am sorry and I would like another chance.")
          )
        ),
        Leaf(
          "engine-deny",
          deny,
          frag(
            p("You may send us an appeal, and a moderator will review it as soon as possible."),
            p(strong("What should I write in my appeal?")),
            p("Be honest and tell us the truth."),
            p(
              "Include everything that you think matters for your case. Only send your appeal once, and don't send any additional messages if they don't add anything important to your appeal. Sending additional messages will not get your appeal dealt with any sooner."
            ),
            p(
              "It is important to be honest from the start. If at first you deny doing anything wrong, we'll treat your appeal accordingly, and we will simply disregard any changes in your position. In other words, don't try to deny things at first only to confess to something later on."
            ),
            p(
              "Note that if your appeal is denied, you are not permitted to open additional accounts on Lichess."
            ),
            newAppealFieldset(newAppeal(AppealTopic.cheat)(deny))
          )
        )
      ),
      content = tap.engineMarkedInfo(a(href := cmsPageUrl("fair-play"))(tap.fairPlay())).some
    )

  private def boostMenu(using Context): Branch =
    val accept = "I accept that I manipulated my rating."
    val acceptFull =
      "I accept that I deliberately manipulated my rating by losing games on purpose, or by playing another account that was deliberately losing games. I am sorry and I would like another chance."
    val deny =
      "I deny having manipulated my rating."
    val denyFull =
      "I deny having manipulated my rating. I have never lost rated games on purpose, or played several games with someone who does."
    Branch(
      "root",
      tap.boosterMarked(),
      List(
        Leaf(
          "boost-accept",
          accept,
          frag(
            sendUsAnAppeal,
            newAppeal(AppealTopic.boost)(acceptFull)
          )
        ),
        Leaf(
          "boost-deny",
          deny,
          frag(
            sendUsAnAppeal,
            newAppeal(AppealTopic.boost)(denyFull)
          )
        )
      ),
      content = tap.boosterMarkedInfo().some
    )

  private def muteMenu(using Context): Branch =
    val accept = "I accept that I have not followed the communication guidelines"
    val acceptFull =
      "I accept that I have not followed the communication guidelines. I will behave better in future, please give me another chance."
    val deny =
      "I have followed the communication guidelines"
    Branch(
      "root",
      tap.accountMuted(),
      List(
        Leaf(
          "mute-accept",
          accept,
          frag(
            p(
              "I accept that I have not followed the ",
              a(href := cmsPageUrl("communication-guidelines"))(
                "communication guidelines"
              ),
              ". I will behave better in future, please give me another chance."
            ),
            sendUsAnAppeal,
            newAppeal(AppealTopic.comm)(acceptFull)
          )
        ),
        Leaf(
          "mute-deny",
          deny,
          frag(
            sendUsAnAppeal,
            newAppeal(AppealTopic.comm)(deny)
          )
        )
      ),
      content = tap
        .accountMutedInfo(
          a(href := cmsPageUrl("communication-guidelines"))(tap.communicationGuidelines())
        )
        .some
    )

  private def rankBanMenu(using Context): Branch =
    val accept = "I accept that I have manipulated my account to get on the leaderboard."
    val deny =
      "I deny having manipulated my account to get on the leaderboard."
    Branch(
      "root",
      tap.excludedFromLeaderboards(),
      List(
        Leaf(
          "rankban-accept",
          accept,
          frag(
            sendUsAnAppeal,
            newAppeal(AppealTopic.rank)(accept)
          )
        ),
        Leaf(
          "rankban-deny",
          deny,
          frag(
            sendUsAnAppeal,
            newAppeal(AppealTopic.rank)(deny)
          )
        )
      ),
      content = tap.excludedFromLeaderboardsInfo().some
    )

  private def arenaBanMenu(using Context): Branch =
    val noPlay = "I have joined many arenas without playing in them"
    val noStart = "I did not move in many arenas games"
    val deny = "I have followed fair-play and arenas rules"
    Branch(
      "root",
      tap.arenaBanned(),
      List(
        Leaf(
          "arena-ban-no-play",
          noPlay,
          frag(
            sendUsAnAppeal,
            newAppeal(AppealTopic.arena)(noPlay)
          )
        ),
        Leaf(
          "arena-ban-not-starting",
          noStart,
          frag(
            sendUsAnAppeal,
            newAppeal(AppealTopic.arena)(noStart)
          )
        ),
        Leaf(
          "arena-ban-deny",
          deny,
          frag(
            sendUsAnAppeal,
            newAppeal(AppealTopic.arena)(deny)
          )
        )
      )
    )

  private def hiddenBlogMenu(using Context): Branch =
    val accept =
      "I accept that I have broken the blog rules"
    val deny =
      "I deny having broken the blog rules."
    Branch(
      "root",
      tap.hiddenBlog(),
      List(
        Leaf(
          "hidden-blog-accept",
          accept,
          frag(
            sendUsAnAppeal,
            newAppeal(AppealTopic.blog)(accept)
          )
        ),
        Leaf(
          "hidden-blog-deny",
          deny,
          frag(
            sendUsAnAppeal,
            newAppeal(AppealTopic.blog)(deny)
          )
        )
      ),
      content = tap.hiddenBlogInfo(a(href := cmsPageUrl("blog-etiquette"))(tap.blogRules())).some
    )

  private def prizebanMenu(using Context): Branch =
    val prizebanExpired = "My ban duration has expired, as I was informed by moderators."
    val deny = "I reject any allegation of wrongdoing that may have prompted a prizeban."
    Branch(
      "root",
      tap.prizeBanned(),
      List(
        Leaf(
          "prizeban-expired",
          prizebanExpired,
          frag(
            sendUsAnAppeal,
            newAppeal(AppealTopic.prize)(prizebanExpired)
          )
        ),
        Leaf(
          "prizeban-deny",
          deny,
          frag(
            sendUsAnAppeal,
            newAppeal(AppealTopic.prize)(deny)
          )
        )
      )
    )

  private def playbanMenu(using Context): Branch =
    Branch(
      "root",
      tap.playTimeout(),
      List(
        Leaf(
          "playban-abort",
          "For having aborted too many games.",
          frag(
            p(
              "We understand your frustration, but temporary play bans for aborting too many games are necessary. It's very annoying for your opponent when the game gets aborted and we have to discourage it."
            ),
            p("A few things we can suggest are:"),
            ul(
              li("Don't send a challenge if you don't want to play and then abort the game."),
              li(
                "If you don't want to face lower or higher rated opponents, set a rating range on your seek."
              ),
              li(
                "Don't abort games if you want to have a particular color, you have to play with both colors."
              )
            )
          )
        ),
        Leaf(
          "playban-timeout",
          "For letting my game clock time run out.",
          p(
            "We understand your frustration, but temporary play bans for stalling in games are necessary, it can be very frustrating for opponents to waste time in lost positions before resigning."
          )
        ),
        Leaf(
          "playban-disconnect",
          "For frequently disconnecting from games.",
          frag(
            p(
              "We understand your frustration, but temporary play bans for losing connection are necessary, even if you don't disconnect on purpose. It's very annoying to suddenly lose your opponent during a game and we have to discourage it."
            ),
            p(
              "The only thing we can suggest to you is that you try to get a better connection or play longer time-control games that are more forgiving of disconnections."
            )
          )
        )
      )
    )

  private def altScreen(using Context) = div(cls := "leaf")(
    h2(tap.closedByModerators()),
    div(cls := "content")(
      p("Did you create multiple accounts? If so, remember that you promised not to, on the sign up page."),
      p(
        "If you violated the terms of service on a previous account, then you are not allowed to make a new one, ",
        "unless it was explicitly allowed by the moderation team during an appeal."
      ),
      p(
        "If you never violated the terms of service, and didn't make several accounts, then you can appeal this account closure:"
      )
    ),
    newAppeal(AppealTopic.close)("")
  )

  def page(topic: Option[AppealTopic], status: UserStatus, appeals: Appeal.ByTopic)(using ctx: Context) =
    ui.page("Appeal a moderation decision"):
      main(cls := "page page-small appeal force-ltr")(
        div(cls := "box box-pad")(
          h1(cls := "box__top")("Appeal"),
          div(
            cls := List(
              "nav-tree" -> true,
              "appeal-marked" -> topic.exists(AppealTopicApi.relevant.contains),
              "appeal-clean" -> status.isClean
            )
          )(
            topic.match
              case Some(AppealTopic.close) => altScreen
              case t =>
                val menu = t.flatMap(topicMenu.get) | (_ ?=> noTopicMenu(status, appeals))
                renderNode(menu, none, forceLtr = true)
          ),
          div(cls := "appeal__rules")(
            p(cls := "text warning-closure", dataIcon := Icon.CautionTriangle)(
              trans.site.closingAccountWithdrawAppeal()
            ),
            p(cls := "text", dataIcon := Icon.InfoCircle)(trans.contact.doNotMessageModerators()),
            p(
              a(cls := "text", dataIcon := Icon.InfoCircle, href := cmsPageUrl("appeal"))(
                "Read more about the appeal process"
              )
            ),
            p(
              a(cls := "text", dataIcon := Icon.Download, href := routes.Account.data)("Export personal data")
            )
          )
        ),
        inactiveAppeals(appeals.values.toList)
      )

  private val topicMenu: Map[AppealTopic, Context ?=> Branch] = Map(
    AppealTopic.cheat -> engineMenu,
    AppealTopic.boost -> boostMenu,
    AppealTopic.comm -> muteMenu,
    AppealTopic.play -> playbanMenu,
    AppealTopic.rank -> rankBanMenu,
    AppealTopic.arena -> arenaBanMenu,
    AppealTopic.prize -> prizebanMenu,
    AppealTopic.blog -> hiddenBlogMenu
  )

  private val sendUsAnAppeal = frag(
    p("Send us an appeal, and a moderator will review it as soon as possible."),
    p("Add any relevant information that could help us process your appeal."),
    p("Please be honest, concise, and on point.")
  )
