var exec = require('cordova/exec');

exports.videocall = function(caller, callee, config, success, error) 
{
    exec(success, error, "CallPlugin", "videocall", [caller, callee, config]);
};
