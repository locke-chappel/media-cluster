/* USER/_common.js */
$$.App.Menus.User = function() {
    var btnJobs = $$.Find("btnJobs");
    if (btnJobs != null) {
        $$.Events.SafeClick(btnJobs, function(event) {
            $$.UI.Dirty.Warn(function() {
                $$.URL.Go("/");
            });
        });
    }
    
    var btnImport = $$.Find("btnImport");
    if (btnImport != null) {
        $$.Events.SafeClick(btnImport, function(event) {
            $$.REST.Call({
                "method" : "POST",
                "url" : "/api/v1/jobs/import",
                "success" : function(response, body) {
                    $$.Banner.ShowMessages({ 
                        "category" : "Application",
                        "severity" : "Info",
                        "number" : 4002,
                        "autohide" : true
                    });
                    
                    if (typeof(event.target.LoadJobs) === "function") {
                        setTimeout(function(){
                            event.target.LoadJobs();
                        }, 5000);
                        $$.Find("btnMenu").click();
                    } else {
                        $$.URL.Go("/");
                    }
                }
            });
        });
    }
    
    var btnNodes = $$.Find("btnNodes");
    if (btnNodes != null) {
        $$.Events.SafeClick(btnNodes, function(event) {
            $$.UI.Dirty.Warn(function() {
                $$.URL.Go("/nodes");
            });
        });
    }
    
    var btnNewNode = $$.Find("btnNewNode");
    if (btnNewNode != null) {
        $$.Events.SafeClick(btnNewNode, function(event) {
            $$.UI.Dirty.Warn(function() {
                if (typeof(event.target.LoadNode) === "function") {
                    event.target.LoadNode();
                    $$.Find("btnMenu").click();
                } else {
                    $$.URL.Go("/nodes");
                }
            });
        });
    }
    
    var btnInformNodes = $$.Find("btnInformNodes");
    if (btnInformNodes != null) {
        $$.Events.SafeClick(btnInformNodes, function(event) {
            $$.REST.Call({
                "method" : "POST",
                "url" : "/api/v1/nodes/inform",
                "success" : function(response, body) {
                    $$.Banner.ShowMessages({ 
                        "category" : "Application",
                        "severity" : "Success",
                        "number" : 2001
                    });
                    
                    $$.Find("btnMenu").click();
                    
                    if (typeof(event.target.Search) === "function") {
                        setTimeout(function(){
                            event.target.Search();
                        }, 5000);
                    }
                }
            });
        });
    }
    
    var btnRefreshNodes = $$.Find("btnRefreshNodes");
    if (btnRefreshNodes != null) {
        $$.Events.SafeClick(btnRefreshNodes, function(event) {
            $$.REST.Call({
                "method" : "POST",
                "url" : "/api/v1/nodes/refresh",
                "success" : function(response, body) {
                    $$.Banner.ShowMessages({ 
                        "category" : "Application",
                        "severity" : "Success",
                        "number" : 2002
                    });
                    
                    $$.Find("btnMenu").click();
                    
                    if (typeof(event.target.Search) === "function") {
                        setTimeout(function(){
                            event.target.Search();
                        }, 5000);
                    }
                }
            });
        });
    }
    
    var btnSettings = $$.Find("btnSettings");
    if (btnSettings != null) {
        $$.Events.SafeClick(btnSettings, function(event) {
            $$.UI.Dirty.Warn(function() {
                $$.URL.Go("/settings");
            });
        });
    }
    
    $$.Events.SafeClick("btnSignOut", $$.App.SignOut);
    $$.Events.SafeClick("btnSignOutHeader", $$.App.SignOut);
};

$$.App.Functions.LoadClusters = function(ddlClusterName, includeNew) {
    $$.L10N.Get([
            "nodes.node.clusterName.new",
            "application.default.cluster"
        ], function(texts){
            $$.REST.Call({
                "method" : "GET",
                "url" : "/api/v1/nodes/clusters",
                "success" : function(response, clusters) {
                    var ddl = $$.Find(ddlClusterName);
                    if (includeNew) {
                        var opt = document.createElement("option");
                        opt.value = "";
                        $$.Set(opt, texts["nodes.node.clusterName.new"]); 
                        $$.Set(ddl, opt);
                    }
                    
                    if (clusters == null) {
                        clusters = [{ "value" : texts["application.default.cluster"] }];
                    }
                    
                    $$.Sorting.Fields(clusters, function(o) { return o.value; });
                
                    $$.ForEach(clusters, function(cluster) {
                        var e = document.createElement("option");
                        $$.Set(e, cluster.value);
                        e.setAttribute("value", cluster.value);
                        $$.Set(ddl, e, true);
                    });
                    
                    ddl.selectedIndex = includeNew == true ? 1 : 0;
                }
            }
        );
    });
};

$$.App.SignOut = function() {
    $$.UI.Dirty.Warn(function() {
        $$.REST.Call({
            "method" : "DELETE",
            "url" : "/api/v1/login",
            "success" : function(response, body) {
                $$.URL.Go(body.value);
            }
        });
    });
};

$$.Timers.Session.Refresh = function() {
    $$.L10N.Get([
            "global.refresh.warning.one",
            "global.refresh.warning.refresh",
            "global.refresh.warning.two",
            "global.refresh.warning.three"
        ], function(texts){
        var refresh = [];
        var span = document.createElement("span");
        $$.Set(span, texts["global.refresh.warning.one"] + " ");
        refresh.push(span);
        
        var countSpan = document.createElement("span");
        countSpan.count = 30;
        countSpan.decrement = function() {
            var e = this;
            $$.Set(e, e.count);
            e.count--;
            if (e.count > 0) {
                e.update = setTimeout(function() {
                    e.decrement();
                }, 1000);
            }
        };
        countSpan.decrement();
        refresh.push(countSpan);
        
        span = document.createElement("span");
        $$.Set(span, " " + texts["global.refresh.warning.two"] + " ");
        refresh.push(span);
        
        span = document.createElement("span");
        span.classList.add("refresh");
        $$.Events.SafeClick(span, function() {
            var e = this;
            $$.REST.Call({
                "method" : "GET",
                "url" : "/api/v1/refresh",
                "success" : function(response, body) {
                    clearTimeout(countSpan.update);
                    e.parentNode.parentNode.querySelectorAll("span.close")[0].click();
                }
            });
        });
        $$.Set(span, texts["global.refresh.warning.refresh"]);
        refresh.push(span);
        
        span = document.createElement("span");
        $$.Set(span, " " + texts["global.refresh.warning.three"]);
        refresh.push(span);
        
        $$.Banner.ShowMessage("info", refresh);
    });
};

$$.Init.Add($$.App.Menus.User);
$$.Init.Add(function() {
    $$.L10N.Get([
            "global.refresh.warning.one",
            "global.refresh.warning.refresh",
            "global.refresh.warning.two",
            "global.refresh.warning.three"
        ], function(texts){
        /* no-op, just cache text values */
    });
});
