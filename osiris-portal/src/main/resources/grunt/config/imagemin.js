'use strict';

module.exports = {
    dist: {
        files: [{
            expand: true,
            cwd: '<%= osiris.app %>/images',
            src: '{,*/}*.{png,jpg,jpeg,gif}',
            dest: '<%= osiris.dist %>/images'
        }]
    }
};
