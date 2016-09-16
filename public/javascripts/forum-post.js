
var oldContents = null;

var editForumPost = function(postId, postNumber) {
    var postSelector = $("#" + postNumber);

    // We grab the text element of the post and turn it into a textarea to make it editable
    oldContents = postSelector.find(".message")

    var postContents = oldContents.text();

    var editForm = $('<form>', {
                                id: 'post-edit-form-' + postNumber,
                                method: 'POST',
                                action:"/forum/post/" + postId
                                });

    var formTextArea = $('<textarea>', {id:'post-edit-area-' + postNumber, name:"changes", class:'edit-post-box'});
    formTextArea.text(postContents);

    var formSubmitButton = $('<input>', {type:'submit', value: 'Submit'});
    var formCancelButton = $('<a>', {'data-icon':'s', onclick:'cancelEdit(' + postNumber + ')' }).text("Cancel");

    editForm.append(formTextArea);
    editForm.append(formSubmitButton);
    editForm.append(formCancelButton);

    oldContents.replaceWith(editForm);

    $("#edit-button-" + postNumber).hide();
};

var cancelEdit = function(postNumber) {
    $('#post-edit-form-' + postNumber).replaceWith(oldContents);

    $("#edit-button-" + postNumber).show();
}