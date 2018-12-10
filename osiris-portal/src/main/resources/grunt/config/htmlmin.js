'use strict';

module.exports = {
    dist: {
        options: {
            collapseWhitespace: true,
            conservativeCollapse: true,
            collapseBooleanAttributes: true,
            removeCommentsFromCDATA: true
        },
        files: [{
            expand: true,
            cwd: '<%= osiris.dist %>',
            src: ['**/*.html'],
            dest: '<%= osiris.dist %>'
        }]
    }
};
