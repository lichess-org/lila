"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.study)window.i18n.study={};let i=window.i18n.study;i['addMembers']="Legg limir aftrat";i['addNewChapter']="Skoyt nýggjan kapittul upp í";i['allowCloning']="Loyv kloning";i['allStudies']="Allar rannsóknir";i['allSyncMembersRemainOnTheSamePosition']="Allir SYNC-limir verða verandi í somu støðu";i['analysisMode']="Greiningarstøða";i['annotateWithGlyphs']="Skriva við teknum";i['automatic']="Sjálvvirkið";i['blackIsWinning']="Svartur stendur til at vinna";i['blunder']="Bukkur";i['brilliantMove']="Framúrskarandi leikur";i['chapterPgn']="PGN kapittul";i['chapterX']=s("Kapittul %s");i['clearAllCommentsInThisChapter']="Skulu allar viðmerkingar, øll tekn og teknað skap strikast úr hesum kapitli?";i['clearAnnotations']="Strika viðmerkingar";i['clearChat']="Rudda kjatt";i['cloneStudy']="Klona";i['commentThisMove']="Viðmerk henda leikin";i['commentThisPosition']="Viðmerk hesa støðuna";i['contributor']="Gevur íkast";i['contributors']="Luttakarar";i['createChapter']="Stovna kapittul";i['createStudy']="Stovna rannsókn";i['currentChapterUrl']="Núverandi URL partur";i['dateAddedNewest']="Eftir dagfesting (nýggjastu)";i['dateAddedOldest']="Eftir dagfesting (eldstu)";i['deleteChapter']="Strika kapittul";i['deleteStudy']="Burturbein rannsókn";i['deleteTheStudyChatHistory']="Skal kjattsøgan í rannsóknini strikast? Til ber ikki at angra!";i['deleteThisChapter']="Strika henda kapittulin? Til ber ikki at angra!";i['downloadAllGames']="Tak øll talv niður";i['downloadGame']="Tak talv niður";i['dubiousMove']="Ivasamur leikur";i['editChapter']="Broyt kapittul";i['editor']="Ritstjóri";i['editStudy']="Ritstjórna rannsókn";i['embedInYourWebsite']="Fell inn í heimasíðu tína ella blogg tín";i['empty']="Tómur";i['enableSync']="Samstilling møgulig";i['everyone']="Øll";i['first']="Fyrsta";i['getAFullComputerAnalysis']="Fá eina fullfíggjaða teldugreining av høvuðsbrigdinum frá ambætaranum.";i['goodMove']="Góður leikur";i['hideNextMoves']="Fjal næstu leikirnar";i['hot']="Heitar";i['interactiveLesson']="Samvirkin frálæra";i['interestingMove']="Áhugaverdur leikur";i['inviteOnly']="Bert innboðin";i['inviteToTheStudy']="Bjóða uppí rannsóknina";i['kick']="Koyr úr";i['last']="Síðsta";i['leaveTheStudy']="Far úr rannsóknini";i['like']="Dáma";i['loadAGameByUrl']="Les inn talv frá URL";i['loadAGameFromPgn']="Les inn talv frá PGN";i['loadAGameFromXOrY']=s("Les talv inn frá %1$s ella %2$s");i['loadAPositionFromFen']="Les inn talvstøðu frá FEN";i['makeSureTheChapterIsComplete']="Tryggja tær, at kapittulin er fullfíggjaður. Tú kanst bert biðja um greining eina ferð.";i['members']="Limir";i['mistake']="Mistak";i['mostPopular']="Best dámdu";i['myFavoriteStudies']="Mínar yndisrannsóknir";i['myPrivateStudies']="Mínar egnu rannsóknir";i['myPublicStudies']="Mínar almennu rannsóknir";i['myStudies']="Mínar rannsóknir";i['nbChapters']=p({"one":"%s kapittul","other":"%s kapitlar"});i['nbGames']=p({"one":"%s talv","other":"%s talv"});i['nbMembers']=p({"one":"%s limur","other":"%s limir"});i['newChapter']="Nýggjur kapittul";i['newTag']="Nýtt frámerki";i['next']="Næsta";i['nobody']="Eingin";i['noLetPeopleBrowseFreely']="Nei: lat fólk kaga frítt";i['noneYet']="Ongar enn.";i['noPinnedComment']="Einki";i['normalAnalysis']="Vanlig greining";i['onlyContributorsCanRequestAnalysis']="Bert tey, ið geva sítt íkast til rannsóknina, kunnu biðja um eina teldugreining.";i['onlyMe']="Bert eg";i['onlyMove']="Einasti leikur";i['onlyPublicStudiesCanBeEmbedded']="Bert almennar rannsóknir kunnu verða feldar inn í!";i['open']="Lat upp";i['orientation']="Helling";i['pasteYourPgnTextHereUpToNbGames']=p({"one":"Set PGN tekstin hjá tær inn her, upp til %s talv","other":"Set PGN tekstin hjá tær inn her, upp til %s talv"});i['pgnTags']="PGN-frámerki";i['pinnedChapterComment']="Føst viðmerking til kapittulin";i['pinnedStudyComment']="Føst rannsóknarviðmerking";i['playing']="Í gongd";i['pleaseOnlyInvitePeopleYouKnow']="Bjóða vinaliga bert fólki, tú kennir, og sum vilja taka virknan lut í rannsóknini.";i['previous']="Undanfarna";i['private']="Egin (privat)";i['public']="Almen";i['readMoreAboutEmbedding']="Les meira um at fella inn í";i['recentlyUpdated']="Nýliga dagførdar";i['rightUnderTheBoard']="Beint undir talvborðinum";i['save']="Goym";i['saveChapter']="Goym kapittulin";i['searchByUsername']="Leita eftir brúkaranavni";i['shareAndExport']="Deil & flyt út";i['shareChanges']="Deil broytingar við áskoðarar, og goym tær á ambætaranum";i['spectator']="Áskoðari";i['start']="Byrja";i['startAtInitialPosition']="Byrja við byrjanarstøðuni";i['startAtX']=s("Byrja við %s");i['startFromCustomPosition']="Byrja við støðu, ið brúkari ger av";i['startFromInitialPosition']="Byrja við byrjanarstøðuni";i['studiesCreatedByX']=s("%s stovnaði hesar rannsóknir");i['studiesIContributeTo']="Rannsóknir, eg gevi mítt íkast til";i['studyNotFound']="Rannsókn ikki funnin";i['studyPgn']="PGN rannsókn";i['studyUrl']="URL rannsókn";i['theChapterIsTooShortToBeAnalysed']="Kapittulin er ov stuttur til at verða greinaður.";i['unlisted']="Ikki skrásett";i['urlOfTheGame']="URL fyri talvini";i['visibility']="Sýni";i['whatAreStudies']="Hvat eru rannsóknir?";i['whereDoYouWantToStudyThat']="Hvar vilt tú rannsaka hatta?";i['whiteIsWinning']="Hvítur stendur til at vinna";i['xBroughtToYouByY']=s("%2$s fekk tær %1$s til vegar");i['yesKeepEveryoneOnTheSamePosition']="Ja: varðveit øll í somu støðu";i['youAreNowAContributor']="Tú ert nú ein, ið leggur aftrat rannsóknini";i['youAreNowASpectator']="Tú ert nú áskoðari";i['youCanPasteThisInTheForumToEmbed']="Tú kanst seta hetta inn í torgið at sýna tað har"})()