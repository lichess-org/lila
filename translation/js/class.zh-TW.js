"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.class)window.i18n.class={};let i=window.i18n.class;i['addLichessUsernames']="加入 Lichess 使用者名稱邀請他們做老師，每行一個";i['addStudent']="增加學生";i['aLinkToTheClassWillBeAdded']="班級的鏈接將會自動添加到訊息尾部，所以您不需要自己添加它。";i['anInvitationHasBeenSentToX']=s("邀請已發送到 %s");i['applyToBeLichessTeacher']="申請成為 Lichess 的教師";i['classDescription']="課程描述";i['className']="課程名稱";i['classNews']="課程消息";i['clickToViewInvitation']="點擊鏈接查看邀請：";i['closeClass']="關閉課程";i['closeDesc1']="學生將永遠無法再次使用此帳戶，關閉是永久性的。請確保學生理解並同意。";i['closeDesc2']="你可能想讓學生操控該帳戶，以便他們能夠繼續使用。";i['closeStudent']="關閉帳戶";i['closeTheAccount']="永久關閉學生帳戶";i['createANewLichessAccount']="創建一個新的帳戶";i['createDesc1']="如果學生沒有 Lichess 帳號，你可以在這裡幫他們建立。";i['createDesc2']="不需要任何電子信箱。課程密碼會自動建立，而您必須將此密碼傳給學生，他們才能登入";i['createDesc3']="注意：學生不得擁有多個帳號。";i['createDesc4']="如果他們已經有帳號，請使用邀請。";i['createMoreClasses']="創建更多班級";i['createMultipleAccounts']="同時開設多個Lichess帳戶";i['createStudentWarning']="僅為每個真實的學生創建賬戶，不要用此功能為你自己創建多個賬戶，否則你會被封號。";i['editNews']="編輯通知";i['features']="功能";i['freeForAllForever']="完全免費，一視同仁。且無廣告及追蹤器";i['generateANewPassword']="重設學生的密碼";i['generateANewUsername']="產生一個新的使用者名稱";i['invitationToClass']=s("您被邀請以學生身份加入班級“%s”。");i['invite']="邀請";i['inviteALichessAccount']="邀請一位玩家";i['inviteDesc1']="如果學生已經有 Lichess 的帳號，你可以邀請他們進入課程。";i['inviteDesc2']="他們會在 Lichess 上收到包含加入課堂網址的訊息。";i['inviteDesc3']="注意：只邀請你認識的、或積極想加入課程的學生。";i['inviteDesc4']="不要隨意邀請任何人";i['invitedToXByY']=s("%1$s被%2$s邀請");i['inviteTheStudentBack']="邀請退出的學生回來";i['lastActiveDate']="活動";i['lichessClasses']="課程";i['lichessProfileXCreatedForY']=s("為 %2$s 創建的 Lichess 檔案 %1$s。");i['lichessUsername']="Lichess 使用者名稱";i['makeSureToCopy']="確認你已複製或記下密碼。 你將無法再次看到它！";i['managed']="管理";i['maxStudentsNote']=s("請註意，一個班級最多可以有 %1$s 名學生。如需管理更多學生，%2$s。");i['messageAllStudents']="通知所有學生關於新教材";i['multipleAccsFormDescription']=s("您也可以%s通過學生名稱列表創建多個 Lichess 帳戶");i['na']="N/A";i['nbPendingInvitations']=p({"other":"%s個待處理的邀請"});i['nbStudents']=p({"other":"%s位學生"});i['nbTeachers']=p({"other":"%s位教師"});i['newClass']="新的課程";i['news']="最新消息";i['newsEdit1']="所有新聞一個字段中。";i['newsEdit2']="在頂部添加最近的新聞。不要刪除以前的新聞。";i['newsEdit3']="用----分離新聞\n它將顯示一條橫線。";i['noClassesYet']="還沒有課程";i['noRemovedStudents']="沒有被移除的學生";i['noStudents']="這個課程內還沒有學生";i['nothingHere']="這裡還沒有任何東西";i['notifyAllStudents']="提醒所有的學生";i['onlyVisibleToTeachers']="僅對班級老師可見";i['orSeparator']="或";i['overDays']="幾天來";i['overview']="概覽";i['passwordX']=s("密碼:%s");i['privateWillNeverBeShown']="隱私。永遠不會在課外顯示。有助回憶學生是誰。";i['progress']="進度";i['quicklyGenerateSafeUsernames']="快速為學生建立安全的使用者名稱及密碼";i['realName']="真實名稱";i['realUniqueEmail']="真實且唯一的學生郵箱。我們將向該郵箱發送一個帶有激活帳號鏈接的確認郵件。";i['release']="正式版";i['releaseDesc1']="已釋放的帳戶不能再次被限製。學生將能切換兒童模式開關和重置密碼。";i['releaseDesc2']="該學生在帳戶激活後將留在課堂。";i['releaseTheAccount']="解放帳戶，以便學生能夠自主管理。";i['removedByX']=s("被 %s 移除");i['removedStudents']="移除";i['removeStudent']="移除學生";i['reopen']="重新開放";i['resetPassword']="重設密碼";i['sendAMessage']="傳送訊息給所有的學生";i['studentCredentials']=s("學生名稱：%1$s\n帳戶名稱：%2$s\n密碼：%3$s");i['students']="學生";i['studentsRealNamesOnePerLine']="學生的真實姓名，每行一個";i['teachClassesOfChessStudents']="使用 Lichess 班級管理工具包將西洋棋的技術交給學生。";i['teachers']="教師";i['teachersOfTheClass']="這堂課程的教師";i['teachersX']=s("%s個教師");i['thisStudentAccountIsManaged']="這個學生帳戶被限製";i['timePlaying']="奕棋時間";i['trackStudentProgress']="紀錄學生在棋局和謎題中的進度";i['upgradeFromManaged']="從受限製的升級為自由的";i['useThisForm']="使用此表單";i['variantXOverLastY']=s("%1$s 超過了最近的 %2$s");i['visibleByBothStudentsAndTeachers']="課程的學生及教師皆可見";i['welcomeToClass']=s("歡迎來到你的班級：%s。\n這裏是訪問班級的鏈接。");i['winrate']="勝率";i['xAlreadyHasAPendingInvitation']=s("%s 已經有待處理的邀請");i['xIsAKidAccountWarning']=s("%1$s 是未成年帳戶，無法接收您的消息。您必須手動給他發送邀請鏈接： %2$s");i['xisNowAStudentOfTheClass']=s("%s 現在是班級的學生");i['youAcceptedThisInvitation']="你接受了此邀請";i['youDeclinedThisInvitation']="你拒絕了此邀請";i['youHaveBeenInvitedByX']=s("%s邀請你加入")})()