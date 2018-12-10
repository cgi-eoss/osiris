'use strict';

// Performs rewrites based on filerev and the useminPrepare configuration
module.exports = {
    html: ['<%= osiris.dist %>/{,*/}*.html'],
    css: ['<%= osiris.dist %>/styles/{,*/}*.css'],
    js: ['<%= osiris.dist %>/scripts/{,*/}*.js'],
    options: {
        assetsDirs: [
          '<%= osiris.dist %>',
          '<%= osiris.dist %>/images',
          '<%= osiris.dist %>/styles'
        ],
        patterns: {
            js: [[/(images\/[^''""]*\.(png|jpg|jpeg|gif|webp|svg))/g, 'Replacing references to images']]
        }
    }
};
