"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.activity)window.i18n.activity={};let i=window.i18n.activity;i['activity']="Son Etkinlikler";i['competedInNbSwissTournaments']=p({"one":"%s İsviçre Sistemi turnuvasına katıldı","other":"%s İsviçre Sistemi turnuvasına katıldı"});i['competedInNbTournaments']=p({"one":"%s turnuvaya katıldı","other":"%s turnuvaya katıldı"});i['completedNbGames']=p({"one":"%s adet yazışmalı oyun tamamladı","other":"%s adet yazışmalı oyun tamamladı"});i['completedNbVariantGames']=p({"one":"%1$s %2$s yazışmalı oyunu tamamladı","other":"%1$s %2$s yazışmalı oyunu tamamladı"});i['createdNbStudies']=p({"one":"%s yeni çalışma oluşturdu","other":"%s yeni çalışma oluşturdu"});i['followedNbPlayers']=p({"one":"%s oyuncuyu takip etmeye başladı","other":"%s oyuncuyu takip etmeye başladı"});i['gainedNbFollowers']=p({"one":"%s yeni takipçi kazandı","other":"%s yeni takipçi kazandı"});i['hostedALiveStream']="Canlı yayın yaptı";i['hostedNbSimuls']=p({"one":"%s simultaneye ev sahipliği yaptı","other":"%s eş zamanlı gösteriye ev sahipliği yaptı"});i['inNbCorrespondenceGames']=p({"one":"(%1$s yazışmalı oyunda)","other":"(%1$s yazışmalı oyunda)"});i['joinedNbSimuls']=p({"one":"%s eş zamanlı gösteriye katıldı","other":"%s eş zamanlı gösteriye katıldı"});i['joinedNbTeams']=p({"one":"%s takıma katıldı","other":"%s takıma katıldı"});i['playedNbGames']=p({"one":"%1$s kez %2$s oynadı","other":"%1$s kez %2$s oynadı"});i['playedNbMoves']=p({"one":"%1$s hamle yaptı","other":"%1$s hamle yaptı"});i['postedNbMessages']=p({"one":"Forumda %1$s mesaj paylaştı: %2$s","other":"%2$s forumunda %1$s mesaj paylaştı"});i['practicedNbPositions']=p({"one":"%2$s üzerine %1$s alıştırma yaptı","other":"%2$s üzerine %1$s alıştırma yaptı"});i['rankedInSwissTournament']=s("%2$s katılımcıları arasında #%1$s. oldu");i['rankedInTournament']=p({"one":"%3$s oyun oynayarak %4$s turnuvasında %1$s. oldu (en iyi %2$s%% içinde)","other":"%3$s oyun oynayarak %4$s turnuvasında %1$s. oldu (en iyi %2$s%% içinde)"});i['signedUp']="lichess.org\\'a üye oldu";i['solvedNbPuzzles']=p({"one":"%s bulmaca çözdü","other":"%s bulmaca çözdü"});i['supportedNbMonths']=p({"one":"lichess.org\\'u %1$s aylığına %2$s olarak destekledi","other":"lichess.org\\'u %1$s aylığına %2$s olarak destekledi"})})()