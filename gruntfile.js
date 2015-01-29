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
        files: ['resources/src/sass/**/*.scss'],
        tasks: ['sass:dev', 'autoprefixer:dev']
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
          'resources/public/styles.css': 'resources/src/sass/*.scss'
        }
      },
      dist: {
        options: {
          sourceMap: false,
          outputStyle: 'compressed'
        },
        files: {
          'resources/public/styles.css': 'resources/src/sass/*.scss'
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
          map: true,
        },
        no_dest: {
          src: 'resources/public/styles.css'
        }
      },
      dist: {
        options: {
          map: false,
        },
        no_dest: {
          src: 'resources/public/styles.css'
        }
      }
    },


    //Optimise Images
    imagemin: {
      files: {                         
        expand: true,
        cwd: 'resources/src/images/',
        src: ['**/*.{png,jpg,gif,svg,ico}'],
        dest: 'resources/public/images/'
      }
    }
  });

  grunt.registerTask('dev',[
    'sass:dev',
    'autoprefixer:dev',
    'imagemin',
    'watch'
  ]);
  
  grunt.registerTask('build',[
    'sass:dist',
    'autoprefixer:dist',
    'imagemin'
  ]);

  grunt.registerTask('default', [
    'build'
  ]);
};