 'use strict';

// Watches less files and coverts them to css
 module.exports = {
     options: {
         paths: ["<%= osiris.app %>/styles/less"],
         yuicompress: true
     },
     files: {
         expand: true,
         cwd: '<%= osiris.app %>/styles/less',
         src: ['**/*.less', '!{variable, mixins}*.less'],
         dest: '.tmp/styles',
         ext: '.css'
     },
 };
