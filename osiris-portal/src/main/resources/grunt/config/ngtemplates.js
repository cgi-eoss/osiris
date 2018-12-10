'use strict';

module.exports = {
    dist: {
        options: {
            module: 'osirisApp',
            htmlmin: '<%= htmlmin.dist.options %>'
        },
        cwd: '<%= osiris.app %>',
        src: 'views/{,*/}*.html',
        dest: '<%= osiris.dist %>/scripts/templateCache.js'
    }
};
