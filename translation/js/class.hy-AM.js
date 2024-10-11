"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.class)window.i18n.class={};let i=window.i18n.class;i['addLichessUsernames']="Ավելացրեք Lichess-ի օգտատերերի անունները` նրանց որպես մարզիչ հրավիրելու համար։ Ամեն տողում` մեկին։";i['addStudent']="Ավելացնել աշակերտ";i['aLinkToTheClassWillBeAdded']="Խմբի հղումն ինքնաբերաբար կավելանա հաղորդագրության վերջում, ինքնուրույն ավելացնելու կարիք չկա։";i['applyToBeLichessTeacher']="Ներկայացնել հայտ՝ Lichess-ի ուսուցիչ դառնալու համար";i['classDescription']="Խմբի նկարագրությունը";i['className']="Խմբի անվանումը";i['classNews']="Դասարանի նորություններ";i['clickToViewInvitation']="Հրավերը դիտելու համար սեղմե՛ք հղումը.";i['closeClass']="Փակել խումբը";i['closeDesc1']="Աշակերտն այլևս չի կարողանա օգտվել այս հաշվից։ Փակումը կլինի վերջնական։ Համոզվեք, որ աշակերտը հասկանում է դա և համաձայն է։";i['closeDesc2']="Դրա փոխարեն Դուք կարող եք աշակերտին փոխանցել հաշվի կառավարումը, որպեսզի նա կարողանա ինքնուրույն օգտագործել այն։";i['closeStudent']="Փակել հաշիվը";i['closeTheAccount']="Ընդմիշտ փակել աշակերտի հաշիվը։";i['createANewLichessAccount']="Ստեղծել նոր հաշիվ Lichess-ում";i['createDesc1']="Եթե աշակերտը դեռևս չունի Lichess-ի մասնակցային հաշիվ, Դուք այն կարող եք ստեղծել այստեղ։";i['createDesc2']="Էլեկտրոնային փոստի հասցե պետք չէ։ Կձևավորվի պատահական գաղտնաբառ, որը պետք է ուղարկեք աշակերտին, որպեսզի նա կարողանա մուտք գործել համակարգ։";i['createDesc3']="Կարևոր է. աշակերտը չպետք է ունենա մի քանի մասնակցային հաշիվ։";i['createDesc4']="Եթե նրանք արդեն ունեն, օգտագործեք հրավերի ձևանմուշը։";i['createMoreClasses']="ստեղծեք ավելի շատ դասարաններ";i['createMultipleAccounts']="Միաժամանակ ստեղծել Lichess-ի մի քանի հաշիվ";i['createStudentWarning']="Ստեղծեք մասնակցային հաշիվներ միայն իրական աշակերտների համար։ Այդ հնարավորությունը մի՛ օգտագործեք անձնական նպատակներով մասնակցային հաշիվներ ստեղծելու համար, այլապես կարգելափակվեք։";i['editNews']="Փոփոխել նորությունը";i['features']="Հնարավորություններ";i['freeForAllForever']="Անվճար է բոլորի համար, ընդմիշտ, 100 տոկոսով առանց գովազդի և հետևման";i['generateANewPassword']="Ստեղծել նոր գաղտնաբառ աշակերտի համար";i['generateANewUsername']="Ստեղծել նոր մասնակցային անուն";i['invitationToClass']=s("Ձեզ հրավիրել են «%s» դասարան որպես աշակերտի։");i['invite']="Հրավիրել";i['inviteALichessAccount']="Հրավիրել Lichess-ի մասնակցային հաշիվ";i['inviteDesc1']="Եթե աշակերտն արդեն ունի Lichess-ի մասնակցային հաշիվ, Դուք կարող եք նրան հրավիրել դասարան։";i['inviteDesc2']="Նրանք Lichess-ում կստանան դասարան մտնելու հղումով հաղորդագրություն։";i['inviteDesc3']="Կարևոր է. հրավիրեք միայն այն մասնակիցներին, որոնց ճանաչում եք, և որոնք իրոք ցանկանում են պարապել դասարանում։";i['inviteDesc4']="Երբեք մի՛ ուղարկեք անցանկալի հրավերներ պատահական խաղացողներին։";i['invitedToXByY']=s("Հրավերը %1$s ստացված է %2$sից");i['inviteTheStudentBack']="Հետ հրավիրել աշակերտին";i['lastActiveDate']="Ակտիվ";i['lichessClasses']="Խմբեր";i['lichessUsername']="Մասնակցային անունը Lichess-ում";i['makeSureToCopy']="Համոզվեք, որ գաղտնաբառը պատճենված է կամ գրված։ Դուք այն այլևս չեք կարող տեսնել։";i['managed']="Կառավարվող";i['messageAllStudents']="Հայտնեք բոլոր աշակերտներին խմբերի նոր նյութերի մասին";i['na']="Կիրառելի չէ";i['nbPendingInvitations']=p({"one":"Ուսումնասիրվող հրավեր","other":"%s ուսումնասիրվող հրավեր"});i['nbStudents']=p({"one":"Աշակերտ","other":"%s աշակերտներ"});i['nbTeachers']=p({"one":"Դասավանդող","other":"%s դասավանդողներ"});i['newClass']="Նոր խումբ";i['news']="Նորություններ";i['newsEdit1']="Դասարանի բոլոր նորությունները մեկ դաշտում։";i['newsEdit2']="Թարմ նորություններն ավելացրեք վերևից։ Մի հեռացրեք նախորդ նորությունները։";i['newsEdit3']="Նորությունները բաժանեք երեք գծիկով --- դրանք կվերածվեն հորիզոնական գծի։";i['noClassesYet']="Խմբեր դեռ չկան։";i['noRemovedStudents']="Հեռացված աշակերտներ չկան։";i['noStudents']="Դասարանում առայժմ չկան աշակերտներ։";i['nothingHere']="Այստեղ դեռ ոչինչ չկա։";i['notifyAllStudents']="Տեղեկացնել բոլոր աշակերտներին";i['onlyVisibleToTeachers']="Հասանելի են միայն դասավանդողին";i['orSeparator']="կամ";i['overDays']="Մի քանի օրում";i['overview']="Դիտում";i['passwordX']=s("Գաղտնաբառն է %s");i['progress']="Առաջընթաց";i['quicklyGenerateSafeUsernames']="Արագ ստեղծեք անվտանգ անուններ և գաղտնաբառեր աշակերտների համար։";i['realName']="Իսկական անունը";i['realUniqueEmail']="Աշակերտի իրական, եզակի email։ Մենք այդ հասցեով կուղարկենք հաստատումով հղում, որով աշակերտին կտրվի նրա մասնակցային հաշիվը։";i['release']="Հանձնել";i['releaseDesc1']="Հանձնված հաշիվը կրկին չի կարող դառնալ կառավարվող։ Աշակերտը կկարողանա ինքնուրույն անջատել/միացնել մանկական ռեժիմը և փոխել գաղտնաբառը։";i['releaseDesc2']="Աշակերտը կմնա դասարանում նույնիսկ երբ նրա հաշիվը հաձնվի իրեն։";i['releaseTheAccount']="Հանձնել հաշիվը աշակերտին, որպեսզի նա կառավարի ինքնուրույն։";i['removedByX']=s("Հեռացված է %s օգտատիրոջ կողմից");i['removedStudents']="Հեռացված է";i['removeStudent']="Հեռացնել աշակերտին";i['reopen']="Վերաբացել";i['resetPassword']="Չեղարկել գաղտնաբառը";i['sendAMessage']="Հաղորդագրություն ուղարկել բոլոր աշակերտներին։";i['students']="Աշակերտներ";i['studentsRealNamesOnePerLine']="Աշակերտների իրական անունները․ մեկական յուրաքանչյուր տողում";i['teachClassesOfChessStudents']="Շախմատն աշակերտներին դասավանդեք «Lichess-ի դասեր» գործիքների օգնությամբ։";i['teachers']="Դասավանդողներ";i['teachersOfTheClass']="Խմբի մարզիչները";i['teachersX']=s("Ուսուցիչներ՝ %s");i['thisStudentAccountIsManaged']="Այս աշակերտի հաշիվը ղեկավարում է դասավանդողը";i['timePlaying']="Խաղաժամանակ";i['trackStudentProgress']="Հետևեք աշակերտների առաջընթացին պարտիաներում և խնդիրներում";i['upgradeFromManaged']="Փոխել կառավարվողը ինքնավարով";i['useThisForm']="օգտագործեք այս ձևը";i['variantXOverLastY']=s("%1$s վերջին %2$sում");i['visibleByBothStudentsAndTeachers']="Երևում է ինչպես մարզչին, այնպես էլ խմբի աշակերտներին";i['welcomeToClass']=s("Բարի՜ գալուստ Ձեր դասարան՝ %s։\nԱհա դասարանի մուտքի հղումը։");i['winrate']="Հաղթանակների տոկոս";i['youAcceptedThisInvitation']="Դուք ընդունեցիք այս հրավերը։";i['youDeclinedThisInvitation']="Դուք մերժեցիք այս հրավերը։";i['youHaveBeenInvitedByX']=s("Դուք հրավիրվել եք %s։")})()