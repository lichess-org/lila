"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.team)window.i18n.team={};let i=window.i18n.team;i['allTeams']="सभी समूह";i['beingReviewed']="टीम लीडर द्वारा आपके सम्मिलित अनुरोध की समीक्षा की जा रही है।";i['closeTeam']="टीम को बंद करें";i['closeTeamDescription']="टीम को हमेशा के लिए बंद कर देता है।";i['completedTourns']="पूर्ण किए गए प्रतियोगिता";i['declinedRequests']="अस्वीकृत अनुरोध";i['entryCode']="टीम एंट्री कोड";i['entryCodeDescriptionForLeader']="(वैकल्पिक) एक कुंजिका जिसे नए सदस्यों को इस टीम में शामिल होने के लिए पता होना चाहिए।";i['incorrectEntryCode']="अवैध प्रवेश कूट";i['joinLichessVariantTeam']=s("समाचार और घटनाओं के लिए आधिकारिक %s टीम में शामिल हों");i['joinTeam']="समूह से जुड़े";i['kickSomeone']="किसी को टीम से बाहर करे";i['leadersChat']="नेताओं के चैट";i['leaderTeams']="नेता के दल";i['manuallyReviewAdmissionRequests']="प्रवेश अनुरोधों की मैन्युअल समीक्षा करें";i['manuallyReviewAdmissionRequestsHelp']="यदि जाँच की जाती है, तो खिलाड़ियों को टीम में शामिल होने के लिए एक अनुरोध लिखना होगा, जिसे आप अस्वीकार या स्वीकार कर सकते हैं।";i['messageAllMembers']="सभी सदस्यों को संदेश दें";i['messageAllMembersLongDescription']="टीम के सभी सदस्यों को एक निजी संदेश भेजें।\nआप इसका उपयोग खिलाड़ियों को टूर्नामेंट या टीम की लड़ाई में शामिल होने के लिए बुला सकते हैं।\nजो खिलाड़ी आपके संदेश प्राप्त करना पसंद नहीं करते वे टीम छोड़ सकते हैं।";i['messageAllMembersOverview']="टीम के प्रत्येक सदस्य को एक निजी संदेश भेजें";i['myTeams']="मेरे समूह";i['nbMembers']=p({"one":"%s सदस्य","other":"%s सदस्य"});i['newTeam']="नया समूह";i['noTeamFound']="कोई समूह नही मिला";i['onlyLeaderLeavesTeam']="कृपया जाने से पहले एक नया टीम लीडर जोड़ें, या टीम को बंद करें।";i['quitTeam']="समूह से हटे";i['requestDeclined']="आपके शामिल होने के अनुरोध को टीम लीडर ने अस्वीकार कर दिया।";i['subToTeamMessages']="टीम संदेश स्वीकार करें";i['swissTournamentOverview']="स्विस् प्रतियोगिता जिसमें आपकी टीम के सदस्य ही शामिल हो सकते हैं";i['team']="टीम";i['teamAlreadyExists']="यह टीम पहले से मौजूद है।";i['teamBattle']="टीम युद्ध";i['teamBattleOverview']="कई टीमों की लड़ाई, प्रत्येक खिलाड़ी अपनी टीम के लिए स्कोर करता है";i['teamLeaders']=p({"one":"समूह का नेता","other":"टीम के नेता"});i['teamPage']="टीम पेज";i['teamRecentMembers']="समूह के नए सदस्य";i['teams']="समूह";i['teamsIlead']="मैं टीम का नेतृत्व करता हूं";i['teamTournament']="टीम टूर्नामेंट";i['teamTournamentOverview']="एक अखाड़ा टूर्नामेंट जिसमें आपकी टीम के सदस्य ही शामिल हो सकते हैं";i['whoToKick']="आप किसे टीम से बाहर करना चाहते हैं?";i['willBeReviewed']="आपके सम्मिलित अनुरोध की समीक्षा टीम लीडर द्वारा की जाएगी।";i['xJoinRequests']=p({"one":"%s प्रवेश अनुरोध","other":"%s प्रवेश अनुरोध"});i['youWayWantToLinkOneOfTheseTournaments']="मैं टीम का नेतृत्व करता हूं"})()