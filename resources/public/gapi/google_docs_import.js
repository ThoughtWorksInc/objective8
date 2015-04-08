// The Browser API key obtained from the Google Developers Console.
// ROB: I think this is unneccessary when using clientId and oAuth
// var developerKey = API-KEY in developer console

// The Client ID obtained from the Google Developers Console. Replace with your own Client ID.
var clientId = "464714390966-pa0jmj4vupam0eqfiap76tq70586lpk6.apps.googleusercontent.com";

// Replace with your own App ID. (Its the first number in your Client ID)
var appId = "464714390966";

// Scope to use to access user's photos.
var scope = ['https://www.googleapis.com/auth/drive.readonly'];

var pickerApiLoaded = false;
var oauthToken;

// Use the Google API Loader script to load the google.picker script.
function loadPicker() {
  gapi.load('picker', {'callback': loadAuth});
}

function loadAuth() {
  gapi.load('auth', {'callback': authLoaded});
}
function authLoaded() {
  window.gapi.auth.authorize(
      {
        'client_id': clientId,
    'scope': scope,
    'immediate': false
      },
      handleAuthResult);
}

function handleAuthResult(authResult) {
  if (authResult && !authResult.error) {
    oauthToken = authResult.access_token;
  }
  loadClient();
}

function loadClient() {
  gapi.load('client', {'callback': loadDrive});
}

function loadDrive() {
  gapi.client.load('drive', 'v2').then(createPicker);
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
    var fileId = data.docs[0].id;
    getFile(fileId);
  }
}

function createPicker() {
  if (oauthToken) {
    var view = new google.picker.DocsView(google.picker.ViewId.DOCUMENTS);
    view.setMode(google.picker.DocsViewMode.LIST);
    var picker = new google.picker.PickerBuilder()
      //.enableFeature(google.picker.Feature.NAV_HIDDEN)
      //.enableFeature(google.picker.Feature.MULTISELECT_ENABLED)
      .setAppId(appId)
      .setOAuthToken(oauthToken)
      .addView(view)
      //      .setDeveloperKey(developerKey)
      .setCallback(pickerCallback)
      .build();
    picker.setVisible(true);
  }
}

function downloadFile(html_download_url) {
  var accessToken = gapi.auth.getToken().access_token;
  var xhr = new XMLHttpRequest();
  xhr.open('GET', html_download_url);
  xhr.setRequestHeader('Authorization', 'Bearer ' + accessToken);
  xhr.onload = function() {
    document.getElementsByClassName('clj-import-draft-preview')[0].innerHTML = xhr.responseText;
    document.getElementsByClassName('clj-google-doc-html-content')[0].value = xhr.responseText;
    document.getElementsByClassName('clj-submit-draft-button')[0].removeAttribute("disabled");
  };
  xhr.onerror = function() {
    alert('error');
  };
  xhr.send();
}
