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
        tasks: ['sass:dist', 'autoprefixer']
      }
    },
    
    //Compile SCSS
    sass: {
      options: {
        sourceMap: true,
        outputStyle: 'compressed'
      },
      dist: {
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
      no_dest: {
        src: 'resources/public/styles.css'
      },
    },
  });

  grunt.registerTask('dev',[
    'sass:dist',
    'autoprefixer',
    'watch'
  ]);
  
  grunt.registerTask('build',[
    'sass:dist',
    'autoprefixer'
  ]);

  grunt.registerTask('default', [
    'build'
  ]);
};