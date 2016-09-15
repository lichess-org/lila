

var editForumPost = function(postNumber) {
    var postSelector = $("#" + postNumber);

    // We grab the text element of the post and turn it into a textarea to make it editable
    var textSelector = postSelector.find(".message")

    var postContents = textSelector.text();

    var editableArea = $("<textarea style='width: 100%; height:300px'>");

    editableArea.text(postContents);

    textSelector.replaceWith(editableArea);

}