module.exports = function(grunt) {
  grunt.initConfig({    
    pkg: grunt.file.readJSON('package.json'),
    
    watch: {
      sass: {
        files: ['resources/src/sass/**/*.scss'],
        tasks: ['sass:dist']
      }
    },
    
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
    }
  });

  grunt.loadNpmTasks('grunt-sass');
  grunt.loadNpmTasks('grunt-contrib-watch');  
  
  grunt.registerTask('build',[
    'sass:dist'
  ]);

  grunt.registerTask('dev',[
    'sass:dist',
    'watch'
  ]);

  grunt.registerTask('default', 'build');
};