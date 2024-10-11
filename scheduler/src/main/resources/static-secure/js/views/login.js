/* login.js */
$$.App.Views.login = {
    Init : function() {
        $$.Find("txtUsername").focus();
        $$.Events.SafeClick("btnLogin", $$.App.Views.login.SignIn);
        $$.Events.Add("txtUsername", "keyup", $$.App.Views.login.onKeyUp);
        $$.Events.Add("txtPassword", "keyup", $$.App.Views.login.onKeyUp);
    },
    onKeyUp : function(event) {
        switch(event.keyCode) {
            case $$.Events.KeyCodes.Enter:
                if (!$$.App.FocusIfBlank("txtUsername") && !$$.App.FocusIfBlank("txtPassword")) {
                    $$.Find("btnLogin").click();
                }
                break;
        }
    },
    SignIn : function() {
        $$.Banner.Clear();
        var authUrl = "#login.auth.url#";
        if ($$.Text.IsBlank(authUrl)) {
            $$.REST.Call({
                "method" : "POST",
                "url" : "/api/v1/login",
                "spinnerDelay" : 100,
                "success" : function(response, body) {
                    $$.URL.Go(body.value);
                },
                "error" : function(response) {
                    $$.Banner.Clear("info");
                },
                "body" : {
                  "username" : $$.Find("txtUsername").value,
                  "password" : $$.Find("txtPassword").value
                }
            });
        } else {
            $$.REST.Call({
                "method" : "POST",
                "url" : authUrl + "/svc/v1/jwt",
                "spinnerDelay" : 500,
                "success" : function(idResponse, idBody) {
                    $$.REST.Call({
                        "method" : "POST",
                        "url" : "/api/v1/login",
                        "spinnerDelay" : 100,
                        "headers" : {
                          "Content-Type" : "text/plain"
                        },
                        "success" : function(response, body) {
                            $$.URL.Go(body.value);
                        },
                        "body" : idBody.token
                    });
                },
                "error" : function(response) {
                    $$.Banner.Clear("info");
                },
                "body" : {
                  "username" : $$.Find("txtUsername").value,
                  "password" : $$.Find("txtPassword").value,
                  "applicationId" : "#login.auth.appId#"
                }
            });
        }
    }
};
