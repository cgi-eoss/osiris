'use strict';

// Make sure there are no obvious mistakes
module.exports = {
    options: {
        jshintrc: '.jshintrc',
        reporter: require('jshint-stylish')
    },
    all: {
        src: [
            '<%= osiris.app %>/{,*/}*.js',
            '<%= osiris.app %>/scripts/controllers/**/*.js',
            '<%= osiris.app %>/scripts/services/**/*.js'
        ]
    },
    test: {
        options: {
            jshintrc: 'test/.jshintrc'
        },
        src: ['test/spec/{,*/}*.js']
    }
};
