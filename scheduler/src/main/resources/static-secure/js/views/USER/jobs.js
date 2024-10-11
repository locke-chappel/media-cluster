/* jobs.js */
$$.App.Views.jobs = {
    _customProfileId : "custom",
    Init : function() {
        $$.Events.SafeClick("btnSaveJob", function(event) {
            $$.App.Views.jobs.SaveJob();
        });
        $$.Events.SafeClick("btnCancelJob", function(event) {
            $$.UI.Dirty.Warn(function() {
                $$.App.Views.jobs.CancelJob();
            });
        });
        
        $$.Events.Add("txtSearchText", "focus", function() { this.select(); });
        $$.Events.Add("txtSearchText", "keyup", function() {
            if (this.previous == this.value) {
                return;
            }
            this.previous = this.value;
            clearTimeout(this.delay);
            this.delay = setTimeout(function(){
                $$.App.Views.jobs.Search();
            }, 250);
        });
        
        $$.Events.Add("ddlJobProfile", "change", function(event) { $$.App.Views.jobs.ProfileChanged(); });
        
        var inputs = $$.Find("ffmpeg").querySelectorAll("input, select, textarea");
        $$.ForEach(inputs, function(input) {
            $$.Events.Add(input, "change", function(event) {
                $$.App.Views.jobs.ProfileEdited();
            });
        });
        
        $$.App.Views.jobs.Search(0, true);
        $$.App.Views.jobs.LoadProfiles();
        $$.App.Views.jobs.LoadJob();
        $$.App.Functions.LoadClusters("ddlClusterName");
        
        $$.Find("btnImport").LoadJobs = function() { $$.App.Views.jobs.Search(0, true); };
        
        $$.UI.Dirty.Ignored["txtSearchText"] = true;
        $$.UI.Dirty.Regsiter();
    },
    CancelJob : function() {
        var job = $$.Find("lblJobSource").job;
        if (job == null) {
            $$.App.Views.jobs.LoadJob();
        } else {
            $$.App.Views.jobs.LoadJob(job.id);
        }
        
        $$.UI.Dirty.Set(false);
    },
    DeleteJob : function(job) {
        $$.L10N.Get([
            "global.buttons.cancel",
            "global.buttons.delete",
            "jobs.job.delete.title",
            "jobs.job.delete.message"
        ], function(texts){
            var id = "deleteJob";
    
            var body = document.createElement("span");
            $$.Set(body, $$.L10N.Replace(texts["jobs.job.delete.message"], { "Source" : job.source }));
            
            var footer = $$.UI.Modal.Footers.Confirm({
                "modalId" : id,
                "ok" : function(event) {
                    $$.REST.Call({
                        "method" : "DELETE",
                        "url" : "/api/v1/jobs/" + job.id,
                        "success" : function(response, body) {
                            $$.Banner.ShowMessages({ 
                                "category" : "Application",
                                "severity" : "Success",
                                "number" : 1
                            });
                            $$.App.Views.jobs.Search(0, true);
                            $$.App.Views.jobs.LoadJob();
                        }
                    });
                 },
                 "okText" : texts["global.buttons.delete"]
            });
            
            $$.UI.Modal.Show({
                "header" : texts["jobs.job.delete.title"],
                "body" : body,
                "footer" : footer,
                "id" : id
            }); 
        });
    },
    LoadJob : function(jobId) {
        var _load = function(job) {
            var ddlJobProfile = $$.Find("ddlJobProfile");
            var profile = JSON.parse(job.profile == null ? "{}" : job.profile);
            if ($$.Text.IsBlank(profile.id) || profile.id.toLowerCase() === $$.App.Views.jobs._customProfileId) {
                $$.App.Templates.ffmpeg.SetAudioArgs(profile.audioArgs);
                $$.App.Templates.ffmpeg.SetVideoArgs(profile.videoArgs);
                $$.App.Templates.ffmpeg.SetCommonArgs(profile.commonArgs);
                $$.App.Templates.ffmpeg.SetExt(profile.ext);
                $$.App.Templates.ffmpeg.SetSliceLength(profile.sliceLength);
            } else {
                ddlJobProfile.value = profile.id;
            }
                        
            $$.App.Views.jobs.ProfileChanged();
            
            var btnSaveJob = $$.Find("btnSaveJob");
            var lblJobSource = $$.Find("lblJobSource");
            if ($$.Text.IsBlank(job.source)) {
                $$.L10N.Get(["jobs.details.source.none"], 
                    function(text){
                        $$.Set(lblJobSource, text);
                        lblJobSource.classList.add("none");
                    }
                );
                lblJobSource.job = null;
                btnSaveJob.disabled = true;
            } else {
                $$.Set(lblJobSource, job.source);
                lblJobSource.classList.remove("none");
                lblJobSource.job = job;
                btnSaveJob.disabled = false;
            }
            
            if (!$$.Text.IsBlank(job.status)) {
                $$.L10N.Get(["application.status." + job.status], 
                    function(text){
                        $$.Set("lblJobStatus", text + " " + $$.Text.Trim(job.statusMessage));
                    }
                );    
            } else {
                $$.Set("lblJobStatus", "");
            }
            
            $$.UI.Dirty.Set(false);
        };
    
        if (jobId == null) {
            _load({});
        } else {
            $$.REST.Call({
                "method" : "GET",
                    "url" : "/api/v1/jobs/" + jobId,
                    "success" : function(response, j) {
                        _load(j);
                    }
            });
        }
    },
    LoadProfiles : function() {
        $$.L10N.Get(["jobs.details.profile.default"], 
            function(text){
                $$.REST.Call({
                    "method" : "GET",
                    "url" : "/api/v1/profiles",
                    "success" : function(response, profiles) {
                        $$.Sorting.Fields(profiles, function(o) { return o.value; });
                        
                        var ddlJobProfile = $$.Find("ddlJobProfile");
                        var opt = document.createElement("option");
                        opt.value = "";
                        $$.Set(opt, text); 
                        $$.Set(ddlJobProfile, opt);
                        
                        $$.ForEach(profiles, function(profile) {
                            opt = document.createElement("option");
                            opt.value = profile.key;
                            $$.Set(opt, profile.value);
                            $$.Set(ddlJobProfile, opt, true);
                        });
                    }
                });
            }
        );
    },
    ProfileChanged : function() {
        var ddlJobProfile = $$.Find("ddlJobProfile");
        if (ddlJobProfile.options.length > 1 && ddlJobProfile.options[1].value ===$$.App.Views.jobs._customProfileId) {
            ddlJobProfile.remove(1);
        }
        
        var id = $$.Text.Trim(ddlJobProfile.value, true);
        if (id == null) {
            $$.App.Templates.ffmpeg.SetAudioArgs();
            $$.App.Templates.ffmpeg.SetVideoArgs();
            $$.App.Templates.ffmpeg.SetCommonArgs();
            $$.App.Templates.ffmpeg.SetExt();
            $$.App.Templates.ffmpeg.SetSliceLength($$.App.Defaults.SliceLength);
        } else {
            $$.REST.Call({
                "method" : "GET",
                "url" : "/api/v1/profiles/" + id,
                "success" : function(response, profile) {
                    $$.App.Templates.ffmpeg.SetAudioArgs(profile.audioArgs);
                    $$.App.Templates.ffmpeg.SetVideoArgs(profile.videoArgs);
                    $$.App.Templates.ffmpeg.SetCommonArgs(profile.commonArgs);
                    $$.App.Templates.ffmpeg.SetExt(profile.ext);
                    $$.App.Templates.ffmpeg.SetSliceLength(profile.sliceLength);
                }
            });
        }
    },
    ProfileEdited : function() {
        var ddlJobProfile = $$.Find("ddlJobProfile");
        if (ddlJobProfile.options.length > 1 && ddlJobProfile.options[1].value === $$.App.Views.jobs._customProfileId) {
            return;
        }
        
        $$.L10N.Get(["application.profiles.custom"], 
            function(text){
                var ddlJobProfile = $$.Find("ddlJobProfile");
                var opt = document.createElement("option");
                opt.value = $$.App.Views.jobs._customProfileId;
                $$.Set(opt, text); 
                ddlJobProfile.insertBefore(opt, ddlJobProfile.childNodes[0].nextSibling);
                ddlJobProfile.selectedIndex = 1;
            }
        );
    },
    SaveJob : function() {
        var job = $$.Find("lblJobSource").job;
        if (job == null) {
            $$.Banner.ShowMessages({ 
                "category" : "Application",
                "severity" : "Error",
                "number" : 3014
            });
            return;
        }
        
        var ddlJobProfile = $$.Find("ddlJobProfile");
        if ($$.Text.IsBlank(ddlJobProfile.value)) {
            $$.Banner.ShowMessages({ 
                "category" : "Application",
                "severity" : "Error",
                "number" : 3010
            });
            return;
        }
        
        var audioArgs = $$.App.Templates.ffmpeg.GetAudioArgs();
        var videoArgs = $$.App.Templates.ffmpeg.GetVideoArgs();
        var commonArgs = $$.App.Templates.ffmpeg.GetCommonArgs();
        var ext = $$.App.Templates.ffmpeg.GetExt();
        var sliceLength = $$.App.Templates.ffmpeg.GetSliceLength();
        var request = {
            "clusterName" : $$.Text.Trim($$.Find("ddlClusterName").value),
            "profile" : JSON.stringify({
                "id" : $$.Text.Trim(ddlJobProfile.value),
                "name" : $$.Text.Trim(ddlJobProfile.options[ddlJobProfile.selectedIndex].text),
                "ext" : ext,
                "sliceLength" : sliceLength,
                "audioArgs" : audioArgs,
                "videoArgs" : videoArgs,
                "commonArgs" : commonArgs
            })
        };
        
        $$.REST.Call({
            "method" : "PUT",
            "url" : "/api/v1/jobs/" + job.id,
            "body" : request,
            "success" : function(response, profile) {
                $$.Banner.ShowMessages({ 
                    "category" : "Application",
                    "severity" : "Success",
                    "number" : 1
                });
                
                $$.App.Views.jobs.Search();
                
                $$.UI.Dirty.Set(false);
            }
        }); 
    },
    Search : function(page, firstLoad) {
        $$._inProgress.Add();
        var _tableId = "tJobs";
        var _pageSize = 10;
        
        if (isNaN(page)) {
            page = 0;
        }
        var queryString = $$.URL.ToParam("pageSize", _pageSize);
        queryString = $$.URL.ToParam("pageNumber", page, queryString);
        queryString = $$.URL.ToParam("text", $$.Find("txtSearchText").value, queryString);
        var sorts = $$.UI.Tables.Cells.GetSorts(_tableId);
        if (firstLoad === true) {
            sorts = {
                "source" : "asc"
            };
        }
        
        $$.ForEach(sorts, function(direction, field) {
            queryString = $$.URL.ToParam(field + "Sort", direction, queryString);
        });
        
        $$.L10N.Get([
            "jobs.search.columns.source",
            "jobs.search.columns.status",
            "jobs.search.columns.source.tooltip",
            "jobs.search.columns.delete.tooltip"
        ], function(texts) {
            $$.REST.Call({
                "method" : "GET",
                "url" : "/api/v1/jobs" + queryString,
                "success" : function(response, body) {
                    var searchResults = $$.Find("searchResults");
                    var table = $$.UI.Tables.Table(body.data, [
                        function(rowData) {
                            return $$.UI.Tables.Cells.LinkButton({
                                "text" : rowData.source,
                                "click" : function(event) { $$.App.Views.jobs.LoadJob(rowData.id); },
                                "tooltip" : texts["jobs.search.columns.source.tooltip"]
                            });
                        },
                        function(rowData) {
                            return $$.UI.Tables.Cells.Text({
                                "text" : rowData.status
                            });
                        },
                        function(rowData) {
                            return $$.UI.Tables.Cells.Icon({
                                "icon" : $$.App.Icons.Delete,
                                "click" :  function(event) { $$.App.Views.jobs.DeleteJob(rowData); },
                                "tooltip" : texts["jobs.search.columns.delete.tooltip"]
                            });
                        }
                    ], [{
                        "field" : "source",
                        "text" : texts["jobs.search.columns.source"],
                        "defaultDirection" : sorts["source"],
                        "sort" : function(direction) {
                            $$.App.Views.jobs.Search(page);
                        }
                    }, {
                        "field" : "status",
                        "text" : texts["jobs.search.columns.status"],
                        "defaultDirection" : sorts["status"], 
                        "sort" : function(direction) {
                            $$.App.Views.jobs.Search(page);
                        }
                    }, {
                        /* Empty Header, Delete Column */
                    }]);
                    table.id = _tableId;
                    $$.Set(searchResults, table);
                    
                    var pages = $$.UI.Tables.Pagination({
                        "total" : body.total,
                        "pageSize" : _pageSize,
                        "currentPage" : page,
                        "onClick" : function(newPage) {
                            $$.App.Views.jobs.Search(newPage);
                        }
                    });
                    $$.Set(searchResults, pages, true);
                    $$._inProgress.Complete();
                }
            });
        });
    }
};
