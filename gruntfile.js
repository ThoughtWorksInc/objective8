'use strict';

module.exports = function(grunt) {

  require('load-grunt-tasks')(grunt);

  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),

    watch: {
      gruntfile: {
        files: ['gruntfile.js']
      },
      sass: {
        files: ['resources/src/scss/**/*.scss'],
        tasks: ['sass:dev', 'autoprefixer:dev']
      },
      jade: {
        files: ['resources/src/jade/**/*.jade'],
        tasks: ['jade:compile']
      },
      express: {
        files:  [ '**/*.js' ],
        tasks:  [ 'express:dev' ],
        options: {
          spawn: false // for grunt-contrib-watch v0.5.0+, "nospawn: true" for lower versions. Without this option specified express won't be reloaded
        }
      }
    },

    //Compile HTML
    jade: {
      compile: {
        options: {
          pretty: true,
          data: {
            javascriptsBase: "/static",
            stylesheetsBase: "/static",
            imagesBase: "/static"
          }
        },
        files: [
          {
            cwd: "resources/src/jade",
            src: [
              '**/*.jade',
              '!**/_*.jade',
              '!layout/**'
            ],
            dest: "resources/templates/jade",
            expand: true,
            ext: ".html"
          }
        ]
      }
    },

    //Compile SCSS
    sass: {
      dev: {
        options: {
          sourceMap: true,
          outputStyle: 'expanded'
        },
        files: {
          'resources/public/basic.css': 'resources/src/scss/basic.scss',
          'resources/public/ie8.css': 'resources/src/scss/ie8.scss',
          'resources/public/modern.css': 'resources/src/scss/modern.scss'
        }
      },
      dist: {
        options: {
          sourceMap: false,
          outputStyle: 'compressed'
        },
        files: {
          'resources/public/basic.css': 'resources/src/scss/basic.scss',
          'resources/public/ie8.css': 'resources/src/scss/ie8.scss',
          'resources/public/modern.css': 'resources/src/scss/modern.scss'
        }
      }
    },
    //Add vendor prefixed styles
    autoprefixer: {
      options: {
        browsers: ['last 2 version', 'ie 8', 'ie 9']
      },
      dev: {
        options: {
          map: true
        },
        no_dest: {
          src: 'resources/public/modern.css'
        }
      },
      dist: {
        options: {
          map: false
        },
        no_dest: {
          src: 'resources/public/modern.css'
        }
      }
    },

    //JAVASCRIPT
    jshint: {
      options: {
        jshintrc: '.jshintrc',
        reporter: require('jshint-stylish')
      },
      all: [
        'resources/src/custom/{,*/}*.js',
        '!resources/src/vendor/*'
      ]
    },
    //Stick everything together, you'll need to specify JS files in the correct order here.
    concat: {
      dist: {
        src: [
          'resources/src/js/vendor/jquery-1.11.2.js',
          'resources/src/js/vendor/webfont.js',
          'resources/src/js/custom/**/*.js'
        ],
        dest: 'resources/public/scripts.js'
      }
    },
    //Uglify
    uglify: {
      modernizr: {
        src: 'resources/src/js/vendor/modernizr.js',
        dest: 'resources/public/modernizr.min.js'
      },
      build: {
        src: 'resources/public/scripts.js',
        dest: 'resources/public/scripts.min.js'
      }
    },

    express: {
      options: {
        port: 1234
      },
      dev: {
        options: {
          script: 'dev-app.js'
        }
      }
    },

    browserSync: {
      default_options: {
        bsFiles: {
          src: [
            "resources/public/*.css",
            "resources/src/jade/**/*.jade"
          ]
        },
        options: {
          watchTask: true,
          proxy: "localhost:1234",
          port: 2345
        }
      }
    }
  });

  grunt.registerTask('dev',[
    'sass:dev',
    'autoprefixer:dev',
    'jade',
    'jshint',
    'concat',
    'uglify',
    'watch'
  ]);

  grunt.registerTask('design',[
    'sass:dev',
    'autoprefixer:dev',
    'jshint',
    'concat',
    'uglify',
    'browserSync',
    'express',
    'watch'
  ]);

  grunt.registerTask('build',[
    'sass:dist',
    'autoprefixer:dist',
    'jade',
    'jshint',
    'concat',
    'uglify'
  ]);

  grunt.registerTask('default', [
    'build'
  ]);
};
