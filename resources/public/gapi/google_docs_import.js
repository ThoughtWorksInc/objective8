// The Browser API key obtained from the Google Developers Console.
// ROB: I think this is unneccessary when using clientId and oAuth
// var developerKey = API-KEY in developer console

// The Client ID obtained from the Google Developers Console. Replace with your own Client ID.
var clientId = "508780744011-9fp3bi7ttoeu54qfejbv11o08hjd07b2.apps.googleusercontent.com";

// Replace with your own App ID. (Its the first number in your Client ID)
var appId = "508780744011";

// Scope to use to access user's photos.
var scope = ['https://www.googleapis.com/auth/drive.readonly'];

var pickerApiLoaded = false;
var clientLoaded = false;
var driveLoaded = false;
var oauthToken;

function downloadFile(html_download_url) {
  var accessToken = gapi.auth.getToken().access_token;
  var xhr = new XMLHttpRequest();
  xhr.open('GET', html_download_url);
  xhr.setRequestHeader('Authorization', 'Bearer ' + accessToken);
  xhr.onload = function() {
    $('.clj-google-doc-html-content').val(xhr.responseText);
    $('.clj-preview-draft-button').removeAttr('disabled');
    $('.clj-preview-draft-button').click();
  };
  xhr.onerror = function() {
    alert('error');
  };
  xhr.send();
}

function getFile(fileId) {
  var request = gapi.client.drive.files.get({
    'fileId': fileId
  });
  request.execute(function(resp) {
    var html_download_url = resp['exportLinks']['text/html'];
    console.log(html_download_url);
    downloadFile(html_download_url); 
  });
}

function pickerCallback(data) {
  if (data.action == google.picker.Action.PICKED) {
    $('.clj-import-draft-form').hide();
    $('.clj-import-draft-title').after('<div style="text-align: center;"><i class="fa fa-spinner fa-spin fa-3x"></i></div>');
    var fileId = data.docs[0].id;
    getFile(fileId);
  }
}

function createPicker() {
  if (oauthToken && pickerApiLoaded && clientLoaded && driveLoaded) {
    var view = new google.picker.DocsView(google.picker.ViewId.DOCUMENTS);
    view.setMode(google.picker.DocsViewMode.LIST);
    var picker = new google.picker.PickerBuilder()
      .setAppId(appId)
      .setOAuthToken(oauthToken)
      .addView(view)
      .setCallback(pickerCallback)
      .build();
    picker.setVisible(true);
  }
}

function handleAuthResult(authResult) {
  if (authResult && !authResult.error) {
    oauthToken = authResult.access_token;
    createPicker();
  }
}

function authoriseApp() {
  window.gapi.auth.authorize(
      { 'client_id': clientId, 'scope': scope, 'immediate': false },
      handleAuthResult);
}

function onPickerApiLoad() {
  pickerApiLoaded = true;
  createPicker();
}

function onDriveApiLoad() {
  driveLoaded = true;
  createPicker();
}

function onClientApiLoad() {
  clientLoaded = true;
  gapi.client.load('drive', 'v2').then(onDriveApiLoad);
}

function onApiLoad() {
  gapi.load('picker', {'callback': onPickerApiLoad});
  gapi.load('client', {'callback': onClientApiLoad});
} 
