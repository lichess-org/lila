"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.features)window.i18n.features={};let i=window.i18n.features;i['allChessBasicsLessons']="Todas as leccións básicas de xadrez";i['allFeaturesAreFreeForEverybody']="Todas as funcións son gratis para todo o mundo, para sempre!";i['allFeaturesToCome']="Todas as funcións por chegar, para sempre!";i['boardEditorAndAnalysisBoardWithEngine']=s("Editor de taboleiro e taboleiro de análise con %s");i['chessInsights']="Estatísticas (análise detallada do teu xogo)";i['cloudEngineAnalysis']="Motor de análise dende a nube";i['contributeToLichessAndGetIcon']="Contribúe a Lichess e obtén unha bárbara icona de Patrón";i['correspondenceWithConditionalPremoves']="Xadrez postal con premovementos condicionais";i['deepXServerAnalysis']=s("Análise profunda no servidor de %s");i['downloadOrUploadAnyGameAsPgn']="Descarga/Sube calquera partida coma un PGN";i['endgameTablebase']="Base de datos de finais de 7 pezas";i['everybodyGetsAllFeaturesForFree']="Si, ambas as dúas contas teñen as mesmas funcións!";i['gamesPerDay']=p({"one":"%s partida por día","other":"%s partidas por día"});i['globalOpeningExplorerInNbGames']=s("Explorador de aperturas global (%s partidas!)");i['ifYouLoveLichess']="Se amas a Lichess,";i['landscapeSupportOnApp']="Teléfonos e tabletas iPhone & Android, con modo apaisado";i['lightOrDarkThemeCustomBoardsPiecesAndBackground']="Tema Claro/Escuro, taboleiros, pezas e fondo á medida";i['personalOpeningExplorer']="Explorador de aperturas persoal";i['personalOpeningExplorerX']=s("%1$s (tamén funciona con %2$s)");i['standardChessAndX']=s("Xadrez estándar e %s");i['studies']="Estudos (compartidos e análise persistente)";i['supportLichess']="Apoiar a Lichess";i['supportUsWithAPatronAccount']="Apóianos cunha conta de Patrón!";i['tacticalPuzzlesFromUserGames']="Crebacabezas de partidas dos xogadores";i['tvForumBlogTeamsMessagingFriendsChallenges']="Blog, foro, equipos, TV, mensaxes, amizades, desafíos";i['ultraBulletBulletBlitzRapidClassicalAndCorrespondenceChess']="UltraBala, Bala, Lóstrego, Rápidas, Clásicas, Xadrez Postal";i['weBelieveEveryChessPlayerDeservesTheBest']="Cremos que todos os xogadores merecen o mellor, polo tanto:";i['zeroAdsAndNoTracking']="Sen anuncios nin rastrexadores"})()