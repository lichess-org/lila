"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.features)window.i18n.features={};let i=window.i18n.features;i['allChessBasicsLessons']="All chess basics lessons";i['allFeaturesAreFreeForEverybody']="All features are free for everybody, forever!";i['allFeaturesToCome']="All features to come, forever!";i['boardEditorAndAnalysisBoardWithEngine']=s("Board editor and analysis board with %s");i['chessInsights']="Chess insights (detailed analysis of your play)";i['cloudEngineAnalysis']="Cloud engine analysis";i['contributeToLichessAndGetIcon']="Contribute to Lichess and get a cool looking Patron icon";i['correspondenceWithConditionalPremoves']="Correspondence chess with conditional premoves";i['deepXServerAnalysis']=s("Deep %s server analysis");i['downloadOrUploadAnyGameAsPgn']="Download/Upload any game as PGN";i['endgameTablebase']="7-piece endgame tablebase";i['everybodyGetsAllFeaturesForFree']="Yes, both accounts have the same features!";i['gamesPerDay']=p({"one":"%s game per day","other":"%s games per day"});i['globalOpeningExplorerInNbGames']=s("Global opening explorer (%s games!)");i['ifYouLoveLichess']="If you love Lichess,";i['landscapeSupportOnApp']="iPhone & Android phones and tablets, landscape support";i['lightOrDarkThemeCustomBoardsPiecesAndBackground']="Light/Dark theme, custom boards, pieces and background";i['personalOpeningExplorer']="Personal opening explorer";i['personalOpeningExplorerX']=s("%1$s (also works on %2$s)");i['standardChessAndX']=s("Standard chess and %s");i['studies']="Studies (shareable and persistent analysis)";i['supportLichess']="Support Lichess";i['supportUsWithAPatronAccount']="Support us with a Patron account!";i['tacticalPuzzlesFromUserGames']="Tactical puzzles from user games";i['tvForumBlogTeamsMessagingFriendsChallenges']="Blog, forum, teams, TV, messaging, friends, challenges";i['ultraBulletBulletBlitzRapidClassicalAndCorrespondenceChess']="UltraBullet, Bullet, Blitz, Rapid, Classical, Correspondence Chess";i['weBelieveEveryChessPlayerDeservesTheBest']="We believe every chess player deserves the best, and so:";i['zeroAdsAndNoTracking']="Zero advertisement, no tracking"})()