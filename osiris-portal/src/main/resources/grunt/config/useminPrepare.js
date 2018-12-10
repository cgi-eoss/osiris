'use strict';

/* Reads HTML for usemin blocks to enable smart builds that automatically
 * concat, minify and revision files. Creates configurations in memory so
 * additional tasks can operate on them. */
module.exports = {
    html: '<%= osiris.app %>/index.html',
    css: ['<%= osiris.app %>/main.css'],
    options: {
        dest: '<%= osiris.dist %>',
        flow: {
            html: {
                steps: {
                    js: ['concat', 'uglifyjs'],
                    css: ['cssmin']
                },
                post: {}
            }
        }
    }
};
