"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.team)window.i18n.team={};let i=window.i18n.team;i['allTeams']="Semua tim";i['beingReviewed']="Permintaan bergabung Anda telah ditinjau oleh ketua tim.";i['closeTeam']="Bubarkan tim";i['closeTeamDescription']="Bubarkan tim selamanya.";i['completedTourns']="Turnamen yang telah selesai";i['declinedRequests']="Permintaan yang Ditolak";i['entryCode']="Kode masuk tim";i['entryCodeDescriptionForLeader']="(Opsional) Kata sandi yang harus diketahui anggota baru untuk bergabung tim ini.";i['incorrectEntryCode']="Kode masuk salah.";i['joinTeam']="Gabung tim";i['kickSomeone']="Keluarkan seseorang dari tim";i['leadersChat']="Obrolan Pemimpin";i['leaderTeams']="Tim yang dipimpin";i['manuallyReviewAdmissionRequests']="Tinjau permintaan masuk secara manual";i['manuallyReviewAdmissionRequestsHelp']="Jika dicentang, pemain harus menulis permintaan untuk bergabung tim, yang dapat kamu tolak atau terima.";i['messageAllMembers']="Pesan semua anggota";i['messageAllMembersLongDescription']="Kirim pesan pribadi ke SEMUA anggota tim.\nAnda dapat menggunakan ini untuk memanggil pemain untuk bergabung ke turnamen atau pertarungan team.\nPemain yang tidak suka menerima pesan anda dapat meninggalkan tim.";i['messageAllMembersOverview']="Kirim pesan pribadi kepada semua anggota tim";i['myTeams']="Tim saya";i['nbMembers']=p({"other":"%s anggota"});i['newTeam']="Tim baru";i['noTeamFound']="Tidak ada tim yang ditemukan";i['quitTeam']="Keluar dari tim";i['requestDeclined']="Permintaan bergabung Anda telah ditolak oleh ketua tim.";i['subToTeamMessages']="Berlangganan pesan tim";i['swissTournamentOverview']="Turnamen Swiss yang hanya dapat diikuti anggota tim";i['team']="Tim";i['teamAlreadyExists']="Tim ini sudah ada.";i['teamBattle']="Pertarungan Tim";i['teamBattleOverview']="Pertarungan antara bebrapa tim, setiap pemain mencetak poin untuk timnya";i['teamLeaders']=p({"other":"Ketua Tim"});i['teamRecentMembers']="Anggota terkini";i['teams']="Tim";i['teamsIlead']="Tim yang anda pimpin";i['teamTournament']="Turnamen tim";i['teamTournamentOverview']="Sebuah arena turnamen yang hanya bisa diikuti oleh anggota tim Anda";i['whoToKick']="Siapa yang ingin Anda keluarkan dari tim?";i['willBeReviewed']="Permintaan bergabung Anda akan ditinjau oleh ketua tim.";i['xJoinRequests']=p({"other":"%s ajukan permintaan"});i['youWayWantToLinkOneOfTheseTournaments']="Apakah anda ingin menautkan salah satu turnamen ini yang akan datang?"})()