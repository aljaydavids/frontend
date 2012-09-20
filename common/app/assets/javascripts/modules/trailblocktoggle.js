/* global guardian: true */
define(['common', 'bonzo', 'bean'], function(common, bonzo, bean) {

    var TrailblockToggle = function () {

        var options = {
            'toggleSelectorClass': '.js-toggle-trailblock',
            'edition' : '',
            'prefName': 'front-trailblocks-'
        };

        var view = {

            showToggleLinks: function () {

                var toggles = common.$g(options.toggleSelectorClass).each(function (toggle) {
                    
                    bonzo(toggle).removeClass('initially-off'); // show the nav links

                    bean.add(toggle, 'click', function (e) {
                        common.mediator.emit('modules:trailblockToggle:toggle', this);
                    });
                });
            },

            toggleTrailblock: function (trigger, manualTrigger) {
                
                var idPrefix = "front-trailblock-";
                var classesToToggle = 'rolled-out rolled-up';

                if (manualTrigger) {
                    trigger = document.getElementById('js-trigger-' + manualTrigger);
                    //classesToToggle = 'initially-off';
                }

                var trailblockId = trigger.getAttribute('data-block-name'),
                    trailblock;
                trailblock = idPrefix;

                if (trailblockId !== "top-stories") {
                    trailblock += trailblockId;
                }

                if (!manualTrigger) {
                    //bonzo.trailblock.removeClass('initially-off');
                }

                trailblock = document.getElementById(trailblock);
                bonzo(trailblock).toggleClass(classesToToggle);

                var trigText = trigger.innerText;
                var hideTrailblock = (trigText === "Hide") ? true : false;
                trigger.innerText = (hideTrailblock) ? "Show" : "Hide";
                
                if (!manualTrigger) { // don't add it to prefs since we're reading from them
                    model.logPreference(hideTrailblock, trailblockId);
                }
            },

            renderUserPreference: function () {
                // bit of duplication here from function below
                if (window.localStorage) {
                    var existingPrefs = guardian.userPrefs.get(options.prefName);

                    if (existingPrefs) {
                        var sectionArray = existingPrefs.split(',');
                        for (var i in sectionArray) {
                            var item = sectionArray[i];
                            common.mediator.emit('modules:trailblockToggle:toggle', null, item);
                        }
                    }
                }
            }

        };

        var model = {

            logPreference: function (shouldHideSection, section) {
                
                if (window.localStorage) {
                    var existingPrefs = guardian.userPrefs.get(options.prefName);
                    
                    if (existingPrefs) {

                        // see if it already exists
                        var sectionArray = existingPrefs.split(',');
                        for (var i in sectionArray) {
                            var item = sectionArray[i];
                            if (item === section) {
                                if (!shouldHideSection) {
                                    sectionArray.splice(i, 1); // remove it from list
                                }
                            }
                        }

                        if (shouldHideSection) {
                            sectionArray.push(section);
                        }

                        var newPrefs = sectionArray.join(',');
                        guardian.userPrefs.set(options.prefName, newPrefs);
                    
                    // need to create it instead
                    } else {
                        guardian.userPrefs.set(options.prefName, section);
                    }

                }
                
            }

        };

        this.go = function (edition) {
            options.edition = edition;
            options.prefName = options.prefName + options.edition;
            view.showToggleLinks();
            view.renderUserPreference();
        };

        //View Listeners
        common.mediator.on('modules:trailblockToggle:toggle', view.toggleTrailblock);

    };

    return TrailblockToggle;

});