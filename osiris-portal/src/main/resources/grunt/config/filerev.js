'use strict';

// Renames files for browser caching purposes
module.exports = {
        dist: {
            src: [
              '<%= osiris.dist %>/scripts/{,*/}*.js',
              '<%= osiris.dist %>/styles/{,*/}*.css',
              '<%= osiris.dist %>/images/{,*/}*.{png,jpg,jpeg,gif,webp,svg}',
              '<%= osiris.dist %>/styles/fonts/*'
            ]
        }
};
