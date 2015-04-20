var express = require('express');
var app = express();
var favicon = require('serve-favicon');
var port = process.env.PORT || 1234;

var pageData = {
  "javascriptsBase": "/public",
  "stylesheetsBase": "/public",
  "imagesBase": "/public/images"
};

app.use('/public', express.static(__dirname + '/resources/public'));

app.use(favicon('./resources/public/favicon.ico'));

app.set('view engine', 'jade');
app.set('views', './assets/jade');

function beforeAllFilter(req, res, next) {
  app.locals.pretty = true;

  next();
}

app.all('*', beforeAllFilter);

app.get('/', function (req, res) {
  res.render('index', pageData);
});

app.get('/:name', function(req , res){
  var pageName = req.params.name;
  res.render(pageName, pageData);
});

app.get('/:folder/:name', function(req , res){
  var pageName = req.params.folder + "/" + req.params.name;
  res.render(pageName, pageData);
});


app.listen(port);