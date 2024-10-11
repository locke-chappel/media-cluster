/* ffmpeg.js */
$$.App.Templates.ffmpeg = {
    _splitArgs : function(args) {
        /* TODO be smarter and accept " or ' (right now single quotes are ignored) */
        return args.split(/ +(?=(?:(?:[^"]*"){2})*[^"]*$)/g);
    },
    Init : function() {
        $$.Events.Add("ddlFfmpegAudioCodec", "change", $$.App.Templates.ffmpeg.AudioCodecChange);
        $$.Events.Add("ddlFfmpegVideoCodec", "change", $$.App.Templates.ffmpeg.VideoCodecChange);
        $$.Events.Add("ddlFfmpegVideoQualityMode", "change", $$.App.Templates.ffmpeg.VideoQualityModeChange);
        
        var inputs = $$.Find("ffmpeg").querySelectorAll("input, select");
        $$.ForEach(inputs, function(input) {
            $$.Events.Add(input, "change", function(event) {
                $$.App.Templates.ffmpeg.UpdateArgs();
            });
        });
        
        $$.Events.Add("txtFfmpegAudioArgs", "change", function(event) {
            $$.App.Templates.ffmpeg.SetAudioArgs($$.App.Templates.ffmpeg.GetAudioArgs());
        });
                
        $$.Events.Add("txtFfmpegVideoArgs", "change", function(event) {
            $$.App.Templates.ffmpeg.SetVideoArgs($$.App.Templates.ffmpeg.GetVideoArgs());
        });
        
        $$.Events.Add("txtFfmpegCommonArgs", "change", function(event) {
            $$.App.Templates.ffmpeg.SetCommonArgs($$.App.Templates.ffmpeg.GetCommonArgs());
        });
        
        $$.Events.Add("txtSliceLength", "keydown", function(event){
            switch (event.keyCode) {
                case 107:
                case 109:
                case 110:
                case 187:
                case 189:
                case 190:
                    $$.Events.Cancel(event);
                    return;
                case $$.Events.KeyCodes.Backsapce:
                case $$.Events.KeyCodes.Tab:
                case $$.Events.KeyCodes.Enter:
                case $$.Events.KeyCodes.Shift:
                case $$.Events.KeyCodes.Control:
                case $$.Events.KeyCodes.Capslock:
                case $$.Events.KeyCodes.Escape:
                case $$.Events.KeyCodes.Left:
                case $$.Events.KeyCodes.Up:
                case $$.Events.KeyCodes.Right:
                case $$.Events.KeyCodes.Down:
                case $$.Events.KeyCodes.Delete:
                case $$.Events.KeyCodes.Meta:
                case $$.Events.KeyCodes.Windows:
                case $$.Events.KeyCodes.Context:
                    return;
                default:
                    break;
            }
            
            if (this.value.length >= 9) {
                $$.Events.Cancel(event);
            }
        });
        
        $$.Events.Add("chkSliceLength", "change", function(event) {
            $$.Find("txtSliceLength").readOnly = $$.Find("chkSliceLength").checked;
        });
        
        $$.Find("ddlFfmpegVideoCodec").CustomVideoCodec = "";
        
        $$.App.Templates.ffmpeg.UpdateArgs();
    },
    AudioCodecChange : function(event) {
        switch($$.Find("ddlFfmpegAudioCodec").value) {
            case "-an":
            case "-c:a copy":
                 $$.Find("ffmpegAudioBitrate").classList.add("hidden");
                $$.Find("ffmpegAudioChannels").classList.add("hidden");
                break;
            default:
                $$.Find("ffmpegAudioBitrate").classList.remove("hidden");
                $$.Find("ffmpegAudioChannels").classList.remove("hidden");
                break;
        }
    },
    GetArgValue : function(args, id, isFlag) {
        for (var i = 0; i < args.length; i++) {
            if (args[i] === id) {
                if (isFlag === true) {
                    return true;
                }
                
                if (i + 1 < args.length) {
                    return args[i + 1];
                }
                return "";
            }
        }
        
        if (isFlag === true) {
            return false;
        }
        return "";
    },
    GetArgFilterValue : function(filters, id, isFlag) {
        var vf = filters.split(",");
        for (var i = 0; i < vf.length; i++) {
            var f = vf[i].split("=");
            if (f[0] === id) {
                if (isFlag === true) {
                    return true;
                }
                
                if (f.length == 2) {
                    return f[1];
                }
                return "";
            }
        }
        
        if (isFlag === true) {
            return false;
        }
        return "";
    },
    GetAudioArgs : function() {
        var args = $$.Text.Trim($$.Find("txtFfmpegAudioArgs").value);
        args = $$.App.Templates.ffmpeg._splitArgs(args);
        args = args.filter(function(a) { return !$$.Text.IsBlank(a); });
        return args;
    },
    GetCommonArgs : function() {
        var args = $$.Text.Trim($$.Find("txtFfmpegCommonArgs").value);
        args = $$.App.Templates.ffmpeg._splitArgs(args);
        args = args.filter(function(a) { return !$$.Text.IsBlank(a); });
        return args;
    },
    GetVideoArgs : function() {
        var args = $$.Text.Trim($$.Find("txtFfmpegVideoArgs").value);
        args = $$.App.Templates.ffmpeg._splitArgs(args);
        args = args.filter(function(a) { return !$$.Text.IsBlank(a); });
        return args;
    },
    GetExt : function() {
        return $$.Text.Trim($$.Find("ddlFfmpegExt").value);
    },
    GetSliceLength : function() {
        if ($$.Find("chkSliceLength").checked === true) {
            return null;
        }
        return parseInt($$.Text.Trim($$.Find("txtSliceLength").value), 10);
    },
    SetAudioArgs : function(args = []) {
        $$.Find("txtFfmpegAudioArgs").value = $$.Text.Trim(args.join(" "));
        var ddlFfmpegAudioCodec = $$.Find("ddlFfmpegAudioCodec");
        var audioCodec = $$.App.Templates.ffmpeg.GetArgValue(args, "-c:a");
        if (audioCodec === "") {
            ddlFfmpegAudioCodec.value = "-an";
            $$.App.Templates.ffmpeg.AudioCodecChange();
            $$.App.Templates.ffmpeg.UpdateArgs();
            return;
        }
        
        ddlFfmpegAudioCodec.value = "-c:a " + audioCodec;
        $$.App.Templates.ffmpeg.AudioCodecChange();
        if (audioCodec !== "copy") {
            var audioBitrate = $$.App.Templates.ffmpeg.GetArgValue(args, "-b:a");
            $$.Find("txtFfmpegAduioBitrate").value = audioBitrate.replace("k", "").replace("m", "");
            var ddlFfmpegAudtioBitrate = $$.Find("ddlFfmpegAudtioBitrate");
            if (audioBitrate.endsWith("k")) {
                ddlFfmpegAudtioBitrate.value = "k";
            } else if (audioBitrate.endsWith("m")) {
                ddlFfmpegAudtioBitrate.value = "m";
            } else {
                ddlFfmpegAudtioBitrate.value = "";
            }
            
            var audioChannels = $$.App.Templates.ffmpeg.GetArgValue(args, "-ac");
            $$.Find("ddlFfmpegAudioChannels").value = audioChannels;
        }
        
        $$.App.Templates.ffmpeg.UpdateArgs();
    },
    SetExt : function(ext = "avi") {
        $$.Find("ddlFfmpegExt").value = ext;
    },
    SetSliceLength : function(length) {
        var txtSliceLength = $$.Find("txtSliceLength");
        var chkSliceLength = $$.Find("chkSliceLength");
        chkSliceLength.checked = length == null;
        txtSliceLength.value = length == null ? $$.App.Defaults.SliceLength : length;
        txtSliceLength.readOnly = chkSliceLength.checked; 
    },
    SetCommonArgs : function(args = []) {
        $$.Find("txtFfmpegCommonArgs").value = $$.Text.Trim(args.join(" "));
        
        var maxQueueSize = $$.App.Templates.ffmpeg.GetArgValue(args, "-max_muxing_queue_size"); 
        $$.Find("chkMaxQueue").checked = $$.Text.Trim(maxQueueSize, true) != null;
        
        $$.App.Templates.ffmpeg.UpdateArgs();
    },
    SetVideoArgs : function(args = []) {
        $$.Find("txtFfmpegVideoArgs").value = $$.Text.Trim(args.join(" "));
        
        var ddlFfmpegVideoCodec = $$.Find("ddlFfmpegVideoCodec");
        var videoCodec = $$.App.Templates.ffmpeg.GetArgValue(args, "-c:v");
        if (videoCodec === "") {
            ddlFfmpegVideoCodec.value = "-vn";
            $$.App.Templates.ffmpeg.VideoCodecChange();
            $$.App.Templates.ffmpeg.UpdateArgs();
            return;
        }
        
        ddlFfmpegVideoCodec.value = "";
        for (var i = 0; i < ddlFfmpegVideoCodec.options.length; i++) {
            if (ddlFfmpegVideoCodec.options[i].value == "-c:v " + videoCodec) {
                ddlFfmpegVideoCodec.selectedIndex = i;
            }
        }
        ddlFfmpegVideoCodec.CustomVideoCodec = "-c:v " + videoCodec;
        $$.App.Templates.ffmpeg.VideoCodecChange();
        
        var ddlFfmpegVideoQualityMode = $$.Find("ddlFfmpegVideoQualityMode");
        var crf = $$.App.Templates.ffmpeg.GetArgValue(args, "-crf");
        if (crf !== "") {
            ddlFfmpegVideoQualityMode.value = "crf";
            
            $$.App.Templates.ffmpeg.VideoQualityModeChange();
            $$.Find("txtFfmpegVideoQuality").value = crf;
        } else {
            ddlFfmpegVideoQualityMode.value = "cbr";
            $$.App.Templates.ffmpeg.VideoQualityModeChange();
            
            var videoBitrate = $$.App.Templates.ffmpeg.GetArgValue(args, "-b:v");
            $$.Find("txtFfmpegVideoBitrate").value = videoBitrate.replace("k", "").replace("m", "");
            var ddlFfmpegVideoBitrate = $$.Find("ddlFfmpegVideoBitrate");
            if (videoBitrate.endsWith("k")) {
                ddlFfmpegVideoBitrate.value = "k";
            } else if (videoBitrate.endsWith("m")) {
                ddlFfmpegVideoBitrate.value = "m";
            } else {
                ddlFfmpegVideoBitrate.value = "";
            }
        }
        
        var ddlFfmpegPreset = $$.Find("ddlFfmpegPreset");
        var preset;
        switch (videoCodec) {
            case "libx264":
            case "libx265":
                preset = $$.App.Templates.ffmpeg.GetArgValue(args, "-preset");
                ddlFfmpegPreset.value = preset;
                break;
            case "h264_nvenc":
            case "hevc_nvenc":
                preset = $$.App.Templates.ffmpeg.GetArgValue(args, "-preset");
                ddlFfmpegPreset.value = "";
                $$.Find("ddlFfmpegNvencPreset").value = preset;
                break;
            default:
                ddlFfmpegPreset.value = "";
                break;
        }
        
        var profile = $$.App.Templates.ffmpeg.GetArgValue(args, "-profile:v");
        var level = $$.App.Templates.ffmpeg.GetArgValue(args, "-level:v");
        profile = profile === "" ? "" : "-profile:v " + profile;
        level = level === "" ? "" : "-level:v " + level;
        switch (videoCodec) {
            case "libx264":
                $$.Find("ddlFfmpegVideoCodecProfileX264").value = profile;
                $$.Find("ddlFfmpegVideoCodecLevelX264").value = level;
                break;
            case "libx265":
                $$.Find("ddlFfmpegVideoCodecProfileX265").value = profile;
                $$.Find("ddlFfmpegVideoCodecLevelX265").value = level;
                break;
        }
            
        var filters = $$.App.Templates.ffmpeg.GetArgValue(args, "-vf");
        var deinterlace = $$.App.Templates.ffmpeg.GetArgFilterValue(filters, "yadif");
        $$.Find("chkDeinterlace").checked = deinterlace !== "";
        
        var denoise = $$.App.Templates.ffmpeg.GetArgFilterValue(filters, "hqdn3d");
        $$.Find("chkDenoise").checked = denoise !== "";
        
        var deshake = $$.App.Templates.ffmpeg.GetArgFilterValue(filters, "deshake", true);
        $$.Find("chkDeshake").checked = deshake === true;
        
        var smartblur = $$.App.Templates.ffmpeg.GetArgFilterValue(filters, "smartblur");
        $$.Find("chkSmartBlur").checked = smartblur !== "";
        
        var pixelFormat = $$.App.Templates.ffmpeg.GetArgValue(args, "-pix_fmt");
        $$.Find("chkTenBit").checked = pixelFormat === "yuv420p10le";
        
        $$.App.Templates.ffmpeg.UpdateArgs();
    },
    UpdateArgs : function() {
        var addVideoFilters = function(existing, toAdd) {
            var filters = existing;
            if ($$.Text.IsBlank(filters)) {
                filters = " -vf " + toAdd;
            } else {
                filters += "," + toAdd;
            }
            return filters;
        };
        
        var audioCodec = $$.Find("ddlFfmpegAudioCodec").value;
        var audioBitrate = $$.Find("txtFfmpegAduioBitrate").value;
        var audioBitrateUnit = $$.Find("ddlFfmpegAudtioBitrate").value;
        var audioChannels = $$.Find("ddlFfmpegAudioChannels").value;
        var videoCodec = $$.Find("ddlFfmpegVideoCodec").value;
        var videoQualityMode = $$.Find("ddlFfmpegVideoQualityMode").value;
        var videoBitrate = $$.Find("txtFfmpegVideoBitrate").value;
        var videoBitrateUnit = $$.Find("ddlFfmpegVideoBitrate").value;
        var videoQuality = $$.Find("txtFfmpegVideoQuality").value;
        var deinterlace = $$.Find("chkDeinterlace").checked;
        var denoise = $$.Find("chkDenoise").checked;
        var deshake = $$.Find("chkDeshake").checked;
        var smartblur = $$.Find("chkSmartBlur").checked;
        var tenBit = $$.Find("chkTenBit").checked;
        var maxQueue = $$.Find("chkMaxQueue").checked;
        
        if (videoCodec == "") {
            videoCodec = $$.Find("ddlFfmpegVideoCodec").CustomVideoCodec;
        }
        
        var preset = null;
        var videoProfile = null;
        var videoLevel = null;
        switch(videoCodec) {
            case "-c:v libx264":
                preset = $$.Find("ddlFfmpegPreset").value;
                videoProfile = $$.Find("ddlFfmpegVideoCodecProfileX264").value;
                videoLevel = $$.Find("ddlFfmpegVideoCodecLevelX264").value;
                break;
            case "-c:v libx265":
                preset = $$.Find("ddlFfmpegPreset").value;
                videoProfile = $$.Find("ddlFfmpegVideoCodecProfileX265").value;
                videoLevel = $$.Find("ddlFfmpegVideoCodecLevelX265").value;
                break;
            case "-c:v h264_nvenc":
            case "-c:v hevc_nvenc":
                preset = $$.Find("ddlFfmpegNvencPreset").value;
                break;
        }
        
        var audioArgs = "";
        var videoArgs = "";
        var commonArgs = "";
        
        switch(audioCodec) {
            case "-an":
            case "-c:a copy":
                audioArgs += audioCodec;
                break;
            default:
                audioArgs += audioCodec;
                
                if (audioBitrate > 0) {
                    audioArgs += " -b:a " + audioBitrate + audioBitrateUnit;
                }
                
                if (audioChannels !== "") {
                    audioArgs += " -ac " + audioChannels;
                } 
                break;
        }
        
        var videoFilters = "";
        if (videoCodec !== "-vn") {
            if (deinterlace) {
                videoFilters = addVideoFilters(videoFilters, "yadif=0:-1:0");
            }
            
            if (denoise) {
                videoFilters = addVideoFilters(videoFilters, "hqdn3d=8:8:3:3");
            }
            
            if (deshake) {
                videoFilters = addVideoFilters(videoFilters, "deshake");
            }
            
            if (smartblur) {
                videoFilters = addVideoFilters(videoFilters, "smartblur=1.5:-0.35:-3.5:0.65:0.25:2.0");
            }
            
            videoArgs += videoFilters;
            
            if (tenBit) {
                videoArgs += " -pix_fmt yuv420p10le";
            }
            
            switch (videoQualityMode) {
                case "cbr":
                    if (videoBitrate > 0) {
                        videoArgs += " -b:v " + videoBitrate + videoBitrateUnit;
                    }
                    break;
                case "crf":
                    if (videoCodec == "-c:v libx264" || videoCodec == "-c:v libx265") {
                        videoArgs += " -crf " + videoQuality;   
                    } else {
                        videoArgs += " -cq " + videoQuality;
                    }
                    break;
            }
        }
        
        videoArgs += " " + videoCodec;
        
        if (videoCodec !== "-vn") {
            if (!$$.Text.IsBlank(videoProfile)) {
                videoArgs += " " + videoProfile;
            }
            
            if (!$$.Text.IsBlank(videoLevel)) {
                videoArgs += " " + videoLevel;
            }
            
            if (!$$.Text.IsBlank(preset)) {
                videoArgs += " -preset " + preset;
            }
        }
        
        if (maxQueue) {
            var maxQueueSize = $$.App.Templates.ffmpeg.GetArgValue($$.App.Templates.ffmpeg.GetCommonArgs(), "-max_muxing_queue_size"); 
            if ($$.Text.Trim(maxQueueSize, true) == null) {
                maxQueueSize = $$.App.Defaults.MaxQueueSize;
            }
            commonArgs += " -max_muxing_queue_size " + maxQueueSize.toString();
        }
        
        $$.Find("txtFfmpegAudioArgs").value = $$.Text.Trim(audioArgs);
        $$.Find("txtFfmpegVideoArgs").value = $$.Text.Trim(videoArgs);
        $$.Find("txtFfmpegCommonArgs").value = $$.Text.Trim(commonArgs);
    },
    VideoCodecChange : function(event) {
        switch($$.Find("ddlFfmpegVideoCodec").value) {
            case "-c:v mpeg4":
                $$.Find("ffmpegPreset").classList.add("hidden");
                $$.Find("ffmpegVideoCodecProfileX264").classList.add("hidden");
                $$.Find("ffmpegVideoCodecProfileX265").classList.add("hidden");
                $$.Find("ffmpegVideoQualityMode").classList.add("hidden");
                $$.Find("ffmpegVideoOptions").classList.remove("hidden");
                $$.App.Templates.ffmpeg.VideoQualityModeChange();
                break;
            case "-c:v libx264":
                $$.Find("ffmpegPreset").classList.remove("hidden");
                $$.Find("ffmpegVideoCodecProfileX264").classList.remove("hidden");
                $$.Find("ffmpegVideoCodecProfileX265").classList.add("hidden");
                $$.Find("ffmpegVideoQualityMode").classList.remove("hidden");
                $$.Find("ffmpegVideoOptions").classList.remove("hidden");
                $$.App.Templates.ffmpeg.VideoQualityModeChange();
                break;
            case "-c:v libx265":
                $$.Find("ffmpegPreset").classList.remove("hidden");
                $$.Find("ffmpegVideoCodecProfileX264").classList.add("hidden");
                $$.Find("ffmpegVideoCodecProfileX265").classList.remove("hidden");
                $$.Find("ffmpegVideoQualityMode").classList.remove("hidden");
                $$.Find("ffmpegVideoOptions").classList.remove("hidden");
                $$.App.Templates.ffmpeg.VideoQualityModeChange();
                break;
            case "-c:v h264_nvenc":
            case "-c:v hevc_nvenc":
                $$.Find("ffmpegPreset").classList.add("hidden");
                $$.Find("ffmpegNvencPreset").classList.remove("hidden");
                $$.Find("ffmpegVideoCodecProfileX264").classList.add("hidden");
                $$.Find("ffmpegVideoCodecProfileX265").classList.add("hidden");
                $$.Find("ffmpegVideoQualityMode").classList.remove("hidden");
                $$.Find("ffmpegVideoOptions").classList.remove("hidden");
                $$.App.Templates.ffmpeg.VideoQualityModeChange();
                break;
            case "-vn":
                $$.Find("ffmpegPreset").classList.add("hidden");
                $$.Find("ffmpegVideoCodecProfileX264").classList.add("hidden");
                $$.Find("ffmpegVideoCodecProfileX265").classList.add("hidden");
                $$.Find("ffmpegVideoQualityMode").classList.add("hidden");
                $$.Find("ffmpegVideoQuality").classList.add("hidden");
                $$.Find("ffmpegVideoBitrate").classList.add("hidden");
                $$.Find("ffmpegVideoOptions").classList.add("hidden");
                break;
            case "":
            default:
                $$.Find("ffmpegPreset").classList.add("hidden");
                $$.Find("ffmpegVideoCodecProfileX264").classList.add("hidden");
                $$.Find("ffmpegVideoCodecProfileX265").classList.add("hidden");
                $$.Find("ffmpegVideoQualityMode").classList.add("hidden");
                $$.Find("ffmpegVideoQuality").classList.add("hidden");
                $$.Find("ffmpegVideoBitrate").classList.add("hidden");
                $$.Find("ffmpegVideoOptions").classList.remove("hidden");
                break;
        }
    },
    VideoQualityModeChange : function(event) {
        switch($$.Find("ddlFfmpegVideoCodec").value) {
            case "-c:v mpeg4":
                $$.Find("ffmpegVideoQuality").classList.add("hidden");
                $$.Find("ffmpegVideoBitrate").classList.remove("hidden");
                break;
            
            case "-c:v libx264":
            case "-c:v libx265":
            case "-c:v h264_nvenc":
            case "-c:v hevc_nvenc":
                switch($$.Find("ddlFfmpegVideoQualityMode").value) {
                    case "cbr":
                        $$.Find("ffmpegVideoQuality").classList.add("hidden");
                        $$.Find("ffmpegVideoBitrate").classList.remove("hidden");
                        break;
                    case "crf":
                        $$.Find("ffmpegVideoBitrate").classList.add("hidden");
                        $$.Find("ffmpegVideoQuality").classList.remove("hidden");
                        break;
                }
                break;
            default:
                $$.Find("ffmpegVideoBitrate").classList.add("hidden");
                $$.Find("ffmpegVideoQuality").classList.add("hidden");
                break;
        }
    }
};

$$.Init.Add(function() {
    if ($$.Find("ffmpeg") != null) {
        $$.App.Templates.ffmpeg.Init();
    }
});
