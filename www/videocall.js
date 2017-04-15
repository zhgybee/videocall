var exec = require('cordova/exec');

exports.videocall = function(caller, callee, success, error) 
{
    exec(success, error, "CallPlugin", "videocall", [caller, callee]);
};
