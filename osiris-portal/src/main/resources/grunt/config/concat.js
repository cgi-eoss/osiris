'use strict';

// Empties folders to start fresh
module.exports = {
    options: {
    },
    dist: {
      src: ['.tmp/post/styles/**/*.css'],
      dest: '<%= osiris.app %>/main.css',
    }
};
