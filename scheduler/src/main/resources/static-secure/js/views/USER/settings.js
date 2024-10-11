/* settings.js */
$$.App.Views.settings = {
    Init : function() {
        $$.Events.SafeClick("btnRestoreBackup", function(event) {
            $$.App.Views.settings.RestoreBackup();
        });
        $$.Events.SafeClick("btnSaveBackup", function(event) {
            $$.App.Views.settings.SaveBackup();
        });
        $$.Events.SafeClick("btnSaveProfile", function(event) {
            $$.App.Views.settings.SaveProfile();
        });
        $$.Events.SafeClick("btnSaveScheduler", function(event) {
            $$.App.Views.settings.SaveScheduler();
        });
        var btnSaveUser = $$.Find("btnSaveUser");
        if (btnSaveUser != null) {
            $$.Events.SafeClick("btnSaveUser", function(event) {
                $$.App.Views.settings.SaveUser();
            });
        }
        var btnCancelUser = $$.Find("btnCancelUser");
        if (btnCancelUser != null) {
            $$.Events.SafeClick("btnCancelUser", function(event) {
                $$.UI.Dirty.Warn(function() {
                    $$.App.Views.settings.CancelUser();
                }, "user");
            });
        }
        
        $$.Events.Add("ddlProfile", "change", function(event) {
            var txtProfile = $$.Find("txtProfile");
            if ($$.Text.IsBlank(this.value)) {
                txtProfile.value = "";
                $$.App.Views.settings.LoadProfile(null);
            } else {
                txtProfile.value = this.options[this.selectedIndex].text;
                $$.App.Views.settings.LoadProfile(this.value);
            }
        });
        
        $$.App.Views.settings.LoadProfiles();
        $$.App.Views.settings.LoadScheduler();
        $$.App.Views.settings.LoadProfile(null);
        
        var divUser = $$.Find("divUser");
        if (divUser != null) {
            $$.UI.Dirty.Regsiter(divUser, "user");
        }
    },
    CancelUser : function() {
        var txtUsername = $$.Find("txtUsername");
        txtUsername.value = txtUsername.getAttribute("Original");
        
        $$.Find("txtPassword").value = null;
        $$.Find("txtPasswordConfirm").value = null;
        
        $$.UI.Dirty.Set(false, "user");
    },
    LoadProfile : function(profile) {
        var ddlProfile = $$.Find("ddlProfile");
        var txtProfile = $$.Find("txtProfile");
        if (typeof(profile) == "string") {
            $$.REST.Call({
                "method" : "GET",
                "url" : "/api/v1/profiles/" + profile,
                "success" : function(response, p) {
                    $$.App.Templates.ffmpeg.SetAudioArgs(p.audioArgs);
                    $$.App.Templates.ffmpeg.SetVideoArgs(p.videoArgs);
                    $$.App.Templates.ffmpeg.SetCommonArgs(p.commonArgs);
                    $$.App.Templates.ffmpeg.SetExt(p.ext);
                    $$.App.Templates.ffmpeg.SetSliceLength(p.sliceLength);
                    txtProfile.profile = p;
                    ddlProfile.value = p.id;
                }
            });
        } else if (profile != null && typeof(profile) == "object") {
            $$.App.Templates.ffmpeg.SetAudioArgs(profile.audioArgs);
            $$.App.Templates.ffmpeg.SetVideoArgs(profile.videoArgs);
            $$.App.Templates.ffmpeg.SetCommonArgs(profile.commonArgs);
            $$.App.Templates.ffmpeg.SetExt(profile.ext);
            $$.App.Templates.ffmpeg.SetSliceLength(profile.sliceLength);
            txtProfile.profile = profile;
            ddlProfile.value = profile.id;
        } else {
            $$.App.Templates.ffmpeg.SetAudioArgs();
            $$.App.Templates.ffmpeg.SetVideoArgs();
            $$.App.Templates.ffmpeg.SetCommonArgs();
            $$.App.Templates.ffmpeg.SetExt();
            $$.App.Templates.ffmpeg.SetSliceLength($$.App.Defaults.SliceLength);
            txtProfile.focus();
            txtProfile.profile = null;
            ddlProfile.value = null;
        }
    },
    LoadProfiles : function(callback) {
        $$.L10N.Get(["settings.profiles.new"], 
            function(text){
                $$.REST.Call({
                    "method" : "GET",
                    "url" : "/api/v1/profiles",
                    "success" : function(response, profiles) {
                        $$.Sorting.Fields(profiles, function(o) { return o.value; });
                        
                        var ddlProfile = $$.Find("ddlProfile");
                        var opt = document.createElement("option");
                        opt.value = "";
                        $$.Set(opt, text); 
                        $$.Set(ddlProfile, opt);
                        
                        $$.ForEach(profiles, function(profile) {
                            opt = document.createElement("option");
                            opt.value = profile.key;
                            $$.Set(opt, profile.value);
                            $$.Set(ddlProfile, opt, true);
                        });
                        
                        if (typeof(callback) === "function") {
                            callback();
                        }
                    }
                });
            });
    },
    LoadScheduler : function() {
        $$.REST.Call({
            "method" : "GET",
            "url" : "/api/v1/nodes/scheduler",
            "success" : function(response, node) {
                var txtSchedulerUrl = $$.Find("txtSchedulerUrl");
                txtSchedulerUrl.nodeId = node.id;
                txtSchedulerUrl.nodeModified = node.modified;
                txtSchedulerUrl.value = $$.Text.Trim(node.url);
                $$.Find("txtSchedulerName").value = $$.Text.Trim(node.name);
            }
        });
    },
    RestoreBackup : function() {
        $$.L10N.Get([
            "messages.Application.Error.2",
            "global.buttons.cancel",
            "global.modal.confirm.header",
            "settings.backup.restore",
            "settings.backup.fileName",
            "settings.backup.modal.restore.select",
            "settings.backup.modal.restore.select.label",
            "settings.backup.modal.restore.title",
            "settings.backup.modal.restore.warning.1",
            "settings.backup.modal.restore.warning.2",
            "settings.backup.modal.restore.warning.3",
            "settings.backup.password"
        ], function(texts){
            var body = document.createElement("div");
            body.classList.add("backup");
            var txtPassword = $$.UI.Input({
                "type" : "password",
                "id" : "txtBackupPassword",
                "inline" : true,
                "max" : 256,
                "label" : texts["settings.backup.password"]
            });
            $$.Set(body, txtPassword);
            var upBackup = $$.UI.Input({
                "type" : "upload",
                "id" : "upBackup",
                "accept" : ".backup",
                "inline" : true,
                "text" : texts["settings.backup.modal.restore.select"],
                "label" : texts["settings.backup.modal.restore.select.label"]
            });
            $$.Set(body, upBackup, true);
            
            var _hasData = function(showMessage) {
                if ($$.Text.IsBlank($$.Find("txtBackupPassword").value)) {
                    if (showMessage === true) {
                        $$.Banner.ShowMessages({ 
                            "category" : "Application",
                            "severity" : "Error",
                            "number" : 2,
                            "vars" : { 
                                "Field" : texts["settings.backup.password"]
                            }
                        });
                    }
                    return false;
                }
                
                if ($$.Find("upBackup").files[0] == null) {
                    if (showMessage === true) {
                        $$.Banner.ShowMessages({ 
                            "category" : "Application",
                            "severity" : "Error",
                            "number" : 2,
                            "vars" : { 
                                "Field" : texts["settings.backup.modal.restore.select.label"]
                            }
                        });
                    }
                    return false;
                }
                
                return true;
            };
            
            $$.Events.Add(txtPassword, "change", function(event) {
                $$.Find("modalRestoreBackup_btnOk").disabled = !_hasData();
            });
            
            $$.Events.Add(upBackup, "change", function(event) {
                $$.Find("modalRestoreBackup_btnOk").disabled = !_hasData();
            });
            
            var footer = $$.UI.Modal.Footers.Confirm({
                "modalId" : "modalRestoreBackup",
                "ok" : function(event) {
                    if (!_hasData(true)) {
                        return true;        
                    }
                    
                    var password = $$.Text.Trim($$.Find("txtBackupPassword").value);
                    var backupFile = $$.Find("upBackup");
                    var reader = new FileReader();
                    reader.onload = function(event){
                        var data = {
                            "password" : password,
                            "data" : event.target.result
                        };

                        var confirmBody = document.createElement("div");
                        confirmBody.classList.add("backupConfirm");
                        var w1 = document.createElement("span");
                        w1.classList.add("w1");
                        $$.Set(w1, texts["settings.backup.modal.restore.warning.1"]);
                        $$.Set(confirmBody, w1, true);
                        var w2 = document.createElement("span");
                        w2.classList.add("w2");
                        $$.Set(w2, texts["settings.backup.modal.restore.warning.2"]);
                        $$.Set(confirmBody, w2, true);
                        var w3 = document.createElement("span");
                        w3.classList.add("w3");
                        $$.Set(w3, texts["settings.backup.modal.restore.warning.3"]);
                        $$.Set(confirmBody, w3, true);
                        
                        $$.UI.Modal.Show({
                            "header" : texts["global.modal.confirm.header"],
                            "body" : confirmBody,
                            "footer" : $$.UI.Modal.Footers.Confirm({
                                "ok" : function(event) {
                                    $$.REST.Call({
                                        "method" : "PUT",
                                        "url" : "/api/v1/backup",
                                        "body" : data,
                                        "success" : function(response, backup) {
                                            $$.Banner.ShowMessages({ 
                                                "category" : "Application",
                                                "severity" : "Success",
                                                "number" : 6005
                                            });
                                        }
                                    });
                                 },
                                 "okText" : texts["global.buttons.delete"],
                                 "okCssClasses" : "delete"
                            })
                        });
                    };

                    reader.readAsText(backupFile.files[0], "UTF-8");
                },
                "okText" : texts["settings.backup.restore"]
            });
            
            $$.UI.Modal.Show({
                "header" : texts["settings.backup.modal.restore.title"],
                "body" : body,
                "footer" : footer,
                "id" : "modalRestoreBackup"
            });
            
            $$.Find("modalRestoreBackup_btnOk").disabled = true;
            $$.Find("txtBackupPassword").focus();
            
            var _onKeyUp = function(event) {
                switch(event.keyCode) {
                    case $$.Events.KeyCodes.Enter:
                        if (!$$.App.FocusIfBlank("txtBackupPassword")) {
                            $$.Find("modal_btnOk").click();
                        }
                        break;
                }
            };
            $$.Events.Add("txtBackupPassword", "keyup", _onKeyUp);
        });
    },
    SaveBackup : function() {
        $$.L10N.Get([
            "global.buttons.cancel",
            "global.buttons.save",
            "settings.backup.fileName",
            "settings.backup.modal.save.title",
            "settings.backup.password"
        ], function(texts){
            var body = document.createElement("div");
            body.classList.add("backup");
            var txtPassword = $$.UI.Input({
                "type" : "password",
                "id" : "txtBackupPassword",
                "inline" : true,
                "max" : 256,
                "label" : texts["settings.backup.password"]
            });
            $$.Set(body, txtPassword);
            
            var footer = $$.UI.Modal.Footers.Confirm({
                "ok" : function(event) {
                    var data = {
                        "password" : $$.Text.Trim($$.Find("txtBackupPassword").value)
                    };
        
                    $$.REST.Call({
                        "method" : "POST",
                        "url" : "/api/v1/backup",
                        "body" : data,
                        "success" : function(response, backup) {
                            $$.Banner.Clear("error");
                            
                            var now = new Date();
                            var date = now.getFullYear() + "-" + (now.getMonth() + 1) + "-" + now.getDate();
                            var blobData = new Blob([backup.data], {type: "text/plain"});
                            var url = window.URL.createObjectURL(blobData);
                            var a = document.createElement("a");
                            a.style = "display: none";
                            document.body.appendChild(a);
                            a.href = url;
                            a.download = $$.L10N.Replace(texts["settings.backup.fileName"], { "date" : date, "node" : $$.Find("txtSchedulerName").value });
                            a.click();
                            window.URL.revokeObjectURL(url);
                            a.remove();
                            
                            footer.CloseModal();
                        }
                    });
                    
                    return true;
                },
                "okText" : texts["global.buttons.save"],
                "cancel" : function(event) {
                    $$.Banner.Clear("error");
                }
            });
            
            $$.UI.Modal.Show({
                "header" : texts["settings.backup.modal.save.title"],
                "body" : body,
                "footer" : footer
            });
            
            $$.Find("txtBackupPassword").focus();
            
            var _onKeyUp = function(event) {
                switch(event.keyCode) {
                    case $$.Events.KeyCodes.Enter:
                        if (!$$.App.FocusIfBlank("txtBackupPassword")) {
                            $$.Find("modal_btnOk").click();
                        }
                        break;
                }
            };
            $$.Events.Add("txtBackupPassword", "keyup", _onKeyUp);
        });
    },
    SaveProfile : function() {
        var txtProfile = $$.Find("txtProfile");
        var existing = txtProfile.profile;
        var audioArgs = $$.App.Templates.ffmpeg.GetAudioArgs();
        var videoArgs = $$.App.Templates.ffmpeg.GetVideoArgs();
        var commonArgs = $$.App.Templates.ffmpeg.GetCommonArgs();
        var ext = $$.App.Templates.ffmpeg.GetExt();
        var sliceLength = $$.App.Templates.ffmpeg.GetSliceLength();
        var profile = {
            "name" : $$.Text.Trim(txtProfile.value),
            "ext" : ext,
            "sliceLength" : sliceLength,
            "audioArgs" : audioArgs,
            "videoArgs" : videoArgs,
            "commonArgs" : commonArgs
        };
        
        var method = "POST";
        var query = "";
        if (existing != null) {
            method = "PUT";
            query = "/" + existing.id;
            profile.modified = existing.modified;
        }
        
        $$.REST.Call({
            "method" : method,
            "url" : "/api/v1/profiles" + query,
            "body" : profile,
            "success" : function(response, profile) {
                $$.Banner.ShowMessages({ 
                    "category" : "Application",
                    "severity" : "Success",
                    "number" : 1
                });
                $$.App.Views.settings.LoadProfiles(function() {
                    $$.App.Views.settings.LoadProfile(profile);    
                });
            }
        }); 
    },
    SaveScheduler : function() {
        var txtSchedulerUrl = $$.Find("txtSchedulerUrl");
        var data = {
            name : $$.Text.Trim($$.Find("txtSchedulerName").value),
            url : $$.Text.Trim(txtSchedulerUrl.value)
        };
        
        if (txtSchedulerUrl.nodeModified != null) {
            data.modified = txtSchedulerUrl.nodeModified;
        }
        
        $$.Banner.Clear();
        $$.REST.Call({
            "method" : "PUT",
            "url" : "/api/v1/nodes/" + txtSchedulerUrl.nodeId,
            "body" : data,
            "success" : function(response, body) {
                $$.Banner.ShowMessages({ 
                    "category" : "Application",
                    "severity" : "Success",
                    "number" : 1
                });
                txtSchedulerUrl.nodeModified = body.modified;
            }
        });
    },
    SaveUser : function() {
        var data ={
            "username" : $$.Text.Trim($$.Find("txtUsername").value),
            "password" : $$.Text.Trim($$.Find("txtPassword").value),
            "confirm" : $$.Text.Trim($$.Find("txtPasswordConfirm").value)
        };
    
        $$.REST.Call({
            "method" : "PUT",
            "url" : "/api/v1/user",
            "body" : data,
            "success" : function(response, body) {
                $$.Banner.ShowMessages({ 
                    "category" : "Application",
                    "severity" : "Success",
                    "number" : 1
                });
            }
        });
    }
};
