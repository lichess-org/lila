"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.study)window.i18n.study={};let i=window.i18n.study;i['addMembers']="Thêm thành viên";i['addNewChapter']="Thêm một chương mới";i['allowCloning']="Cho phép tạo bản sao";i['allStudies']="Tất cả các nghiên cứu";i['allSyncMembersRemainOnTheSamePosition']="Đồng bộ hóa tất cả các thành viên trên cùng một thế cờ";i['alphabetical']="Theo thứ tự chữ cái";i['analysisMode']="Chế độ phân tích";i['annotateWithGlyphs']="Chú thích bằng dấu";i['attack']="Tấn công";i['automatic']="Tự động";i['back']="Quay Lại";i['blackIsBetter']="Bên đen lợi thế hơn";i['blackIsSlightlyBetter']="Bên đen có một chút lợi thế";i['blackIsWinning']="Bên đen đang thắng dần";i['blunder']="Sai lầm nghiêm trọng";i['brilliantMove']="Nước đi thiên tài";i['chapterPgn']="PGN chương";i['chapterX']=s("Chương %s");i['clearAllCommentsInThisChapter']="Xóa tất cả bình luận, dấu chú thích và hình vẽ trong chương này";i['clearAnnotations']="Xóa chú thích";i['clearChat']="Xóa trò chuyện";i['clearVariations']="Xóa các biến";i['cloneStudy']="Nhân bản";i['commentThisMove']="Bình luận về nước cờ này";i['commentThisPosition']="Bình luận về thế cờ này";i['confirmDeleteStudy']=s("Xóa toàn bộ nghiên cứu? Không có cách nào để khôi phục lại! Nhập tên của nghiên cứu để xác nhận: %s");i['contributor']="Người đóng góp";i['contributors']="Những người đóng góp";i['copyChapterPgn']="Sao chép PGN";i['counterplay']="Phản công";i['createChapter']="Tạo chương";i['createStudy']="Tạo nghiên cứu";i['currentChapterUrl']="URL chương hiện tại";i['dateAddedNewest']="Ngày được thêm (mới nhất)";i['dateAddedOldest']="Ngày được thêm (cũ nhất)";i['deleteChapter']="Xóa chương";i['deleteStudy']="Xóa nghiên cứu";i['deleteTheStudyChatHistory']="Xóa lịch sử trò chuyện nghiên cứu? Không thể khôi phục lại!";i['deleteThisChapter']="Xóa chương này. Sẽ không có cách nào để có thể khôi phục lại!";i['development']="Phát triển";i['downloadAllGames']="Tải về tất cả ván đấu";i['downloadGame']="Tải về ván cờ";i['dubiousMove']="Nước đi mơ hồ";i['editChapter']="Sửa chương";i['editor']="Chỉnh sửa bàn cờ";i['editStudy']="Chỉnh sửa nghiên cứu";i['embedInYourWebsite']="Nhúng vào trang web của bạn";i['empty']="Trống";i['enableSync']="Kích hoạt tính năng đồng bộ hóa";i['equalPosition']="Thế trận cân bằng";i['everyone']="Mọi người";i['first']="Trang đầu";i['getAFullComputerAnalysis']="Nhận phân tích máy tính phía máy chủ đầy đủ về biến chính.";i['goodMove']="Nước tốt";i['hideNextMoves']="Ẩn các nước tiếp theo";i['hot']="Thịnh hành";i['importFromChapterX']=s("Nhập từ chương %s");i['initiative']="Chủ động";i['interactiveLesson']="Bài học tương tác";i['interestingMove']="Nước đi hay";i['inviteOnly']="Chỉ những người được mời";i['inviteToTheStudy']="Mời vào nghiên cứu";i['kick']="Đuổi";i['last']="Trang cuối";i['leaveTheStudy']="Rời khỏi nghiên cứu";i['like']="Thích";i['loadAGameByUrl']="Tải ván cờ bằng URL";i['loadAGameFromPgn']="Tải ván cờ từ PGN";i['loadAGameFromXOrY']=s("Tải ván cờ từ %1$s hoặc %2$s");i['loadAPositionFromFen']="Tải thế cờ từ chuỗi FEN";i['makeSureTheChapterIsComplete']="Hãy chắc chắn chương đã hoàn thành. Bạn chỉ có thể yêu cầu phân tích 1 lần.";i['manageTopics']="Quản lý chủ đề";i['members']="Thành viên";i['mistake']="Sai lầm";i['mostPopular']="Phổ biến nhất";i['myFavoriteStudies']="Các nghiên cứu yêu thích của tôi";i['myPrivateStudies']="Nghiên cứu riêng tư của tôi";i['myPublicStudies']="Nghiên cứu công khai của tôi";i['myStudies']="Các nghiên cứu của tôi";i['myTopics']="Chủ đề của tôi";i['nbChapters']=p({"other":"%s Chương"});i['nbGames']=p({"other":"%s Ván cờ"});i['nbMembers']=p({"other":"%s Thành viên"});i['newChapter']="Chương mới";i['newTag']="Nhãn mới";i['next']="Trang tiếp theo";i['nextChapter']="Chương tiếp theo";i['nobody']="Không ai cả";i['noLetPeopleBrowseFreely']="Không: để mọi người tự do xem xét";i['noneYet']="Chưa có gì cả.";i['noPinnedComment']="Không có";i['normalAnalysis']="Phân tích thường";i['novelty']="Mới lạ";i['onlyContributorsCanRequestAnalysis']="Chỉ những người đóng góp nghiên cứu mới có thể yêu cầu máy tính phân tích.";i['onlyMe']="Chỉ mình tôi";i['onlyMove']="Nước duy nhất";i['onlyPublicStudiesCanBeEmbedded']="Chỉ các nghiên cứu công khai mới được nhúng!";i['open']="Mở";i['orientation']="Nghiên cứu cho bên";i['pasteYourPgnTextHereUpToNbGames']=p({"other":"Dán PGN ở đây, tối đa %s ván"});i['pgnTags']="Nhãn PGN";i['pinnedChapterComment']="Đã ghim bình luận chương";i['pinnedStudyComment']="Bình luận nghiên cứu được ghim";i['playAgain']="Chơi lại";i['playing']="Đang chơi";i['pleaseOnlyInvitePeopleYouKnow']="Vui lòng chỉ mời những người bạn biết và những người tích cực muốn tham gia nghiên cứu này.";i['popularTopics']="Chủ đề phổ biến";i['prevChapter']="Chương trước";i['previous']="Trang trước";i['private']="Riêng tư";i['public']="Công khai";i['readMoreAboutEmbedding']="Đọc thêm về việc nhúng";i['recentlyUpdated']="Được cập nhật gần đây";i['rightUnderTheBoard']="Ngay dưới bàn cờ";i['save']="Lưu";i['saveChapter']="Lưu chương";i['searchByUsername']="Tìm kiếm theo tên người dùng";i['shareAndExport']="Chia sẻ & xuất";i['shareChanges']="Chia sẻ các thay đổi với khán giả và lưu chúng trên máy chủ";i['showEvalBar']="Thanh lợi thế";i['spectator']="Khán giả";i['start']="Bắt đầu";i['startAtInitialPosition']="Bắt đầu từ thế cờ ban đầu";i['startAtX']=s("Bắt đầu tại nước %s");i['startFromCustomPosition']="Bắt đầu từ thế cờ tùy chỉnh";i['startFromInitialPosition']="Bắt đầu từ thế cờ ban đầu";i['studiesCreatedByX']=s("Các nghiên cứu được tạo bởi %s");i['studiesIContributeTo']="Các nghiên cứu tôi đóng góp";i['studyActions']="Các thao tác trong nghiên cứu";i['studyNotFound']="Không tìm thấy nghiên cứu nào";i['studyPgn']="PGN nghiên cứu";i['studyUrl']="URL nghiên cứu";i['theChapterIsTooShortToBeAnalysed']="Chương này quá ngắn để có thể được phân tích.";i['timeTrouble']="Sắp hết thời gian";i['topics']="Chủ đề";i['unclearPosition']="Thế cờ không rõ ràng";i['unlike']="Bỏ thích";i['unlisted']="Không công khai";i['urlOfTheGame']="URL của các ván, một URL mỗi dòng";i['visibility']="Khả năng hiển thị";i['whatAreStudies']="Nghiên cứu là gì?";i['whatWouldYouPlay']="Bạn sẽ làm gì ở thế cờ này?";i['whereDoYouWantToStudyThat']="Bạn muốn nghiên cứu ở đâu?";i['whiteIsBetter']="Bên trắng lợi thế hơn";i['whiteIsSlightlyBetter']="Bên trắng có một chút lợi thế";i['whiteIsWinning']="Bên trắng đang thắng dần";i['withCompensation']="Có bù đắp";i['withTheIdea']="Với ý tưởng";i['xBroughtToYouByY']=s("%1$s được lấy từ %2$s");i['yesKeepEveryoneOnTheSamePosition']="Có: giữ tất cả mọi người trên 1 thế cờ";i['youAreNowAContributor']="Bây giờ bạn là một người đóng góp";i['youAreNowASpectator']="Bây giờ bạn là một khán giả";i['youCanPasteThisInTheForumToEmbed']="Bạn có thể dán cái này để nhúng vào diễn đàn hoặc blog Lichess cá nhân của bạn";i['youCompletedThisLesson']="Chúc mừng! Bạn đã hoàn thành bài học này.";i['zugzwang']="Zugzwang"})()