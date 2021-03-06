var gulp = require('gulp');
var filter = require('gulp-filter');
var mkdirp = require('mkdirp');
var del = require('del');

 var destLib = 'libs'
 var srcLib = ['node_modules/**/**',
 'bower_components/**/**',
 '!node_modules/**/node_modules/**',
 //'!node_modules/**/lib/**',
 '!node_modules/**/test/**',
 '!node_modules/**/examples/**',
 ];

 /* temporary lib cleaner */
 gulp.task('clean-node', function (cb) {
 	del(['node_modules'], cb);
 });

 gulp.task('clean-bower', function (cb) {
 	del(['bower_components'], cb);
 });

 /* for resteing libs copy */
 gulp.task('clean-libs', function (cb) {
 	del(['libs'], cb);
 });

  gulp.task('clean-all',['clean-libs','clean-bower','clean-node']);


 /* default tasks */
 gulp.task('mkdir', function() {
 	mkdirp(destLib, function (err) {
 		if (err) console.error(err);
 	});
 });
 gulp.task('js', function() {
 	gulp.src(srcLib)
 	.pipe(filter('**/*.min.js'))
 	.pipe(gulp.dest(destLib));

 	gulp.src(srcLib)
 	.pipe(filter('**/*.js'))
 	.pipe(gulp.dest(destLib));
 
 	gulp.src(srcLib)
 	.pipe(filter('**/*.min.js.map'))
 	.pipe(gulp.dest(destLib));
 });

 gulp.task('css', function() {
 	 gulp.src(srcLib)
 	.pipe(filter('**/*.css'))
 	.pipe(gulp.dest(destLib));
 });


// Default Task
gulp.task('default', ['mkdir','js','css']);

// reset and copy
gulp.task('reset-default', ['clean-libs','mkdir','js']);