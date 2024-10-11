"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.team)window.i18n.team={};let i=window.i18n.team;i['allTeams']="Tüm takımlar";i['battleOfNbTeams']=p({"one":"%s takımın savaşı","other":"%s takımın savaşı"});i['beingReviewed']="Katılma talebiniz takım lideri tarafından inceleniyor.";i['closeTeam']="Takımı kapat";i['closeTeamDescription']="Takımı sonsuza dek kapatır.";i['completedTourns']="Tamamlanan turnuvalar";i['declinedRequests']="Geri çevrilen istekler";i['entryCode']="Takıma giriş kodu";i['entryCodeDescriptionForLeader']="(İsteğe bağlı) Takıma yeni katılan üyelerin bu kodu bilmesi gerekir.";i['incorrectEntryCode']="Giriş kodu yanlış.";i['innerTeam']="İçerideki takım";i['joinLichessVariantTeam']=s("Haber ve etkinlikler için resmî %s takımına katılın");i['joinTeam']="Takıma katıl";i['kickSomeone']="Birini takımdan at";i['leadersChat']="Liderler arası sohbet";i['leaderTeams']="Lideri olduğun takımlar";i['listTheTeamsThatWillCompete']="Bu savaşta yarışacak takımları listeleyin.";i['manuallyReviewAdmissionRequests']="Katılım başvuruları onayınızdan geçsin";i['manuallyReviewAdmissionRequestsHelp']="Şık işaretliyse, oyuncuların mesaj yazarak yapabileceği takıma katılım başvurusu onayınıza sunulacak.";i['messageAllMembers']="Tüm üyelere mesaj gönder";i['messageAllMembersLongDescription']="Takımın tüm üyelerine özel bir mesaj gönderin.\nOyuncuları bir turnuvaya veya bir takım savaşına katılmaya çağırmak için kullanabilirsiniz.\nMesajlarınızı almayı sevmeyen oyuncular takımdan ayrılabilir.";i['messageAllMembersOverview']="Takımdaki herkese özel mesaj gönder";i['myTeams']="Takımlarım";i['nbLeadersPerTeam']=p({"one":"Her takımda bir lider","other":"%s lider takım başına"});i['nbMembers']=p({"one":"%s üyeler","other":"%s üye"});i['newTeam']="Yeni takım";i['noTeamFound']="Takım bulunamadı";i['numberOfLeadsPerTeam']="Her takımdaki liderlerin sayısı. Onların puanlarının toplamı takımın puanıdır.";i['numberOfLeadsPerTeamHelp']="Turnuva başladıktan sonra bu değeri değiştirmemeniz daha iyi olur!";i['oneTeamPerLine']="Her satırda bir takım kullanın. Otomatik tamamlayıcıyı kullanabilirsiniz.";i['oneTeamPerLineHelp']="Bu listedeki takımları bir turnuvadan diğerine kopyalayabilirsiniz!\n\nBir oyuncu zaten bu takımlardan birine katıldıysa, takımı kaldıramazsınız.";i['onlyLeaderLeavesTeam']="Takımdan ayrılmadan önce yeni bir takım lideri belirleyin veya takımı kapatın.";i['quitTeam']="Takımdan ayrıl";i['requestDeclined']="Katılma talebiniz bir takım lideri tarafından reddedildi.";i['subToTeamMessages']="Takım mesajlarına abone ol";i['swissTournamentOverview']="Yalnızca takım üyelerinizin katılabileceği İsviçre sistemi turnuvası";i['team']="Takım";i['teamAlreadyExists']="Böyle bir takım zaten mevcut.";i['teamBattle']="Takım Çarpışması";i['teamBattleOverview']="Bu birden fazla takımın mücadelesidir, her oyuncu kendi takımına puan kazandırır";i['teamLeaders']=p({"one":"Takım lideri","other":"Takım liderleri"});i['teamPage']="Takım sayfası";i['teamRecentMembers']="Son katılan üyeler";i['teams']="Takımlar";i['teamsIlead']="Yönettiğim takımlar";i['teamTournament']="Takım turnuvası";i['teamTournamentOverview']="Sadece takım üyelerinizin katılabileceği bir Arena turnuvası";i['thisTeamBattleIsOver']="Bu turnuva sona erdi ve takımlar artık güncellenemez.";i['upcomingTournaments']="Yaklaşan turnuvalar";i['whoToKick']="Takımdan kimi atmak istiyorsunuz?";i['willBeReviewed']="Katılma talebiniz takım lideri tarafından incelenecek.";i['xJoinRequests']=p({"one":"%s katılma talebi","other":"%s katılma talebi"});i['youWayWantToLinkOneOfTheseTournaments']="Yaklaşan bu turnuvalardan birine link vermek ister miydiniz?"})()