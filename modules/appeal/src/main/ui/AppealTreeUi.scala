package lila.appeal
package ui

import lila.core.id.CmsPageKey
import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }

final class AppealTreeUi(helpers: Helpers, ui: AppealUi)(
    newAppeal: AppealTopic => String => Context ?=> Frag,
    inactiveAppeals: List[Appeal] => (Context, Me) ?=> Frag
):
  import helpers.{ *, given }

  import trans.appeal as tap
  import lila.ui.navTree.*
  import lila.ui.navTree.Node.*

  private def cmsPageUrl(key: String) = routes.Cms.lonePage(CmsPageKey(key))

  private def screeningStepsThenLeaf(id: String, topic: AppealTopic, name: Frag, content: Frag): Branch =
    val withAccounts = AppealTopicApi.requiresAccounts(topic)
    val leaf = Leaf(id, name, content, showBack = false)
    val afterInfoTree: Node =
      if withAccounts then
        Branch(
          s"accounts-$id",
          "Declare accounts",
          List(leaf),
          content = accountsForm(id).some,
          showLinks = false,
          showBack = false
        )
      else leaf
    Branch(
      s"important-info-$id",
      name,
      List(afterInfoTree),
      content = importantInfo(id, nextId = if withAccounts then s"accounts-$id" else id).some,
      showLinks = false,
      showBack = false
    )

  private def importantInfo(id: String, nextId: String) =
    div(cls := "appeal-info")(
      div(cls := "appeal-info__terms")(
        p(
          "Providing accurate details in your appeal is the best way to ensure a quick and fair review. Misleading information may result in delays or affect the outcome of your request."
        ),
        p(
          "The appeal will be reviewed by a team of expert human moderators who will be different from those who originally took action against your account."
        ),
        p("If you close your account after making an appeal, we will consider the appeal as withdrawn."),
        p(
          "If you use foul, inappropriate or offensive language in your appeal, we reserve the right to not review, and deny your appeal on that basis."
        ),
        p(
          "The purpose of the appeal process is to verify whether the correct action was taken based on our policies. As such, this is a final review rather than an open discussion, and we will not look to engage in debate regarding the evidence or our policies in themselves."
        ),
        p("You will normally receive a response to your appeal within 72 hours."),
        p(
          "See more at our ",
          a(href := cmsPageUrl("appeal"))("FAQ for Appeals"),
          "."
        )
      ),
      div(cls := "appeal-info__agree form-check__container")(
        form3.nativeCheckbox(s"appeal-info-agree-$id", "agree", checked = false),
        label(cls := "form-label", `for` := s"appeal-info-agree-$id")(
          "I have read and agree to the terms stated above"
        )
      ),
      div(cls := "form-actions")(
        a(href := s"#help-$nextId", cls := "button appeal-info__continue disabled")("Continue")
      )
    )

  private def accountsForm(id: String) =
    div(cls := "appeal-accounts", attr("data-leaf") := id)(
      div(
        "Please share with us the usernames of your other accounts. It does not matter if they are closed now."
      ),
      div(cls := "appeal-accounts__choice")(
        div(cls := "appeal-accounts__option")(
          input(
            tpe := "radio",
            st.name := s"appeal-accounts-choice-$id",
            st.id := s"appeal-accounts-only-$id",
            value := "only",
            cls := "appeal-accounts__only"
          ),
          label(`for` := s"appeal-accounts-only-$id")("I have created only this account")
        ),
        div(cls := "appeal-accounts__option")(
          input(
            tpe := "radio",
            st.name := s"appeal-accounts-choice-$id",
            st.id := s"appeal-accounts-others-$id",
            value := "others",
            cls := "appeal-accounts__others-radio"
          ),
          label(`for` := s"appeal-accounts-others-$id")("I have created the following accounts")
        )
      ),
      textarea(
        cls := "appeal-accounts__others",
        rows := 3,
        disabled,
        placeholder := "Usernames of your other accounts"
      ),
      div(cls := "appeal-accounts__forgotten form-check__container")(
        form3.nativeCheckbox(s"appeal-accounts-forgotten-$id", "forgotten", checked = false),
        label(cls := "form-label", `for` := s"appeal-accounts-forgotten-$id")(
          "There are more accounts but I don't remember their usernames anymore"
        )
      ),
      div(
        "Remember that an account can only be used by one person at all times.",
        br,
        "If members of your household play on Lichess please share their usernames and tell which account belongs to which person."
      ),
      textarea(
        cls := "appeal-accounts__household",
        rows := 3,
        placeholder := "Household accounts (optional)"
      ),
      div(cls := "form-actions")(
        a(href := s"#help-$id", cls := "button appeal-accounts__continue disabled")("Continue")
      )
    )

  private def noTopicMenu(status: UserStatus, appeals: UserAppeals)(using Context): Branch =
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
        Option.when(status.modMessage && appeals.get(AppealTopic.warning).forall(_.isOpen)):
          screeningStepsThenLeaf(
            "clean-warning",
            AppealTopic.warning,
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
        Option.when(status.chatTimeout && appeals.get(AppealTopic.chat).forall(_.isOpen)):
          screeningStepsThenLeaf(
            "clean-chat-timeout",
            AppealTopic.chat,
            "I want to discuss a chat timeout I received",
            frag(
              p(
                "Please note that chat timeouts are only temporary restrictions, and that your account has not been permanently restricted currently.",
                br,
                "If you still want to file an appeal, use the following form:"
              ),
              newAppeal(AppealTopic.chat)("")
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

  private val engineDenyContent = frag(
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
    )
  )

  private def engineMenu(using Context): Branch =
    val accept =
      "I accept that I used external assistance in my games."
    val deny =
      "I deny having used external assistance in my games."
    Branch(
      "root",
      tap.engineMarked(),
      List(
        screeningStepsThenLeaf(
          "engine-accept",
          AppealTopic.cheat,
          accept,
          frag(
            sendUsAnAppeal,
            newAppeal(AppealTopic.cheat)(accept)
          )
        ),
        screeningStepsThenLeaf(
          "engine-deny",
          AppealTopic.cheat,
          deny,
          frag(
            engineDenyContent,
            newAppeal(AppealTopic.cheat)(deny)
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
        screeningStepsThenLeaf(
          "boost-accept",
          AppealTopic.boost,
          accept,
          frag(sendUsAnAppeal, newAppeal(AppealTopic.boost)(acceptFull))
        ),
        screeningStepsThenLeaf(
          "boost-deny",
          AppealTopic.boost,
          deny,
          frag(sendUsAnAppeal, newAppeal(AppealTopic.boost)(denyFull))
        )
      ),
      content = frag(
        p(strong("Rating manipulation")),
        p("This includes, but isn’t limited to:"),
        ul(
          li(strong("Sandbagging:"), " deliberately losing rated games"),
          li(
            strong("Boosting:"),
            " playing an excessive number of rated games against someone who was deliberately losing."
          )
        ),
        p(strong("Account sharing")),
        p(
          "Your account can only be used by you. We consider your account to have violated the rules if any of the following occur:"
        ),
        ul(
          li("someone else plays games using your account, with or without your permission"),
          li("you asked another person’s advice during rated games.")
        ),
        p("You are responsible for all activity in your account.")
      ).some
    )

  private def muteMenu(using Context): Branch =
    val accept = "I accept that I have not followed the communication guidelines"
    val acceptFull =
      "I accept that I have not followed the communication guidelines. I will behave better in future, please give me another chance."
    val deny =
      "I have followed the community guidelines and don’t understand why I was muted"
    Branch(
      "root",
      tap.accountMuted(),
      List(
        screeningStepsThenLeaf(
          "mute-accept",
          AppealTopic.comm,
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
        screeningStepsThenLeaf(
          "mute-deny",
          AppealTopic.comm,
          deny,
          frag(sendUsAnAppeal, newAppeal(AppealTopic.comm)(deny))
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
    val chooseAccount = "I want to choose which account appears on leaderboards"
    Branch(
      "root",
      tap.excludedFromLeaderboards(),
      List(
        screeningStepsThenLeaf(
          "rankban-accept",
          AppealTopic.rank,
          accept,
          frag(sendUsAnAppeal, newAppeal(AppealTopic.rank)(accept))
        ),
        screeningStepsThenLeaf(
          "rankban-deny",
          AppealTopic.rank,
          deny,
          frag(sendUsAnAppeal, newAppeal(AppealTopic.rank)(deny))
        ),
        screeningStepsThenLeaf(
          "rankban-choose",
          AppealTopic.rank,
          chooseAccount,
          frag(sendUsAnAppeal, newAppeal(AppealTopic.rank)(chooseAccount))
        )
      ),
      content = tap.onlyOneAccountOnLeaderboards().some
    )

  private def arenaBanMenu(using Context): Branch =
    val noPlay = "I have joined many arenas without playing in them"
    val noStart = "I did not move in many arenas games"
    val deny = "I have followed fair-play and arenas rules"
    Branch(
      "root",
      tap.arenaBanned(),
      List(
        screeningStepsThenLeaf(
          "arena-ban-no-play",
          AppealTopic.arena,
          noPlay,
          frag(sendUsAnAppeal, newAppeal(AppealTopic.arena)(noPlay))
        ),
        screeningStepsThenLeaf(
          "arena-ban-not-starting",
          AppealTopic.arena,
          noStart,
          frag(sendUsAnAppeal, newAppeal(AppealTopic.arena)(noStart))
        ),
        screeningStepsThenLeaf(
          "arena-ban-deny",
          AppealTopic.arena,
          deny,
          frag(sendUsAnAppeal, newAppeal(AppealTopic.arena)(deny))
        )
      ),
      content = frag(
        p("We do not allow:"),
        ul(
          li("joining multiple arenas at the same time and then not playing in them"),
          li("joining an arena and repeatedly losing games by not making a move (for any reason)")
        )
      ).some
    )

  private def hiddenBlogMenu(using Context): Branch =
    val accept =
      "I regret my actions and would like to appeal"
    val deny =
      "I don’t understand what I did wrong and would like to appeal"
    Branch(
      "root",
      tap.blogRestriction(),
      List(
        screeningStepsThenLeaf(
          "hidden-blog-accept",
          AppealTopic.blog,
          accept,
          frag(sendUsAnAppeal, newAppeal(AppealTopic.blog)(accept))
        ),
        screeningStepsThenLeaf(
          "hidden-blog-deny",
          AppealTopic.blog,
          deny,
          frag(sendUsAnAppeal, newAppeal(AppealTopic.blog)(deny))
        )
      ),
      content = tap.blogRestrictionInfo(a(href := cmsPageUrl("blog-etiquette"))(tap.blogEtiquette())).some
    )

  private def prizebanMenu(using Context): Branch =
    val prizebanExpired = "My ban duration has expired and I want it to be lifted"
    val deny = "I reject any allegation of wrongdoing that may have prompted a prizeban."
    Branch(
      "root",
      tap.prizeBanned(),
      List(
        screeningStepsThenLeaf(
          "prizeban-expired",
          AppealTopic.prize,
          prizebanExpired,
          frag(sendUsAnAppeal, newAppeal(AppealTopic.prize)(prizebanExpired))
        ),
        screeningStepsThenLeaf(
          "prizeban-deny",
          AppealTopic.prize,
          deny,
          frag(sendUsAnAppeal, newAppeal(AppealTopic.prize)(deny))
        )
      ),
      content = frag(
        p(
          "This is due to a fair play restriction in your previous account and your status of titled player."
        ),
        p(
          "You were informed of this during the appeal of the previous account and/or via direct message by a public moderator."
        ),
        p(
          "If you disagree with the restriction or believe its term has expired and want it to be removed, send an appeal."
        )
      ).some
    )

  private def reportbanMenu(using Context): Branch =
    val accept =
      "I regret my mistakes and will behave better in future, please give me a second chance"
    val deny =
      "I reject any allegations of wrongdoing that may have prompted a reportban"
    Branch(
      "root",
      tap.reportBanned(),
      List(
        screeningStepsThenLeaf(
          "reportban-accept",
          AppealTopic.report,
          accept,
          frag(sendUsAnAppeal, newAppeal(AppealTopic.report)(accept))
        ),
        screeningStepsThenLeaf(
          "reportban-deny",
          AppealTopic.report,
          deny,
          frag(sendUsAnAppeal, newAppeal(AppealTopic.report)(deny))
        )
      ),
      content = tap.reportBannedInfo().some
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

  private def altMenu(using Context): Branch =
    Branch(
      "root",
      tap.closedByModerators(),
      List(
        screeningStepsThenLeaf(
          "close-appeal",
          AppealTopic.close,
          "I want to appeal",
          newAppeal(AppealTopic.close)("")
        )
      ),
      content = frag(
        p(
          "On the sign-up page you agreed not to create an excessive number of accounts, generally not more than 3. Violating this term is considered abuse of infrastructure."
        ),
        p(
          "If you violated our Terms of Service in a previous account and tried to open a new one, this is considered ban evasion. In order to keep using Lichess you must obtain explicit permission by moderators."
        ),
        p("If you have done nothing wrong and believe this is a mistake, send an appeal.")
      ).some
    )

  def page(topic: Option[AppealTopic], status: UserStatus, appeals: UserAppeals)(using Context, Me) =
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
            renderNode(
              topic.flatMap(topicMenu.get) | (_ ?=> noTopicMenu(status, appeals)),
              none,
              forceLtr = true
            )
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
        inactiveAppeals(appeals.value.values.toList)
      )

  private val topicMenu: Map[AppealTopic, Context ?=> Branch] = Map(
    AppealTopic.cheat -> engineMenu,
    AppealTopic.boost -> boostMenu,
    AppealTopic.comm -> muteMenu,
    AppealTopic.play -> playbanMenu,
    AppealTopic.rank -> rankBanMenu,
    AppealTopic.arena -> arenaBanMenu,
    AppealTopic.prize -> prizebanMenu,
    AppealTopic.report -> reportbanMenu,
    AppealTopic.blog -> hiddenBlogMenu,
    AppealTopic.close -> altMenu
  )

  private val sendUsAnAppeal = frag(
    p("Send us an appeal, and a moderator will review it as soon as possible."),
    p("Add any relevant information that could help us process your appeal."),
    p("Please be honest, concise, and on point.")
  )
