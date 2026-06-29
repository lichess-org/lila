package lila.study
package ui

import play.api.data.Form
import scalalib.paginator.Paginator

import lila.core.study.{ StudyOrder, StudyFormat }
import lila.study.Study.WithChaptersAndLiked
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class StudyListUi(helpers: Helpers, bits: StudyBits):
  import helpers.{ *, given }
  import trans.study as trs

  def all(pag: Paginator[WithChaptersAndLiked], order: StudyOrder, format: Option[StudyFormat] = None)(using
      Context
  ) =
    page(
      title = trs.allStudies.txt(),
      active = StudyGroup.all,
      order = order,
      pag = pag,
      searchFilter = "",
      url = routes.Study.all(_),
      format = format
    )
      .hrefLangs(lila.ui.LangPath(routes.Study.allDefault()))

  def byOwner(
      pag: Paginator[WithChaptersAndLiked],
      order: StudyOrder,
      owner: User,
      format: Option[StudyFormat] = None
  )(using Context) =
    page(
      title = trs.studiesCreatedByX.txt(owner.titleUsername),
      active = StudyGroup.byOwner,
      order = order,
      pag = pag,
      searchFilter = s"owner:${owner.username}",
      url = routes.Study.byOwner(owner.username, _),
      format = format
    )

  def mine(
      pag: Paginator[WithChaptersAndLiked],
      order: StudyOrder,
      topics: StudyTopics,
      format: Option[StudyFormat] = None
  )(using
      ctx: Context,
      me: Me
  ) =
    page(
      title = trs.myStudies.txt(),
      active = StudyGroup.mine,
      order = order,
      pag = pag,
      searchFilter = s"owner:${me.username}",
      url = routes.Study.mine(_),
      topics = topics.some,
      format = format
    )

  def mineLikes(
      pag: Paginator[WithChaptersAndLiked],
      order: StudyOrder,
      format: Option[StudyFormat] = None
  )(using Context) =
    page(
      title = trs.myFavoriteStudies.txt(),
      active = StudyGroup.mineLikes,
      order = order,
      pag = pag,
      searchFilter = "",
      url = routes.Study.mineLikes(_),
      format = format
    )

  def mineMember(
      pag: Paginator[WithChaptersAndLiked],
      order: StudyOrder,
      topics: StudyTopics,
      format: Option[StudyFormat] = None
  )(using
      ctx: Context,
      me: Me
  ) =
    page(
      title = trs.studiesIContributeTo.txt(),
      active = StudyGroup.mineMember,
      order = order,
      pag = pag,
      searchFilter = s"member:${me.username}",
      url = routes.Study.mineMember(_),
      topics = topics.some,
      format = format
    )

  def minePublic(
      pag: Paginator[WithChaptersAndLiked],
      order: StudyOrder,
      format: Option[StudyFormat] = None
  )(using Context)(using me: Me) =
    page(
      title = trs.myPublicStudies.txt(),
      active = StudyGroup.minePublic,
      order = order,
      pag = pag,
      searchFilter = s"owner:${me.username}",
      url = routes.Study.minePublic(_),
      format = format
    )

  def minePrivate(
      pag: Paginator[WithChaptersAndLiked],
      order: StudyOrder,
      format: Option[StudyFormat] = None
  )(using Context)(using me: Me) =
    page(
      title = trs.myPrivateStudies.txt(),
      active = StudyGroup.minePrivate,
      order = order,
      pag = pag,
      searchFilter = s"owner:${me.username}",
      url = routes.Study.minePrivate(_),
      format = format
    )

  def search(
      pag: Paginator[WithChaptersAndLiked],
      order: StudyOrder,
      text: String,
      format: Option[StudyFormat] = None
  )(using Context) =
    val url = (o: StudyOrder) => routes.Study.search(text, 1, o.some, format)
    Page(text)
      .css("analyse.study.index")
      .js(infiniteScrollEsmInit):
        main(cls := "page-menu")(
          menu(StudyGroup.search, Some(order), format = format),
          main(cls := "page-menu__content study-index box")(
            div(cls := "box__top")(
              searchForm(trans.search.search.txt(), text, order, format),
              bits.orderSelect(
                order,
                StudyGroup.search,
                url = url
              ),
              bits.formatToggle(url(order).url, format),
              bits.newForm()
            ),
            paginate(pag, routes.Study.search(text, 1, order.some), format)
          )
        )

  private def page(
      title: String,
      active: StudyGroup,
      order: StudyOrder,
      pag: Paginator[WithChaptersAndLiked],
      url: StudyOrder => Call,
      searchFilter: String,
      topics: Option[StudyTopics] = None,
      format: Option[StudyFormat]
  )(using Context): Page =
    Page(title)
      .css("analyse.study.index")
      .js(infiniteScrollEsmInit):
        main(cls := "page-menu")(
          menu(active, Some(order), topics.so(_.value), format),
          main(cls := "page-menu__content study-index box")(
            div(cls := "box__top")(
              searchForm(title, s"$searchFilter${searchFilter.nonEmpty.so(" ")}", order, format),
              bits.orderSelect(order, active, url, format),
              bits.formatToggle(url(order).url, format),
              bits.newForm()
            ),
            topics.map: ts =>
              div(cls := "box__pad")(topic.topicsList(ts, StudyOrder.mine)),
            paginate(pag, url(order), format)
          )
        )

  private def paginate(
      pager: Paginator[WithChaptersAndLiked],
      url: Call,
      format: Option[StudyFormat]
  )(using
      Context
  ) =
    val baseUrl = format.fold(url.url): f =>
      addQueryParam(url.url, "format", f.name)
    val nextPageUrl = (np: Int) =>
      addQueryParam(
        baseUrl,
        "page",
        np.toString
      )
    if pager.currentPageResults.isEmpty then
      div(cls := "nostudies")(
        iconTag(Icon.StudyBoard),
        p(trs.noneYet())
      )
    else if format.contains(StudyFormat.compact) then
      div(cls := "studies compact infinite-scroll")(
        pager.currentPageResults.map { s =>
          div(cls := "study compact paginated")(
            span(cls := "study__icon")(
              s.study.flair
                .map(iconFlair)
                .getOrElse(iconTag(Icon.StudyBoard))
            ),
            a(href := routes.Study.show(s.study.id))(s.study.name.value)
          )
        },
        pagerNext(pager, nextPageUrl)
      )
    else
      div(cls := "studies list infinite-scroll")(
        pager.currentPageResults.map { s =>
          div(cls := "study paginated")(bits.widget(s))
        },
        pagerNext(pager, np => nextPageUrl(np))
      )

  def menu(active: StudyGroup, order: Option[StudyOrder], topics: List[StudyTopic] = Nil, format: Option[StudyFormat] = None)(using
      ctx: Context
  ) =
    def defaultOrder(group: StudyGroup): Option[StudyOrder] =
      if group == StudyGroup.search || group == StudyGroup.staffPicks then None
      else if group.isTopic then Some(StudyOrder.mine)
      else if group.isPersonal then Some(StudyOrder.updated)
      else Some(StudyOrder.hot)
    def newOrder(newGroup: StudyGroup): StudyOrder =
      (if defaultOrder(active).forall(order.contains) then defaultOrder(newGroup) else order)
        .getOrElse(Orders.default)
    def activeCls(group: StudyGroup) = cls := (
      group match
        case StudyGroup.topic(None) => active.isTopic
        case _ => group == active
    ).option("active")
    lila.ui.bits.pageMenuSubnav(
      a(activeCls(StudyGroup.all), href := bits.addFormatToUrl(routes.Study.all(newOrder(StudyGroup.all)).url, format))(trs.allStudies()),
      ctx.isAuth.option(bits.authLinks(activeCls, newOrder, format)),
      a(activeCls(StudyGroup.topic(None)), href := bits.addFormatToUrl(routes.Study.topics.url, format))(trs.topics()),
      topics.map: topic =>
        val group = StudyGroup.topic(topic.some)
        a(activeCls(group), href := bits.addFormatToUrl(routes.Study.byTopic(topic.value, newOrder(group)).url, format))(

          topic.value
        )
      ,
      a(activeCls(StudyGroup.staffPicks), href := routes.Study.staffPicks)("Staff picks"),
      a(
        dataIcon := Icon.InfoCircle,
        href := "/@/lichess/blog/study-chess-the-lichess-way/V0KrLSkA"
      )(trs.whatAreStudies())
    )

  def searchForm(placeholder: String, value: String, order: StudyOrder, format: Option[StudyFormat] = None) =
    logger.info(s"searchForm: placeholder=$placeholder, value=$value, order=$order, format=$format")
    form(cls := "search", action := routes.Study.search(), method := "get")(
      form3.hidden("order", order.key),
      format.map(f => form3.hidden("format", f.name)),
      input(name := "q", st.placeholder := placeholder, st.value := value, enterkeyhint := "search"),
      submitButton(cls := "button", dataIcon := Icon.Search)
    )

  object topic:

    def topicsList(topics: StudyTopics, order: StudyOrder = Orders.default) =
      div(cls := "topic-list")(
        topics.value.map: t =>
          a(href := routes.Study.byTopic(t.value, order))(t.value)
      )

    def index(popular: StudyTopics, mine: Option[StudyTopics], myForm: Option[Form[?]])(using Context) =
      Page(trans.study.topics.txt())
        .css("analyse.study.index", "bits.form3", "bits.tagify")
        .js(Esm("analyse.study.topic.form")):
          main(cls := "page-menu")(
            menu(StudyGroup.topic(None), Some(StudyOrder.mine), mine.so(_.value)),
            main(cls := "page-menu__content study-topics box box-pad")(
              h1(cls := "box__top")(trans.study.topics()),
              myForm.map { form =>
                frag(
                  h2(trans.study.myTopics()),
                  postForm(cls := "form3", action := routes.Study.topics)(
                    form3.textarea(form("topics"))(rows := 10, attrData("max") := StudyTopics.userMax),
                    form3.submit(trans.site.save())
                  )
                )
              },
              h2(trans.study.popularTopics()),
              topicsList(popular)
            )
          )

    def show(
        topic: StudyTopic,
        pag: Paginator[WithChaptersAndLiked],
        order: StudyOrder,
        myTopics: Option[StudyTopics],
        format: Option[StudyFormat] = None
    )(using Context) =
      val active = StudyGroup.topic(topic.some)
      val url = (o: StudyOrder) => routes.Study.byTopic(topic.value, o)
      Page(topic.value)
        .css("analyse.study.index")
        .js(infiniteScrollEsmInit):
          main(cls := "page-menu")(
            menu(active, Some(order), myTopics.so(_.value)),
            main(cls := "page-menu__content study-index box")(
              boxTop(
                h1(topic.value),
                bits.orderSelect(order, active, url, format),
                bits.formatToggle(url(order).url, format),
                bits.newForm()
              ),
              myTopics.ifTrue(order == StudyOrder.mine).map { ts =>
                div(cls := "box__pad")(
                  topicsList(ts, StudyOrder.mine)
                )
              },
              paginate(pag, url(order), format)
            )
          )
