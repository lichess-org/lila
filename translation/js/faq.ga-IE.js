"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.faq)window.i18n.faq={};let i=window.i18n.faq;i['accounts']="Cuntais";i['acplExplanation']="Is é an ceinticeithearnach an t-aonad tomhais a úsáidtear i bhficheall mar léiriú ar an mbuntáiste. Tá ceinticeithearnach cothrom le 1/100ú ceithearnach. Dá bhrí sin 100 ceinticeithearnach = 1 ceithearnach. Níl aon ról foirmiúil ag na luachanna seo sa chluiche ach tá siad úsáideach d’imreoirí, agus riachtanach i bhficheall ríomhaire, chun suíomhanna a mheas.\n\nCaillfidh an beart is fearr ó ríomhaire nialas ceinticeithearnach, ach beidh meath ar an suíomh, arna thomhas i gcéadchosach, mar thoradh ar ghluaiseachtaí níos lú.\n\nIs féidir an luach seo a úsáid mar caighdeán na himeartha. An níos lú ceinticeithearnach a chailleann duine in aghaidh an ghluaiseachta, is láidre an imirt.\n\nTá an anailís ríomhaire ar Lichess faoi thiomáint ag Stockfish.";i['aHourlyBulletTournament']="comórtas Bullet gach uair an chloig";i['areThereWebsitesBasedOnLichess']="An bhfuil suíomhanna Gréasáin bunaithe ar Lichess?";i['asWellAsManyNMtitles']="go leor máistir-teidil náisiúnta";i['basedOnGameDuration']=s("Tá rialtán ama Lichess bunaithe ar fhad cluiche measta = %1$s\nMar shampla, is é fad measta cluiche 5+3 ná 5 × 60 + 40 × 3 = 420 soicind.");i['beingAPatron']="a bheith ina phátrún";i['beInTopTen']="bí sa 10 is fearr sa rátáil seo.";i['breakdownOfOurCosts']="miondealú ar ár gcostais";i['canIbecomeLM']="An féidir liom an teideal Máistir Lichess (LM) a fháil?";i['canIChangeMyUsername']="An bhfuil cead agam m’ainm úsáideora a athrú?";i['configure']="cumraigh";i['connexionLostCanIGetMyRatingBack']="Chaill mé cluiche de bharr moille/dícheangail. An féidir liom mo chuid pointí rátála a fháil ar ais?";i['discoveringEnPassant']="Cén fáth gur féidir le ceithearnach ceithearnach eile a mharú nuair nach bhfuil sé díreach ar fiar? (en passant)";i['displayPreferences']="taispeáin roghanna";i['durationFormula']="(am tosaigh an chloig) + 40 × (incrimint clog)";i['eightVariants']="8 leagan fichille";i['enableDisableNotificationPopUps']="Cumasaigh fógraí mír aníos nó díchumasaigh iad?";i['enableZenMode']=s("Cumasaigh mód Zen sa %1$s, nó trí %2$s a bhrú i lár cluiche.");i['explainingEnPassant']=s("Is beart dlíthiúil é seo d\\'arbh ainm \\\"en passant\\\". Tugann an t-alt Wikipedia %1$s.\n\nDéantar cur síos air i gcuid 3.7 (d) den %2$s:\n\nEn passant (ón bhFraincis: \\\"ag trasnú thar” [an ceithearnach]). Is\ngabh speisialta é an beart “En passant” ina bhfuil ceithearnach ábalta\nceithearnach eile a thógáil, faoi rialacha sainiúil. Téann sé mar seo: \n\nIs féidir le ceithearnach bogadh dhá chearnóg nó cearnóg amháin ar aghaidh sa chéad bheart. Má dhéanann imreoir beart le dhá chearnóg (an chéad gluais ón ceithearnach) agus tá ceithearnach a chéile comhraic díreach in aice leis ar an\nchúigiú rang is féidir leis an céile comhraic an ceithearnach a thógáil fiarthrasna más mian. \nSa riail “en passant” \n1. Ní féidir fanacht. Más maith leat é a thógáil “en passant”, caithfear é a\ndhéanamh díreach sa chéad bheart eile.\n2. Is iad na ceithearnaigh amháin atá in ann an riail seo a úsáid.\n3. Cosúil le aon tógáil eile, tá an beart seo deonach.\n4. Is féidir an beart “en passant” a dhéanamh níos mó ná uair amháin sa\nchluiche.\n\nTá an beart seo neamhchoitianta i bhficheall toisc go bhfuil tú ag dul\nisteach i gcearnóg éagsúil ón gcearnóg ina bhfuil an píosa atá maraithe\nsuite. I nodaireacht fichille scríobhtar “ep”.\n\nFéach ar %3$s ar an mbeart seo le haghaidh cleachtadh leis.");i['fairPlay']="Cothrom na Féinne";i['fairPlayPage']="leathanach cothrom na Féinne";i['faqAbbreviation']="CCanna";i['fideHandbookX']=s("Lámhleabhair FIDE %s");i['findMoreAndSeeHowHelp']=s("Is féidir tuilleadh a fháil amach faoi %1$s (%2$s san áireamh). Más mian leat cabhrú le Lichess trí do chuid ama agus scileanna a chur ar fáil go deonach, tá go leor %3$s ann.");i['frequentlyAskedQuestions']="Ceisteanna Coitianta";i['gameplay']="Imirt chomhoibritheach";i['goldenZeeExplanation']="Bhí ZugAddict ag sruthú agus le 2 uair an chloig anuas bhí sé ag iarraidh an ruaig a chur ar A.I. leibhéal 8 i gcluiche 1+0, gan rath. Dúirt Thibault leis, dá n-éireodh leis é a dhéanamh, go bhfaigheadh sé trófaí uathúil. Uair an chloig ina dhiaidh sin, bhris sé Stockfish, agus tugadh onóir don gheallúint.";i['goodIntroduction']="réamhrá maith";i['guidelines']="treoirlínte";i['havePlayedARatedGameAtLeastOneWeekAgo']="imir cluiche rátáilte laistigh den tseachtain seo caite don rátáil seo,";i['havePlayedMoreThanThirtyGamesInThatRating']="imir 30 cluiche rátáilte ar a laghad i rátáil ar leith,";i['hearItPronouncedBySpecialist']="Éist le fuaimniú speisialtóir.";i['howBulletBlitzEtcDecided']="Conas a chinntear Bullet, Blitz agus rialuithe ama eile?";i['howCanIBecomeModerator']="Conas is féidir liom a bheith i mo mhodhnóir?";i['howCanIContributeToLichess']="Conas is féidir liom tacú le Lichess?";i['howDoLeaderoardsWork']="Conas a oibríonn céimeanna agus cláracha ceann riain?";i['howToHideRatingWhilePlaying']="Conas rátálacha a cuir i bhfolach agus tú ag imirt?";i['howToThreeDots']="Conas...";i['inferiorThanXsEqualYtimeControl']=s("< %1$ss = %2$s");i['inOrderToAppearsYouMust']=s("Chun na %1$s a fháil caithfidh tú:");i['insufficientMaterial']="Ag cailleadh ar an clog, cluichí cothroma agus gan go leoir píosaí";i['isCorrespondenceDifferent']="An bhfuil ficheall comhfhreagrais difriúil ó ghnáth-fhicheall?";i['leavingGameWithoutResigningExplanation']="Má ghiorraíonn / fágann do chéile comhraic cluichí go minic, faigheann siad “cosc ar imirt”, rud a chiallaíonn go gcuirtear cosc orthu go sealadach cluichí a imirt. Ní léirítear é seo go poiblí ar a bpróifíl. Má leantar leis an iompar seo, méadaíonn fad an cosc - agus d’fhéadfadh dúnadh cuntas a bheith mar thoradh ar iompar fada den chineál seo.";i['leechess']="lee-chess";i['lichessCanOptionnalySendPopUps']="Is féidir le Lichess fógraí mír aníos a sheoladh go roghnach, mar shampla nuair is é do sheal é nó nuair a fuair tú teachtaireacht phríobháideach.\n\nCliceáil ar an deilbhín glasála in aice leis an seoladh lichess.org i mbarra URL do bhrabhsálaí.\n\nAnsin roghnaigh fógraí ó Lichess a cheadú nó a bhac.";i['lichessCombinationLiveLightLibrePronounced']=s("Is meascán de bheo/éadrom/saoirse agus ficheall é Lichess. Fuaimnítear é %1$s.");i['lichessFollowFIDErules']=s("Sa chás go rithfidh imreoir amháin as am, is iondúil go gcaillfidh an t-imreoir sin an cluiche. Is cluiche cothrom an cluiche, áfach, má tá an suíomh sa chaoi nach féidir leis an céile comhraic rí an imreora a marbhsháinnú le haon tsraith bearta dlithiúil (%1$s).\n\nI gcásanna neamhchoitianta bíonn sé deacair air seo a chinneadh go huathoibríoch (línte éigeantacha, daingne). De réir réamhshocraithe bímid i gcónaí ar taobh an imreoir nár rith as am.\n\nTabhair faoi deara gur féidir marbhsháinnú le ridire nó easpag amháin má tá píosa ag an céile comhraic a d’fhéadfadh bac a chur ar an rí.");i['lichessPoweredByDonationsAndVolunteers']="Reáchtáiltear Lichess trí bronntanais ó phátrúin agus iarrachtaí foireann oibrithe deonacha.";i['lichessRatings']="Rátálacha Lichess";i['lichessRecognizeAllOTBtitles']=s("Aithníonn Lichess gach teideal FIDE a fuarthas ó dhráma OTB (thar an gclár), chomh maith le %1$s. Seo liosta de theidil FIDE:");i['lichessSupportChessAnd']=s("Tugann Lichess tacaíocht do gnáth-fhicheall agus %1$s.");i['lichessTraining']="Traenáil Lichess";i['lMtitleComesToYouDoNotRequestIt']="Tá an teideal onórach seo neamhoifigiúil agus níl sé ar fáíl ach ar Lichess.\n\nIs annamh a bhronnann muid é ar ficheallaí suntasacha atá ina saoránaigh mhaithe Lichess, faoi rogha fúinn. Ní bhfaigheann tú an teideal LM, faigheann an teideal LM thú. Má cháilíonn tú, gheobhaidh tú teachtaireacht uainn maidir leis agus an rogha glacadh leis nó diúltú.\n\nNá iarr ar an teideal LM.";i['notPlayedEnoughRatedGamesAgainstX']=s("Níor chríochnaigh an ficheallaí go leor cluichí rátáilte go fóill i gcoinne %1$s sa chatagóir rátála.");i['notPlayedRecently']="Níor imir an ficheallaí a ndóthain cluichí le déanaí. Ag brath ar líon na gcluichí a d’imir tú, b’fhéidir go dtógfadh sé timpeall bliana neamhghníomhaíochta do rátáil a bheith sealadach arís.";i['notRepeatedMoves']="Ní dhearna muid bearta athshuíomh. Cén fáth go raibh an cluiche fós réitithe mar cluiche cothrom trí athshuíomh?";i['noUpperCaseDot']="Ní féidir.";i['otherWaysToHelp']="bealaí eile le cuidiú";i['ownerUniqueTrophies']=s("Tá an trófaí sin ar leithligh i stair Lichess, ní bheidh sé ag éinne riamh seachas %1$s.");i['pleaseReadFairPlayPage']=s("Le haghaidh tuilleadh eolais, léigh ár %s");i['positions']="suíomhanna";i['preventLeavingGameWithoutResigning']="Cad a dhéantar faoi imreoirí ag fágáil cluichí gan éirí as?";i['provisionalRatingExplanation']="Ciallaíonn an comhartha ceiste go bhfuil an rátáil sealadach. Cúiseanna san áireamh:";i['ratingDeviationLowerThanXinChessYinVariants']=s("diall rátála a bheith agat atá níos ísle ná %1$s, i gnáth-fhicheall, agus níos ísle ná %2$s in leaganacha,");i['ratingDeviationMorethanOneHundredTen']="I ndáiríre, ciallaíonn sé go bhfuil diall Glicko-2 níos mó ná 110. Is é an diall an leibhéal muiníne atá ag an gcóras sa rátáil. Dá ísle an diall, is iontaofa an rátáil.";i['ratingLeaderboards']="bord ceannais rátála";i['ratingRefundExplanation']="Nóiméad tar éis imreoir a mharcáil, tógtar a 40 cluiche rátáilte is déanaí le 3 lá anuas. Más tusa a chéile comhraic sna cluichí sin, chaill tú rátáil (mar gheall ar chaillteanas nó tarraingt), agus mura raibh do rátáil sealadach, faigheann tú aisíocaíocht rátála. Cuirtear teorainn leis an aisíocaíocht bunaithe ar do bhuaic-rátáil agus ar do dhul chun cinn rátála tar éis an chluiche.\n  (Mar shampla, má mhéadaigh do rátáil go mór tar éis na gcluichí sin, b’fhéidir nach bhfaighfeá aisíocaíocht ar bith nó aisíocaíocht pháirteach.) Ní bhfaigheann tú aisíocaíocht os cionn 150 pointe riamh.";i['ratingSystemUsedByLichess']="Ríomhtar rátálacha ag baint úsáide as an modh rátála Glicko-2 a d’fhorbair Mark Glickman. Is modh rátála an-tóir é seo, agus úsáideann líon suntasach eagraíochtaí fichille é (is sampla maith é FIDE, toisc go n-úsáideann siad an córas rátála Elo dátaithe fós).\n\nGo bunúsach, úsáideann rátálacha Glicko “eatraimh muiníne” agus do rátáil á ríomh agus á léiriú. Nuair a thosaíonn tú ag úsáid an suíomh gréasáin den chéad uair, tosaíonn do rátáil ag 1500 ± 700. Léiríonn an 1500 do rátáil, agus is ionann an 700 agus an t-eatramh muiníne.\n\nGo bunúsach, tá an córas 90% cinnte go bhfuil do rátáil áit éigin idir 800 agus 2200. Tá sé dochreidte éiginnte. Mar gheall air seo, nuair a bhíonn imreoir díreach ag tosú amach, athróidh a rátáil go suntasach, cúpla céad pointe ag an am, b’fhéidir. Ach tar éis roinnt cluichí i gcoinne imreoirí bhunaithe laghdóidh an t-eatramh muiníne, agus laghdóidh méid na bpointí a ghnóthaítear/a chaillfear tar éis gach cluiche.\n\nPointe eile le tabhairt faoi deara ná, de réir mar a théann am thart, go dtiocfaidh méadú ar an eatramh muiníne. Ligeann sé seo duit pointí a fháil/a chailleadh níos gasta le bheith níos cóngaraí do do leibhéal scil thar an aga seo.";i['repeatedPositionsThatMatters']=s("Baineann athshuíomh trí athshuíomh %1$s, agus ní bearta. Ní gá go dtarlódh athshuíomh i ndiaidh a chéile.");i['secondRequirementToStopOldPlayersTrustingLeaderboards']="Is é an 2ú riachtanas ná go stopfaidh imreoirí nach n-úsáideann a gcuntais a thuilleadh spás a thógáil suas sna cláir ceann riain.";i['showYourTitle']=s("Má tá teideal OTB agat, is féidir leat iarratas a dhéanamh go dtaispeánfar é seo ar do chuntas trí na %1$s a críochnú, lena n-áirítear íomhá shoiléir de dhoiciméad / chárta aitheantais agus féinín díot leis an doiciméad/cárta.\n\nTrí fhíorú mar imreoir le teideal tugtar Lichess rochtain ar imirt sna himeachtaí Arena Teidealta.\n\nFaoi dheireadh tá teideal oinigh %2$s ann.");i['similarOpponents']="céile comhraic den chaighdeán céanna";i['superiorThanXsEqualYtimeControl']=s("≥ %1$ss = %2$s");i['threeFoldHasToBeClaimed']=s("Caithfear athshuíomh a éileamh ag ceann de na himreoirí. Is féidir leat é sin a dhéanamh trí brú an cnaipe a thaispeántar, nó trí cluiche cothrom a offráil roimh do bheart deiridh. is cuma má dhiúltaíonn do chéile comhraic an cluiche cothrom, éileofar an athhshuíomh ar aon nós. Féadfaidh tú %1$s Lichess an athshuíomh a maígh ar do shon go huathoibríoch. Mar rud breis, má dtarlíonn athshuíomh cúig huaire críochnaíonn an cluiche láithreach.");i['threefoldRepetition']="Athshuíomh faoi thrí";i['threefoldRepetitionExplanation']=s("Má tharlaíonn an suíomh céanna trí huaire, is féidir le himreoirí cluiche cothrom a éileamh faoi %1$s. Úsáideann Lichess rialacha oifigiúla FIDE, mar atá curtha síos in Airteagal 9.2 de %2$s.");i['threefoldRepetitionLowerCase']="athshuíomh faoi thrí";i['titlesAvailableOnLichess']="Cad iad na teidil atá ar Lichess?";i['uniqueTrophies']="Trófaithe uathúla";i['usernamesCannotBeChanged']="Ní féidir ainmneacha úsáideora a athrú ar chúiseanna teicniúla agus praiticiúla. Cuirtear ainmneacha úsáideoirí i bhfeidhm san iomarca áiteanna: bunachair sonraí, onnmhairí, logaí agus intinn daoine. Is féidir leat an ceannlitriú a athrú uair amháin.";i['usernamesNotOffensive']=s("Go ginearálta, níor cheart go mbeadh ainmneacha úsáideora: maslach, aithris a dhéanamh ar dhuine eile, nó fógraíocht a dhéanamh. Is féidir leat níos mó a léamh faoin %1$s.");i['verificationForm']="foirm fíoraithe";i['viewSiteInformationPopUp']="Féach ar eolas ar mír aníos an suíomh gréasáin";i['watchIMRosenCheckmate']=s("Féach ar marbhsháinn an Máistir Idirnáisiúnta Eric Rosen %s.");i['wayOfBerserkExplanation']=s("Chun é a fháil, thug hiimgosu dúshlán dó féin Báiní a dhéanamh agus cluichí 100 %% de %s a bhuachan.");i['weCannotDoThatEvenIfItIsServerSideButThatsRare']="Ar an drochuair, ní féidir linn pointí rátála a thabhairt ar ais do chluichí a cailleadh mar gheall ar mhoill nó dícheangal, is cuma an raibh an fhadhb ag do thaobh nó ag ár dtaobh. Tá an dara ceann an-annamh áfach. Tabhair faoi deara freisin nuair a atosaíonn Lichess agus má chailleann tú am mar gheall air sin, déanaimid ginmhilleadh ar an gcluiche chun caillteanas éagórach a chosc.";i['weRepeatedthreeTimesPosButNoDraw']="Rinneamar athshuíomh trí huaire. Cén fáth nach raibh an cluiche réithe mar cluiche cothrom?";i['whatIsACPL']="Cad é an meánchaillteanas céadúceithearnach (ACPL - average centipawn loss)?";i['whatIsProvisionalRating']="Cén fáth go bhfuil comhartha ceiste (?) In aice le rátáil?";i['whatUsernameCanIchoose']="Cad is féidir a bheith ar m\\'ainm úsáideora?";i['whatVariantsCanIplay']="Cad iad na leaganacha fichille is féidir liom a imirt ar Lichess?";i['whenAmIEligibleRatinRefund']="Cathain a bheidh mé incháilithe don aisíocaíocht rátála uathoibríoch ó caimiléirí?";i['whichRatingSystemUsedByLichess']="Cén córas rátála a úsáideann Lichess?";i['whyAreRatingHigher']="Cén fáth go bhfuil rátálacha níos airde i gcomparáid le suíomhanna agus eagraíochtaí eile mar FIDE, USCF agus an ICC?";i['whyAreRatingHigherExplanation']="Is fearr gan smaoineamh ar rátálacha mar uimhreacha iomlán, ná iad a chur i gcomparáid le heagraíochtaí eile. Tá leibhéil éagsúla imreoirí ag eagraíochtaí difriúla, córais rátála éagsúla (Elo, Glicko, Glicko-2, nó leagan modhnaithe den mhéid thuasluaite). Is féidir leis na tosca seo dul i bhfeidhm go mór ar na huimhreacha iomlána (rátálacha).\n\nIs fearr smaoineamh ar rátálacha mar fhigiúirí “coibhneasta” (seachas figiúirí “dearbh”): Laistigh de líon imreoirí, cuideoidh a ndifríochtaí coibhneasta sna rátálacha leat meastachán a dhéanamh ar cé a bhuaigh / a tharraingeoidh / a chaillfidh, agus cé chomh minic. Ní chiallaíonn \\\"Tá rátáil X agam\\\" rud ar bith mura bhfuil imreoirí eile ann chun an rátáil sin a chur i gcomparáid le.";i['whyIsLichessCalledLichess']="Cén fáth a dtugtar Lichess ar Lichess?";i['whyIsLilaCalledLila']=s("Ar an dóigh chéanna, seasann an cód foinseach do Lichess, %1$s, do li[ficheall in sca]la, ag féachaint dó go bhfuil an chuid is mó de Lichess scríofa i %2$s, teanga ríomhchláraithe iomasach.");i['whyLiveLightLibre']="Beo, toisc go nimrítear agus féachtar ar chluichí i bhfíor-am 24/7; éadrom agus saor toisc gur foinse oscailte í Lichess agus neamhualaithe ó bruscar dílseánaigh a phléann suíomhanna Gréasáin eile.";i['yesLichessInspiredOtherOpenSourceWebsites']=s("Go deimhin, spreag Lichess suíomhanna foinse oscailte eile a úsáideann ár %1$s, %2$s, nó %3$s.");i['youCannotApply']="Ní féidir iarratas a dhéanamh chun bheith i do mhodhnóir. Má fheicimid duine a cheapfaimis a bheadh go maith mar mhodhnóir, rachaimid i dteagmháil leo go díreach.";i['youCanUseOpeningBookNoEngine']="Maidir le Lichess, is é an príomhdhifríocht sna rialacha maidir le fichille comhfhreagrais ná go gceadaítear leabhar oscailte. Tá cosc fós ar innill a úsáid agus beidh bratach ar chúnamh innill dá bharr. Cé go gceadaíonn ICCF úsáid innill i gcomhfhreagras, ní cheadaíonn Lichess."})()