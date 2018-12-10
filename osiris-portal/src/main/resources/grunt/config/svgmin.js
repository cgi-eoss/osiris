'use strict';

module.exports = {
    dist: {
        files: [{
            expand: true,
            cwd: '<%= osiris.app %>/images',
            src: '{,*/}*.svg',
            dest: '<%= osiris.dist %>/images'
        }]
    }
};
