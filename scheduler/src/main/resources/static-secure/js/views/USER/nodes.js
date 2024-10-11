/* nodes.js */
$$.App.Views.nodes = {
    Init : function() {
        $$.Events.SafeClick("btnConfig", function(event) {
            $$.App.Views.nodes.NewConfig();
        });
        $$.Events.SafeClick("btnSave", function(event) {
            $$.App.Views.nodes.SaveNode();
        });
        $$.Events.SafeClick("btnCancel", function(event) {
            $$.UI.Dirty.Warn(function() {
                $$.App.Views.nodes.LoadNode($$.Find("txtName").nodeId);
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
                $$.App.Views.nodes.Search();
            }, 250);
        });
        
        $$.Events.Add("ddlClusterName", "change", function(event) {
            var txtClusterName = $$.Find("txtClusterName");
            if ($$.Text.IsBlank(this.value)) {
                txtClusterName.value = "";
            } else {
                txtClusterName.value = this.options[this.selectedIndex].text;
            }
        });
        
        $$.App.Views.nodes.LoadClusters();
        $$.App.Views.nodes.Search(0, true);
        $$.App.Views.nodes.LoadNode();
        
        $$.Find("btnNewNode").LoadNode = $$.App.Views.nodes.LoadNode;
        $$.Find("btnRefreshNodes").Search = $$.App.Views.nodes.Search;
        $$.Find("btnInformNodes").Search = $$.App.Views.nodes.Search;
        
        $$.UI.Dirty.Ignored["txtSearchText"] = true;
        $$.UI.Dirty.Regsiter();
    },
    DeleteNode  : function(node) {
        $$.L10N.Get([
            "global.buttons.cancel",
            "global.buttons.delete",
            "nodes.node.delete.title",
            "nodes.node.delete.message"
        ], function(texts){
            var body = document.createElement("span");
            $$.Set(body, $$.L10N.Replace(texts["nodes.node.delete.message"], { "Name" : node.name }));
            
            var footer = $$.UI.Modal.Footers.Confirm({
                "ok" : function(event) {
                    $$.REST.Call({
                        "method" : "DELETE",
                        "url" : "/api/v1/nodes/" + node.id,
                        "success" : function(response, body) {
                            $$.Banner.ShowMessages({ 
                                "category" : "Application",
                                "severity" : "Success",
                                "number" : 1
                            });
                            $$.App.Views.nodes.Search(0, true);
                            $$.App.Views.nodes.LoadNode();
                        }
                    });
                 },
                 "okText" : texts["global.buttons.delete"]
            });
            
            $$.UI.Modal.Show({
                "header" : texts["nodes.node.delete.title"],
                "body" : body,
                "footer" : footer
            }); 
        });
    },
    DisplayConfig : function(config) {
        $$.L10N.Get([
            "global.buttons.close",
            "nodes.node.config.title"
        ], function(texts){
            var body = document.createElement("div");
            body.classList.add("newConfig");
    
            var txtConfig = $$.UI.Input({
                "type" : "textarea",
                "id" : "txtConfig",
                "value" : config,
                "customize" : function(input, divInput) {
                    input.setAttribute("readonly", "");
                }    
            });
            $$.Set(body, txtConfig, true);
            
            var footer = $$.UI.Modal.Footers.Confirm({
                 "singleButton" : true,
                 "cancelText" : texts["global.buttons.close"]
            });
            
            $$.UI.Modal.Show({
                "header" : texts["nodes.node.config.title"],
                "body" : body,
                "footer" : footer
            });
            
            $$.Find("txtConfig").select();
        });
    },
    LoadClusters : function() {
        $$.App.Functions.LoadClusters("ddlClusterName", true);
    },
    LoadClusterName : function(name) {
        var ddlClusterName = $$.Find("ddlClusterName");
        if (ddlClusterName.options.length < 1) {
            setTimeout(function(){
                $$.App.Views.nodes.LoadClusterName(name);
            }, 50);
            return;
        }
        
        if ($$.Text.IsBlank(name)) {
            ddlClusterName.selectedIndex = 1;
        } else {
            ddlClusterName.value = name;
        }
        $$.Find("txtClusterName").value = ddlClusterName.value;
    },
    LoadNode : function(id) {
        var _loadNode = function(node) {
            var txtName = $$.Find("txtName");
            txtName.nodeId = node.id;
            txtName.nodeModified = node.modified;
            txtName.value = $$.Text.Trim(node.name);

            $$.App.Views.nodes.LoadClusterName(node.clusterName);

            var txtUrl = $$.Find("txtUrl");
            var url = $$.Text.Trim(node.url, true);
            if (url == null) {
                txtUrl.value = $$.Text.Trim(txtUrl.getAttribute("urlPrefix"));
            } else {
                txtUrl.value = $$.Text.Trim(url);
            }
            
            $$.Find("chkAudio").checked = node.allowAudio === true;
            $$.Find("chkVideo").checked = node.allowVideo === true;
            $$.Find("chkScan").checked = node.allowScan === true;
            $$.Find("chkMux").checked = node.allowMux === true;
            $$.Find("chkMerge").checked = node.allowMerge === true;
            
            $$.UI.Dirty.Set(false);
            
            if (node.id == null) {
                $$.Find("btnConfig").classList.add("hidden");
            } else {
                $$.Find("btnConfig").classList.remove("hidden");
            }
        };
        
        if (id == null) {
            _loadNode({
                "allowAudio" : true,
                "allowVideo" : true,
                "allowScan" : true,
                "allowMux" : true,
                "allowMerge" : true
            });
        } else {
            $$.REST.Call({
                "method" : "GET",
                "url" : "/api/v1/nodes/" + id,
                "success" : function(response, node) {
                    _loadNode(node);
                }
            });
        }
    },
    NewConfig : function() {
        $$.L10N.Get([
            "global.buttons.yes",
            "global.buttons.delete",
            "nodes.node.newConfig.title",
            "nodes.node.newConfig.message"
        ], function(texts){
            var nodeName = $$.Find("txtName").value;
            var body = document.createElement("span");
            $$.Set(body, $$.L10N.Replace(texts["nodes.node.newConfig.message"], { "Name" : nodeName }));
            
            var footer = $$.UI.Modal.Footers.Confirm({
                "ok" : function(event) {
                    var nodeId = $$.Find("txtName").nodeId;
                    $$.REST.Call({
                        "method" : "POST",
                        "url" : "/api/v1/nodes/" + nodeId  + "/config",
                        "success" : function(response, data) {
                            $$.App.Views.nodes.DisplayConfig(data.config);
                        }
                    });
                 },
                 "okText" : texts["global.buttons.yes"]
            });
            
            $$.UI.Modal.Show({
                "header" : texts["nodes.node.newConfig.title"],
                "body" : body,
                "footer" : footer
            });
        });
    },
    SaveNode : function() {
        var txtName = $$.Find("txtName");
        var data = {
            "name" : $$.Text.Trim(txtName.value),
            "clusterName" : $$.Text.Trim($$.Find("txtClusterName").value),
            "url" : $$.Text.Trim($$.Find("txtUrl").value),
            "allowAudio" : $$.Find("chkAudio").checked === true,
            "allowVideo" : $$.Find("chkVideo").checked === true,
            "allowScan" : $$.Find("chkScan").checked === true,
            "allowMux" : $$.Find("chkMux").checked === true,
            "allowMerge" : $$.Find("chkMerge").checked === true
        };
        
        if (txtName.nodeModified != null) {
            data.modified = txtName.nodeModified;
        }
        
        var method = "POST";
        var url = "/api/v1/nodes";
        if (txtName.nodeId != null) {
            method = "PUT";
            url += "/" + txtName.nodeId;
        }
        
        $$.Banner.Clear();
        $$.REST.Call({
            "method" : method,
            "url" : url,
            "body" : data,
            "success" : function(response, body) {
                $$.Banner.ShowMessages({ 
                    "category" : "Application",
                    "severity" : "Success",
                    "number" : 1
                });
                txtName.nodeId = body.id;
                txtName.nodeModified = body.modified;
                $$.UI.Dirty.Set(false);
                
                if (body.config != null) {
                    $$.App.Views.nodes.DisplayConfig(body.config);
                }
                
                $$.Find("btnConfig").classList.remove("hidden");
                
                $$.App.Views.nodes.LoadClusters();
                $$.App.Views.nodes.Search($$.UI.Tables.GetCurrentPage("tNodes"));
            }
        });
    },
    Search : function(page, firstLoad) {
        $$._inProgress.Add();
        var _tableId = "tNodes";
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
                "name" : "asc"
            };
        }
        
        $$.ForEach(sorts, function(direction, field) {
            queryString = $$.URL.ToParam(field + "Sort", direction, queryString);
        });
        
        $$.L10N.Get([
            "nodes.search.columns.clusterName",
            "nodes.search.columns.name",
            "nodes.search.columns.status",
            "nodes.search.columns.url",
            "nodes.search.columns.name.tooltip",
            "nodes.search.columns.url.tooltip",
            "nodes.search.columns.delete.tooltip"
        ], function(texts) {
            $$.REST.Call({
                "method" : "GET",
                "url" : "/api/v1/nodes" + queryString,
                "success" : function(response, body) {
                    var searchResults = $$.Find("searchResults");
                    var table = $$.UI.Tables.Table(body.data, [
                        function(rowData) {
                            return $$.UI.Tables.Cells.LinkButton({
                                "text" : rowData.name,
                                "click" : function(event) { $$.App.Views.nodes.LoadNode(rowData.id); },
                                "tooltip" : texts["nodes.search.columns.name.tooltip"]
                            });
                        },
                        function(rowData) {
                            return $$.UI.Tables.Cells.Text({
                                "text" : rowData.clusterName,
                            });
                        },
                        function(rowData) {
                            return $$.UI.Tables.Cells.Text({
                                "text" : rowData.status,
                            });
                        },
                        function(rowData) {
                            return $$.UI.Tables.Cells.Link({
                                "text" : rowData.url,
                                "url" : rowData.url,
                                "tooltip" : texts["nodes.search.columns.url.tooltip"]
                            });
                        },
                        function(rowData) {
                            return $$.UI.Tables.Cells.Icon({
                                "icon" : $$.App.Icons.Delete,
                                "click" :  function(event) { $$.App.Views.nodes.DeleteNode(rowData); },
                                "tooltip" : texts["nodes.search.columns.delete.tooltip"]
                            });
                        }
                    ], [{
                        "field" : "name",
                        "text" : texts["nodes.search.columns.name"],
                        "defaultDirection" : sorts["name"],
                        "sort" : function(direction) {
                            $$.App.Views.nodes.Search(page);
                        }
                    }, {
                        "field" : "clusterName",
                        "text" : texts["nodes.search.columns.clusterName"],
                        "defaultDirection" : sorts["clusterName"],
                        "sort" : function(direction) {
                            $$.App.Views.nodes.Search(page);
                        }
                    }, {
                        "field" : "status",
                        "text" : texts["nodes.search.columns.status"],
                        "defaultDirection" : sorts["status"],
                        "sort" : function(direction) {
                            $$.App.Views.nodes.Search(page);
                        }
                    }, {
                        "field" : "url",
                        "text" : texts["nodes.search.columns.url"],
                        "defaultDirection" : sorts["url"], 
                        "sort" : function(direction) {
                            $$.App.Views.nodes.Search(page);
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
                            $$.App.Views.nodes.Search(newPage);
                        }
                    });
                    $$.Set(searchResults, pages, true);
                    $$._inProgress.Complete();
                }
            });
        });
    }
};
