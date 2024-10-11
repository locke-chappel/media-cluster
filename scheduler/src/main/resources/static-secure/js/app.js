/* app.js */
$$.App = {
    Defaults : {
        MaxQueueSize : 1024,
        SliceLength : 1000
    },
    Functions : {
    },
    Icons : {
        Delete : "\uf2ed",
        Refresh : "\uf2f1"
    },
    Views : {
    },
    Menus : {
    },
    Templates : {
    },
    Patterns : {
    },
    FocusIfBlank : function(input) {
        var field = $$.Find(input);
        if ($$.Text.IsBlank(field.value)) {
            field.focus();
            return true;
        }
        return false;
    }
};

$$.Init.Add(function() { 
    $$.UI.Menu.Configure("btnMenu", "menu");
    $$.L10N.Get([
        "messages.Application.Error.1"
    ]);
});
