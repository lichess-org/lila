"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.features)window.i18n.features={};let i=window.i18n.features;i['allChessBasicsLessons']="Toate lecțiile de bază ale șahului";i['allFeaturesAreFreeForEverybody']="Toate caracteristicile sunt gratuite pentru toată lumea, pentru totdeauna!";i['allFeaturesToCome']="Toate caracteristicile în curând, pentru totdeauna!";i['boardEditorAndAnalysisBoardWithEngine']=s("Editor de tablă și tablă de analiză cu %s");i['chessInsights']="Informații despre șah (analiză detaliată a jocului tău)";i['cloudEngineAnalysis']="Analiza motorului în cloud";i['contributeToLichessAndGetIcon']="Contribuie la Lichess și obține o pictogramă Patron cu aspect interesant";i['correspondenceWithConditionalPremoves']="Șah de corespondență cu mutări prealabile condiționate";i['deepXServerAnalysis']=s("Analiză %s în profunzime");i['downloadOrUploadAnyGameAsPgn']="Descărcați/Încărcați orice joc ca PGN";i['endgameTablebase']="endgame cu 7 piese";i['everybodyGetsAllFeaturesForFree']="Da, ambele conturi au aceleaşi caracteristici!";i['gamesPerDay']=p({"one":"%s partidă pe zi","few":"%s partidă pe zi","other":"%s jocuri pe zi"});i['globalOpeningExplorerInNbGames']=s("Explorator de deschidere globală (%s jocuri!)");i['ifYouLoveLichess']="Dacă iubești Lichess,";i['landscapeSupportOnApp']="iPhone & Telefoane Android și tablete, suportă modul landscape";i['lightOrDarkThemeCustomBoardsPiecesAndBackground']="Temă luminoasă/întunecată, plăci personalizate, piese şi fundal";i['personalOpeningExplorer']="Explorator de deschidere personal";i['personalOpeningExplorerX']=s("%1$s (de asemenea, funcționează pe %2$s)");i['standardChessAndX']=s("Șah standard și %s");i['studies']="Studii (analiză partajabilă și persistentă)";i['supportLichess']="Susține Lichess";i['supportUsWithAPatronAccount']="Susține-ne cu un cont Patron!";i['tacticalPuzzlesFromUserGames']="puzzle-uri tactice din jocurile utilizatorului";i['tvForumBlogTeamsMessagingFriendsChallenges']="Blog, forum, echipe, TV, mesagerie, prieteni, provocări";i['ultraBulletBulletBlitzRapidClassicalAndCorrespondenceChess']="UltraBullet, Bullet, Blitz, Rapid, Clasic, Correspondence Chess";i['weBelieveEveryChessPlayerDeservesTheBest']="Credem că fiecare jucător de șah merită cel mai bun și de asemenea:";i['zeroAdsAndNoTracking']="Fară reclame, zero tracking"})()