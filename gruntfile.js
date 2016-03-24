'use strict';

module.exports = function(grunt) {

  require('load-grunt-tasks')(grunt);

  var folders = {
    src: 'assets',
    output: 'resources'
  };

  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),
    folders: folders,

    watch: {
      gruntfile: {
        files: ['gruntfile.js']
      },
      sass: {
        files: ['<%= folders.src %>/scss/**/*.scss'],
        tasks: ['sass:dev', 'autoprefixer:dev']
      },
      jade: {
        files: ['<%= folders.src %>/jade/**/*.jade'],
        tasks: ['jade:compile']
      },
      images: {
        files: ['<%= folders.src %>/images/**/*.*'],
        tasks: ['copy:images']
      },
      express: {
        files:  [ '<%= folders.src %>/js/**/*.js' ],
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
            cwd: '<%= folders.src %>/jade',
            src: [
              '**/*.jade',
              '!**/_*.jade',
              '!layout/**'
            ],
            dest: '<%= folders.output %>/templates/jade',
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
          //sourceMap: true,
          sourceComments: true,
          outputStyle: 'expanded'
        },
        files: {
          '<%= folders.output %>/public/basic.css': '<%= folders.src %>/scss/basic.scss',
          '<%= folders.output %>/public/ie8.css': '<%= folders.src %>/scss/ie8.scss',
          '<%= folders.output %>/public/modern.css': '<%= folders.src %>/scss/modern.scss'
        }
      },
      dist: {
        options: {
          sourceMap: false,
          outputStyle: 'compressed'
        },
        files: {
          '<%= folders.output %>/public/basic.css': '<%= folders.src %>/scss/basic.scss',
          '<%= folders.output %>/public/ie8.css': '<%= folders.src %>/scss/ie8.scss',
          '<%= folders.output %>/public/modern.css': '<%= folders.src %>/scss/modern.scss'
        }
      }
    },
    //Add vendor prefixed styles
    autoprefixer: {
      options: {
        browsers: ['last 2 versions', 'ie 9']
      },
      dev: {
        options: {
          map: true
        },
        files: {
          '<%= folders.output %>/public/modern.css': '<%= folders.output %>/public/modern.css'
        }
      },
      dist: {
        options: {
          map: false
        },
        files: {
          '<%= folders.output %>/public/modern.css': '<%= folders.output %>/public/modern.css'
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
        '<%= folders.src %>/custom/{,*/}*.js',
        '!<%= folders.output %>/src/vendor/*'
      ]
    },
    //Stick everything together, you'll need to specify JS files in the correct order here.
    concat: {
      dist: {
        src: [
          '<%= folders.src %>/js/vendor/jquery-1.11.2.js',
          '<%= folders.src %>/js/vendor/jquery.scrollto.js',
          '<%= folders.src %>/js/custom/**/*.js'
        ],
        dest: '<%= folders.output %>/public/scripts.js'
      }
    },
    //Uglify
    uglify: {
      modernizr: {
        src: '<%= folders.src %>/js/vendor/modernizr.js',
        dest: '<%= folders.output %>/public/modernizr.min.js'
      },
      cookieconsent: {
        src: '<%= folders.src %>/js/vendor/cookieconsent.js',
        dest: '<%= folders.output %>/public/cookieconsent.min.js'
      },
      build: {
        src: '<%= folders.output %>/public/scripts.js',
        dest: '<%= folders.output %>/public/scripts.min.js'
      }
    },

    copy: {
      images: {
        files: [{
            expand: true,
            cwd: '<%= folders.src %>/images',
            src: ['**'],
            dest: '<%= folders.output %>/public/images'
          }]
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

    clean: {
      build: {
        src: [ '<%= folders.output %>/templates/jade' ]
      }
    },

    browserSync: {
      default_options: {
        bsFiles: {
          src: [
            '<%= folders.output %>/public/*.css',
            '<%= folders.src %>/jade/**/*.jade'
          ]
        },
        options: {
          watchTask: true,
          proxy: "localhost:1234",
          port: 2345,
          startPath: '/_routes'
        }
      }
    }
  });

  grunt.registerTask('dev',[
    'clean:build',
    'sass:dev',
    'autoprefixer:dev',
    'copy:images',
    'jade',
    'jshint',
    'concat',
    'uglify',
    'watch'
  ]);

  grunt.registerTask('design',[
    'sass:dev',
    'autoprefixer:dev',
    'copy:images',
    'jshint',
    'concat',
    'uglify',
    'browserSync',
    'express',
    'watch'
  ]);

  grunt.registerTask('build',[
    'clean:build',
    'sass:dist',
    'autoprefixer:dist',
    'copy:images',
    'jade',
    'jshint',
    'concat',
    'uglify'
  ]);

  grunt.registerTask('default', [
    'build'
  ]);
};
