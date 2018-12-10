'use strict';

// Make sure code styles are up to par
module.exports = {
    options: {
        config: '.jscsrc',
        verbose: true
    },
    all: {
        src: [
          '<%= osiris.app %>/scripts/{,*/}*.js'
        ]
    },
    test: {
        src: ['test/spec/{,*/}*.js']
    }
};
